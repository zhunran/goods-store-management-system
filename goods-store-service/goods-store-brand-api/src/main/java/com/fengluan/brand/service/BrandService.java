package com.fengluan.brand.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.fengluan.brand.entity.BrandEntity;
import com.fengluan.spi.brand.dto.BrandCreateRequest;
import com.fengluan.spi.brand.dto.BrandQueryRequest;
import com.fengluan.spi.brand.dto.BrandUpdateRequest;
import com.fengluan.spi.brand.vo.BrandVO;
import com.fengluan.spi.brand.vo.PageVO;

public interface BrandService extends IService<BrandEntity> {
    /**
     * 分页查询品牌
     */
    PageVO<BrandVO> page(BrandQueryRequest query);

    /**
     * 品牌详情
     */
    BrandVO detail(Long id);

    /**
     * 新增品牌
     */
    BrandVO create(BrandCreateRequest request);

    /**
     * 修改品牌
     */
    BrandVO update(Long id, BrandUpdateRequest request);

    /**
     * 逻辑删除品牌
     */
    void delete(Long id);
}