package com.fengluan.auth.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fengluan.auth.entity.AdminUserEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminUserMapper extends BaseMapper<AdminUserEntity> {
}