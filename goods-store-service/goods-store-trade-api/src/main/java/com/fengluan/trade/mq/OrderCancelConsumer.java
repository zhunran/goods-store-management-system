package com.fengluan.trade.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fengluan.trade.config.RabbitMqConfig;
import com.fengluan.trade.entity.OrderEntity;
import com.fengluan.trade.entity.OrderItemEntity;
import com.fengluan.trade.enums.OrderStatus;
import com.fengluan.trade.remote.TradeProductClient;
import com.fengluan.trade.repository.OrderItemMapper;
import com.fengluan.trade.repository.OrderMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 订单取消消息消费者：消费的是裸 orderNo 字符串（来自 orderTimeoutQueue 死信）
 * 仅取消仍为 PENDING 的订单，并恢复库存
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderCancelConsumer {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final TradeProductClient productClient;

    @RabbitListener(queues = RabbitMqConfig.ORDER_CANCEL_QUEUE)
    public void handle(String orderNo, Channel channel,
                       @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws Exception {
        try {
            OrderEntity order = orderMapper.selectOne(new LambdaQueryWrapper<OrderEntity>()
                    .eq(OrderEntity::getOrderNo, orderNo));
            if (order == null || !OrderStatus.PENDING.getCode().equals(order.getStatus())) {
                log.info("订单 {} 无需取消（不存在或非待付款）", orderNo);
                channel.basicAck(tag, false);
                return;
            }
            order.setStatus(OrderStatus.CANCELLED.getCode());
            orderMapper.updateById(order);
            List<OrderItemEntity> items = orderItemMapper.selectByOrderId(order.getId().intValue());
            if (items != null) {
                for (OrderItemEntity it : items) {
                    productClient.restoreStock(it.getGoodId().longValue(), it.getCount());
                }
            }
            log.info("订单超时自动取消: {}", orderNo);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("消费订单取消消息异常: {}", orderNo, e);
            channel.basicNack(tag, false, true);
        }
    }
}