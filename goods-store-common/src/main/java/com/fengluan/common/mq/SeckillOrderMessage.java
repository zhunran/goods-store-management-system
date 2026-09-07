package com.fengluan.common.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillOrderMessage {
    private static final long serialVersionUID=1L;
    //秒杀活动ID
    private Long seckillId;
    //秒杀商品ID
    private Long seckillGoodId;
    //商品ID
    private Long goodId;
    //会员Id
    private Long memberId;
    //秒杀价格
    private BigDecimal seckillPrice;
    //生成的订单号
    private String orderNo;
}
