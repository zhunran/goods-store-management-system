package com.fengluan.spi.trade.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDetailVO {
    private Long id;
    private String orderNo;
    private String memberAccount;
    private BigDecimal totalPay;
    private String payType;
    private String status;
    private LocalDateTime checkoutTime;
    private LocalDateTime payTime;
    private LocalDateTime shipTime;
    private LocalDateTime acceptTime;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddrDetail;
    private String orderComment;
    private List<OrderItemVO> items;
}