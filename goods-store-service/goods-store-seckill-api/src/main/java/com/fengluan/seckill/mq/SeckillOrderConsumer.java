package com.fengluan.seckill.mq;

import com.fengluan.common.mq.SeckillOrderMessage;
import com.fengluan.seckill.config.SeckillMqConfig;
import com.fengluan.seckill.entity.SeckillOrderEntity;
import com.fengluan.seckill.entity.SeckillOrderItemEntity;
import com.fengluan.seckill.remote.SeckillMemberClient;
import com.fengluan.seckill.remote.SeckillProductClient;
import com.fengluan.seckill.repository.SeckillOrderItemMapper;
import com.fengluan.seckill.repository.SeckillOrderMapper;
import com.fengluan.spi.product.vo.GoodVO;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 秒杀订单消费：幂等建单（order + order_item），失败补偿 Redis（恢复库存 + 清防重）。
 * 手动 ack；补偿后 ack，避免失败消息无限重试。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SeckillOrderConsumer {

    private final SeckillOrderMapper orderMapper;
    private final SeckillOrderItemMapper orderItemMapper;
    private final SeckillProductClient productClient;
    private final SeckillMemberClient memberClient;
    private final SeckillRedisCompensator compensator;
    private final SeckillMessageProducer seckillMessageProducer;
    private final PlatformTransactionManager transactionManager;

    @RabbitListener(queues = SeckillMqConfig.SECKILL_ORDER_QUEUE)
    public void handle(SeckillOrderMessage msg, Channel channel,
                       @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {
            // 幂等：订单已存在（如超时重投）直接 ack
            if (orderMapper.existsByOrderNo(msg.getOrderNo()) > 0) {
                channel.basicAck(tag, false);
                return;
            }
            GoodVO good = productClient.getById(msg.getGoodId());
            // 业务失败：商品不存在或已删除，无单可建，补偿 Redis 并确认
            if (good == null || Boolean.TRUE.equals(good.getIsDel())) {
                compensate(msg);
                channel.basicAck(tag, false);
                return;
            }
            // 内部接口 getAccount 无越权校验（MQ 线程无 HTTP 上下文，getProfile 的 assertOwned 必抛 401）
            String account = memberClient.getAccount(msg.getMemberId());
            if (account == null || account.isBlank()) {
                compensate(msg);
                channel.basicAck(tag, false);
                return;
            }

            // 主单 + 明细同事务，任一失败整体回滚，避免留下无明细的孤儿主单
            buildOrder(msg, good, account);

            // 建单成功后再补发超时消息，30 分钟未支付自动关单
            seckillMessageProducer.sendSeckillOrderTimeout(msg);

            channel.basicAck(tag, false);
            log.info("秒杀订单创建成功：orderNo={}", msg.getOrderNo());
        } catch (Exception e) {
            // 临时故障（DB/远程）：事务已回滚，不补偿 Redis，重投后重试
            log.error("秒杀建单临时失败，将重试：orderNo={}", msg.getOrderNo(), e);
            channel.basicNack(tag, false, true);
        }
    }

    private void buildOrder(SeckillOrderMessage msg, GoodVO good, String account) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            SeckillOrderEntity order = new SeckillOrderEntity();
            order.setOrderNo(msg.getOrderNo());
            order.setSeckillNo(msg.getOrderNo());
            order.setMemberAccount(account);
            order.setTotalPay(good.getPrice());
            order.setStatus("10"); // PENDING
            order.setCheckoutTime(LocalDateTime.now());
            order.setCreatedTime(LocalDateTime.now());
            order.setUpdatedTime(LocalDateTime.now());
            orderMapper.insert(order);

            SeckillOrderItemEntity item = new SeckillOrderItemEntity();
            item.setOrderId(order.getId().intValue());
            item.setGoodId(msg.getGoodId().intValue());
            item.setDealPrice(good.getPrice());
            item.setCount(1);
            item.setGoodName(good.getName());
            item.setGoodPic(good.getPic());
            orderItemMapper.insert(item);
        });
    }

    private void compensate(SeckillOrderMessage msg) {
        compensator.restore(msg.getSeckillGoodId(), msg.getMemberId());
    }
}