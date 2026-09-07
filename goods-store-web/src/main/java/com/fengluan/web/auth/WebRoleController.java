package com.fengluan.web.auth;

import com.fengluan.common.result.ApiResult;
import com.fengluan.spi.auth.dto.RolePermissionAssignRequest;
import com.fengluan.spi.auth.vo.PermissionVO;
import com.fengluan.spi.auth.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色管理聚合入口：/app/api/role/**（管理端，仅 role:manage 权限可见/可调用）。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/app/api/role")
public class WebRoleController {

    private final RoleFeignClient roleFeignClient;

    @GetMapping("/list")
    public ApiResult<List<RoleVO>> listRoles() {
        return ApiResult.success(roleFeignClient.listRoles());
    }

    @GetMapping("/permission/list")
    public ApiResult<List<PermissionVO>> listPermissions() {
        return ApiResult.success(roleFeignClient.listPermissions());
    }

    @GetMapping("/{roleId}/permissions")
    public ApiResult<List<Long>> listRolePermissions(@PathVariable Long roleId) {
        return ApiResult.success(roleFeignClient.listRolePermissions(roleId));
    }

    @PutMapping("/{roleId}/permissions")
    public ApiResult<Void> assignRolePermissions(@PathVariable Long roleId,
                                                 @RequestBody RolePermissionAssignRequest request) {
        roleFeignClient.assignRolePermissions(roleId, request);
        return ApiResult.success();
    }
}