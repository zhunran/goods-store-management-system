package com.fengluan.spi.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 角色视图对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleVO {

    private Long id;

    /** 角色编码，如 ROLE_ADMIN / ROLE_OPERATOR */
    private String code;

    /** 角色名称 */
    private String name;
}