package com.fengluan.seckill.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fengluan.seckill.entity.SeckillOrderEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface SeckillOrderMapper extends BaseMapper<SeckillOrderEntity> {

    /** 幂等校验：订单是否已存在 */
    @Select("SELECT COUNT(1) FROM `order` WHERE order_no = #{orderNo}")
    long existsByOrderNo(@Param("orderNo") String orderNo);

    /** CAS 关单：仅待付款可关（与支付竞争，谁后到谁让步）；返回影响行数 */
    @Update("UPDATE `order` SET status = '50', updated_time = #{now} WHERE order_no = #{orderNo} AND status = '10'")
    int cancelPending(@Param("orderNo") String orderNo, @Param("now") LocalDateTime now);
}