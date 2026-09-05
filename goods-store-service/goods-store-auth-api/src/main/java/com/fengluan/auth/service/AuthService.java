package com.fengluan.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fengluan.auth.entity.AuthUserEntity;
import com.fengluan.auth.repository.AuthUserMapper;
import com.fengluan.auth.util.JwtUtil;
import com.fengluan.common.exception.BusinessException;
import com.fengluan.common.exception.ErrorCode;
import com.fengluan.spi.auth.dto.ChangePwdRequest;
import com.fengluan.spi.auth.dto.LoginRequest;
import com.fengluan.spi.auth.dto.LoginResponse;
import com.fengluan.spi.auth.dto.RefreshRequest;
import com.fengluan.spi.auth.dto.RegisterRequest;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 会员认证服务：注册 / 登录 / 刷新 / 登出 / 改密。账号位于 member 表。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TYPE = "member";

    private final AuthUserMapper authUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenService tokenService;

    public void register(RegisterRequest req) {
        long count = authUserMapper.selectCount(new LambdaQueryWrapper<AuthUserEntity>()
                .eq(AuthUserEntity::getAccount, req.getAccount()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.MEMBER_ACCOUNT_EXISTS);
        }
        AuthUserEntity e = new AuthUserEntity();
        e.setAccount(req.getAccount());
        e.setPassword(passwordEncoder.encode(req.getPassword()));
        e.setPhone(req.getPhone());
        e.setEmail(req.getEmail());
        e.setEnabled(true);
        authUserMapper.insert(e);
    }

    public LoginResponse login(LoginRequest req) {
        if (tokenService.isLocked(req.getAccount())) {
            throw new BusinessException(ErrorCode.MEMBER_DISABLED);
        }
        AuthUserEntity u = authUserMapper.selectOne(new LambdaQueryWrapper<AuthUserEntity>()
                .eq(AuthUserEntity::getAccount, req.getAccount()));
        if (u == null || !passwordEncoder.matches(req.getPassword(), u.getPassword())) {
            tokenService.incrFailCount(req.getAccount());
            throw new BusinessException(ErrorCode.MEMBER_PASSWORD_ERROR);
        }
        if (u.getEnabled() == null || !u.getEnabled()) {
            throw new BusinessException(ErrorCode.MEMBER_DISABLED);
        }
        tokenService.clearFailCount(req.getAccount());
        return buildLogin(u.getId().toString(), "member");
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
        String accessToken = jwtUtil.generateAccessToken(userId, TYPE, List.of("ROLE_USER"));
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(req.getRefreshToken())
                .expiresIn(jwtUtil.getAccessExpireMs() / 1000)
                .build();
    }

    public void logout(String authorization) {
        tokenService.blacklist(stripBearer(authorization));
    }

    public void changePassword(String authorization, ChangePwdRequest req) {
        String accessToken = stripBearer(authorization);
        Claims c = jwtUtil.parseToken(accessToken);
        AuthUserEntity u = authUserMapper.selectById(c.getSubject());
        if (u == null || !passwordEncoder.matches(req.getOldPassword(), u.getPassword())) {
            throw new BusinessException(ErrorCode.MEMBER_PASSWORD_ERROR);
        }
        u.setPassword(passwordEncoder.encode(req.getNewPassword()));
        authUserMapper.updateById(u);
        // 改密后使旧 Refresh 失效
        tokenService.clearRefresh(u.getId().toString(), TYPE);
    }

    private LoginResponse buildLogin(String userId, String type) {
        String accessToken = jwtUtil.generateAccessToken(userId, type, List.of("ROLE_USER"));
        String refreshToken = jwtUtil.generateRefreshToken(userId);
        tokenService.saveRefresh(userId, type, refreshToken);
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtUtil.getAccessExpireMs() / 1000)
                .build();
    }

    private String stripBearer(String authorization) {
        return authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7)
                : authorization;
    }
}