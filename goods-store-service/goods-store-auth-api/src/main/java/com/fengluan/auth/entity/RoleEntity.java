package com.fengluan.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色，映射 role 表。
 */
@Data
@TableName("role")
public class RoleEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色编码，如 ADMIN、OPERATOR */
    private String code;

    /** 角色名称 */
    private String name;
}