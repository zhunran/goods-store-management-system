package com.fengluan.seckill.mq;

import com.fengluan.seckill.config.RedisConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 秒杀 Redis 回补公共组件：恢复库存 + 清除防重键。
 * 供建单失败补偿（SeckillOrderConsumer）与超时关单（SeckillOrderTimeoutConsumer）共用。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SeckillRedisCompensator {

    private final StringRedisTemplate redisTemplate;

    public void restore(Long seckillGoodId, Long memberId) {
        String stockKey = RedisConfig.STOCK_KEY_PREFIX + seckillGoodId;
        String orderKey = RedisConfig.ORDER_KEY_PREFIX + memberId + ":" + seckillGoodId;
        redisTemplate.opsForValue().increment(stockKey);
        redisTemplate.delete(orderKey);
        log.info("秒杀回补：恢复库存+清防重 seckillGoodId={} memberId={}", seckillGoodId, memberId);
    }
}