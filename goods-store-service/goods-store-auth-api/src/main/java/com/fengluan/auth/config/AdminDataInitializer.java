package com.fengluan.auth.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fengluan.auth.entity.AdminUserEntity;
import com.fengluan.auth.entity.AdminUserRoleEntity;
import com.fengluan.auth.entity.PermissionEntity;
import com.fengluan.auth.entity.RoleEntity;
import com.fengluan.auth.entity.RolePermissionEntity;
import com.fengluan.auth.repository.AdminUserMapper;
import com.fengluan.auth.repository.AdminUserRoleMapper;
import com.fengluan.auth.repository.PermissionMapper;
import com.fengluan.auth.repository.RoleMapper;
import com.fengluan.auth.repository.RolePermissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 管理员种子数据初始化：确保存在 admin/123456 超级管理员（ROLE_ADMIN）及其权限，
 * 供管理端登录演示使用。仅在表为空时写入，幂等。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminDataInitializer implements CommandLineRunner {

    private final AdminUserMapper adminUserMapper;
    private final AdminUserRoleMapper adminUserRoleMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        RoleEntity adminRole = ensureRole("ROLE_ADMIN", "超级管理员");
        RoleEntity operatorRole = ensureRole("ROLE_OPERATOR", "运营");

        // ADMIN：全量权限（含角色管理）；OPERATOR：无秒杀/会员/角色管理
        bindCodes(adminRole, List.of(
                "dashboard:view",
                "good:list", "good:create", "good:update", "good:delete",
                "brand:list", "brand:create", "brand:update", "brand:delete",
                "seckill:list", "seckill:create", "seckill:update", "seckill:delete",
                "order:list", "order:ship", "order:refund",
                "member:list",
                "role:manage"
        ));
        bindCodes(operatorRole, List.of(
                "dashboard:view",
                "good:list", "good:create", "good:update", "good:delete",
                "brand:list", "brand:create", "brand:update", "brand:delete",
                "order:list", "order:ship", "order:refund"
        ));

        createAdminUser("admin", adminRole);
        createAdminUser("operator", operatorRole);
    }

    private void bindCodes(RoleEntity role, List<String> permissionCodes) {
        for (String code : permissionCodes) {
            PermissionEntity perm = ensurePermission(code, code);
            bindRolePermission(role.getId(), perm.getId());
        }
    }

    private void createAdminUser(String username, RoleEntity role) {
        AdminUserEntity user = adminUserMapper.selectOne(new LambdaQueryWrapper<AdminUserEntity>()
                .eq(AdminUserEntity::getUsername, username));
        if (user == null) {
            user = new AdminUserEntity();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode("123456"));
            user.setStatus(1);
            adminUserMapper.insert(user);
            log.info("[init] 已创建管理员 {} / 123456", username);
        }
        Long count = adminUserRoleMapper.selectCount(new LambdaQueryWrapper<AdminUserRoleEntity>()
                .eq(AdminUserRoleEntity::getAdminUserId, user.getId())
                .eq(AdminUserRoleEntity::getRoleId, role.getId()));
        if (count == null || count == 0) {
            AdminUserRoleEntity rel = new AdminUserRoleEntity();
            rel.setAdminUserId(user.getId());
            rel.setRoleId(role.getId());
            adminUserRoleMapper.insert(rel);
        }
    }

    private RoleEntity ensureRole(String code, String name) {
        RoleEntity role = roleMapper.selectOne(new LambdaQueryWrapper<RoleEntity>()
                .eq(RoleEntity::getCode, code));
        if (role == null) {
            role = new RoleEntity();
            role.setCode(code);
            role.setName(name);
            roleMapper.insert(role);
        }
        return role;
    }

    private PermissionEntity ensurePermission(String code, String name) {
        PermissionEntity perm = permissionMapper.selectOne(new LambdaQueryWrapper<PermissionEntity>()
                .eq(PermissionEntity::getCode, code));
        if (perm == null) {
            perm = new PermissionEntity();
            perm.setCode(code);
            perm.setName(name);
            permissionMapper.insert(perm);
        }
        return perm;
    }

    private void bindRolePermission(Long roleId, Long permissionId) {
        Long count = rolePermissionMapper.selectCount(new LambdaQueryWrapper<RolePermissionEntity>()
                .eq(RolePermissionEntity::getRoleId, roleId)
                .eq(RolePermissionEntity::getPermissionId, permissionId));
        if (count == null || count == 0) {
            RolePermissionEntity rel = new RolePermissionEntity();
            rel.setRoleId(roleId);
            rel.setPermissionId(permissionId);
            rolePermissionMapper.insert(rel);
        }
    }
}
