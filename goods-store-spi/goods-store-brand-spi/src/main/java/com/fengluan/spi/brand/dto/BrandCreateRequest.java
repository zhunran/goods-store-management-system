package com.fengluan.spi.brand.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 新增品牌请求
 */
@Data
public class BrandCreateRequest {
    @NotBlank(message = "品牌名称不能为空")
    private String name;
    private String company;
    private String logo;
    private String site;
    private String description;
}