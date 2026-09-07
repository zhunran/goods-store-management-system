package com.fengluan.spi.trade.vo;

import lombok.Data;

/**
 * 购物车项 VO（trade 侧只回基础字段，商品图/名由 web 聚合补齐）
 */
@Data
public class CartVO {
    private Long id;
    private Long memberId;
    private Long goodId;
    private Integer qty;
    private Boolean selected;
}