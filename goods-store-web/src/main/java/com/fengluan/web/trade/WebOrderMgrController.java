package com.fengluan.web.trade;

import com.fengluan.common.result.ApiResult;
import com.fengluan.spi.trade.dto.OrderPayRequest;
import com.fengluan.spi.trade.dto.PageVO;
import com.fengluan.spi.trade.vo.OrderDetailVO;
import com.fengluan.spi.trade.vo.OrderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * web 层订单管理聚合入口：/app/api/order/page|detail|cancel|confirm
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/app/api/order")
public class WebOrderMgrController {

    private final WebOrderMgrService webOrderMgrService;

    @GetMapping("/page")
    public ApiResult<PageVO<OrderVO>> page(@RequestHeader(value = "X-User-Id", required = false) Long memberId,
                                           @RequestParam Long pageNum,
                                           @RequestParam Long pageSize,
                                           @RequestParam(required = false) String status) {
        return ApiResult.success(webOrderMgrService.page(memberId, pageNum, pageSize, status));
    }

    @GetMapping("/{id}")
    public ApiResult<OrderDetailVO> detail(@RequestHeader(value = "X-User-Id", required = false) Long memberId,
                                           @PathVariable Long id) {
        return ApiResult.success(webOrderMgrService.detail(memberId, id));
    }

    @PutMapping("/{id}/cancel")
    public ApiResult<Void> cancel(@RequestHeader(value = "X-User-Id", required = false) Long memberId,
                                  @PathVariable Long id) {
        webOrderMgrService.cancel(memberId, id);
        return ApiResult.success(null);
    }

    @PutMapping("/{id}/confirm")
    public ApiResult<Void> confirm(@RequestHeader(value = "X-User-Id", required = false) Long memberId,
                                   @PathVariable Long id) {
        webOrderMgrService.confirm(memberId, id);
        return ApiResult.success(null);
    }

    @PutMapping("/{id}/pay")
    public ApiResult<Void> pay(@RequestHeader(value = "X-User-Id", required = false) Long memberId,
                               @PathVariable Long id,
                               @Valid @RequestBody OrderPayRequest request) {
        webOrderMgrService.pay(memberId, id, request);
        return ApiResult.success(null);
    }

    @PutMapping("/{id}/refund")
    public ApiResult<Void> refund(@RequestHeader(value = "X-User-Id", required = false) Long memberId,
                                  @PathVariable Long id) {
        webOrderMgrService.refund(memberId, id);
        return ApiResult.success(null);
    }
}