package com.fengluan.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.fengluan.common.exception.BusinessException;
import com.fengluan.common.exception.ErrorCode;
import com.fengluan.common.util.SnowflakeUtil;
import com.fengluan.spi.member.vo.MemberAddressVO;
import com.fengluan.spi.product.vo.GoodVO;
import com.fengluan.spi.trade.dto.OrderCreateRequest;
import com.fengluan.spi.trade.dto.OrderQueryRequest;
import com.fengluan.spi.trade.dto.PageVO;
import com.fengluan.spi.trade.vo.OrderCreateResponse;
import com.fengluan.spi.trade.vo.OrderDetailVO;
import com.fengluan.spi.trade.vo.OrderItemVO;
import com.fengluan.spi.trade.vo.OrderVO;
import com.fengluan.trade.entity.CartEntity;
import com.fengluan.trade.entity.OrderEntity;
import com.fengluan.trade.entity.OrderItemEntity;
import com.fengluan.trade.enums.OrderStatus;
import com.fengluan.trade.mq.OrderMessageProducer;
import com.fengluan.trade.remote.TradeMemberClient;
import com.fengluan.trade.remote.TradeProductClient;
import com.fengluan.trade.repository.CartMapper;
import com.fengluan.trade.repository.OrderItemMapper;
import com.fengluan.trade.repository.OrderMapper;
import com.fengluan.trade.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, OrderEntity> implements OrderService {

    private final CartMapper cartMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final TradeProductClient productClient;
    private final TradeMemberClient memberClient;
    private final RedissonClient redissonClient;
    private final OrderMessageProducer orderMessageProducer;
    private final SnowflakeUtil snowflakeUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderCreateResponse createOrder(Long memberId, OrderCreateRequest request) {
        List<CartEntity> carts = cartMapper.selectList(new LambdaQueryWrapper<CartEntity>()
                .eq(CartEntity::getMemberId, memberId.intValue()));
        if (carts.isEmpty()) {
            throw new BusinessException(ErrorCode.CART_EMPTY);
        }

        String orderNo = snowflakeUtil.nectIdStr();
        RLock lock = redissonClient.getLock("lock:order:" + memberId);
        try {
            if (!lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                throw new BusinessException("系统繁忙，请稍后重试", ErrorCode.INTERNAL_ERROR.getCode());
            }

            // 1. 校验库存/下架 + 汇总明细
            BigDecimal totalPay = BigDecimal.ZERO;
            List<OrderItemEntity> items = new ArrayList<>();
            for (CartEntity c : carts) {
                GoodVO g = productClient.getById(c.getGoodId().longValue());
                if (g == null || Boolean.TRUE.equals(g.getIsDel())) {
                    throw new BusinessException(ErrorCode.GOOD_NOT_FOUND);
                }
                if (g.getQty() == null || g.getQty() < c.getQty()) {
                    throw new BusinessException(ErrorCode.GOOD_STOCK_INSUFFICIENT);
                }
                totalPay = totalPay.add(g.getPrice().multiply(BigDecimal.valueOf(c.getQty())));
                OrderItemEntity oi = new OrderItemEntity();
                oi.setGoodId(c.getGoodId());
                oi.setCount(c.getQty());
                oi.setDealPrice(g.getPrice());
                oi.setGoodName(g.getName());
                oi.setGoodPic(g.getPic());
                oi.setGoodDesc(g.getSummary());
                items.add(oi);
            }

            // 2. 收货地址（简化：一律取默认地址）
            MemberAddressVO addr = memberClient.getDefaultAddress(memberId);
            if (addr == null) {
                throw new BusinessException("请先设置收货地址", ErrorCode.BAD_REQUEST.getCode());
            }

            // 3. 会员账号（order.member_account）
            String account = memberClient.getProfile(memberId).getAccount();

            // 4. 事务内写订单 + 明细
            OrderEntity order = buildOrder(orderNo, account, totalPay, addr, request.getComment());
            orderMapper.insert(order);
            items.forEach(oi -> {
                oi.setOrderId(order.getId().intValue());
                orderItemMapper.insert(oi);
            });

            // 事务提交后再发 MQ（订单创建用于异步扣库存 + 超时取消）
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    orderMessageProducer.sendOrderCreate(order.getOrderNo(), memberId, carts);
                    orderMessageProducer.sendOrderTimeout(order.getOrderNo());
                }
            });

            // 5. 清空已下单的购物车
            cartMapper.deleteByMemberId(memberId.intValue());

            OrderCreateResponse resp = new OrderCreateResponse();
            resp.setOrderNo(orderNo);
            resp.setTotalPay(totalPay);
            resp.setStatus(order.getStatus());
            return resp;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public PageVO<OrderVO> page(Long memberId, Long pageNum, Long pageSize, String status) {
        String account = memberClient.getProfile(memberId).getAccount();
        long num = pageNum == null ? 1 : pageNum;
        long size = pageSize == null ? 10 : pageSize;
        Page<OrderEntity> p = new Page<>(num, size);
        Page<OrderEntity> result = orderMapper.selectPage(p, new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getMemberAccount, account)
                .eq(status != null && !status.isBlank(), OrderEntity::getStatus, status)
                .orderByDesc(OrderEntity::getId));
        PageVO<OrderVO> vo = new PageVO<>();
        vo.setTotal(result.getTotal());
        vo.setPageNum(num);
        vo.setPageSize(size);
        vo.setRecords(result.getRecords().stream().map(this::toOrderVO).toList());
        return vo;
    }

    @Override
    public OrderDetailVO detail(Long memberId, Long id) {
        String account = memberClient.getProfile(memberId).getAccount();
        OrderEntity order = requireOrder(id, account);
        return toDetailVO(order);
    }

    private OrderDetailVO toDetailVO(OrderEntity order) {
        OrderDetailVO d = new OrderDetailVO();
        d.setId(order.getId());
        d.setOrderNo(order.getOrderNo());
        d.setMemberAccount(order.getMemberAccount());
        d.setTotalPay(order.getTotalPay());
        d.setPayType(order.getPayType());
        d.setStatus(order.getStatus());
        d.setCheckoutTime(order.getCheckoutTime());
        d.setPayTime(order.getPayTime());
        d.setShipTime(order.getShipTime());
        d.setAcceptTime(order.getAcceptTime());
        d.setReceiverName(order.getReceiverName());
        d.setReceiverPhone(order.getReceiverPhone());
        d.setReceiverAddrDetail(order.getReceiverAddrDetail());
        d.setOrderComment(order.getOrderComment());
        List<OrderItemEntity> items = orderItemMapper.selectByOrderId(order.getId().intValue());
        d.setItems(items == null ? new ArrayList<>() : items.stream().map(it -> {
            OrderItemVO iv = new OrderItemVO();
            iv.setGoodId(it.getGoodId().longValue());
            iv.setGoodName(it.getGoodName());
            iv.setGoodPic(it.getGoodPic());
            iv.setDealPrice(it.getDealPrice());
            iv.setCount(it.getCount());
            return iv;
        }).toList());
        return d;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long memberId, Long id) {
        String account = memberClient.getProfile(memberId).getAccount();
        OrderEntity order = requireOrder(id, account);
        if (!OrderStatus.PENDING.getCode().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }
        order.setStatus(OrderStatus.CANCELLED.getCode());
        orderMapper.updateById(order);
        // 用户主动取消同步恢复库存
        List<OrderItemEntity> items = orderItemMapper.selectByOrderId(order.getId().intValue());
        if (items != null) {
            for (OrderItemEntity it : items) {
                productClient.restoreStock(it.getGoodId().longValue(), it.getCount());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long memberId, Long id) {
        String account = memberClient.getProfile(memberId).getAccount();
        OrderEntity order = requireOrder(id, account);
        if (!OrderStatus.SHIPPED.getCode().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }
        order.setStatus(OrderStatus.COMPLETED.getCode());
        order.setAcceptTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Override
    public PageVO<OrderVO> adminPage(OrderQueryRequest query) {
        long num = query.getPageNum() == null ? 1 : query.getPageNum();
        long size = query.getPageSize() == null ? 10 : query.getPageSize();
        Page<OrderEntity> p = new Page<>(num, size);
        Page<OrderEntity> result = orderMapper.selectPage(p, new LambdaQueryWrapper<OrderEntity>()
                .eq(query.getOrderNo() != null && !query.getOrderNo().isBlank(),
                        OrderEntity::getOrderNo, query.getOrderNo())
                .like(query.getMemberAccount() != null && !query.getMemberAccount().isBlank(),
                        OrderEntity::getMemberAccount, query.getMemberAccount())
                .eq(query.getStatus() != null && !query.getStatus().isBlank(),
                        OrderEntity::getStatus, query.getStatus())
                .orderByDesc(OrderEntity::getId));
        PageVO<OrderVO> vo = new PageVO<>();
        vo.setTotal(result.getTotal());
        vo.setPageNum(num);
        vo.setPageSize(size);
        vo.setRecords(result.getRecords().stream().map(this::toOrderVO).toList());
        return vo;
    }

    @Override
    public OrderDetailVO adminDetail(Long id) {
        OrderEntity order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        return toDetailVO(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ship(Long id) {
        OrderEntity order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!OrderStatus.PAID.getCode().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }
        order.setStatus(OrderStatus.SHIPPED.getCode());
        order.setShipTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    private OrderEntity requireOrder(Long id, String account) {
        OrderEntity order = orderMapper.selectById(id);
        if (order == null || !order.getMemberAccount().equals(account)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    private OrderVO toOrderVO(OrderEntity o) {
        OrderVO v = new OrderVO();
        v.setId(o.getId());
        v.setOrderNo(o.getOrderNo());
        v.setMemberAccount(o.getMemberAccount());
        v.setTotalPay(o.getTotalPay());
        v.setPayType(o.getPayType());
        v.setStatus(o.getStatus());
        v.setCheckoutTime(o.getCheckoutTime());
        v.setPayTime(o.getPayTime());
        v.setShipTime(o.getShipTime());
        v.setCreatedTime(o.getCreatedTime());
        v.setUpdatedTime(o.getUpdatedTime());
        return v;
    }

    private OrderEntity buildOrder(String orderNo, String account, BigDecimal totalPay,
                                   MemberAddressVO addr, String comment) {
        LocalDateTime now = LocalDateTime.now();
        OrderEntity order = new OrderEntity();
        order.setOrderNo(orderNo);
        order.setMemberAccount(account);
        order.setTotalPay(totalPay);
        order.setReceiverAddrId(addr.getId().intValue());
        order.setReceiverName(addr.getReceiver());
        order.setReceiverPhone(addr.getPhone());
        order.setReceiverAddrDetail(addr.getAddrDetail());
        order.setOrderComment(comment);
        order.setStatus(OrderStatus.PENDING.getCode()); // 待付款
        order.setCheckoutTime(now);
        order.setIsDel(false);
        order.setCreatedTime(now);
        order.setUpdatedTime(now);
        return order;
    }
}