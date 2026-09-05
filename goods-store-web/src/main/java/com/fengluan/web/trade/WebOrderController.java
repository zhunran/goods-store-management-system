package com.fengluan.web.trade;

import com.fengluan.common.result.ApiResult;
import com.fengluan.spi.trade.dto.OrderCreateRequest;
import com.fengluan.spi.trade.vo.OrderCreateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * web 层下单聚合入口：/app/api/order/submit
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/app/api/order")
public class WebOrderController {

    private final WebOrderService webOrderService;

    @PostMapping("/submit")
    public ApiResult<OrderCreateResponse> submit(@RequestHeader(value = "X-User-Id", required = false) Long memberId,
                                                 @RequestBody OrderCreateRequest request) {
        return ApiResult.success(webOrderService.submit(memberId, request));
    }
}