package com.fengluan.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("spec_group")
public class SpecGroupEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 规格组名称 */
    private String name;
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