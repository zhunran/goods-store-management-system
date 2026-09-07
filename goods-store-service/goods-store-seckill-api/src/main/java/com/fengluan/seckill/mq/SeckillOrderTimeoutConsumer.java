package com.fengluan.seckill.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fengluan.common.mq.SeckillOrderMessage;
import com.fengluan.seckill.config.SeckillMqConfig;
import com.fengluan.seckill.entity.SeckillOrderEntity;
import com.fengluan.seckill.repository.SeckillOrderMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 秒杀订单超时关单消费者（消费延迟队列死信，消息体为 SeckillOrderMessage，携带回补所需的 memberId/seckillGoodId）：
 * 仅取消仍为 PENDING("10") 的订单，并恢复 Redis 库存 + 清防重键。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SeckillOrderTimeoutConsumer {

    private static final String PENDING = "10";
    private static final String CANCELLED = "50";

    private final SeckillOrderMapper orderMapper;
    private final SeckillRedisCompensator compensator;

    @RabbitListener(queues = SeckillMqConfig.SECKILL_ORDER_CANCEL_QUEUE)
    public void handle(SeckillOrderMessage msg, Channel channel,
                       @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {
            SeckillOrderEntity order = orderMapper.selectOne(new LambdaQueryWrapper<SeckillOrderEntity>()
                    .eq(SeckillOrderEntity::getOrderNo, msg.getOrderNo()));
            if (order == null || !PENDING.equals(order.getStatus())) {
                log.info("秒杀订单 {} 无需取消（不存在或非待付款）", msg.getOrderNo());
                channel.basicAck(tag, false);
                return;
            }
            order.setStatus(CANCELLED);
            order.setUpdatedTime(LocalDateTime.now());
            orderMapper.updateById(order);
            // 秒杀不扣 DB 库存，Redis 是唯一扣减点：回补只操作 Redis
            compensator.restore(msg.getSeckillGoodId(), msg.getMemberId());
            channel.basicAck(tag, false);
            log.info("秒杀订单超时自动取消：orderNo={}", msg.getOrderNo());
        } catch (Exception e) {
            log.error("秒杀订单超时关单异常：orderNo={}", msg.getOrderNo(), e);
            channel.basicNack(tag, false, true);
        }
    }
}