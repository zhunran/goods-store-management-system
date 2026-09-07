package com.fengluan.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.fengluan.common.exception.BusinessException;
import com.fengluan.common.exception.ErrorCode;
import com.fengluan.product.entity.CategoryEntity;
import com.fengluan.product.entity.GoodDetailPicsEntity;
import com.fengluan.product.entity.GoodEntity;
import com.fengluan.product.remote.BrandRemoteService;
import com.fengluan.product.repository.CategoryMapper;
import com.fengluan.product.repository.GoodDetailPicsMapper;
import com.fengluan.product.repository.GoodMapper;
import com.fengluan.product.service.GoodService;
import com.fengluan.spi.product.dto.GoodCreateRequest;
import com.fengluan.spi.product.dto.GoodQueryRequest;
import com.fengluan.spi.product.dto.GoodUpdateRequest;
import com.fengluan.spi.product.vo.GoodVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoodServiceImpl extends ServiceImpl<GoodMapper, GoodEntity> implements GoodService {

    private final CategoryMapper categoryMapper;
    private final GoodDetailPicsMapper goodDetailPicsMapper;
    private final BrandRemoteService brandRemoteService;
    private final GoodMapper goodMapper;
    private final StringRedisTemplate redisTemplate;
    private final JsonMapper jsonMapper;

    /** 详情缓存 key 前缀 */
    private static final String GOOD_CACHE_KEY = "product:good:";
    /** 详情缓存互斥锁 key 前缀 */
    private static final String GOOD_LOCK_KEY = "product:good:lock:";
    /** 物理 TTL：很长，避免热 key 过期后集体回源 */
    private static final Duration GOOD_CACHE_PHYSICAL_TTL = Duration.ofHours(24);
    /** 逻辑 TTL：内嵌在 value 中，过期后旧值仍可用，由抢到锁的线程异步重建 */
    private static final Duration GOOD_CACHE_LOGIC_TTL = Duration.ofMinutes(30);
    /** 互斥锁 TTL：防止重建线程崩溃后死锁 */
    private static final Duration GOOD_LOCK_TTL = Duration.ofSeconds(10);

    @Override
    public List<GoodVO> page(GoodQueryRequest query) {
        LambdaQueryWrapper<GoodEntity> wrapper = new LambdaQueryWrapper<GoodEntity>()
                .like(StringUtils.hasText(query.getName()), GoodEntity::getName, query.getName())
                .eq(query.getCategoryId() != null, GoodEntity::getCategoryId, query.getCategoryId())
                .eq(query.getBrandId() != null, GoodEntity::getBrandId, query.getBrandId())
                .ge(query.getMinPrice() != null, GoodEntity::getPrice, query.getMinPrice())
                .le(query.getMaxPrice() != null, GoodEntity::getPrice, query.getMaxPrice())
                .eq(query.getIsHot() != null, GoodEntity::getIsHot, query.getIsHot());
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword();
            wrapper.and(w -> w.like(GoodEntity::getName, kw)
                    .or().like(GoodEntity::getAlias, kw)
                    .or().like(GoodEntity::getSummary, kw));
        }
        wrapper.orderByAsc(GoodEntity::getId);

        Page<GoodEntity> entities = this.page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return assemble(entities.getRecords());
    }

    @Override
    public GoodVO detail(Long id) {
        String key = GOOD_CACHE_KEY + id;
        String json = redisTemplate.opsForValue().get(key);
        if (json != null) {
            GoodCacheValue cache = toCacheValue(json);
            if (cache != null && cache.getData() != null) {
                // 未逻辑过期：直接命中返回
                if (cache.getExpireAt() > System.currentTimeMillis()) {
                    return cache.getData();
                }
                // 逻辑过期：先返回旧值（保可用），抢到锁者异步重建（防 DB 打穿）
                rebuildCacheAsync(id);
                return cache.getData();
            }
        }
        // 缓存不存在 / 数据损坏：互斥重建，防止并发回源击穿 DB
        return loadDetailWithMutex(id, key);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GoodVO create(GoodCreateRequest request) {
        GoodEntity entity = new GoodEntity();
        BeanUtils.copyProperties(request, entity, "detailPicList");
        entity.setSpuNo("SPU" + System.currentTimeMillis());
        entity.setIsDel(false);
        entity.setCreatedTime(LocalDateTime.now());
        entity.setUpdatedTime(LocalDateTime.now());
        save(entity);
        if (request.getDetailPicList() != null && !request.getDetailPicList().isEmpty()) {
            replaceDetailPics(entity.getId(), request.getDetailPicList());
        }
        return detail(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GoodVO update(Long id, GoodUpdateRequest request) {
        GoodEntity existed = super.getById(id);
        if (existed == null) {
            throw new BusinessException(ErrorCode.GOOD_NOT_FOUND);
        }
        GoodEntity entity = new GoodEntity();
        BeanUtils.copyProperties(request, entity, "detailPicList", "id");
        entity.setId(id);
        entity.setUpdatedTime(LocalDateTime.now());
        updateById(entity);
        // detailPicList 非空才替换全部详情图
        if (request.getDetailPicList() != null) {
            replaceDetailPics(id, request.getDetailPicList());
        }
        evictCache(id);
        return detail(id);
    }

    @Override
    public void delete(Long id) {
        if (super.getById(id) == null) {
            throw new BusinessException(ErrorCode.GOOD_NOT_FOUND);
        }
        // 逻辑删除（good.is_del=1，全局 logic-delete-field 生效）
        super.removeById(id);
        evictCache(id);
    }

    @Override
    public void updateStatus(Long id, Boolean takeDown) {
        GoodEntity entity = super.getById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.GOOD_NOT_FOUND);
        }
        entity.setIsTakeDown(takeDown);
        entity.setUpdatedTime(LocalDateTime.now());
        updateById(entity);
        evictCache(id);
    }

    @Override
    public boolean deductStock(Long id, Integer count) {
        boolean ok = goodMapper.deductStock(id, count) > 0;
        if (ok) {
            // 扣减成功后失效详情缓存，避免逻辑过期/长 TTL 旧库存继续被读 → 超卖
            evictCache(id);
        }
        return ok;
    }

    @Override
    public void restoreStock(Long id, Integer count) {
        goodMapper.restoreStock(id, count);
        evictCache(id);
    }

    /**
     * 直接回源查询商品详情（含品牌/分类名 + 详情图），供缓存命中回填与重建使用
     */
    private GoodVO loadFromDb(Long id) {
        GoodEntity entity = super.getById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.GOOD_NOT_FOUND);
        }
        GoodVO vo = toVO(entity);
        fillNames(Collections.singletonList(vo));
        fillDetailPics(vo);
        return vo;
    }

    /**
     * 缓存不存在 / 数据损坏时互斥回源重建：setnx 抢锁，抢到者回源并写缓存；未抢到者短暂等待后重试读取，
     * 仍无则直接回源兜底，保证并发下只有一次 DB 回源。
     */
    private GoodVO loadDetailWithMutex(Long id, String key) {
        String lockKey = GOOD_LOCK_KEY + id;
        boolean locked = Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(lockKey, "1", GOOD_LOCK_TTL));
        if (!locked) {
            sleepQuietly(50);
            String json = redisTemplate.opsForValue().get(key);
            GoodCacheValue cache = toCacheValue(json);
            if (cache != null && cache.getData() != null) {
                return cache.getData();
            }
            return loadFromDb(id);
        }
        try {
            // 抢到锁后再读一次，避免重复回源
            String json = redisTemplate.opsForValue().get(key);
            GoodCacheValue cache = toCacheValue(json);
            if (cache != null && cache.getData() != null) {
                return cache.getData();
            }
            GoodVO vo = loadFromDb(id);
            writeCacheValue(id, vo);
            return vo;
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    /**
     * 逻辑过期后的异步重建：setnx 抢锁，抢到者在后台线程回源并刷新缓存；未抢到者不作处理（旧值已返回给调用方）
     */
    private void rebuildCacheAsync(Long id) {
        String lockKey = GOOD_LOCK_KEY + id;
        boolean locked = Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(lockKey, "1", GOOD_LOCK_TTL));
        if (!locked) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                GoodVO vo = loadFromDb(id);
                writeCacheValue(id, vo);
            } catch (Exception e) {
                log.warn("逻辑过期重建商品详情缓存失败, id={}", id, e);
            } finally {
                redisTemplate.delete(lockKey);
            }
        });
    }

    private void writeCacheValue(Long id, GoodVO vo) {
        GoodCacheValue value = new GoodCacheValue(vo, System.currentTimeMillis() + GOOD_CACHE_LOGIC_TTL.toMillis());
        try {
            redisTemplate.opsForValue().set(GOOD_CACHE_KEY + id, jsonMapper.writeValueAsString(value), GOOD_CACHE_PHYSICAL_TTL);
        } catch (Exception e) {
            log.warn("写入商品详情缓存失败, id={}", id, e);
        }
    }

    private void evictCache(Long id) {
        redisTemplate.delete(GOOD_CACHE_KEY + id);
    }

    private GoodCacheValue toCacheValue(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return jsonMapper.readValue(json, GoodCacheValue.class);
        } catch (Exception e) {
            log.warn("解析商品详情缓存失败，将回源重建", e);
            return null;
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** 缓存值：内嵌逻辑过期时间，物理 TTL 长、逻辑 TTL 短 */
    private static class GoodCacheValue {
        private GoodVO data;
        private long expireAt;

        GoodCacheValue() {
        }

        GoodCacheValue(GoodVO data, long expireAt) {
            this.data = data;
            this.expireAt = expireAt;
        }

        public GoodVO getData() {
            return data;
        }

        public void setData(GoodVO data) {
            this.data = data;
        }

        public long getExpireAt() {
            return expireAt;
        }

        public void setExpireAt(long expireAt) {
            this.expireAt = expireAt;
        }
    }

    /**
     * 替换某商品的全部详情图（先删该商品图片，再按新列表重插）
     */
    private void replaceDetailPics(Long goodId, List<String> urls) {
        goodDetailPicsMapper.delete(new LambdaQueryWrapper<GoodDetailPicsEntity>()
                .eq(GoodDetailPicsEntity::getGoodId, goodId.intValue()));
        List<GoodDetailPicsEntity> toSave = new ArrayList<>();
        IntStream.range(0, urls.size()).forEach(i -> {
            GoodDetailPicsEntity pic = new GoodDetailPicsEntity();
            pic.setGoodId(goodId.intValue());
            pic.setUrl(urls.get(i));
            pic.setSort(i);
            toSave.add(pic);
        });
        if (!toSave.isEmpty()) {
            for (GoodDetailPicsEntity pic : toSave) {
                goodDetailPicsMapper.insert(pic);
            }
        }
    }

    /**
     * 批量组装品牌名 / 分类名
     */
    private List<GoodVO> assemble(List<GoodEntity> entities) {
        List<GoodVO> vos = entities.stream().map(this::toVO).toList();
        fillNames(vos);
        return vos;
    }

    private void fillNames(List<GoodVO> vos) {
        if (vos.isEmpty()) {
            return;
        }
        Set<Integer> brandIds = vos.stream()
                .map(GoodVO::getBrandId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Integer, String> brandNames = brandRemoteService.getBrandNameMap(brandIds);

        Set<Integer> categoryIds = vos.stream()
                .map(GoodVO::getCategoryId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Integer, String> categoryNames = categoryIds.isEmpty() ? Collections.emptyMap() : categoryIds.stream()
                .collect(Collectors.toMap(c -> c, this::loadCategoryName));

        for (GoodVO vo : vos) {
            vo.setBrandName(vo.getBrandId() == null ? "-" : brandNames.getOrDefault(vo.getBrandId(), "-"));
            vo.setCategoryName(vo.getCategoryId() == null ? "-" : categoryNames.getOrDefault(vo.getCategoryId(), "-"));
        }
    }

    private String loadCategoryName(Integer categoryId) {
        CategoryEntity c = categoryMapper.selectById(categoryId);
        return c == null ? "-" : c.getName();
    }

    private void fillDetailPics(GoodVO vo) {
        List<GoodDetailPicsEntity> pics = goodDetailPicsMapper.selectList(
                new LambdaQueryWrapper<GoodDetailPicsEntity>()
                        .eq(GoodDetailPicsEntity::getGoodId, vo.getId().intValue())
                        .orderByAsc(GoodDetailPicsEntity::getSort));
        vo.setDetailPicList(pics.stream().map(GoodDetailPicsEntity::getUrl).toList());
    }

    private GoodVO toVO(GoodEntity entity) {
        GoodVO vo = new GoodVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}