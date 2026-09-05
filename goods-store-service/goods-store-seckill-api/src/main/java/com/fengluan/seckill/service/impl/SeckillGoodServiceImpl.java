package com.fengluan.seckill.service.impl;

import com.fengluan.common.exception.BusinessException;
import com.fengluan.common.exception.ErrorCode;
import com.fengluan.seckill.config.RedisConfig;
import com.fengluan.seckill.entity.SeckillGoodEntity;
import com.fengluan.seckill.repository.SeckillGoodMapper;
import com.fengluan.seckill.service.SeckillActivityService;
import com.fengluan.seckill.service.SeckillGoodService;
import com.fengluan.seckill.util.CurrentUserUtil;
import com.fengluan.spi.seckill.dto.SeckillGoodAddRequest;
import com.fengluan.spi.seckill.vo.SeckillGoodVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeckillGoodServiceImpl implements SeckillGoodService {

    private final SeckillGoodMapper seckillGoodMapper;
    private final SeckillActivityService seckillActivityService;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeckillGoodVO addGood(Long seckillId, SeckillGoodAddRequest request) {
        seckillActivityService.getById(seckillId); // 活动必须存在，否则 4001
        SeckillGoodEntity e = new SeckillGoodEntity();
        e.setSeckillId(seckillId.intValue());
        e.setGoodId(request.getGoodId().intValue());
        e.setDescription(request.getDescription());
        try {
            seckillGoodMapper.insert(e); // 唯一约束 uq_seckill_good 冲突抛 DuplicateKeyException
        } catch (DuplicateKeyException ex) {
            throw new BusinessException("该商品已关联到此秒杀活动", ErrorCode.BAD_REQUEST.getCode());
        }
        return toVO(e);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeGood(Long id) {
        seckillGoodMapper.deleteById(id);
    }

    @Override
    public List<SeckillGoodVO> listByActivity(Long seckillId) {
        return seckillGoodMapper.selectByActivity(seckillId);
    }

    @Override
    public List<SeckillGoodVO> activeList() {
        LocalDateTime now = LocalDateTime.now();
        List<SeckillGoodVO> list = seckillGoodMapper.selectActiveSeckillGoods(now);
        for (SeckillGoodVO v : list) {
            if (v.getStartTime() == null || v.getEndTime() == null) {
                v.setStatus("IN_PROGRESS");
                v.setCountdownSec(0L);
                continue;
            }
            if (now.isBefore(v.getStartTime())) {
                v.setStatus("NOT_STARTED");
                v.setCountdownSec(ChronoUnit.SECONDS.between(now, v.getStartTime()));
            } else if (now.isAfter(v.getEndTime())) {
                v.setStatus("ENDED");
                v.setCountdownSec(-1L);
            } else {
                v.setStatus("IN_PROGRESS");
                v.setCountdownSec(0L);
            }
        }
        fillRobState(list);
        return list;
    }

    /**
     * 填充抢购状态（供前端区分"已抢到/已售空"）：
     * - stockLeft：Redis 预热后的剩余库存，未预热返回 null（不算售空，可正常抢购触发即时回源）
     * - robbed：防重键 seckill:order:{memberId}:{sgId} 存在即已抢到（建单失败时消费者会删键回滚，语义准确）
     */
    private void fillRobState(List<SeckillGoodVO> list) {
        if (list.isEmpty()) {
            return;
        }
        // 列表允许匿名浏览，未登录时 robbed 全为 false
        Long memberId = CurrentUserUtil.currentUserIdOrNull();
        for (SeckillGoodVO v : list) {
            String stock = redisTemplate.opsForValue().get(RedisConfig.STOCK_KEY_PREFIX + v.getId());
            v.setStockLeft(stock == null ? null : Long.valueOf(stock));
            v.setRobbed(memberId != null && Boolean.TRUE.equals(
                    redisTemplate.hasKey(RedisConfig.ORDER_KEY_PREFIX + memberId + ":" + v.getId())));
        }
    }

    private SeckillGoodVO toVO(SeckillGoodEntity e) {
        SeckillGoodVO v = new SeckillGoodVO();
        v.setId(e.getId());
        v.setGoodId(e.getGoodId().longValue());
        v.setDescription(e.getDescription());
        return v;
    }
}