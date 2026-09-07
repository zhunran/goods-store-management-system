package com.fengluan.spi.trade.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 购物车批量操作请求（批量删除）
 */
@Data
public class CartBatchRequest {
    /** 目标购物车项 id 列表 */
    @NotEmpty(message = "购物车项不能为空")
    private List<Long> cartIds;
}