package com.fengluan.web.trade;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 购物车项聚合 VO（web 补齐商品图/名/价）
 */
@Data
public class CartItemVO {
    private Long cartId;
    private Long goodId;
    private Integer qty;
    private String goodName;
    private String goodPic;
    private BigDecimal price;
}