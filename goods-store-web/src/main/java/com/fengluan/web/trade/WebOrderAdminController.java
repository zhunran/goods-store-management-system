package com.fengluan.web.trade;

import com.fengluan.common.result.ApiResult;
import com.fengluan.spi.trade.dto.OrderQueryRequest;
import com.fengluan.spi.trade.dto.PageVO;
import com.fengluan.spi.trade.vo.OrderDetailVO;
import com.fengluan.spi.trade.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单管理聚合入口：/app/api/order/admin/**（管理端，需 admin 角色）
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/app/api/order/admin")
public class WebOrderAdminController {

    private final WebOrderMgrService webOrderMgrService;

    @GetMapping("/page")
    public ApiResult<PageVO<OrderVO>> page(OrderQueryRequest query) {
        return ApiResult.success(webOrderMgrService.adminPage(query));
    }

    @GetMapping("/{id}")
    public ApiResult<OrderDetailVO> detail(@PathVariable Long id) {
        return ApiResult.success(webOrderMgrService.adminDetail(id));
    }

    @PutMapping("/{id}/ship")
    public ApiResult<Void> ship(@PathVariable Long id) {
        webOrderMgrService.ship(id);
        return ApiResult.success();
    }
}
