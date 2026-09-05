package com.fengluan.web.trade;

import com.fengluan.spi.trade.dto.OrderCreateRequest;
import com.fengluan.spi.trade.vo.OrderCreateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 下单聚合服务（BFF）：前端已先调购物车，此处直接调下单生成订单号
 */
@Service
@RequiredArgsConstructor
public class WebOrderService {

    private final TradeOrderFeignClient tradeOrderFeignClient;

    public OrderCreateResponse submit(Long memberId, OrderCreateRequest request) {
        return tradeOrderFeignClient.createOrder(request);
    }
}