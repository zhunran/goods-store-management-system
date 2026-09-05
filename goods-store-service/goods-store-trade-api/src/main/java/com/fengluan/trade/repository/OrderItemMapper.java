package com.fengluan.trade.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fengluan.trade.entity.OrderItemEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItemEntity> {

    /** 按订单查询明细（取消恢复库存用） */
    @Select("SELECT * FROM order_item WHERE order_id = #{orderId}")
    List<OrderItemEntity> selectByOrderId(@Param("orderId") Integer orderId);
}