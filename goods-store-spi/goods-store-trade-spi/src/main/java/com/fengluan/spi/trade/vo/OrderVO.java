package com.fengluan.spi.trade.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderVO {
    private Long id;
    private String orderNo;
    private String memberAccount;
    private BigDecimal totalPay;
    private String payType;
    private String status;
    private LocalDateTime checkoutTime;
    private LocalDateTime payTime;
    private LocalDateTime shipTime;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}