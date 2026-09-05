package com.fengluan.spi.trade.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemVO {
    private Long goodId;
    private BigDecimal dealPrice;
    private Integer count;
    private String goodName;
    private String goodPic;
}