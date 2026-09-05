# Day 2 操作执行手册 — 基础设施搭建（下）

> 对应计划：[14-day-implementation-plan.md](file:///d:/.workspace/javaproject/goods-store-management-system-parent/doc/schedule/14-day-implementation-plan.md) Day 2
> 版本：v1.0　编写日期：2026-09-01　执行日期：待填
> 主题：Druid 数据源集成 + 网关路由/JWT 鉴权 + 健康检查
> 依赖：Day 1 已完成（Nacos 配置中心 + 公共模块 + 6 个 API 服务接入配置中心）

---

## 一、Day 2 目标与产出物

### 目标

1. 让各 API 服务真正用上 Druid 连接池（替换默认 HikariCP），开启 SQL 监控与防火墙
2. 网关完成路由全景 + JWT 全局鉴权过滤器 + 白名单 + 跨域 + 限流
3. 所有服务配置 Nacos 就绪健康检查，避免网关路由到未就绪实例

### 产出物清单

| 类型            | 产出物                                                                                     |
| --------------- | ------------------------------------------------------------------------------------------ |
| Druid 配置类    | 各 API 服务 `DruidDataSourceConfig`（核心包手动装配，Boot 4 兼容）                          |
| Druid 监控      | `StatViewServlet` 注册（WebMVC 服务），访问 `/druid` 控制台                                |
| 网关 JWT 工具   | `JwtProperties` + `JwtUtil`（jjwt 0.12.6 HS256 解析）                                       |
| 网关过滤/配置   | `JwtAuthFilter`、`WhiteListConfig`、`CorsConfig`、`RateLimiterConfig`                       |
| 网关依赖        | `jjwt-api/impl/jackson`、`spring-boot-starter-data-redis`（黑名单校验）                     |
| 健康检查        | 所有服务 `application.yaml` 加 `readiness-state` + 网关开启 discovery locator              |

---

##二、Day 2 实施状态盘点（基于 Day 1 现状）

> ⚠️ **Day 1 完成后遗留一个端口冲突，执行前必须先解决**（见步骤 0）。

| 计划任务 | 内容                 | 当前状态                                             | 手册步骤 |
| -------- | -------------------- | ---------------------------------------------------- | -------- |
| 2.1      | 各服务 POM 引 Druid  | ✅ 已在 `service/pom.xml` 引**核心包** `com.alibaba:druid` | 步骤 1（只差配置类） |
| 2.2      | 网关路由完整配置     | ✅ 6 条路由已就位（Boot 4 新属性路径）                | 步骤 2（核对）        |
| 2.3      | JWT 全局过滤器       | ❌ 尚未创建                                           | 步骤 3                |
| 2.4      | 白名单配置           | ❌ 尚未创建                                           | 步骤 4                |
| 2.5      | 跨域配置             | ❌ 尚未创建                                           | 步骤 5                |
| 2.6      | 限流配置             | ⚠️ 网关已接 Sentinel，计划中的 Guava 可跳过/二选一     | 步骤 6                |
| 2.7      | auth-api 模块注册    | ✅ 已注册到 `service/pom.xml` `<modules>`             | -                      |
| 2.8      | auth-api 启动类/配置 | ✅ 启动类已存在，application.yaml 已配                 | -                      |
| 2.9      | Nacos 健康检查       | ❌ 未配置                                             | 步骤 7                |

> **关于 2.1 的 Druid 版本说明**：Day 1 已因 Boot 4 兼容问题改引**核心包** `com.alibaba:druid`（starter `druid-spring-boot-3-starter` 不兼容）。因此 Day 2 需要**手写 `DruidDataSourceConfig`** 装配数据源并绑定 `spring.datasource.druid.*`（该段在共享 `goods-store-common.yaml` 已预置，见 Day 1 手册），而不是靠 starter 自动装配。

---

## 三、详细操作步骤

### 步骤 0：解决端口冲突（前置）

**现象**：`goods-store-auth-api` 与 `goods-store-web` 的 `application.yaml` 当前都是 `server.port: 8083`。两者同时启动时后者会因端口占用失败。

**根因**：Day 1 因本机 QQ 占用 8082，把 auth 从计划的 8082 改成了 8083，而 web 原本就是 8083，撞车。

**处理（二选一，推荐 A）**：
- 方案 A（推荐）：web 改到空闲端口 `8090`，auth 保持 `8083`。
- 方案 B：auth 改回 `8082`（若 8082 已恢复空闲），web 保持 `8083`。

> 无论选哪个，请同步检查并**统一本手册 §五 的端口分配表**，本手册后续以此为准。同步更新 `goods-store-web/src/main/resources/application.yaml` 的 `server.port`。

---

### 步骤 1：手写 Druid 数据源配置类（任务 2.1）

> 在**每个需要访问数据库的 API 服务**（brand/product/member/trade/seckill；auth 若后续要连库再加）新增配置类，**复用共享配置里的 `spring.datasource.druid.*` 参数**。

以 brand-api 为例，新建 `goods-store-service/goods-store-brand-api/src/main/java/com/fengluan/brand/config/DruidDataSourceConfig.java`：

```java
package com.fengluan.brand.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * Druid 核心包手动装配（Boot 4 下 druid-spring-boot-3-starter 因包迁移不兼容，故手写配置类）。
 * 参数来自共享配置 goods-store-common.yaml 的 spring.datasource.* / spring.datasource.druid.*
 */
@Configuration
public class DruidDataSourceConfig {

    @Value("${spring.datasource.url}")     private String url;
    @Value("${spring.datasource.username}") private String username;
    @Value("${spring.datasource.password}") private String password;
    @Value("${spring.datasource.driver-class-name:com.mysql.cj.jdbc.Driver}") private String driverClassName;

    @Bean
    @Primary
    public DataSource dataSource(@Value("${spring.datasource.druid.initial-size:3}") int initialSize,
                                 @Value("${spring.datasource.druid.min-idle:3}") int minIdle,
                                 @Value("${spring.datasource.druid.max-active:10}") int maxActive,
                                 @Value("${spring.datasource.druid.max-wait:60000}") long maxWait,
                                 @Value("${spring.datasource.druid.validation-query:SELECT 1}") String validationQuery,
                                 @Value("${spring.datasource.druid.filter.stat.enabled:true}") boolean statEnabled) throws SQLException {
        DruidDataSource ds = new DruidDataSource();
        ds.setUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDriverClassName(driverClassName);

        ds.setInitialSize(initialSize);
        ds.setMinIdle(minIdle);
        ds.setMaxActive(maxActive);
        ds.setMaxWait(maxWait);
        ds.setValidationQuery(validationQuery);
        ds.setTestWhileIdle(true);
        ds.setTestOnBorrow(false);
        ds.setTestOnReturn(false);

        // SQL 监控（StatFilter 需要）
        if (statEnabled) {
            ds.addFilter("stat");
        }
        // 防火墙（WallFilter）
        ds.addFilter("wall");
        return ds;
    }
}
```

> 说明：
> - **为什么不用 starter**：见上。核心包需显式 `new DruidDataSource()` + 注册为 `@Primary DataSource`，MyBatis-Plus starter 会自动拾取该 DataSource。
> - 若同一配置类要在 5 个服务重复，各服务各自创建（保持服务独立）；也可抽到 common，但因 common 不引 druid（避免强依赖），Day 2 先在各服务手写。

**Druid 监控页面（StatViewServlet）**，同服务再加一个配置类 `DruidStatConfig.java`（WebMVC 服务专用，网关是 WebFlux 不需要）：

```java
package com.fengluan.brand.config;

import com.alibaba.druid.support.http.StatViewServlet;
import jakarta.servlet.Servlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DruidStatConfig {

    @Bean
    public ServletRegistrationBean<Servlet> druidStatViewServlet() {
        StatViewServlet servlet = new StatViewServlet();
        ServletRegistrationBean<Servlet> reg = new ServletRegistrationBean<>(servlet, "/druid/*");
        // 访问 /druid 控制台（开发期开放，生产需改 login 用户/密码）
        reg.addInitParameter("loginUsername", "admin");
        reg.addInitParameter("loginPassword", "admin");
        return reg;
    }
}
```

---

### 步骤 2：核对/修正网关路由全景（任务 2.2）

Day 1 已在 [gateway application.yaml](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-gateway/src/main/resources/application.yaml#L29-L59) 配置 6 条路由，注意两点与计划差异（保持现状即可，勿改回）：

1. **属性路径**：Boot 4 / SC 2025.1 迁移为 `spring.cloud.gateway.server.webflux.routes`，**不要**再写旧的 `spring.cloud.gateway.routes`。
2. **product 前缀**：商品服务实际是 `/good/api/**`（非计划的 `/product/api/**`），已按实际配置。

> 当前路由已完整（auth/brand/good/member/trade/seckill），若启动后路由不生效，优先检查是否为上述两个原因。

---

### 步骤 3：网关 JWT 鉴权（任务 2.3）

**① 网关 pom 补依赖**（[gateway/pom.xml](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-gateway/pom.xml) `<dependencies>` 追加，版本由根 pom 管理）：

```xml
<!-- JWT 三件套 -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <scope>runtime</scope>
</dependency>
<!-- Redis：令牌黑名单校验 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

> 网关是 WebFlux 栈，Redis 操作用**响应式** `ReactiveStringRedisTemplate`，避免阻塞事件循环。

**② JwtProperties** `com/fengluan/gateway/config/JwtProperties.java`：

```java
package com.fengluan.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 与 Day 3 auth 签发的 JWT 保持同一 secret，此处只读不回写 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    /** 签名密钥（HS256，≥32 字节），与 auth 服务一致 */
    private String secret;
}
```

**③ JwtUtil** `com/fengluan/gateway/util/JwtUtil.java`（jjwt 0.12.6 新 API）：

```java
package com.fengluan.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/** 网关侧仅做解析校验（签发在 auth 服务），断言过期时间 */
public final class JwtUtil {

    private JwtUtil() {}

    public static Claims parseToken(String secret, String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
    }

    public static boolean isExpired(Claims claims) {
        return claims.getExpiration() == null || claims.getExpiration().before(new Date());
    }
}
```

**④ JwtAuthFilter** `com/fengluan/gateway/filter/JwtAuthFilter.java`（实现 `GlobalFilter, Ordered`）：

```java
package com.fengluan.gateway.filter;

import com.fengluan.gateway.config.JwtProperties;
import com.fengluan.gateway.config.WhiteListConfig;
import com.fengluan.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final WhiteListConfig whiteListConfig;
    private final JwtProperties jwtProperties;
    private final ReactiveStringRedisTemplate redisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 1. 白名单放行
        if (whiteListConfig.isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        // 2. 提取 Bearer Token
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "缺少 Token");
        }
        String token = authHeader.substring(7);

        // 3. 校验签名 + 有效期
        Claims claims;
        try {
            claims = JwtUtil.parseToken(jwtProperties.getSecret(), token);
        } catch (Exception e) {
            log.warn("[gateway] JWT 校验失败, path={}, err={}", path, e.getMessage());
            return unauthorized(exchange, "Token 无效或已过期");
        }
        if (JwtUtil.isExpired(claims)) {
            return unauthorized(exchange, "Token 已过期");
        }

        // 4. Redis 黑名单（登出后 token 失效）
        String userId = claims.getSubject();
        return redisTemplate.hasKey("token:blacklist:" + userId)
                .flatMap(inBlack -> {
                    if (Boolean.TRUE.equals(inBlack)) {
                        return unauthorized(exchange, "Token 已注销");
                    }
                    // 5. 透传用户信息给下游
                    ServerWebExchange mutated = exchange.mutate().request(
                            exchange.getRequest().mutate()
                                    .header("X-User-Id", userId)
                                    .header("X-User-Roles", String.join(",", claims.get("roles", List.class)))
                                    .build()).build();
                    return chain.filter(mutated);
                });
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String msg) {
        log.warn("[gateway] 拒绝访问, path={}, reason={}", exchange.getRequest().getURI().getPath(), msg);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory()
                        .wrap(("{\"code\":401,\"message\":\"" + msg + "\"}").getBytes())));
    }

    @Override
    public int getOrder() {
        return -100; // 最先执行（在基于路由 order 之前）
    }
}
```

---

### 步骤 4：白名单配置（任务 2.4）

> 登录/注册/商品浏览/品牌浏览等免 Token。支持从 Nacos 或本地 yaml 配置，未配置时用内置默认值。

`com/fengluan/gateway/config/WhiteListConfig.java`：

```java
package com.fengluan.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.List;

/** 免鉴权路径白名单，prefix 配置来源：auth.whitelist（可放 Nacos/本地，覆盖默认值） */
@Data
@Component
@ConfigurationProperties(prefix = "auth")
public class WhiteListConfig {

    private final AntPathMatcher matcher = new AntPathMatcher();

    /** 默认白名单：登录/注册 + 品牌/商品浏览 + 网关自身健康检查 */
    private List<String> whitelist = new ArrayList<>(List.of(
            "/auth/api/login",
            "/auth/api/register",
            "/brand/api/**",
            "/good/api/**",
            "/actuator/**"
    ));

    public boolean isWhiteListed(String path) {
        return whitelist.stream().anyMatch(p -> matcher.match(p, path));
    }
}
```

> 若要 Nacos 热更新白名单：把 `auth.whitelist` 写入 Nacos 某个配置（如 `goods-store-gateway.yaml`）并给本类加 `@RefreshScope`。

---

### 步骤 5：跨域配置（任务 2.5）

`com/fengluan/gateway/config/CorsConfig.java`（开发期允许所有来源）：

```java
package com.fengluan.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(List.of("*"));          // 开发期放开；生产按前端域名收紧
        cfg.setAllowedMethods(List.of("*"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setMaxAge(Duration.ofSeconds(3600));
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return new CorsWebFilter(source);
    }
}
```

---

### 步骤 6：限流（任务 2.6）

> ⚠️ **决策点**：当前网关**已引入 Sentinel**（Day 1 配置了 Dashboard 连接），计划书里写的是 **Guava RateLimiter 按 IP 限流**。二者二选一，避免重复引入依赖。

- **推荐**：用已就绪的 **Sentinel 网关流控**（`sentinel-spring-cloud-gateway-v6x-adapter` 已依赖），在 Dashboard 里对 `/seckill/**`、`/trade/api/**` 配置 QPS 规则，可视化且支持多实例（配合 Provider 规则推送即可分布式）。
- **若仍要用 Guava（单机）**：为网关 pom 加 `com.google.guava:guava`（版本由 BOM 管理），并新建 `RateLimiterConfig` 返回一个构造了每 IP 限制器的 `ReactiveRateLimiter`。**但注意这是单机限流**，与计划 Day 14 备注一致，多实例部署需升级 Redis 令牌桶。

> Day 2 建议暂用 Sentinel 内置流控即可，Guava 方案作为备选记录在手册，不强制落地。执行时请在下方验收勾选用哪种。

---

### 步骤 7：Nacos 就绪健康检查（任务 2.9）

**① 所有服务 `application.yaml`（6 个 API + web + gateway）**，在 `spring.cloud.nacos.discovery` 下追加：

```yaml
spring:
  cloud:
    nacos:
      discovery:
        metadata:
          # 服务就绪后才在 Nacos 标记为健康，避免网关路由到未启动完成实例
          readiness-state: enabled
```

示例（brand 记得保留原有 `ip: 192.168.110.98`）：

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
        username: nacos
        password: nacos
        group: DEFAULT_GROUP
        ip: 192.168.110.98        # 仅 brand 有，其余服务无此行
        metadata:
          readiness-state: enabled
```

**② 网关开启容器发现**（[gateway application.yaml](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-gateway/src/main/resources/application.yaml) `spring.cloud.gateway` 下）：

```yaml
spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
```

> 说明：`readiness-state=enabled` 需服务暴露健康端点并依赖 Spring Boot Actuator 的就绪组。若当前服务未引 `spring-boot-starter-actuator`，需在 `service/pom.xml` 统一补充：
> ```xml
> <dependency>
>     <groupId>org.springframework.boot</groupId>
>     <artifactId>spring-boot-starter-actuator</artifactId>
> </dependency>
> ```

---

### 步骤 8：编译与启动（统一次序）

```powershell
# 0. 先解决端口冲突（步骤 0）
# 1. 父 POM install（service/pom 若补了 actuator 必须先 install）
mvn -N install
# 2. 全量编译
mvn clean compile
# 3. 启动顺序：Nacos → MySQL → Redis →（可选 RabbitMQ）→ 各服务
cd goods-store-service/goods-store-brand-api && mvn spring-boot:run
# gateway 单独终端启动
cd goods-store-gateway && mvn spring-boot:run
```

---

## 四、验收标准

按顺序逐项验证，全部通过才算 Day 2 完成。

### 4.1 编译与启动

| #  | 验收项                    | 验证方法                                   | 预期结果                  |
|----|---------------------------|--------------------------------------------|---------------------------|
| 1  | 全模块编译通过            | `mvn clean compile`                        | BUILD SUCCESS             |
| 2  | 无关服务端口无冲突        | 依次启动 6 个 API + web + gateway         | 无 "Port already in use"  |
| 3  | 各服务注册 Nacos 且健康   | Nacos 控制台 → 服务管理                    | 实例状态"健康"，网关可路由 |

### 4.2 网关路由与鉴权

| #  | 验收项                           | 验证方法                                                     | 预期结果                         |
|----|----------------------------------|--------------------------------------------------------------|----------------------------------|
| 4  | 白名单放行                       | 无 Token `curl http://localhost:8888/brand/api/page?pageNum=1&pageSize=10` | 200 正常返回                    |
| 5  | 非白名单无 Token 拒绝            | 无 Token `curl http://localhost:8888/trade/api/cart`          | 401                            |
| 6  | 无效 Token 拒绝                  | 带 `Authorization: Bearer abc` 重复上一步                    | 401 且提示 Token 无效           |
| 7  | 有效 Token 透传                  | 用 auth 签发的 token（Day 3 后）访问受保护接口                | 401 消失，下游拿到 `X-User-Id`  |
| 8  | 网关日志                         | 观察网关终端                                               | 输出 path / 耗时 / 来源 IP       |
| 9  | Druid 监控                       | `http://localhost:8081/druid`（admin/admin）                 | 监控页可访问，连接池在运行       |

### 4.3 限流（二选一）

| #  | 方案       | 验证方法                                       | 预期结果                     |
|----|-----------|------------------------------------------------|------------------------------|
| 10 | Sentinel  | Dashboard 对 `/seckill/**` 配 QPS=1，快速连拍    | 触发限流返回降级/拒绝        |
| -  | Guava     | 快速连拍受保护接口                              | 超阈值返回 429（若采用）     |

> 说明：5/6 项需要网关已启动且已注册。若 auth 尚未实现签发模块（Day 3），第 7 项可延后到 Day 3，Day 2 重点验证 4/5/6/9 即可。

---

## 五、端口/雪花 ID 分配表（更新版）

> 与 Day 1 计划表不同，本表为**当前实际**采用值，请以此为准，并用于核对 §步骤 0 的端口冲突。

| 服务                | workerId | 端口（当前） | 备注                     |
| ------------------- | -------- | ------------ | ------------------------ |
| goods-store-auth    | 1        | **8083**     | Day 1 因 8082 被占改     |
| goods-store-brand   | 2        | 8081         | 保留 `ip:192.168.110.98` |
| goods-store-product | 3        | 8084         |                          |
| goods-store-member  | 4        | 8085         |                          |
| goods-store-trade   | 5        | 8086         |                          |
| goods-store-seckill | 6        | 8087         |                          |
| goods-store-web     | 7        | **再看**      | 与 auth 撞 8083，需改     |
| goods-store-gateway | -        | 8888         | 无雪花需求                |

> ⚠️ workerId 在计划中是「按服务静态分配」，本表 auth=1..seckill=6、web=7，与 Day 1 手册 §2.4 的旧表（含 web=3/product=4…）不一致——**Day 2 请以此更新后的表为准**，并在穿插时更新 day01-manual §2.4，消除文档冲突。

---

## 六、常见问题排查

### 6.1 端口占用 / "Port already in use"
- 见步骤 0，先统一 auth/web 端口。
- 查看占用：`netstat -ano | findstr 8083`，按 PID 释放。

### 6.2 路由不生效
- 确认用 `spring.cloud.gateway.server.webflux.routes`（Boot 4 新路径），勿写旧 `spring.cloud.gateway.routes`。
- 确认服务已注册 Nacos，且 `uri: lb://服务名` 与注册名一致。

### 6.3 JWT 校验总是 401
- 确认 `jwt.secret` 已配置，且与 **auth 签发端** 的 secret 完全一致（≥32 字节）。
- 确认 Redis 可用（黑名单查询会连 Redis，Redis 挂也会导致失败）。
- 检查共享配置里是否已有 `jwt` 段；若鉴权逻辑有变更需重启网关（WebFlux 过滤器不热更）。

### 6.4 Druid 监控页 404
- 确认该服务是 WebMVC（webmvc starter）而非 WebFlux。
- 确认连的是 `/druid/*` 且服务端口正确（brand 8081）。

### 6.5 Sentinel 流控不生效
- 确认网关已配 Dashboard 地址且能心跳（eager: true 已设）。
- 流控规则优先在 Dashboard 手动配置验证；若控不住检查规则针对的资源名是否为路由 id。

---

## 七、本日产出核对表（提交前自查）

- [ ] 端口冲突已解决（auth/web 端口唯一）
- [ ] Druid 依赖（核心包）已在 `service/pom.xml` ✅（Day 1 完成）
- [ ] 各数据库服务 `DruidDataSourceConfig` 就位，绑 `spring.datasource.druid.*`
- [ ] WebMVC 服务 `DruidStatConfig` 注册 `/druid` 监控
- [ ] 网关补 jjwt 三件套 + data-redis 依赖
- [ ] `JwtProperties` / `JwtUtil` / `JwtAuthFilter` 创建
- [ ] `WhiteListConfig`（默认白名单）创建
- [ ] `CorsConfig` 创建
- [ ] 限流方案二选一确定（Sentinel 优先）
- [ ] 所有服务 `readiness-state: enabled`，网关开 discovery locator
- [ ] 4.1/4.2 验收全通过（7 项可延后 Day 3）
- [ ] Git 提交：`feat(day2): Druid集成+网关JWT鉴权+健康检查`

---

## 八、明日预告（Day 3 衔接）

Day 3 认证服务（上）：本项目搭建 + RBAC 5 表实体 + 登录注册接口。届时 **auth 服务会用与网关一致的 `jwt.secret` 签发双 Token**，并实现 login/register 供网关白名单放行。因此：
- 本日 `JwtAuthFilter` 已预留 `X-User-Id`/`X-User-Roles` 透传，Day 3 auth 签发 claims 需包含 `subject=userId` 与自定义 `roles` claim。
- 提醒：`auth.snowflake.worker-id` 已配为 1，Day 3 生成会员/管理员 ID 时直接复用。
- Day 4 会实现刷新/登出，登出即写 `token:blacklist:{userId}`，与本日过滤器第 4 步闭环。