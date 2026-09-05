package com.fengluan.brand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.fengluan.brand.entity.BrandEntity;
import com.fengluan.brand.repository.BrandMapper;
import com.fengluan.brand.service.BrandService;
import com.fengluan.common.exception.BusinessException;
import com.fengluan.common.exception.ErrorCode;
import com.fengluan.spi.brand.dto.BrandCreateRequest;
import com.fengluan.spi.brand.dto.BrandQueryRequest;
import com.fengluan.spi.brand.dto.BrandUpdateRequest;
import com.fengluan.spi.brand.vo.BrandVO;
import com.fengluan.spi.brand.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl extends ServiceImpl<BrandMapper, BrandEntity> implements BrandService {

    @Override
    public PageVO<BrandVO> page(BrandQueryRequest query) {
        Page<BrandEntity> page = this.page(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<BrandEntity>()
                        .like(StringUtils.isNotBlank(query.getName()), BrandEntity::getName, query.getName())
                        .orderByDesc(BrandEntity::getId));

        PageVO<BrandVO> voPage = new PageVO<>(page.getTotal(),
                page.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    public BrandVO detail(Long id) {
        BrandEntity entity = getExisting(id);
        return toVO(entity);
    }

    @Override
    public BrandVO create(BrandCreateRequest request) {
        checkNameUnique(request.getName(), null);
        BrandEntity entity = new BrandEntity();
        BeanUtils.copyProperties(request, entity);
        save(entity);
        return toVO(entity);
    }

    @Override
    public BrandVO update(Long id, BrandUpdateRequest request) {
        BrandEntity existing = getExisting(id);
        checkNameUnique(request.getName(), id);
        existing.setName(request.getName());
        existing.setCompany(request.getCompany());
        existing.setLogo(request.getLogo());
        existing.setSite(request.getSite());
        existing.setDescription(request.getDescription());
        updateById(existing);
        return toVO(existing);
    }

    @Override
    public void delete(Long id) {
        getExisting(id);
        removeById(id);
    }

    /** 校验名称唯一（编辑时排除自身） */
    private void checkNameUnique(String name, Long excludeId) {
        Long count = lambdaQuery()
                .eq(BrandEntity::getName, name)
                .ne(excludeId != null, BrandEntity::getId, excludeId)
                .count();
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.BRAND_EXISTS);
        }
    }

    private BrandEntity getExisting(Long id) {
        BrandEntity entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.BRAND_NOT_FOUND);
        }
        return entity;
    }

    private BrandVO toVO(BrandEntity entity) {
        BrandVO vo = new BrandVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}