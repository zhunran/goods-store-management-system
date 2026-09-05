package com.fengluan.seckill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

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
}