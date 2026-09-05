package com.fengluan.product.service;

import com.fengluan.spi.product.vo.CategoryTreeVO;

import java.util.List;

public interface CategoryService {

    /**
     * 全量分类树（内存递归构建）
     */
    List<CategoryTreeVO> getTree();
}