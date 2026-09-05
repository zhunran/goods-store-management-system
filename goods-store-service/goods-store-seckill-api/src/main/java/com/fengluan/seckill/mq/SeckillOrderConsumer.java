package com.fengluan.seckill.mq;

import com.fengluan.common.mq.SeckillOrderMessage;
import com.fengluan.seckill.config.RedisConfig;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

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
    private final StringRedisTemplate redisTemplate;

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
            // 商品不存在或已删除：无单可建，补偿 Redis
            if (good == null || Boolean.TRUE.equals(good.getIsDel())) {
                compensate(msg);
                channel.basicAck(tag, false);
                return;
            }
            // 内部接口 getAccount 无越权校验（MQ 线程无 HTTP 上下文，getProfile 的 assertOwned 必抛 401）
            String account = memberClient.getAccount(msg.getMemberId());

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

            channel.basicAck(tag, false);
            log.info("秒杀订单创建成功：orderNo={}", msg.getOrderNo());
        } catch (Exception e) {
            log.error("秒杀建单失败，补偿 Redis：orderNo={}", msg.getOrderNo(), e);
            compensate(msg);
            channel.basicAck(tag, false); // 补偿后确认，不无限重试
        }
    }

    private void compensate(SeckillOrderMessage msg) {
        String stockKey = RedisConfig.STOCK_KEY_PREFIX + msg.getSeckillGoodId();
        String orderKey = RedisConfig.ORDER_KEY_PREFIX + msg.getMemberId() + ":" + msg.getSeckillGoodId();
        redisTemplate.opsForValue().increment(stockKey);
        redisTemplate.delete(orderKey);
        log.info("秒杀补偿：恢复库存+清防重 seckillGoodId={} memberId={}", msg.getSeckillGoodId(), msg.getMemberId());
    }
}