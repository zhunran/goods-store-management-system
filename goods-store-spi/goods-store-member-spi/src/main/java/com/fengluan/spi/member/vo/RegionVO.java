package com.fengluan.spi.member.vo;

import lombok.Data;

@Data
public class RegionVO {
    /** 主键 id（数据库 CRI_ID） */
    private Long id;
    /** 上级 id（数据库 CRI_PARENT_ID，省级为 null） */
    private Long parentId;
    /** 行政区划代码（CRI_CODE） */
    private String code;
    /** 名称（CRI_NAME，如：浙江省/杭州市/西湖区） */
    private String name;
    /** 简称（CRI_SHORT_NAME） */
    private String shortName;
    /** 层级（CRI_LEVEL：1 省 / 2 市 / 3 区县） */
    private Integer level;
}