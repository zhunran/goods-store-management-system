package com.fengluan.spi.member.vo;

import lombok.Data;

@Data
public class MemberAddressVO {
    private Long id;
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