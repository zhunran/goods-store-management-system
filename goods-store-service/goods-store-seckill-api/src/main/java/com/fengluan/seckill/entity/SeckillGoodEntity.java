package com.fengluan.seckill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("seckill_good")
public class SeckillGoodEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 秒杀活动编号 */
    private Integer seckillId;
    /** 商品编号 */
    private Integer goodId;
    /** 备注 */
    private String description;
    /** 秒杀价 */
    private BigDecimal seckillPrice;
    /** 限量库存 */
    private Integer stockCount;
    /** 已售（DB 账本） */
    private Integer stockSold;
}