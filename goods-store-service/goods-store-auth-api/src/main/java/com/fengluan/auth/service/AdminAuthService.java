package com.fengluan.auth.service;

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
import com.fengluan.auth.util.JwtUtil;
import com.fengluan.common.exception.BusinessException;
import com.fengluan.common.exception.ErrorCode;
import com.fengluan.spi.auth.dto.LoginRequest;
import com.fengluan.spi.auth.dto.LoginResponse;
import com.fengluan.spi.auth.dto.RefreshRequest;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理员认证服务：RBAC 登录，返回 JWT + 角色/权限列表。
 */
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private static final String TYPE = "admin";

    private final AdminUserMapper adminUserMapper;
    private final AdminUserRoleMapper adminUserRoleMapper;
    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenService tokenService;

    public LoginResponse login(LoginRequest req) {
        if (tokenService.isLocked(req.getAccount())) {
            throw new BusinessException("登录失败次数过多，账号已临时锁定15分钟", ErrorCode.AUTH_USER_DISABLED.getCode());
        }
        AdminUserEntity u = adminUserMapper.selectOne(new LambdaQueryWrapper<AdminUserEntity>()
                .eq(AdminUserEntity::getUsername, req.getAccount()));
        if (u == null || !passwordEncoder.matches(req.getPassword(), u.getPassword())) {
            tokenService.incrFailCount(req.getAccount());
            throw new BusinessException(ErrorCode.AUTH_PASSWORD_ERROR);
        }
        if (u.getStatus() == null || u.getStatus() != 1) {
            throw new BusinessException(ErrorCode.AUTH_USER_DISABLED);
        }
        tokenService.clearFailCount(req.getAccount());

        List<Long> roleIds = adminUserRoleMapper.selectList(
                        new LambdaQueryWrapper<AdminUserRoleEntity>()
                                .eq(AdminUserRoleEntity::getAdminUserId, u.getId()))
                .stream().map(AdminUserRoleEntity::getRoleId).toList();

        List<String> roleCodes = roleIds.isEmpty() ? List.of()
                : roleMapper.selectBatchIds(roleIds).stream().map(RoleEntity::getCode).toList();

        List<String> permissions = permissionCodes(roleIds);

        String accessToken = jwtUtil.generateAccessToken(u.getId().toString(), "admin", roleCodes, permissions);
        String refreshToken = jwtUtil.generateRefreshToken(u.getId().toString());
        tokenService.saveRefresh(u.getId().toString(), TYPE, refreshToken);

        LoginResponse.UserInfo info = LoginResponse.UserInfo.builder()
                .userId(u.getId())
                .account(req.getAccount())
                .type("admin")
                .roles(roleCodes)
                .permissions(permissions)
                .build();
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtUtil.getAccessExpireMs() / 1000)
                .userInfo(info)
                .build();
    }

    public LoginResponse refresh(RefreshRequest req) {
        Claims c;
        try {
            c = jwtUtil.parseToken(req.getRefreshToken());
        } catch (Exception ex) {
            throw new BusinessException("Token 已过期或无效", ErrorCode.UNAUTHORIZED.getCode());
        }
        String userId = c.getSubject();
        if (!tokenService.compareRefresh(userId, TYPE, req.getRefreshToken())) {
            throw new BusinessException("Refresh Token 已失效", ErrorCode.UNAUTHORIZED.getCode());
        }
        AdminUserEntity u = adminUserMapper.selectById(Long.valueOf(userId));
        if (u == null || u.getStatus() == null || u.getStatus() != 1) {
            throw new BusinessException(ErrorCode.AUTH_USER_DISABLED);
        }

        List<Long> roleIds = adminUserRoleMapper.selectList(
                        new LambdaQueryWrapper<AdminUserRoleEntity>()
                                .eq(AdminUserRoleEntity::getAdminUserId, u.getId()))
                .stream().map(AdminUserRoleEntity::getRoleId).toList();

        List<String> roleCodes = roleIds.isEmpty() ? List.of()
                : roleMapper.selectBatchIds(roleIds).stream().map(RoleEntity::getCode).toList();

        List<String> permissions = permissionCodes(roleIds);

        String accessToken = jwtUtil.generateAccessToken(u.getId().toString(), TYPE, roleCodes, permissions);
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(req.getRefreshToken())
                .expiresIn(jwtUtil.getAccessExpireMs() / 1000)
                .userInfo(LoginResponse.UserInfo.builder()
                        .userId(u.getId())
                        .account(u.getUsername())
                        .type(TYPE)
                        .roles(roleCodes)
                        .permissions(permissions)
                        .build())
                .build();
    }

    private List<String> permissionCodes(List<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return List.of();
        }
        List<Long> permissionIds = rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<RolePermissionEntity>()
                                .in(RolePermissionEntity::getRoleId, roleIds))
                .stream().map(RolePermissionEntity::getPermissionId).distinct().toList();
        if (permissionIds.isEmpty()) {
            return List.of();
        }
        return permissionMapper.selectBatchIds(permissionIds).stream()
                .map(PermissionEntity::getCode).collect(Collectors.toList());
    }
}