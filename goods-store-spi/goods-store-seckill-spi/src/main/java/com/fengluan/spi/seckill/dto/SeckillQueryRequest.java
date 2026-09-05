package com.fengluan.spi.seckill.dto;

import lombok.Data;

@Data
public class SeckillQueryRequest {
    /** 当前页码，默认第一页 */
    private Long pageNum = 1L;
    /** 每页条数，默认10条 */
    private Long pageSize = 10L;
    /** 秒杀名称，模糊查询 */
    private String name;
}