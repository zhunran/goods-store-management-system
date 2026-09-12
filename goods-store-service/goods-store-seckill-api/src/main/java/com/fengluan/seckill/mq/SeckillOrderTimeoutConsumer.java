package com.fengluan.seckill.mq;

import com.fengluan.common.mq.SeckillOrderMessage;
import com.fengluan.seckill.config.SeckillMqConfig;
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
 * 秒杀订单超时关单（延迟队列死信）：
 * CAS 关单（仅 PENDING 可关，已支付让步）→ DB+Redis 双回补（补偿器内部幂等）。
 * 异常 nack 重试：重试时 CAS=0 或补偿标记已存在，均幂等跳过，不会多补。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SeckillOrderTimeoutConsumer {

    private final SeckillOrderMapper orderMapper;
    private final SeckillCompensator compensator;

    @RabbitListener(queues = SeckillMqConfig.SECKILL_ORDER_CANCEL_QUEUE)
    public void handle(SeckillOrderMessage msg, Channel channel,
                       @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {
            // CAS 关单：已支付（status=20）时影响 0 行 → 由补偿器按订单实际状态让步
            orderMapper.cancelPending(msg.getOrderNo(), LocalDateTime.now());
            // 回补：内部校验订单确为 CANCELLED + orderNo 幂等标记，双条件防漏补/多补
            compensator.compensateForTimeout(msg);
            channel.basicAck(tag, false);
            log.info("秒杀订单超时处理完成：orderNo={}", msg.getOrderNo());
        } catch (Exception e) {
            log.error("秒杀订单超时关单异常：orderNo={}", msg.getOrderNo(), e);
            channel.basicNack(tag, false, true);
        }
    }
}
