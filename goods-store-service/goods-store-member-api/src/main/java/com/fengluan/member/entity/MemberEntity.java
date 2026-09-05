package com.fengluan.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("member")
public class MemberEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 用户名 */
    private String account;
    /** 密码 */
    private String password;
    /** 是否启用 */
    private Boolean enabled;
    /** 姓名 */
    private String name;
    /** 拼音 */
    private String pinyin;
    /** 无音调拼音 */
    private String pinyinUntoned;
    /** 名 */
    private String firstName;
    /** 姓 */
    private String lastName;
    /** 性别 */
    private String sex;
    /** 出生日期 */
    private LocalDate birthday;
    /** 身高 cm */
    private Integer height;
    /** 体重 kg */
    private BigDecimal weight;
    /** 智商 */
    private Integer iq;
    /** QQ号 */
    private String qq;
    /** 微信号 */
    private String wechat;
    /** 手机号 */
    private String phone;
    /** 邮箱 */
    private String email;
    /** 籍贯 */
    private Integer nativePlaceId;
    /** 身份证号 */
    private String cardId;
    /** 婚姻状况 */
    private String wedlock;
    /** 政治面貌 */
    private String politicalOrientation;
    /** 家庭住址编号 */
    private Integer addressId;
    /** 地址详情 */
    private String addressDetail;
    /** 民族 */
    private String race;
    /** 宗教 */
    private String religion;
    /** 国籍 */
    private String nationality;
    /** 头像 */
    private String portrait;
    /** 备注 */
    private String description;
    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;
    /** 最后登录IP */
    private String lastLoginIp;
    /** 额外信息 JSON */
    private String extraInfo;
    /** 创建时间 */
    private LocalDateTime createdTime;
    /** 创建人 */
    private String createdBy;
    /** 最后修改时间 */
    private LocalDateTime updatedTime;
    /** 最后修改人 */
    private String updatedBy;
    /** 版本号 */
    private Integer version;
}