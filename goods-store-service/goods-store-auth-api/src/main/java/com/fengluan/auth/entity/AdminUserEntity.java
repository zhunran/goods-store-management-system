package com.fengluan.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 管理员，映射 admin_user 表。
 */
@Data
@TableName("admin_user")
public class AdminUserEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** BCrypt 加密 */
    private String password;

    /** 1=正常 0=禁用（映射 admin_user.enabled） */
    @TableField("enabled")
    private Integer status;
}