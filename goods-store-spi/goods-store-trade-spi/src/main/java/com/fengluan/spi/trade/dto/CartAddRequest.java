package com.fengluan.spi.trade.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 添加购物车请求
 */
@Data
public class CartAddRequest {
    /** 商品编号 */
    @NotNull
    private Long goodId;
    /** 数量 */
    @NotNull
    @Min(1)
    private Integer qty;
}