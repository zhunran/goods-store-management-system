package com.fengluan.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * JWT 工具（jjwt 0.12.6 API）。
 * <p>Access Token 有效期 30 分钟，Refresh Token 有效期 7 天。
 * 密钥从配置 {@code jwt.secret} 读取，与网关保持一致，避免签名不一致导致 401。</p>
 */
@Component
public class JwtUtil {

    private static final long ACCESS_MIN = 30 * 60 * 1000L;              // 30 分钟
    private static final long REFRESH_DAY = 7L * 24 * 60 * 60 * 1000L;   // 7 天

    private final SecretKey key;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 生成 Access Token，携带 subject(userId)、type(member/admin)、roles；权限码默认空 */
    public String generateAccessToken(String userId, String type, List<String> roles) {
        return generateAccessToken(userId, type, roles, List.of());
    }

    /** 生成 Access Token，携带 subject(userId)、type(member/admin)、roles、permissions */
    public String generateAccessToken(String userId, String type, List<String> roles, List<String> permissions) {
        return Jwts.builder()
                .subject(userId)
                .claim("type", type)
                .claim("roles", roles)
                .claim("permissions", permissions)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ACCESS_MIN))
                .signWith(key)
                .compact();
    }

    /** 生成 Refresh Token，仅携带 subject(userId) */
    public String generateRefreshToken(String userId) {
        return Jwts.builder()
                .subject(userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + REFRESH_DAY))
                .signWith(key)
                .compact();
    }

    /** 解析并校验签名/过期，非法或过期将抛异常 */
    public Claims parseToken(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
    }

    public long getAccessExpireMs() {
        return ACCESS_MIN;
    }
}