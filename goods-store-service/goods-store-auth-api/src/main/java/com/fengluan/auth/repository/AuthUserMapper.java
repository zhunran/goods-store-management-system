package com.fengluan.auth.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fengluan.auth.entity.AuthUserEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuthUserMapper extends BaseMapper<AuthUserEntity> {
}