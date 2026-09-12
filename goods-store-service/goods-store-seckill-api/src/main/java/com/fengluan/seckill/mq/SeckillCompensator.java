package com.fengluan.seckill.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fengluan.common.mq.SeckillOrderMessage;
import com.fengluan.seckill.config.RedisConfig;
import com.fengluan.seckill.entity.SeckillOrderEntity;
import com.fengluan.seckill.repository.SeckillGoodMapper;
import com.fengluan.seckill.repository.SeckillOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * 秒杀补偿器：统一幂等骨架 —— orderNo 补偿标记（setnx 48h）先抢权，抢到才执行。
 * 原则：宁可少回补（少卖一件），绝不多回补（多卖一件）。
 * 崩溃窗口：标记写入后、回补执行前崩溃 → 重试跳过 → 少回补 1 件（偏保守，对账 SQL 兜底）。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SeckillCompensator {

    private static final String CANCELLED = "50";

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> restoreScript;
    private final SeckillGoodMapper seckillGoodMapper;
    private final SeckillOrderMapper orderMapper;

    /**
     * 建单失败补偿：DB 未扣成（事务回滚），仅回补 Redis。
     * 键存在才回补（Lua），防凭空造库存；orderNo 标记保证只补一次。
     */
    public void compensateForCreateFailure(SeckillOrderMessage msg) {
        if (!tryAcquireCompensation(msg.getOrderNo())) {
            return;
        }
        restoreRedis(msg.getSeckillGoodId(), msg.getMemberId());
        log.info("秒杀建单失败补偿（仅 Redis）：orderNo={} seckillGoodId={} memberId={}",
                msg.getOrderNo(), msg.getSeckillGoodId(), msg.getMemberId());
    }

    /**
     * 超时关单补偿：DB 已扣（建单时），DB 回补 + Redis 回补。
     * 内部先校验订单确为 CANCELLED —— 已支付订单不回补（与 pay 竞争的让步方）；
     * 与建单失败共用 orderNo 标记 → 两条路径天然互斥，不会双补。
     */
    public void compensateForTimeout(SeckillOrderMessage msg) {
        SeckillOrderEntity order = orderMapper.selectOne(new LambdaQueryWrapper<SeckillOrderEntity>()
                .eq(SeckillOrderEntity::getOrderNo, msg.getOrderNo()));
        if (order == null || !CANCELLED.equals(order.getStatus())) {
            return;
        }
        if (!tryAcquireCompensation(msg.getOrderNo())) {
            return;
        }
        seckillGoodMapper.restoreStock(msg.getSeckillGoodId());
        restoreRedis(msg.getSeckillGoodId(), msg.getMemberId());
        log.info("秒杀超时关单补偿（DB+Redis）：orderNo={} seckillGoodId={} memberId={}",
                msg.getOrderNo(), msg.getSeckillGoodId(), msg.getMemberId());
    }

    /** 抢补偿权：setnx 幂等标记，48h TTL 覆盖活动全周期 */
    private boolean tryAcquireCompensation(String orderNo) {
        Boolean first = redisTemplate.opsForValue()
                .setIfAbsent(RedisConfig.COMP_KEY_PREFIX + orderNo, "1", Duration.ofHours(48));
        return Boolean.TRUE.equals(first);
    }

    /** Redis 回补：库存键存在才 INCR + 清防重键（Lua 原子） */
    private void restoreRedis(Long seckillGoodId, Long memberId) {
        String stockKey = RedisConfig.STOCK_KEY_PREFIX + seckillGoodId;
        String orderKey = RedisConfig.ORDER_KEY_PREFIX + memberId + ":" + seckillGoodId;
        Long restored = redisTemplate.execute(restoreScript, List.of(stockKey, orderKey));
        if (restored == null || restored == 0L) {
            log.info("秒杀 Redis 库存键不存在，跳过回补（活动结束/键已过期）：seckillGoodId={}", seckillGoodId);
        }
    }
}
