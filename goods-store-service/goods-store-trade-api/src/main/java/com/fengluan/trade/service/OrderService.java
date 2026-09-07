package com.fengluan.trade.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.fengluan.spi.trade.dto.OrderCreateRequest;
import com.fengluan.spi.trade.dto.OrderQueryRequest;
import com.fengluan.spi.trade.dto.PageVO;
import com.fengluan.spi.trade.vo.OrderCreateResponse;
import com.fengluan.spi.trade.vo.OrderDetailVO;
import com.fengluan.spi.trade.vo.OrderVO;
import com.fengluan.trade.entity.OrderEntity;

public interface OrderService extends IService<OrderEntity> {

    OrderCreateResponse createOrder(Long memberId, OrderCreateRequest request);

    PageVO<OrderVO> page(Long memberId, Long pageNum, Long pageSize, String status);

    OrderDetailVO detail(Long memberId, Long id);

    void cancel(Long memberId, Long id);

    void confirm(Long memberId, Long id);

    /** 模拟支付（仅待付款；CAS 条件更新 + 支付流水落库） */
    void pay(Long memberId, Long id, String payType);

    /** 申请退款（仅已支付未发货，当前会员） */
    void refund(Long memberId, Long id);

    // —— 管理端 ——
    PageVO<OrderVO> adminPage(OrderQueryRequest query);

    OrderDetailVO adminDetail(Long id);

    void ship(Long id);

    /** 退款（管理端，不按会员过滤，仅已支付未发货） */
    void adminRefund(Long id);
}