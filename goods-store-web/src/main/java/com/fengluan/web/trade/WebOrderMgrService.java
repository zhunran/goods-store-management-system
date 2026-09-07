package com.fengluan.web.trade;

import com.fengluan.spi.trade.dto.OrderPayRequest;
import com.fengluan.spi.trade.dto.OrderQueryRequest;
import com.fengluan.spi.trade.dto.PageVO;
import com.fengluan.spi.trade.vo.OrderDetailVO;
import com.fengluan.spi.trade.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 我的订单聚合服务（BFF）：磨平分页/状态
 */
@Service
@RequiredArgsConstructor
public class WebOrderMgrService {

    private final TradeOrderFeignClient tradeOrderFeignClient;

    public PageVO<OrderVO> page(Long memberId, Long pageNum, Long pageSize, String status) {
        return tradeOrderFeignClient.page(pageNum, pageSize, status);
    }

    public OrderDetailVO detail(Long memberId, Long id) {
        return tradeOrderFeignClient.detail(id);
    }

    public void cancel(Long memberId, Long id) {
        tradeOrderFeignClient.cancel(id);
    }

    public void confirm(Long memberId, Long id) {
        tradeOrderFeignClient.confirm(id);
    }

    public void pay(Long memberId, Long id, OrderPayRequest request) {
        tradeOrderFeignClient.pay(id, request);
    }

    public void refund(Long memberId, Long id) {
        tradeOrderFeignClient.refund(id);
    }

    // —— 管理端 ——
    public PageVO<OrderVO> adminPage(OrderQueryRequest query) {
        return tradeOrderFeignClient.adminPage(query);
    }

    public OrderDetailVO adminDetail(Long id) {
        return tradeOrderFeignClient.adminDetail(id);
    }

    public void ship(Long id) {
        tradeOrderFeignClient.ship(id);
    }

    public void adminRefund(Long id) {
        tradeOrderFeignClient.adminRefund(id);
    }
}