package com.fengluan.spi.brand.dto;

import lombok.Data;

@Data
public class BrandQueryRequest {
    /** 当前页码，默认第一页 */
    private Long pageNum = 1L;
    /** 每页条数，默认10条 */
    private Long pageSize = 10L;
    /** 品牌名称，模糊查询 */
    private String name;
}