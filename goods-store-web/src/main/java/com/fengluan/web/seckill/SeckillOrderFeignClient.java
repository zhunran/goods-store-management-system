package com.fengluan.web.seckill;

import com.fengluan.spi.seckill.SeckillOrderApi;
import org.springframework.cloud.openfeign.FeignClient;

/** web 秒杀抢购/结果轮询契约（Feign 只能单继承一个父接口） */
@FeignClient(name = "goods-store-seckill-api", contextId = "seckillOrderFeignClient", path = "/seckill/api")
public interface SeckillOrderFeignClient extends SeckillOrderApi {
}