package com.fengluan.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("cart")
public class CartEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 会员编号 */
    private Integer memberId;
    /** 商品编号 */
    private Integer goodId;
    /** 数量 */
    private Integer qty;
}