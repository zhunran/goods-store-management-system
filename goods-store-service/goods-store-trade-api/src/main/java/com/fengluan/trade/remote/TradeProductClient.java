package com.fengluan.trade.remote;

import com.fengluan.spi.product.ProductApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * 交易服务消费商品契约（extends ProductApi）
 */
@FeignClient(name = "goods-store-product-api", path = "/good/api")
public interface TradeProductClient extends ProductApi {
}