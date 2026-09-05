package com.fengluan.spi.trade.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 下单结果 VO
 */
@Data
public class OrderCreateResponse {
    /** 订单号（雪花算法） */
    private String orderNo;
    /** 订单总价 */
    private BigDecimal totalPay;
    /** 订单状态 */
    private String status;
}