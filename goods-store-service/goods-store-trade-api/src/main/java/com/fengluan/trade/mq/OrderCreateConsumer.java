package com.fengluan.trade.mq;

import com.fengluan.common.mq.OrderCreateMessage;
import com.fengluan.trade.config.RabbitMqConfig;
import com.fengluan.trade.remote.TradeProductClient;
import com.fengluan.trade.repository.OrderMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * 订单创建消息消费者：异步扣减库存（幂等）
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderCreateConsumer {

    private final OrderMapper orderMapper;
    private final TradeProductClient productClient;

    @RabbitListener(queues = RabbitMqConfig.ORDER_CREATE_QUEUE)
    public void handle(OrderCreateMessage msg, Channel channel,
                       @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws Exception {
        try {
            // 幂等：订单号不存在则跳过（订单未入库不应发生，此处为防御）
            if (orderMapper.existsByOrderNo(msg.getOrderNo()) == 0) {
                log.warn("订单 {} 不存在，跳过扣库存", msg.getOrderNo());
                channel.basicAck(tag, false);
                return;
            }
            for (OrderCreateMessage.Item item : msg.getItems()) {
                Boolean ok = productClient.deductStock(item.getGoodId(), item.getCount());
                if (!Boolean.TRUE.equals(ok)) {
                    log.error("库存扣减失败 orderNo={}, goodId={}, count={}",
                            msg.getOrderNo(), item.getGoodId(), item.getCount());
                }
            }
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("消费订单创建消息异常: {}", msg.getOrderNo(), e);
            channel.basicNack(tag, false, true); // 重回队列重试
        }
    }
}