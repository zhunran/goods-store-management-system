package com.fengluan.gateway.filter;


import com.fengluan.gateway.config.JwtProperties;
import com.fengluan.gateway.config.WhiteListConfig;
import com.fengluan.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter,Ordered {
    private final WhiteListConfig whiteListConfig;
    private final JwtProperties jwtProperties;
    private final ReactiveStringRedisTemplate redisTemplate;
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path=exchange.getRequest().getURI().getPath();
        //白名单放行
        if (whiteListConfig.isWhiteListed(path))
        {
            return chain.filter(exchange);
        }
        //提取Bear token
        String authHeader=exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader==null||!authHeader.startsWith("Bearer "))
        {
            return unauthorized(exchange,"缺少Token");
        }
        String token=authHeader.substring(7);

        //校验签名—有效期
        Claims claims;
        try {
            claims= JwtUtil.parseToken(jwtProperties.getSecret(),token);
            
        }catch (Exception e)
        {
            log.warn("[gateway] JWT 校验失败, path={}, err={}", path, e.getMessage());
            return unauthorized(exchange, "Token 无效或已过期");
        }
        if (JwtUtil.isExpired(claims))
        {
            return unauthorized(exchange,"token已经过期");
        }
        //Redis黑名单（key 与 auth-api 保持一致：token:blacklist:{userId}:{type}）
        //value 存的是被注销的 accessToken 本身，必须比对值而非 key 是否存在：
        //登出后（黑名单 TTL 最长 30 分钟）重新登录会签发新 token，若只判 key 存在会把新 token 误杀
        String userId=claims.getSubject();
        String type=claims.get("type", String.class);
        return redisTemplate.opsForValue().get("token:blacklist:"+userId+":"+type)
                .map(blacklisted -> blacklisted.equals(token))
                .defaultIfEmpty(false)
                .flatMap(inBlack->{
                    if(inBlack){
                        return unauthorized(exchange,"Token已注销");
                    }
                    //透传用户信息给下游
                    ServerWebExchange mutated=exchange.mutate().request(
                            exchange.getRequest().mutate()
                                    .header("X-User-Id",userId)
                                    .header("X-User-Roles",String.join(",",claims.get("roles", List.class)))
                                    .build()).build();
                    return chain.filter(mutated);
                });
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        log.warn("[gateway] 拒绝访问, path={}, reason={}", exchange.getRequest().getURI().getPath(), message);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory()
                        .wrap(("{\"code\":401,\"message\":\"" + message + "\"}").getBytes())));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
