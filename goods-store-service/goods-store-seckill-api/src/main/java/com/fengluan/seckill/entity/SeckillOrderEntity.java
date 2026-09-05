package com.fengluan.seckill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 秒杀订单（映射共享库 order 表，秒杀即时成交语义；order 为保留字须反引号转义） */
@Data
@TableName("`order`")
public class SeckillOrderEntity {
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
    /** 订单状态（PENDING=10） */
    private String status;
    /** 下单时间 */
    private LocalDateTime checkoutTime;
    /** 创建时间 */
    private LocalDateTime createdTime;
    /** 最后修改时间 */
    private LocalDateTime updatedTime;
    /** 是否逻辑删除 */
    private Boolean isDel;
}