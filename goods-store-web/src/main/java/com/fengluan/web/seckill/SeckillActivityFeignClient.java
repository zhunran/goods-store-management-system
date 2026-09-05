package com.fengluan.web.seckill;

import com.fengluan.spi.seckill.SeckillActivityApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * web 层秒杀活动管理 Feign 客户端：extends seckill 活动纯契约
 */
@FeignClient(name = "goods-store-seckill-api", contextId = "seckillActivityFeignClient", path = "/seckill/api")
public interface SeckillActivityFeignClient extends SeckillActivityApi {
}
