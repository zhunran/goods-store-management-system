package com.fengluan.seckill.remote;

import com.fengluan.spi.product.ProductApi;
import org.springframework.cloud.openfeign.FeignClient;

/** seckill 消费 product：仅 extends 契约 */
@FeignClient(name = "goods-store-product-api", contextId = "seckillProductClient", path = "/good/api")
public interface SeckillProductClient extends ProductApi {
}