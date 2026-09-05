package com.fengluan.seckill.service;

import com.fengluan.spi.seckill.dto.SeckillOrderResponse;
import com.fengluan.spi.seckill.dto.SeckillOrderResultVO;

public interface SeckillOrderService {

    /** 抢购：原子预减库存 → 发 MQ 异步建单 → 返回排队中 */
    SeckillOrderResponse seckill(Long seckillGoodId, Long memberId);

    /** 秒杀结果轮询：有单=SUCCESS(已抢到)，无单=PROCESSING(排队中) */
    SeckillOrderResultVO result(String orderNo, Long memberId);
}