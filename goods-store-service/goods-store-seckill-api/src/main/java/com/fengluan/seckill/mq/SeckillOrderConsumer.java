package com.fengluan.seckill.mq;

import com.fengluan.common.mq.SeckillOrderMessage;
import com.fengluan.seckill.config.SeckillMqConfig;
import com.fengluan.seckill.entity.SeckillOrderEntity;
import com.fengluan.seckill.entity.SeckillOrderItemEntity;
import com.fengluan.seckill.remote.SeckillMemberClient;
import com.fengluan.seckill.remote.SeckillProductClient;
import com.fengluan.seckill.repository.SeckillGoodMapper;
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
 * 秒杀订单消费：幂等建单（DB CAS 扣减 + order + order_item 同事务），秒杀价计价。
 * 业务失败补偿 Redis（orderNo 幂等）；临时故障 nack 重试。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SeckillOrderConsumer {

    private final SeckillOrderMapper orderMapper;
    private final SeckillOrderItemMapper orderItemMapper;
    private final SeckillGoodMapper seckillGoodMapper;      // DB 账本扣减
    private final SeckillProductClient productClient;
    private final SeckillMemberClient memberClient;
    private final SeckillCompensator compensator;
    private final SeckillMessageProducer seckillMessageProducer;
    private final PlatformTransactionManager transactionManager;

    @RabbitListener(queues = SeckillMqConfig.SECKILL_ORDER_QUEUE)
    public void handle(SeckillOrderMessage msg, Channel channel,
                       @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {
            // 幂等：订单已存在（消息重投）→ 补发超时消息后 ack，
            // 避免上次「建单成功但超时消息未发出」的崩溃窗口留下永不关单的孤儿单
            if (orderMapper.existsByOrderNo(msg.getOrderNo()) > 0) {
                seckillMessageProducer.sendSeckillOrderTimeout(msg);
                channel.basicAck(tag, false);
                return;
            }
            // 业务前置校验（确定性失败 → SeckillBusinessException → 补偿 + ack）
            if (seckillGoodMapper.selectById(msg.getSeckillGoodId()) == null) {
                throw new SeckillBusinessException("秒杀商品不存在");
            }
            GoodVO good = productClient.getById(msg.getGoodId());
            if (good == null || Boolean.TRUE.equals(good.getIsDel())) {
                throw new SeckillBusinessException("商品不存在或已删除");
            }
            // 内部接口 getAccount 无越权校验（MQ 线程无 HTTP 上下文，getProfile 的 assertOwned 必抛 401）
            String account = memberClient.getAccount(msg.getMemberId());
            if (account == null || account.isBlank()) {
                throw new SeckillBusinessException("会员无效");
            }

            // DB CAS 扣减 + 建单同事务，秒杀价计价
            buildOrder(msg, good, account);

            // 建单成功后再补发超时消息，30 分钟未支付自动关单
            seckillMessageProducer.sendSeckillOrderTimeout(msg);

            channel.basicAck(tag, false);
            log.info("秒杀订单创建成功：orderNo={} totalPay={}", msg.getOrderNo(), msg.getSeckillPrice());
        } catch (SeckillBusinessException e) {
            // 业务失败：DB 未扣成功（事务已回滚），补偿 Redis 后 ack，避免无限重试
            log.warn("秒杀建单业务失败，补偿后确认：orderNo={} reason={}", msg.getOrderNo(), e.getMessage());
            compensator.compensateForCreateFailure(msg);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            // 临时故障（DB/远程）：事务已回滚，不补偿 Redis，nack 重试
            log.error("秒杀建单临时失败，将重试：orderNo={}", msg.getOrderNo(), e);
            channel.basicNack(tag, false, true);
        }
    }

    private void buildOrder(SeckillOrderMessage msg, GoodVO good, String account) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            // 第三道闸门：DB 账本 CAS 扣减，售罄抛业务异常（整体回滚）
            if (seckillGoodMapper.deductStock(msg.getSeckillGoodId()) == 0) {
                throw new SeckillBusinessException("秒杀商品已售罄");
            }
            SeckillOrderEntity order = new SeckillOrderEntity();
            order.setOrderNo(msg.getOrderNo());
            order.setSeckillNo(msg.getOrderNo());
            order.setMemberAccount(account);
            order.setTotalPay(msg.getSeckillPrice());   // 秒杀价计价（抢购时刻快照）
            order.setStatus("10"); // PENDING
            order.setCheckoutTime(LocalDateTime.now());
            order.setCreatedTime(LocalDateTime.now());
            order.setUpdatedTime(LocalDateTime.now());
            orderMapper.insert(order);

            SeckillOrderItemEntity item = new SeckillOrderItemEntity();
            item.setOrderId(order.getId().intValue());
            item.setGoodId(msg.getGoodId().intValue());
            item.setDealPrice(msg.getSeckillPrice());    // 秒杀价快照，不用商品原价
            item.setCount(1);
            item.setGoodName(good.getName());
            item.setGoodPic(good.getPic());
            orderItemMapper.insert(item);
        });
    }
}
