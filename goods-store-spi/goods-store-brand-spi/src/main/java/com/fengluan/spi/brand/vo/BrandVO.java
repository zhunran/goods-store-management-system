package com.fengluan.spi.brand.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BrandVO {
    private Long id;
    private String name;
    private String company;
    private String logo;
    private String site;
    private String description;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}