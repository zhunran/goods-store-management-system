package com.fengluan.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("spec_item")
public class SpecItemEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 规格组编号 */
    private Integer specGroupId;
    /** 规格名称 */
    private String name;
    /** 规格值 */
    private String value;
    /** 备注 */
    private String description;
}