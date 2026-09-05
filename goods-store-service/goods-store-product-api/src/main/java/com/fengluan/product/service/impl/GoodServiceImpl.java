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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        GoodEntity entity = super.getById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.GOOD_NOT_FOUND);
        }
        GoodVO vo = toVO(entity);
        fillNames(Collections.singletonList(vo));
        fillDetailPics(vo);
        return vo;
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
        return detail(id);
    }

    @Override
    public void delete(Long id) {
        if (super.getById(id) == null) {
            throw new BusinessException(ErrorCode.GOOD_NOT_FOUND);
        }
        // 逻辑删除（good.is_del=1，全局 logic-delete-field 生效）
        super.removeById(id);
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
    }

    @Override
    public boolean deductStock(Long id, Integer count) {
        return goodMapper.deductStock(id, count) > 0;
    }

    @Override
    public void restoreStock(Long id, Integer count) {
        goodMapper.restoreStock(id, count);
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