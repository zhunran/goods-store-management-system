package com.fengluan.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付/退款流水（模拟支付，无真实资金流）
 * 幂等兜底：uk_order_biz(order_id, biz_type) 唯一索引，同一订单同一业务类型只允许一条流水
 */
@Data
@TableName("pay_log")
public class PayLogEntity {
    /** 雪花ID */
    @TableId(type = IdType.INPUT)
    private Long id;
    /** 订单ID */
    private Long orderId;
    /** 订单编号 */
    private String orderNo;
    /** 会员账号 */
    private String memberAccount;
    /** 业务类型：PAY-支付 / REFUND-退款 */
    private String bizType;
    /** 渠道：ALIPAY/WECHAT（均模拟） */
    private String channel;
    /** 模拟渠道流水号（雪花生成） */
    private String tradeNo;
    /** 金额 */
    private BigDecimal amount;
    /** 创建时间 */
    private LocalDateTime createdTime;
}
