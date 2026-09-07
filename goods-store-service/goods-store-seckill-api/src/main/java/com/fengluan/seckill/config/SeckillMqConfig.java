package com.fengluan.seckill.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 秒杀消息配置：声明秒杀订单队列与绑定，共用餐exchange goods.order.exchange（与 trade RabbitMqConfig 同名幂等安全）。
 */
@Configuration
public class SeckillMqConfig {

    public static final String ORDER_EXCHANGE = "goods.order.exchange";
    public static final String SECKILL_ORDER_QUEUE = "goods.seckill.order.queue";
    public static final String SECKILL_ORDER_KEY = "seckill.order.create";

    // 秒杀订单超时队列（延迟队列，30 分钟未支付进入死信）
    public static final String SECKILL_ORDER_TIMEOUT_QUEUE = "goods.seckill.order.timeout.queue";
    public static final String SECKILL_ORDER_TIMEOUT_KEY = "seckill.order.timeout";

    // 秒杀订单取消队列（死信队列）
    public static final String SECKILL_ORDER_CANCEL_QUEUE = "goods.seckill.order.cancel.queue";
    public static final String SECKILL_ORDER_CANCEL_KEY = "seckill.order.cancel";

    @Bean
    public TopicExchange seckillOrderExchange() {
        return new TopicExchange(ORDER_EXCHANGE, true, false);
    }

    @Bean
    public Queue seckillOrderQueue() {
        return QueueBuilder.durable(SECKILL_ORDER_QUEUE).build();
    }

    @Bean
    public Queue seckillOrderTimeoutQueue() {
        return QueueBuilder.durable(SECKILL_ORDER_TIMEOUT_QUEUE)
                .ttl(30 * 60 * 1000)  // 30分钟
                .deadLetterExchange(ORDER_EXCHANGE)
                .deadLetterRoutingKey(SECKILL_ORDER_CANCEL_KEY)
                .build();
    }

    @Bean
    public Queue seckillOrderCancelQueue() {
        return QueueBuilder.durable(SECKILL_ORDER_CANCEL_QUEUE).build();
    }

    @Bean
    public Binding seckillOrderBinding() {
        return BindingBuilder.bind(seckillOrderQueue()).to(seckillOrderExchange()).with(SECKILL_ORDER_KEY);
    }

    @Bean
    public Binding seckillOrderTimeoutBinding() {
        return BindingBuilder.bind(seckillOrderTimeoutQueue()).to(seckillOrderExchange()).with(SECKILL_ORDER_TIMEOUT_KEY);
    }

    @Bean
    public Binding seckillOrderCancelBinding() {
        return BindingBuilder.bind(seckillOrderCancelQueue()).to(seckillOrderExchange()).with(SECKILL_ORDER_CANCEL_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter seckillMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}