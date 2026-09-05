package com.fengluan.web.trade;

import com.fengluan.spi.trade.OrderApi;
import com.fengluan.spi.trade.dto.OrderQueryRequest;
import com.fengluan.spi.trade.dto.PageVO;
import com.fengluan.spi.trade.vo.OrderVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * web 层下单 Feign 客户端：extends trade 下单纯契约
 */
@FeignClient(name = "goods-store-trade-api", contextId = "tradeOrderFeignClient", path = "/trade/api")
public interface TradeOrderFeignClient extends OrderApi {

    /**
     * GET 请求的 POJO 参数必须用 @SpringQueryMap 展开为 query 参数。
     */
    @GetMapping("/order/admin/page")
    PageVO<OrderVO> adminPage(@SpringQueryMap OrderQueryRequest query);
}
