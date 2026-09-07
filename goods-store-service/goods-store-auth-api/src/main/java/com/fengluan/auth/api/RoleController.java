package com.fengluan.auth.api;

import com.fengluan.auth.service.RoleService;
import com.fengluan.spi.auth.RoleApi;
import com.fengluan.spi.auth.dto.RolePermissionAssignRequest;
import com.fengluan.spi.auth.vo.PermissionVO;
import com.fengluan.spi.auth.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色管理控制器：兑现 spi 契约 RoleApi（角色列表 / 权限列表 / 角色权限分配）。
 * 仅拥有 role:manage 权限的管理员（ROLE_ADMIN）可访问。
 */
@RestController
@RequestMapping("/auth/api")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('role:manage')")
public class RoleController implements RoleApi {

    private final RoleService roleService;

    @Override
    public List<RoleVO> listRoles() {
        return roleService.listRoles();
    }

    @Override
    public List<PermissionVO> listPermissions() {
        return roleService.listPermissions();
    }

    @Override
    public List<Long> listRolePermissions(Long roleId) {
        return roleService.listRolePermissions(roleId);
    }

    @Override
    public Void assignRolePermissions(Long roleId, RolePermissionAssignRequest request) {
        return roleService.assignRolePermissions(roleId, request);
    }
}