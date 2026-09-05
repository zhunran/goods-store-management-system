package com.fengluan.common.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;


/**
 * 订单创建消息，下单成功后由trade-api发出，消费者完成坤村扣减
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateMessage implements Serializable {
    private static final long serialVersionUID=1L;
    //订单号
    private String  orderNo;
    //会员ID
    private Long memberId;
    //订单明细
    private List<Item> items;


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item implements Serializable{
        private static final long serialVersionUID=1L;
        //商品ID
        private  Long goodId;
        //sku ID
        private Long skuId;
        //购买数量
        private Integer count;
    }
}
