package com.fengluan.auth.service;

import com.fengluan.auth.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Token 存取/黑名单服务，Redis key 规范：
 * refresh -> token:refresh:{userId}:{type}；黑名单 -> token:blacklist:{userId}:{type}
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    public static final int FAIL_MAX = 3;          // 连续失败次数上限
    public static final long LOCK_MINUTES = 15;    // 超过上限锁定时长
    private static final long LOGIN_FAIL_TTL_MIN = 15;

    private final StringRedisTemplate redis;
    private final JwtUtil jwtUtil;

    private String refreshKey(String userId, String type) {
        return "token:refresh:" + userId + ":" + type;
    }

    private String blacklistKey(String userId, String type) {
        return "token:blacklist:" + userId + ":" + type;
    }

    /** Refresh Token 落 Redis，7 天 */
    public void saveRefresh(String userId, String type, String refreshToken) {
        redis.opsForValue().set(refreshKey(userId, type), refreshToken, 7, TimeUnit.DAYS);
    }

    /** 校验传入 refreshToken 与 Redis 缓存一致且未过期（签名由 parseToken 兜底校验） */
    public boolean compareRefresh(String userId, String type, String refreshToken) {
        String cached = redis.opsForValue().get(refreshKey(userId, type));
        return cached != null && cached.equals(refreshToken);
    }

    /** 登出：Access 写黑名单（TTL=剩余有效期），删除 Refresh */
    public void blacklist(String accessToken) {
        Claims c = jwtUtil.parseToken(accessToken);
        String type = c.get("type", String.class);
        long remain = c.getExpiration().getTime() - System.currentTimeMillis();
        if (remain > 0) {
            redis.opsForValue().set(blacklistKey(c.getSubject(), type),
                    accessToken, remain, TimeUnit.MILLISECONDS);
        }
        clearRefresh(c.getSubject(), type);
    }

    /** 是否已在黑名单（已登出） */
    public boolean isBlacklisted(String accessToken) {
        Claims c = jwtUtil.parseToken(accessToken);
        String type = c.get("type", String.class);
        return Boolean.TRUE.equals(redis.hasKey(blacklistKey(c.getSubject(), type)));
    }

    /** 删除 Refresh Token（登出/改密后失效旧 Refresh） */
    public void clearRefresh(String userId, String type) {
        redis.delete(refreshKey(userId, type));
    }

    /** 登录失败计数自增并返回当前次数 */
    public long incrFailCount(String account) {
        String key = "login:fail:" + account;
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1) {
            redis.expire(key, LOGIN_FAIL_TTL_MIN, TimeUnit.MINUTES);
        }
        return count == null ? 0 : count;
    }

    /** 是否被锁定：失败计数达 3 次及以上（键 15 分钟后自动过期即解锁） */
    public boolean isLocked(String account) {
        String v = redis.opsForValue().get("login:fail:" + account);
        return v != null && Long.parseLong(v) >= FAIL_MAX;
    }

    /** 登录成功后清除失败计数 */
    public void clearFailCount(String account) {
        redis.delete("login:fail:" + account);
    }
}