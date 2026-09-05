package com.fengluan.product.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.fengluan.product.entity.GoodEntity;
import com.fengluan.spi.product.dto.GoodCreateRequest;
import com.fengluan.spi.product.dto.GoodQueryRequest;
import com.fengluan.spi.product.dto.GoodUpdateRequest;
import com.fengluan.spi.product.vo.GoodVO;

import java.util.List;

public interface GoodService extends IService<GoodEntity> {

    /**
     * 商品分页列表（多条件 + 关键词 + 品牌/分类名/详情图组装）
     */
    List<GoodVO> page(GoodQueryRequest query);

    /**
     * 商品详情（品牌/分类名 + 详情图）
     */
    GoodVO detail(Long id);

    /**
     * 新增商品（含详情图）
     */
    GoodVO create(GoodCreateRequest request);

    /**
     * 编辑商品（含详情图替换）
     */
    GoodVO update(Long id, GoodUpdateRequest request);

    /**
     * 删除商品（逻辑删除）
     */
    void delete(Long id);

    /**
     * 上下架
     */
    void updateStatus(Long id, Boolean takeDown);

    /**
     * 扣减库存：true=成功，false=库存不足
     */
    boolean deductStock(Long id, Integer count);

    /**
     * 恢复库存
     */
    void restoreStock(Long id, Integer count);
}