package com.fengluan.auth.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fengluan.auth.entity.RolePermissionEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermissionEntity> {
}