package com.fengluan.spi.auth;

import com.fengluan.spi.auth.dto.RolePermissionAssignRequest;
import com.fengluan.spi.auth.vo.PermissionVO;
import com.fengluan.spi.auth.vo.RoleVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 角色管理纯 HTTP 契约（相对路径基于实现方类级前缀 /auth/api）。
 * 角色与权限数据均落在 auth-api 的 RBAC 五表中；本组接口仅供管理端角色管理页调用。
 */
public interface RoleApi {

    /** 角色列表 */
    @GetMapping("/role/list")
    List<RoleVO> listRoles();

    /** 全量权限列表（供权限分配穿梭框展示） */
    @GetMapping("/permission/list")
    List<PermissionVO> listPermissions();

    /** 某角色已绑定的权限 ID 集合 */
    @GetMapping("/role/{roleId}/permissions")
    List<Long> listRolePermissions(@PathVariable("roleId") Long roleId);

    /** 全量覆盖某角色的权限集合 */
    @PutMapping("/role/{roleId}/permissions")
    Void assignRolePermissions(@PathVariable("roleId") Long roleId,
                               @RequestBody RolePermissionAssignRequest request);
}