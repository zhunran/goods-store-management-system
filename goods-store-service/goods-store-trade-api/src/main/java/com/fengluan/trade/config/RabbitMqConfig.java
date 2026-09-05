package com.fengluan.trade.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String ORDER_EXCHANGE = "goods.order.exchange";

    // 订单创建队列
    public static final String ORDER_CREATE_QUEUE = "goods.order.create.queue";
    public static final String ORDER_CREATE_KEY = "order.create";

    // 订单超时队列（延迟队列）
    public static final String ORDER_TIMEOUT_QUEUE = "goods.order.timeout.queue";
    public static final String ORDER_TIMEOUT_KEY = "order.timeout";

    // 订单取消队列（死信队列）
    public static final String ORDER_CANCEL_QUEUE = "goods.order.cancel.queue";
    public static final String ORDER_CANCEL_KEY = "order.cancel";

    // 秒杀订单队列
    public static final String SECKILL_ORDER_QUEUE = "goods.seckill.order.queue";
    public static final String SECKILL_ORDER_KEY = "seckill.order.create";

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderCreateQueue() {
        return QueueBuilder.durable(ORDER_CREATE_QUEUE).build();
    }

    @Bean
    public Queue orderTimeoutQueue() {
        return QueueBuilder.durable(ORDER_TIMEOUT_QUEUE)
                .ttl(30 * 60 * 1000)  // 30分钟
                .deadLetterExchange(ORDER_EXCHANGE)
                .deadLetterRoutingKey(ORDER_CANCEL_KEY)
                .build();
    }

    @Bean
    public Queue orderCancelQueue() {
        return QueueBuilder.durable(ORDER_CANCEL_QUEUE).build();
    }

    @Bean
    public Queue seckillOrderQueue() {
        return QueueBuilder.durable(SECKILL_ORDER_QUEUE).build();
    }

    @Bean
    public Binding orderCreateBinding() {
        return BindingBuilder.bind(orderCreateQueue()).to(orderExchange()).with(ORDER_CREATE_KEY);
    }

    @Bean
    public Binding orderTimeoutBinding() {
        return BindingBuilder.bind(orderTimeoutQueue()).to(orderExchange()).with(ORDER_TIMEOUT_KEY);
    }

    @Bean
    public Binding orderCancelBinding() {
        return BindingBuilder.bind(orderCancelQueue()).to(orderExchange()).with(ORDER_CANCEL_KEY);
    }

    @Bean
    public Binding seckillOrderBinding() {
        return BindingBuilder.bind(seckillOrderQueue()).to(orderExchange()).with(SECKILL_ORDER_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}