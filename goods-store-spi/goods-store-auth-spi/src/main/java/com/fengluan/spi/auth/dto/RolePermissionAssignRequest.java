package com.fengluan.spi.auth.dto;

import lombok.Data;

import java.util.List;

/**
 * 角色权限分配请求：一次请求全量覆盖该角色的权限集合。
 */
@Data
public class RolePermissionAssignRequest {

    /** 授权给该角色的权限 ID 集合（空列表表示清空权限） */
    private List<Long> permissionIds;
}