package com.fengluan.spi.seckill.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 秒杀抢购响应：orderNo/status(PENDING)/message */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillOrderResponse {
    /** 秒杀订单号 */
    private String orderNo;
    /** 状态：PENDING=排队中 */
    private String status;
    /** 提示信息 */
    private String message;
}