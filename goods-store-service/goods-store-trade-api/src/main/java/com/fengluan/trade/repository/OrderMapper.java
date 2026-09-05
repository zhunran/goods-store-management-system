package com.fengluan.trade.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fengluan.trade.entity.OrderEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {

    /** 判断订单号是否存在（MQ 幂等） */
    @Select("SELECT COUNT(1) FROM `order` WHERE order_no = #{orderNo}")
    long existsByOrderNo(@Param("orderNo") String orderNo);
}