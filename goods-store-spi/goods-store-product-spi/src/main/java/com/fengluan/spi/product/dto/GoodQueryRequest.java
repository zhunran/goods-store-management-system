package com.fengluan.spi.product.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GoodQueryRequest {
    /** 当前页码，默认第一页 */
    private Long pageNum = 1L;
    /** 每页条数，默认10条 */
    private Long pageSize = 10L;
    /** 商品名称，模糊查询（兼容旧参数） */
    private String name;
    /** 分类id */
    private Integer categoryId;
    /** 品牌id */
    private Integer brandId;
    /** 最低价 */
    private BigDecimal minPrice;
    /** 最高价 */
    private BigDecimal maxPrice;
    /** 是否热销 */
    private Boolean isHot;
    /** 关键词：匹配 name/alias/summary */
    private String keyword;
}