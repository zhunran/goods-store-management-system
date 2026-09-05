package com.fengluan.spi.seckill.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SeckillGoodAddRequest {
    @NotNull
    private Long goodId;
    private String description;
}