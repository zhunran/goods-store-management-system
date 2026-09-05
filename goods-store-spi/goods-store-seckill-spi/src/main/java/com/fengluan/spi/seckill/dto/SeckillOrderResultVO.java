package com.fengluan.spi.seckill.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 秒杀结果轮询 VO：有单=SUCCESS(已抢到)，无单=PROCESSING(排队中) */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillOrderResultVO {
    /** 秒杀订单号 */
    private String orderNo;
    /** 状态：PROCESSING=排队中 / SUCCESS=已抢到 */
    private String status;
    /** 提示信息 */
    private String message;
}