package com.fengluan.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("member_address")
public class MemberAddressEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 会员账号 */
    private String memberAccount;
    /** 收货人姓名 */
    private String receiver;
    /** 手机号 */
    private String phone;
    /** 街道编号 */
    private Integer addrId;
    /** 地址详情 */
    private String addrDetail;
    /** 是否默认地址 */
    private Boolean isDefault;
}