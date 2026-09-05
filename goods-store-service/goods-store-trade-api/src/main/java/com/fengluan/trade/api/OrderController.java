package com.fengluan.trade.api;

import com.fengluan.spi.trade.OrderApi;
import com.fengluan.spi.trade.dto.OrderCreateRequest;
import com.fengluan.spi.trade.dto.OrderQueryRequest;
import com.fengluan.spi.trade.dto.PageVO;
import com.fengluan.spi.trade.vo.OrderCreateResponse;
import com.fengluan.spi.trade.vo.OrderDetailVO;
import com.fengluan.spi.trade.vo.OrderVO;
import com.fengluan.trade.service.OrderService;
import com.fengluan.trade.util.CurrentUserUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/trade/api")
public class OrderController implements OrderApi {

    private final OrderService orderService;

    @Override
    public OrderCreateResponse createOrder(@Valid @RequestBody OrderCreateRequest request) {
        return orderService.createOrder(CurrentUserUtil.currentUserId(), request);
    }

    @Override
    public PageVO<OrderVO> page(@RequestParam Long pageNum,
                                @RequestParam Long pageSize,
                                @RequestParam(required = false) String status) {
        return orderService.page(CurrentUserUtil.currentUserId(), pageNum, pageSize, status);
    }

    @Override
    public OrderDetailVO detail(@PathVariable Long id) {
        return orderService.detail(CurrentUserUtil.currentUserId(), id);
    }

    @Override
    public Void cancel(@PathVariable Long id) {
        orderService.cancel(CurrentUserUtil.currentUserId(), id);
        return null;
    }

    @Override
    public Void confirm(@PathVariable Long id) {
        orderService.confirm(CurrentUserUtil.currentUserId(), id);
        return null;
    }

    @Override
    public PageVO<OrderVO> adminPage(OrderQueryRequest query) {
        return orderService.adminPage(query);
    }

    @Override
    public OrderDetailVO adminDetail(@PathVariable Long id) {
        return orderService.adminDetail(id);
    }

    @Override
    public Void ship(@PathVariable Long id) {
        orderService.ship(id);
        return null;
    }
}