package com.fengluan.web.trade;

import com.fengluan.spi.trade.CartApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * web 层购物车 Feign 客户端：extends trade 购物车纯契约
 */
@FeignClient(name = "goods-store-trade-api", contextId = "tradeCartFeignClient", path = "/trade/api")
public interface TradeCartFeignClient extends CartApi {
}