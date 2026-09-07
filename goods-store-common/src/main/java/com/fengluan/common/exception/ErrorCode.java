package com.fengluan.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // 通用错误
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或 Token 已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 业务错误（按模块分段，后续逐步扩充）
    // 会员 1xxx
    MEMBER_NOT_FOUND(1001, "会员不存在"),
    MEMBER_ACCOUNT_EXISTS(1002, "账号已存在"),
    MEMBER_PASSWORD_ERROR(1003, "密码错误"),
    MEMBER_DISABLED(1004, "账号已被禁用"),
    MEMBER_ADDRESS_NOT_FOUND(1005, "收货地址不存在"),

    // 商品 2xxx
    GOOD_NOT_FOUND(2001, "商品不存在"),
    GOOD_STOCK_INSUFFICIENT(2002, "库存不足"),
    CATEGORY_NOT_FOUND(2003, "分类不存在"),

    // 交易 3xxx
    ORDER_NOT_FOUND(3001, "订单不存在"),
    ORDER_STATUS_ERROR(3002, "订单状态异常"),
    CART_EMPTY(3003, "购物车为空"),

    // 秒杀 4xxx
    SECKILL_NOT_FOUND(4001, "秒杀活动不存在"),
    SECKILL_NOT_STARTED(4002, "秒杀尚未开始"),
    SECKILL_ENDED(4003, "秒杀已结束"),
    SECKILL_STOCK_EMPTY(4004, "秒杀库存已空"),
    SECKILL_ALREADY(4005, "您已参与过该秒杀"),

    // 品牌 5xxx
    BRAND_NOT_FOUND(5001, "品牌不存在"),
    BRAND_EXISTS(5002, "品牌名称已存在"),

    // 认证 6xxx
    AUTH_USER_NOT_FOUND(6001, "管理员不存在"),
    AUTH_PASSWORD_ERROR(6002, "密码错误"),
    AUTH_USER_DISABLED(6003, "管理员已被禁用"),
    ROLE_NOT_FOUND(6004, "角色不存在"),
    PERMISSION_NOT_FOUND(6005, "权限不存在"),
    CAPTCHA_REQUIRED(6006, "请输入验证码"),
    CAPTCHA_ERROR(6007, "验证码错误或已过期");
    private final Integer code;
    private final String message;

}
