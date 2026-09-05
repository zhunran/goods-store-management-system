package com.fengluan.seckill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("seckill")
public class SeckillEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 秒杀名称 */
    private String name;
    /** 是否开启 */
    private Boolean enabled;
    /** 开始时间 */
    private LocalDateTime startTime;
    /** 结束时间 */
    private LocalDateTime endTime;
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