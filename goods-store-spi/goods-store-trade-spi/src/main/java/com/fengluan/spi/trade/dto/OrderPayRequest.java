package com.fengluan.spi.trade.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 模拟支付请求
 */
@Data
public class OrderPayRequest {
    /** 支付方式：ALIPAY / WECHAT（均为模拟渠道） */
    @NotBlank(message = "支付方式不能为空")
    private String payType;
}
