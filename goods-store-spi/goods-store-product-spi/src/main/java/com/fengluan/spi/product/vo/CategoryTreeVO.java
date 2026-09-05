package com.fengluan.spi.product.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CategoryTreeVO {
    private Long id;
    private Integer parentId;
    private String name;
    private String icon;
    private List<CategoryTreeVO> children = new ArrayList<>();
}