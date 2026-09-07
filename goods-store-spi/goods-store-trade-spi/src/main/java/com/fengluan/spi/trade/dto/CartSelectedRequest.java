package com.fengluan.spi.trade.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 购物车勾选请求：一次请求支持全选/全不选/部分勾选
 */
@Data
public class CartSelectedRequest {
    /** 目标购物车项 id 列表 */
    @NotEmpty(message = "购物车项不能为空")
    private List<Long> cartIds;
    /** 是否选中 */
    @NotNull(message = "选中状态不能为空")
    private Boolean selected;
}