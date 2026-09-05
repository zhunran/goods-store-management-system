package com.fengluan.spi.member.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class MemberProfileUpdateRequest {
    /** 姓名 */
    private String name;
    /** 性别 */
    private String sex;
    /** 出生日期 */
    private LocalDate birthday;
    /** 头像 */
    private String portrait;
    /** 邮箱 */
    private String email;
    /** 手机号 */
    private String phone;
}