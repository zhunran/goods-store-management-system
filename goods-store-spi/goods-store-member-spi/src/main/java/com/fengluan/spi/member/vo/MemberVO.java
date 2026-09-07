package com.fengluan.spi.member.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class MemberVO {
    private Long id;
    private String account;
    private String name;
    private String pinyin;
    private String sex;
    private LocalDate birthday;
    /** 是否启用（管理端启用/禁用） */
    private Boolean enabled;
    /** 手机号（脱敏值，3-4-4） */
    private String phone;
    /** 身份证号脱敏（仅保留前后4位） */
    private String maskedCardId;
    private String email;
    private String portrait;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}