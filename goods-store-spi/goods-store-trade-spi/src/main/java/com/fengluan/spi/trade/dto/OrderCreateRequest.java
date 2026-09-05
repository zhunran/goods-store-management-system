package com.fengluan.spi.trade.dto;

import lombok.Data;

/**
 * 下单请求
 */
@Data
public class OrderCreateRequest {
    /** 收货地址 id（缺省取默认地址） */
    private Long addressId;
    /** 订单备注 */
    private String comment;
}