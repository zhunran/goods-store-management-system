package com.fengluan.auth.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fengluan.auth.entity.PermissionEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PermissionMapper extends BaseMapper<PermissionEntity> {
}