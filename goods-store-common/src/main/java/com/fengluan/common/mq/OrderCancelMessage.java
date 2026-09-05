package com.fengluan.common.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 订单取消消息：超时未支付/用户主动取消后发出，消费者恢复库存
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单号 */
    private String orderNo;

    /** 取消原因：TIMEOUT-超时取消 / USER-用户主动取消 */
    private String reason;

    /** 需恢复库存的商品明细（复用 OrderCreateMessage.Item） */
    private List<OrderCreateMessage.Item> items;
}