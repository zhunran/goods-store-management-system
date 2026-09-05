package com.fengluan.seckill.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fengluan.seckill.entity.SeckillOrderEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SeckillOrderMapper extends BaseMapper<SeckillOrderEntity> {

    /** 幂等校验：订单是否已存在 */
    @Select("SELECT COUNT(1) FROM `order` WHERE order_no = #{orderNo}")
    long existsByOrderNo(@Param("orderNo") String orderNo);
}