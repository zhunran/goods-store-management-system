package com.fengluan.product.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fengluan.product.entity.CategoryEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper extends BaseMapper<CategoryEntity> {
}