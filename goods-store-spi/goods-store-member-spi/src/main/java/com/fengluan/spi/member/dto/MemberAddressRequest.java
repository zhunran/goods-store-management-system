package com.fengluan.spi.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MemberAddressRequest {
    /** 收货人姓名 */
    @NotBlank(message = "收货人不能为空")
    private String receiver;
    /** 手机号 */
    @NotBlank(message = "手机号不能为空")
    private String phone;
    /** 街道编号 */
    private Integer addrId;
    /** 地址详情 */
    private String addrDetail;
    /** 是否设为默认地址 */
    private Boolean isDefault;
}