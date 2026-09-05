package com.fengluan.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 管理员-角色关联，映射 admin_user_role 表。
 */
@Data
@TableName("admin_user_role")
public class AdminUserRoleEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 映射 admin_user_role.user_id */
    @TableField("user_id")
    private Long adminUserId;

    private Long roleId;
}