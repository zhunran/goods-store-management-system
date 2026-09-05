package com.fengluan.member.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fengluan.member.entity.MemberEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemberMapper extends BaseMapper<MemberEntity> {
}