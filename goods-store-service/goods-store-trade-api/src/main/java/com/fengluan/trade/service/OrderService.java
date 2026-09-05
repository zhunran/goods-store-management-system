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

    // —— 管理端 ——
    PageVO<OrderVO> adminPage(OrderQueryRequest query);

    OrderDetailVO adminDetail(Long id);

    void ship(Long id);
}