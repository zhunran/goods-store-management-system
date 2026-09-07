package com.fengluan.seckill.mq;

import com.fengluan.common.mq.SeckillOrderMessage;
import com.fengluan.seckill.config.SeckillMqConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/** 秒杀订单消息生产者 */
@Component
@RequiredArgsConstructor
public class SeckillMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendSeckillOrder(SeckillOrderMessage message) {
        rabbitTemplate.convertAndSend(SeckillMqConfig.ORDER_EXCHANGE,
                SeckillMqConfig.SECKILL_ORDER_KEY, message);
    }

    /** 秒杀订单超时延迟消息（30 分钟未支付自动取消） */
    public void sendSeckillOrderTimeout(SeckillOrderMessage message) {
        rabbitTemplate.convertAndSend(SeckillMqConfig.ORDER_EXCHANGE,
                SeckillMqConfig.SECKILL_ORDER_TIMEOUT_KEY, message);
    }
}