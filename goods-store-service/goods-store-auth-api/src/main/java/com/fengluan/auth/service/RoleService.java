package com.fengluan.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fengluan.auth.entity.PermissionEntity;
import com.fengluan.auth.entity.RoleEntity;
import com.fengluan.auth.entity.RolePermissionEntity;
import com.fengluan.auth.repository.PermissionMapper;
import com.fengluan.auth.repository.RoleMapper;
import com.fengluan.auth.repository.RolePermissionMapper;
import com.fengluan.common.exception.BusinessException;
import com.fengluan.common.exception.ErrorCode;
import com.fengluan.spi.auth.dto.RolePermissionAssignRequest;
import com.fengluan.spi.auth.vo.PermissionVO;
import com.fengluan.spi.auth.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色管理服务：角色列表 / 权限列表 / 角色权限分配（管理端角色管理页）。
 */
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;

    public List<RoleVO> listRoles() {
        return roleMapper.selectList(null).stream()
                .map(r -> RoleVO.builder().id(r.getId()).code(r.getCode()).name(r.getName()).build())
                .toList();
    }

    public List<PermissionVO> listPermissions() {
        return permissionMapper.selectList(null).stream()
                .map(p -> PermissionVO.builder().id(p.getId()).code(p.getCode()).name(p.getName()).build())
                .toList();
    }

    public List<Long> listRolePermissions(Long roleId) {
        ensureRole(roleId);
        return rolePermissionMapper.selectList(new LambdaQueryWrapper<RolePermissionEntity>()
                        .eq(RolePermissionEntity::getRoleId, roleId))
                .stream().map(RolePermissionEntity::getPermissionId).toList();
    }

    @Transactional
    public Void assignRolePermissions(Long roleId, RolePermissionAssignRequest request) {
        ensureRole(roleId);
        // 全量覆盖：先删后插，空集合即清空权限
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermissionEntity>()
                .eq(RolePermissionEntity::getRoleId, roleId));
        List<Long> permissionIds = request.getPermissionIds();
        if (permissionIds != null) {
            for (Long permissionId : permissionIds.stream().distinct().toList()) {
                PermissionEntity perm = permissionMapper.selectById(permissionId);
                if (perm == null) {
                    throw new BusinessException(ErrorCode.PERMISSION_NOT_FOUND);
                }
                RolePermissionEntity rel = new RolePermissionEntity();
                rel.setRoleId(roleId);
                rel.setPermissionId(permissionId);
                rolePermissionMapper.insert(rel);
            }
        }
        return null;
    }

    private void ensureRole(Long roleId) {
        if (roleMapper.selectById(roleId) == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
    }
}