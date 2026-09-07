package com.fengluan.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 行政区划映射实体，表为全大写列名（CRI_*），需显式指定列。
 */
@Data
@TableName("t_cn_region_info")
public class RegionEntity {

    @TableId(value = "CRI_ID", type = IdType.AUTO)
    private Long id;

    @TableField("CRI_PARENT_ID")
    private Long parentId;

    @TableField("CRI_CODE")
    private String code;

    @TableField("CRI_NAME")
    private String name;

    @TableField("CRI_SHORT_NAME")
    private String shortName;

    @TableField("CRI_LEVEL")
    private Integer level;

    @TableField("CRI_SORT")
    private Integer sort;

    @TableField("CRI_DATA_STATE")
    private Integer dataState;
}