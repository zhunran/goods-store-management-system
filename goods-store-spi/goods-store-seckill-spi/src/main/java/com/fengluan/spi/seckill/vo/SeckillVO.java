package com.fengluan.spi.seckill.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SeckillVO {
    private Long id;
    private String name;
    private Boolean enabled;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String description;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}