package com.fengluan.spi.brand.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改品牌请求
 */
@Data
public class BrandUpdateRequest {
    @NotNull(message = "品牌ID不能为空")
    private Long id;
    @NotBlank(message = "品牌名称不能为空")
    private String name;
    private String company;
    private String logo;
    private String site;
    private String description;
}