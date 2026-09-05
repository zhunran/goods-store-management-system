package com.fengluan.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("order_item")
public class OrderItemEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 订单编号 */
    private Integer orderId;
    /** 商品编号 */
    private Integer goodId;
    /** 成交价格 */
    private BigDecimal dealPrice;
    /** 数量 */
    private Integer count;
    /** 商品名称 */
    private String goodName;
    /** 商品图片 */
    private String goodPic;
    /** 商品摘要 */
    private String goodDesc;
    /** 备注 */
    private String description;
}