package com.fengluan.seckill.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fengluan.seckill.config.RedisConfig;
import com.fengluan.seckill.entity.SeckillEntity;
import com.fengluan.seckill.entity.SeckillGoodEntity;
import com.fengluan.seckill.repository.SeckillGoodMapper;
import com.fengluan.seckill.repository.SeckillMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/** 秒杀库存预热：每分钟将「进行中 + 5 分钟内开始」活动下的商品库存写入 Redis */
@Component
@Slf4j
@RequiredArgsConstructor
public class StockPreheatJob {

    private final SeckillMapper seckillMapper;
    private final SeckillGoodMapper seckillGoodMapper;
    private final StringRedisTemplate redisTemplate;

    @Scheduled(cron = "0 * * * * ?")
    public void preheatStock() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fiveMinutesLater = now.plusMinutes(5);

        // 覆盖两类活动：5 分钟内即将开始的（正常预热）+ 进行中的
        // （活动开始后才添加的秒杀商品、服务重启跨过开始时间等场景，保证其库存键最终存在）
        List<SeckillEntity> upcoming = seckillMapper.selectList(
                new LambdaQueryWrapper<SeckillEntity>()
                        .eq(SeckillEntity::getEnabled, true)
                        .le(SeckillEntity::getStartTime, fiveMinutesLater)
                        .ge(SeckillEntity::getEndTime, now)
        );

        for (SeckillEntity seckill : upcoming) {
            List<SeckillGoodEntity> goods = seckillGoodMapper.selectList(
                    new LambdaQueryWrapper<SeckillGoodEntity>()
                            .eq(SeckillGoodEntity::getSeckillId, seckill.getId())
            );
            for (SeckillGoodEntity sg : goods) {
                String key = RedisConfig.STOCK_KEY_PREFIX + sg.getId();
                // 预热值改为 DB 账本剩余量（限量-已售），与商品库存款彻底解耦
                int left = Math.max(0, sg.getStockCount() - sg.getStockSold());
                String stock = String.valueOf(left);
                // setnx 一步写入（幂等，已预热的跳过；stock_count=0 的存量行写入 0 即售罄安全态）
                Boolean absent = redisTemplate.opsForValue().setIfAbsent(key, stock);
                if (Boolean.FALSE.equals(absent)) {
                    continue; // 已预热，跳过
                }
                long expire = Duration.between(now, seckill.getEndTime()).getSeconds() + 3600;
                redisTemplate.expire(key, Duration.ofSeconds(expire));
                log.info("秒杀库存预热 seckillGoodId={}, stock={}", sg.getId(), stock);
            }
        }
    }
}