package com.fengluan.spi.trade.dto;

import lombok.Data;

@Data
public class OrderQueryRequest {
    /** 当前页码，默认第一页 */
    private Long pageNum = 1L;
    /** 每页条数，默认10条 */
    private Long pageSize = 10L;
    /** 订单编号 */
    private String orderNo;
    /** 会员账号 */
    private String memberAccount;
    /** 订单状态 */
    private String status;
}