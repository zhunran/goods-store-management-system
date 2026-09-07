package com.fengluan.trade.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 订单状态：待付款10 已支付20 已发货30 已完成40 已取消50 已退款60 */
@Getter
@AllArgsConstructor
public enum OrderStatus {
    PENDING("10", "待付款"),
    PAID("20", "已支付"),
    SHIPPED("30", "已发货"),
    COMPLETED("40", "已完成"),
    CANCELLED("50", "已取消"),
    REFUNDED("60", "已退款");

    private final String code;
    private final String desc;

    public static OrderStatus of(String code) {
        for (OrderStatus s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        throw new IllegalArgumentException("未知订单状态: " + code);
    }
}