package com.fengluan.spi.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 权限视图对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionVO {

    private Long id;

    /** 权限编码，如 good:list、order:ship */
    private String code;

    /** 权限名称 */
    private String name;
}