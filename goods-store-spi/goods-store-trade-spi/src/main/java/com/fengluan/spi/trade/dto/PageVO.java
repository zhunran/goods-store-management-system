package com.fengluan.spi.trade.dto;

import lombok.Data;

import java.util.List;

/** 通用分页返回（spi 自建，不依赖 common） */
@Data
public class PageVO<T> {
    private Long total;
    private Long pageNum;
    private Long pageSize;
    private List<T> records;
}