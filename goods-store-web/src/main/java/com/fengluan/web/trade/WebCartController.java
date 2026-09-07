package com.fengluan.web.trade;

import com.fengluan.common.result.ApiResult;
import com.fengluan.spi.trade.dto.CartAddRequest;
import com.fengluan.spi.trade.dto.CartBatchRequest;
import com.fengluan.spi.trade.dto.CartSelectedRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * web 层购物车聚合入口：/app/api/cart/**
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/app/api/cart")
public class WebCartController {

    private final WebCartService webCartService;

    @GetMapping("/list")
    public ApiResult<List<CartItemVO>> list(@RequestHeader(value = "X-User-Id", required = false) Long memberId) {
        return ApiResult.success(webCartService.list(memberId));
    }

    @PostMapping("/add")
    public ApiResult<Void> add(@RequestHeader(value = "X-User-Id", required = false) Long memberId,
                               @RequestBody CartAddRequest request) {
        webCartService.add(memberId, request);
        return ApiResult.success();
    }

    @PutMapping("/{cartId}")
    public ApiResult<Void> updateQty(@RequestHeader(value = "X-User-Id", required = false) Long memberId,
                                     @PathVariable Long cartId,
                                     @RequestParam Integer qty) {
        webCartService.updateQty(memberId, cartId, qty);
        return ApiResult.success();
    }

    @PutMapping("/selected")
    public ApiResult<Void> updateSelected(@RequestHeader(value = "X-User-Id", required = false) Long memberId,
                                          @RequestBody CartSelectedRequest request) {
        webCartService.updateSelected(memberId, request);
        return ApiResult.success();
    }

    @DeleteMapping("/batch")
    public ApiResult<Void> removeBatch(@RequestHeader(value = "X-User-Id", required = false) Long memberId,
                                       @RequestBody CartBatchRequest request) {
        webCartService.removeBatch(memberId, request);
        return ApiResult.success();
    }

    @DeleteMapping("/{cartId}")
    public ApiResult<Void> remove(@RequestHeader(value = "X-User-Id", required = false) Long memberId,
                                  @PathVariable Long cartId) {
        webCartService.remove(memberId, cartId);
        return ApiResult.success();
    }
}