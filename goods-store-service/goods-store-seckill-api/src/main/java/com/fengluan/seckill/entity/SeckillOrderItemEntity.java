package com.fengluan.seckill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/** 秒杀订单明细（映射共享库 order_item 表） */
@Data
@TableName("order_item")
public class SeckillOrderItemEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 订单 id */
    private Integer orderId;
    /** 商品 id */
    private Integer goodId;
    /** 成交价 */
    private BigDecimal dealPrice;
    /** 数量 */
    private Integer count;
    /** 商品名称 */
    private String goodName;
    /** 商品主图 */
    private String goodPic;
}