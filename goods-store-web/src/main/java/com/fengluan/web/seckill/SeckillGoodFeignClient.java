package com.fengluan.web.seckill;

import com.fengluan.spi.seckill.SeckillGoodApi;
import org.springframework.cloud.openfeign.FeignClient;

/** web 秒杀浏览契约（Feign 只能单继承一个父接口） */
@FeignClient(name = "goods-store-seckill-api", contextId = "seckillGoodFeignClient", path = "/seckill/api")
public interface SeckillGoodFeignClient extends SeckillGoodApi {
}