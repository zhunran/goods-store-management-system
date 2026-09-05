package com.fengluan.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("`order`")
public class OrderEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 订单编号 */
    private String orderNo;
    /** 秒杀编号 */
    private String seckillNo;
    /** 会员账号 */
    private String memberAccount;
    /** 订单总价 */
    private BigDecimal totalPay;
    /** 支付方式 */
    private String payType;
    /** 支付宝交易号 */
    private String alipayTradeNo;
    /** 下单时间 */
    private LocalDateTime checkoutTime;
    /** 支付时间 */
    private LocalDateTime payTime;
    /** 发货时间 */
    private LocalDateTime shipTime;
    /** 确认收货时间 */
    private LocalDateTime acceptTime;
    /** 订单状态 */
    private String status;
    /** 收货人地址编号 */
    private Integer receiverAddrId;
    /** 收货人姓名 */
    private String receiverName;
    /** 收货人手机号 */
    private String receiverPhone;
    /** 收货人地址 */
    private String receiverAddrDetail;
    /** 订单备注 */
    private String orderComment;
    /** 是否逻辑删除 */
    private Boolean isDel;
    /** 备注 */
    private String description;
    /** 创建时间 */
    private LocalDateTime createdTime;
    /** 创建人 */
    private String createdBy;
    /** 最后修改时间 */
    private LocalDateTime updatedTime;
    /** 最后修改人 */
    private String updatedBy;
}