package com.fengluan.spi.seckill.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SeckillGoodAddRequest {
    @NotNull
    private Long goodId;
    /** 秒杀价 */
    @NotNull
    @DecimalMin(value = "0.01", message = "秒杀价必须大于 0")
    private BigDecimal seckillPrice;
    /** 限量库存 */
    @NotNull
    @Min(value = 1, message = "限量库存至少为 1")
    private Integer stockCount;
    private String description;
}
