package com.fengluan.spi.seckill;

import com.fengluan.spi.seckill.dto.SeckillOrderResponse;
import com.fengluan.spi.seckill.dto.SeckillOrderResultVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 秒杀抢购纯 HTTP 契约：抢购 + 结果轮询。
 * 相对路径基于实现方 Controller 类级前缀 /seckill/api（SeckillOrderController）；
 * 当前用户 id 由网关透传 X-User-Id，实现方从 RequestContextHolder 读取。
 */
public interface SeckillOrderApi {

    /** 抢购秒杀商品，返回排队中（MQ 异步建单） */
    @PostMapping("/order/{seckillGoodId}")
    SeckillOrderResponse seckill(@PathVariable Long seckillGoodId);

    /** 秒杀结果轮询：有单=SUCCESS(已抢到)，无单=PROCESSING(排队中) */
    @GetMapping("/order/{orderNo}/result")
    SeckillOrderResultVO result(@PathVariable String orderNo);
}