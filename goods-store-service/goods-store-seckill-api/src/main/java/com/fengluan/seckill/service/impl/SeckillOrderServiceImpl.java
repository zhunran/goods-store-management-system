package com.fengluan.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fengluan.common.exception.BusinessException;
import com.fengluan.common.exception.ErrorCode;
import com.fengluan.common.mq.SeckillOrderMessage;
import com.fengluan.common.util.SnowflakeUtil;
import com.fengluan.seckill.config.RedisConfig;
import com.fengluan.seckill.entity.SeckillEntity;
import com.fengluan.seckill.entity.SeckillGoodEntity;
import com.fengluan.seckill.entity.SeckillOrderEntity;
import com.fengluan.seckill.mq.SeckillMessageProducer;
import com.fengluan.seckill.remote.SeckillMemberClient;
import com.fengluan.seckill.repository.SeckillGoodMapper;
import com.fengluan.seckill.repository.SeckillOrderMapper;
import com.fengluan.seckill.service.SeckillActivityService;
import com.fengluan.seckill.service.SeckillOrderService;
import com.fengluan.spi.seckill.dto.SeckillOrderResponse;
import com.fengluan.spi.seckill.dto.SeckillOrderResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeckillOrderServiceImpl implements SeckillOrderService {

    private final SeckillGoodMapper seckillGoodMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private final SeckillActivityService seckillActivityService;
    private final SeckillMemberClient memberClient;
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> seckillScript;
    private final SeckillMessageProducer seckillMessageProducer;
    private final SnowflakeUtil snowflakeUtil;

    @Override
    public SeckillOrderResponse seckill(Long seckillGoodId, Long memberId) {
        // 1. 秒杀商品存在性
        SeckillGoodEntity sg = seckillGoodMapper.selectById(seckillGoodId);
        if (sg == null) {
            throw new BusinessException(ErrorCode.SECKILL_NOT_FOUND);
        }
        // 2. 活动窗口校验
        SeckillEntity seckill = seckillActivityService.getById(sg.getSeckillId().longValue());
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(seckill.getStartTime())) {
            throw new BusinessException(ErrorCode.SECKILL_NOT_STARTED);
        }
        if (now.isAfter(seckill.getEndTime())) {
            throw new BusinessException(ErrorCode.SECKILL_ENDED);
        }
        // 3. Lua 原子预减 + 防重（-1=无库存键 / 0=售罄 / -2=已参与 / 1=成功）
        String stockKey = RedisConfig.STOCK_KEY_PREFIX + seckillGoodId;
        String orderKey = RedisConfig.ORDER_KEY_PREFIX + memberId + ":" + seckillGoodId;
        // 防重键 TTL 对齐库存键：活动结束 +1h（超时关单回补防重键后，活动期内仍不可重复抢）
        long keyTtl = Duration.between(now, seckill.getEndTime()).getSeconds() + 3600;
        Long result = redisTemplate.execute(seckillScript, List.of(stockKey, orderKey), String.valueOf(keyTtl));
        if (Long.valueOf(-1).equals(result)) {
            // 库存键不存在（活动开始后才添加商品/错过预热窗口）：即时回源预热后重试一次
            result = preheatAndRetry(sg, seckill, stockKey, orderKey);
        }
        if (Long.valueOf(-2).equals(result)) {
            throw new BusinessException(ErrorCode.SECKILL_ALREADY);
        }
        if (result == null || result != 1L) {
            throw new BusinessException(ErrorCode.SECKILL_STOCK_EMPTY);
        }
        // 4. 发 MQ 异步建单（此时已由 Lua 预扣库存、记防重标记）；价格取下单时刻快照
        String orderNo = snowflakeUtil.nectIdStr();
        SeckillOrderMessage msg = SeckillOrderMessage.builder()
                .seckillId(sg.getSeckillId().longValue())
                .seckillGoodId(seckillGoodId)
                .goodId(sg.getGoodId().longValue())
                .memberId(memberId)
                .seckillPrice(sg.getSeckillPrice())
                .orderNo(orderNo)
                .build();
        seckillMessageProducer.sendSeckillOrder(msg);
        log.info("秒杀抢购成功，进入排队：memberId={} seckillGoodId={} orderNo={}", memberId, seckillGoodId, orderNo);
        return new SeckillOrderResponse(orderNo, "PENDING", "排队中，请稍后查询结果");
    }

    /**
     * 库存键缺失时的即时回源：按 DB 账本（限量-已售）setnx 原子写入，
     * 过期时间对齐 StockPreheatJob（活动结束后 1 小时），然后重试一次 Lua 扣减。
     */
    private Long preheatAndRetry(SeckillGoodEntity sg, SeckillEntity seckill, String stockKey, String orderKey) {
        int left = Math.max(0, sg.getStockCount() - sg.getStockSold());
        redisTemplate.opsForValue().setIfAbsent(stockKey, String.valueOf(left));
        long expire = Duration.between(LocalDateTime.now(), seckill.getEndTime()).getSeconds() + 3600;
        redisTemplate.expire(stockKey, Duration.ofSeconds(expire));
        log.info("秒杀库存即时回源 seckillGoodId={}, stock={}", sg.getId(), left);
        return redisTemplate.execute(seckillScript, List.of(stockKey, orderKey), String.valueOf(expire));
    }

    @Override
    public SeckillOrderResultVO result(String orderNo, Long memberId) {
        SeckillOrderEntity order = seckillOrderMapper.selectOne(
                new LambdaQueryWrapper<SeckillOrderEntity>()
                        .eq(SeckillOrderEntity::getOrderNo, orderNo));
        if (order == null) {
            // 消费者尚未建单：返回排队中
            return new SeckillOrderResultVO(orderNo, "PROCESSING", "排队中，请稍后查询结果");
        }
        // 归属校验：订单账号须与当前用户一致，否则视为不存在（312 复用交易错误码）
        String account = memberClient.getAccount(memberId);
        if (!account.equals(order.getMemberAccount())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        return new SeckillOrderResultVO(orderNo, "SUCCESS", "已抢到");
    }
}