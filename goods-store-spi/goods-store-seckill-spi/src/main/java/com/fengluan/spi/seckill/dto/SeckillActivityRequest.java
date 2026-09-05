package com.fengluan.spi.seckill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SeckillActivityRequest {
    @NotBlank
    private String name;
    @NotNull
    private Boolean enabled;
    @NotNull
    private LocalDateTime startTime;
    @NotNull
    private LocalDateTime endTime;
    private String description;
}