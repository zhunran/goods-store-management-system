package com.fengluan.spi.trade;

import com.fengluan.spi.trade.dto.OrderCreateRequest;
import com.fengluan.spi.trade.dto.OrderQueryRequest;
import com.fengluan.spi.trade.dto.PageVO;
import com.fengluan.spi.trade.vo.OrderCreateResponse;
import com.fengluan.spi.trade.vo.OrderDetailVO;
import com.fengluan.spi.trade.vo.OrderVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 交易服务-订单管理 纯 HTTP 契约
 * 相对路径基于实现方 Controller 类级前缀 /trade/api（OrderController）。
 * 当前会员由实现方从 X-User-Id 解析。
 */
public interface OrderApi {

    /** 下单，返回订单号 */
    @PostMapping("/order")
    OrderCreateResponse createOrder(@Valid @RequestBody OrderCreateRequest request);

    /** 订单分页（当前会员） */
    @GetMapping("/order/page")
    PageVO<OrderVO> page(@RequestParam Long pageNum,
                         @RequestParam Long pageSize,
                         @RequestParam(required = false) String status);

    /** 订单详情（含明细） */
    @GetMapping("/order/{id}")
    OrderDetailVO detail(@PathVariable Long id);

    /** 取消订单（仅待付款） */
    @PutMapping("/order/{id}/cancel")
    Void cancel(@PathVariable Long id);

    /** 确认收货（仅已发货） */
    @PutMapping("/order/{id}/confirm")
    Void confirm(@PathVariable Long id);

    // —— 管理端 ——
    /** 全部订单分页（管理端） */
    @GetMapping("/order/admin/page")
    PageVO<OrderVO> adminPage(OrderQueryRequest query);

    /** 订单详情（管理端，不按会员过滤） */
    @GetMapping("/order/admin/{id}")
    OrderDetailVO adminDetail(@PathVariable Long id);

    /** 发货（仅已支付） */
    @PutMapping("/order/{id}/ship")
    Void ship(@PathVariable Long id);
}