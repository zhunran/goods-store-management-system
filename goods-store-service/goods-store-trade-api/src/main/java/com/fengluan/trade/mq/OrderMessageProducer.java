package com.fengluan.trade.mq;

import com.fengluan.common.mq.OrderCreateMessage;
import com.fengluan.trade.config.RabbitMqConfig;
import com.fengluan.trade.entity.CartEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 订单消息生产者：事务提交后发送创建/超时消息
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    /** 订单创建（含明细，供异步扣库存） */
    public void sendOrderCreate(String orderNo, Long memberId, List<CartEntity> carts) {
        List<OrderCreateMessage.Item> items = carts.stream().map(c ->
                OrderCreateMessage.Item.builder()
                        .goodId(c.getGoodId().longValue())
                        .count(c.getQty())
                        .build()).toList();
        OrderCreateMessage msg = OrderCreateMessage.builder()
                .orderNo(orderNo)
                .memberId(memberId)
                .items(items).build();
        rabbitTemplate.convertAndSend(RabbitMqConfig.ORDER_EXCHANGE, RabbitMqConfig.ORDER_CREATE_KEY, msg);
    }

    /** 延迟消息（30min 未支付超时取消） */
    public void sendOrderTimeout(String orderNo) {
        rabbitTemplate.convertAndSend(RabbitMqConfig.ORDER_EXCHANGE, RabbitMqConfig.ORDER_TIMEOUT_KEY, orderNo);
    }
}