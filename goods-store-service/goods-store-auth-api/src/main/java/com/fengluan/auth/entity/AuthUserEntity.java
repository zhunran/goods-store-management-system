package com.fengluan.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 会员账号，映射 member 表（Day3-4 聚焦架构，账号密码字段暂挂此表）。
 */
@Data
@TableName("member")
public class AuthUserEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String account;

    /** BCrypt 加密 */
    private String password;

    private String phone;

    private String email;

    /** 是否启用（member 表实际列为 enabled bit(1)，无 status 列） */
    private Boolean enabled;
}