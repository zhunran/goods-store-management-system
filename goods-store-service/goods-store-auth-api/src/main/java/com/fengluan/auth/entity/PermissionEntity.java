package com.fengluan.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 权限，映射 permission 表。
 */
@Data
@TableName("permission")
public class PermissionEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 权限编码，如 brand:read、brand:write */
    private String code;

    /** 权限名称 */
    private String name;
}