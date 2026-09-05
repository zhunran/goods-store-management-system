package com.fengluan.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public final class JwtUtil {
    private JwtUtil(){}
    public static Claims parseToken(String secret,String token){
        SecretKey key= Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
    }
    public static boolean isExpired(Claims claims)
    {
        return claims.getExpiration()==null||claims.getExpiration().before(new Date());
    }
}
