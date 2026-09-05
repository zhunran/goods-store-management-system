package com.fengluan.member.util;

import org.springframework.util.StringUtils;

/**
 * 敏感信息脱敏工具
 */
public final class DesensitizeUtil {

    private DesensitizeUtil() {
    }

    /** 手机号 3-4-4：中间 4 位打码 */
    public static String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /** 身份证：仅保留前4后4，中间打码 */
    public static String maskIdCard(String idCard) {
        if (!StringUtils.hasText(idCard) || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 4) + "********" + idCard.substring(idCard.length() - 4);
    }
}