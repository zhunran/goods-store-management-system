package com.fengluan.trade.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fengluan.trade.entity.CartEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CartMapper extends BaseMapper<CartEntity> {

    /**
     * 添加购物车：同一商品重复添加数量累加（复用 cart 表 uq_member_good 唯一索引）
     */
    @Insert("INSERT INTO cart (member_id, good_id, qty) VALUES (#{memberId}, #{goodId}, #{qty}) " +
            "ON DUPLICATE KEY UPDATE qty = qty + #{qty}")
    int insertOrUpdate(@Param("memberId") Integer memberId,
                       @Param("goodId") Integer goodId,
                       @Param("qty") Integer qty);

    /**
     * 下单成功后清空当前会员购物车
     */
    @Delete("DELETE FROM cart WHERE member_id = #{memberId}")
    int deleteByMemberId(@Param("memberId") Integer memberId);
}