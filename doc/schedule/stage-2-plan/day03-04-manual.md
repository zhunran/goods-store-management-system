# Day 3-4 操作执行手册 — 认证服务 + spi 契约 + web 收口脚手架

> 对应计划：[14-day-implementation-plan.md](file:///d:/.workspace/javaproject/goods-store-management-system-parent/doc/schedule/14-day-implementation-plan.md) Day 3-4
> 版本：v1.2（分层架构规范后首份手册）　编写日期：2026-09-01　执行日期：待填
> 主题：认证服务（RBAC + 登录注册 + 双 Token）＋ auth-spi 契约 ＋ web BFF 收口脚手架
> 依赖：Day 1-2 已完成（Nacos 配置中心 + 公共模块 + 6 个 API 服务接入配置中心 + Druid + 网关路由/JWT 鉴权）
> **架构前提**：Day 3 起强制遵循 14 天计划新增的「⭐ 分层架构规范」——spi 是**纯 HTTP 契约层**（禁 `@FeignClient`），web 是 **BFF 聚合层**（前端唯一入口 `/app/api/**`）。

---

## 一、Day 3-4 目标与产出物

### 目标

1. **auth-api** 交付完整认证能力：会员注册/登录、管理员登录（RBAC）、JWT 双 Token、刷新、登出黑名单、密码修改
2. **auth-spi** 交付纯 HTTP 契约 `AuthApi`，`AuthController implements AuthApi`（契约即实现）
3. **web 收口脚手架**落地：web 接 openfeign、`/app/api/auth/*` 三件套打通「前端→网关→web→Feign→auth-api」链路
4. 网关新增 `/app/api/**` 前端收口路由 + 登录白名单

### 产出物清单

| 类型           | 产出物                                                                                                          |
| -------------- | --------------------------------------------------------------------------------------------------------------- |
| auth-spi 模块  | `goods-store-auth-spi`：`AuthApi`（纯契约，无 `@FeignClient`）+ `LoginRequest/LoginResponse/RegisterRequest`    |
| RBAC 实体/映射 | `AdminUserEntity/RoleEntity/PermissionEntity/AdminUserRoleEntity/RolePermissionEntity` + 5 个 Mapper            |
| 安全配置       | `SecurityConfig`（放行登录/注册）+ `BCryptPasswordEncoder`                                                      |
| 认证接口       | `AuthController implements AuthApi`（register/login/refresh/logout/password）+ `AdminAuthController`            |
| JWT 工具       | `JwtUtil`（jjwt 0.12.6，Access 30min / Refresh 7天）+ `TokenService`（Redis）                                   |
| web 三件套     | `GoodsStoreWebApplication`（`@EnableFeignClients`）+ `AuthFeignClient` + `WebAuthService` + `WebAuthController` |
| 网关收口       | `/app/api/**` 前端路由 → `lb://goods-store-web`、登录白名单                                                     |

---

## 二、技术要点

### Day 3 技术要点

| 要点            | 说明                                                                                                                                                       |
| --------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Spring Security | 仅 auth-api 引入；`SecurityFilterChain` 放行 `/auth/api/login`、`/auth/api/register`，其余需认证                                                           |
| BCrypt 加密     | 密码用 `BCryptPasswordEncoder`，强度默认 10                                                                                                                |
| 会员/管理员分离 | 会员走 `member` 表（account+password），不参与 RBAC；管理员走 `admin_user` + RBAC 权限模型                                                                 |
| spi 契约即实现  | `AuthApi` 只写 `@PostMapping` + DTO/VO，**不加 `@FeignClient`**；`AuthController implements AuthApi`                                                       |
| web 收口链路    | 前端 `POST /app/api/auth/login` → 网关 `/app/api/**` → `lb://goods-store-web` → `WebAuthController` → `AuthFeignClient` → `lb://goods-store-auth-api`      |
| Feign 扫描      | `@EnableFeignClients(basePackages="com.fengluan")`：契约接口（无 `@FeignClient`）会被忽略，`XxxFeignClient`（web 侧定义，带 `@FeignClient`）才注册为客户端 |

### Day 4 技术要点

| 要点           | 说明                                                                                                      |
| -------------- | --------------------------------------------------------------------------------------------------------- |
| 双 Token       | Access(30min) + Refresh(7天)，Redis 存储；Refresh 换新 Access，Refresh 不变                               |
| 登出黑名单     | Access 写黑名单 `token:blacklist:{userId}:{role}`（TTL=剩余有效期），删除 `token:refresh:{userId}:{role}` |
| 密码修改       | 旧密码验签 + 新密码 BCrypt 加密                                                                           |
| 账号锁定       | 连续 3 次密码错误，账号临时锁定 15 分钟                                                                   |
| 网关黑名单对接 | `JwtAuthFilter` 校验白名单之外的请求时，先查 Redis 黑名单，命中则 401                                     |

---

## 三、Day 3-4 实施状态盘点

> 基于当前工程实测。

| 计划任务 | 内容                   | 当前状态                                                                            | 手册步骤 |
| -------- | ---------------------- | ----------------------------------------------------------------------------------- | -------- |
| 3.1      | auth-api POM 补充依赖  | ⚠️ 仅继承 service 父 POM，缺 common/auth-spi/security/redis/jjwt                    | 步骤 2   |
| 3.2/3.3  | RBAC 5 实体 + 5 Mapper | ❌ 尚未创建                                                                         | 步骤 3   |
| 3.4      | SecurityConfig         | ❌ 尚未创建                                                                         | 步骤 4   |
| 3.5/3.6  | 会员注册/登录          | ❌ 只有空启动类 `AuthApiApplication`                                                | 步骤 5/6 |
| 3.7      | 管理员登录             | ❌ 尚未创建                                                                         | 步骤 6   |
| 3.8      | auth-spi 契约          | ❌ `goods-store-auth-spi` 模块不存在（spi 下仅 brand/product/member/trade/seckill） | 步骤 1   |
| 3.9/3.10 | web 聚合脚手架         | ⚠️ web 无 openfeign/spi 依赖、无 Feign 扫描、端口 8083、仅有测试控制器              | 步骤 7   |
| 网关收口 | `/app/api/**` 前端路由 | ❌ 未配置（当前仅各 api 路由）                                                      | 步骤 8   |

---

## 四、详细操作步骤

### 前置说明（关于端口与 spi 契约约定）

- **web 端口**：当前 `goods-store-web/application.yaml` 是 `8083`，与 auth-api(8083) 冲突且作为前端收口入口，**统一改为 `8090`**。
- **spi 契约规范**：`auth-spi` 接口**不得写 `@FeignClient`**。消费方（web）自行定义带 `@FeignClient` 的客户端接口并 `extends AuthApi`。

---

### Day 3 操作步骤

#### 步骤 1：新建 goods-store-auth-spi 模块（任务 3.8）

**1a. 建模块目录**：`goods-store-spi/goods-store-auth-spi/`，并注册到聚合 POM。

编辑 [goods-store-spi/pom.xml](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-spi/pom.xml)，在 `<modules>` 中加入：

```xml
<module>goods-store-auth-spi</module>
```

**1b. auth-spi pom.xml**（parent 指向 goods-store-spi，仅携带契约所需 web/校验注解类）：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.fengluan</groupId>
        <artifactId>goods-store-spi</artifactId>
        <version>1.0.0</version>
    </parent>
    <artifactId>goods-store-auth-spi</artifactId>
    <packaging>jar</packaging>
    <dependencies>
        <!-- 契约接口需 @GetMapping/@RequestBody 等 -->
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-web</artifactId>
        </dependency>
        <!-- @NotBlank/@Email 等校验注解 -->
        <dependency>
            <groupId>jakarta.validation</groupId>
            <artifactId>jakarta.validation-api</artifactId>
        </dependency>
    </dependencies>
</project>
```

**1c. 契约类（纯接口，无 `@FeignClient`）**：

`src/main/java/com/fengluan/spi/auth/AuthApi.java`

```java
package com.fengluan.spi.auth;

import com.fengluan.spi.auth.dto.LoginRequest;
import com.fengluan.spi.auth.dto.LoginResponse;
import com.fengluan.spi.auth.dto.RegisterRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 认证服务纯 HTTP 契约（Day 3 起规范：spi 禁 @FeignClient，controller implements、消费方 extends 复用）
 */
public interface AuthApi {

    @PostMapping("/login")
    LoginResponse login(@RequestBody LoginRequest request);

    @PostMapping("/register")
    Void register(@RequestBody RegisterRequest request);
}
```

**1d. DTO/VO**（`com.fengluan.spi.auth.dto`）：

```java
// LoginRequest.java
public class LoginRequest {
    @NotBlank private String account;      // 会员 account / 管理员 username
    @NotBlank private String password;
    private String loginType;              // member / admin
}

// LoginResponse.java
public class LoginResponse {
    private String accessToken;            // 30min
    private String refreshToken;           // 7天
    private Long expiresIn;                // 1800 秒
    private UserInfo userInfo;             // 可内嵌静态类
}

// RegisterRequest.java
public class RegisterRequest {
    @NotBlank @Size(min = 3, max = 32) private String account;
    @NotBlank @Size(min = 6, max = 32) private String password;
    @Pattern(regexp = "^1[3-9]\\d{9}$") private String phone;
    @Email private String email;
}
```

> swagger/knife4j 若按 spi 契约描述接口，可在 `AuthApi` 方法上加 springdoc/knife4j 注解，后续 Day14 统一处理。

#### 步骤 2：auth-api POM 补充依赖（任务 3.1）

编辑 [goods-store-auth-api/pom.xml](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-service/goods-store-auth-api/pom.xml)，在 `<properties>` 后新增 `<dependencies>`：

```xml
<dependencies>
    <!-- 公共模块：ApiResult/ErrorCode/BaseEntity/Snowflake 等 -->
    <dependency>
        <groupId>com.fengluan</groupId>
        <artifactId>goods-store-common</artifactId>
        <version>1.0.0</version>
    </dependency>
    <!-- auth 纯契约（含 DTO/VO） -->
    <dependency>
        <groupId>com.fengluan</groupId>
        <artifactId>goods-store-auth-spi</artifactId>
        <version>1.0.0</version>
    </dependency>
    <!-- Spring Security（仅 auth-api） -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <!-- Redis（Refresh Token / 黑名单 / 账号锁定） -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    <!-- JWT：jjwt 0.12.6 API + 实现 + Jackson 序列化 -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.6</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.12.6</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.12.6</version>
        <scope>runtime</scope>
    </dependency>
    <!-- 参数校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
</dependencies>
```

#### 步骤 3：RBAC 5 实体 + 5 Mapper（任务 3.2/3.3）

在 `src/main/java/com/fengluan/auth/entity/` 下建 5 个实体，映射管理员侧 RBAC 表。均为简单 POJO：驼峰字段自动映射列、`Long id @TableId(type=IdType.AUTO)`、`@TableName("xxx")`、`@Data`。

| 实体                   | 表                | 备注                     |
| ---------------------- | ----------------- | ------------------------ |
| `AdminUserEntity`      | `admin_user`      | username/password/status |
| `RoleEntity`           | `role`            | code/name                |
| `PermissionEntity`     | `permission`      | code/name                |
| `AdminUserRoleEntity`  | `admin_user_role` | adminUserId/roleId       |
| `RolePermissionEntity` | `role_permission` | roleId/permissionId      |

示例（其余雷同）：

```java
@Data
@TableName("admin_user")
public class AdminUserEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;   // BCrypt
    private Integer status;    // 1=正常 0=禁用
}
```

`src/main/java/com/fengluan/auth/repository/` 下 5 个 Mapper，全部 `extends BaseMapper<Xxx>`：

```java
public interface AdminUserMapper extends BaseMapper<AdminUserEntity> {}
public interface RoleMapper extends BaseMapper<RoleEntity> {}
public interface PermissionMapper extends BaseMapper<PermissionEntity> {}
public interface AdminUserRoleMapper extends BaseMapper<AdminUserRoleEntity> {}
public interface RolePermissionMapper extends BaseMapper<RolePermissionEntity> {}
```

> 5 个 Mapper 需能被 MyBatis 扫描。在各服务中通常用 `@MapperScan("com.fengluan.auth.repository")`（放在启动类或配置类）。

#### 步骤 4：SecurityConfig（任务 3.4）

`src/main/java/com/fengluan/auth/config/SecurityConfig.java`：

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();          // 强度默认 10
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))  // 无状态，用 JWT 而非 Session
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/api/login", "/auth/api/register", "/auth/api/refresh").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint(
                (req, res, ex) -> res.setStatus(401)));                  // 未认证返回 401（暂不返回 JSON，Day14 统一）
        return http.build();
    }
}
```

> 说明：auth-api 内我们也会用 JWT，但 JWT 解签统一放在**网关** `JwtAuthFilter`（Day2 已建）。auth-api 的 Security 主要起兜底/注解鉴权作用，允许后续用 `@PreAuthorize` 控制管理员角色。

#### 步骤 5：JwtUtil（基础版，Day3 先能签发）

`src/main/java/com/fengluan/auth/util/JwtUtil.java`（jjwt 0.12.6 API）：

```java
package com.fengluan.auth.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtil {
    // 生产应从配置读取；此处示例固定密钥，长度须 >= 32 字节
    private static final String SECRET = "change-me-this-is-a-very-long-32byte-secret-key!!";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    private static final long ACCESS_MIN = 30 * 60 * 1000L;        // 30 分钟
    private static final long REFRESH_DAY = 7 * 24 * 60 * 60 * 1000L; // 7 天

    public String generateAccessToken(String userId, String type, List<String> roles) {
        return Jwts.builder()
                .subject(userId)
                .claim("type", type)
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ACCESS_MIN))
                .signWith(KEY)
                .compact();
    }

    public String generateRefreshToken(String userId) {
        return Jwts.builder()
                .subject(userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + REFRESH_DAY))
                .signWith(KEY)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser().verifyWith(KEY).build().parseSignedClaims(token).getPayload();
    }
}
```

#### 步骤 6：认证服务 + Controller（任务 3.5/3.6/3.7）

**6a. service**：`AuthService`（注册/登录）与 `AdminAuthService`（管理员登录）。

```java
@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthUserMapper authUserMapper;   // 映射 member 表（见下注）
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public void register(RegisterRequest req) {
        // 校验 account 唯一
        long c = authUserMapper.selectCount(new LambdaQueryWrapper<AuthUserEntity>()
                .eq(AuthUserEntity::getAccount, req.getAccount()));
        if (c > 0) throw new BusinessException(ErrorCode.ACCOUNT_EXISTS);
        AuthUserEntity e = new AuthUserEntity();
        e.setAccount(req.getAccount());
        e.setPassword(passwordEncoder.encode(req.getPassword()));
        e.setPhone(req.getPhone());
        e.setEmail(req.getEmail());
        authUserMapper.insert(e);
    }

    public LoginResponse login(LoginRequest req) {
        AuthUserEntity u = authUserMapper.selectOne(new LambdaQueryWrapper<AuthUserEntity>()
                .eq(AuthUserEntity::getAccount, req.getAccount()));
        if (u == null || !passwordEncoder.matches(req.getPassword(), u.getPassword()))
            throw new BusinessException(ErrorCode.LOGIN_FAILED, ErrorCode.LOGIN_FAILED); // 1003 密码错误
        return LoginResponse.ok(
                jwtUtil.generateAccessToken(u.getId().toString(), "member", List.of("ROLE_USER")),
                jwtUtil.generateRefreshToken(u.getId().toString()), 1800L);
    }
}
```

> 注：计划中会员登录用 `member` 表。若 `member` 归属 member-api 侧管理，这里可以先建一张轻量 `member` 读写说明；为 Day3-4 聚焦，采用 auth-api 内的 `AuthUserEntity` 映射 `member` 表即可（账号密码字段挂在此表）。后续 Day8 再拆分。

**6b. Controller implements AuthApi（契约即实现）+ 管理员登录**：

```java
@RestController
@RequestMapping("/auth/api")
@RequiredArgsConstructor
public class AuthController implements AuthApi {           // 兑现 spi 契约

    private final AuthService authService;

    @Override
    public LoginResponse login(LoginRequest request) { return authService.login(request); }

    @Override
    public Void register(RegisterRequest request) { authService.register(request); return null; }
}

@RestController
@RequestMapping("/auth/api")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/admin/login")
    public LoginResponse adminLogin(@RequestBody LoginRequest request) {
        return adminAuthService.login(request);   // 校验 admin_user + RBAC，返回 JWT + 角色权限列表
    }
}
```

> 契约路径与 controller 前缀一致：`@PostMapping("/login")` + `@RequestMapping("/auth/api")` → 实际 `POST /auth/api/login`，与网关 `/auth/api/**` 路由吻合。

#### 步骤 7：web 收口脚手架（任务 3.9/3.10）

**7a. web pom 加依赖**：编辑 [goods-store-web/pom.xml](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-web/pom.xml)，新增：

```xml
<!-- Feign 客户端（web 作为 BFF 聚合层调用各 spi 契约） -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
<!-- auth 契约 -->
<dependency>
    <groupId>com.fengluan</groupId>
    <artifactId>goods-store-auth-spi</artifactId>
    <version>1.0.0</version>
</dependency>
<!-- 公共模块（ApiResult/ErrorCode 等） -->
<dependency>
    <groupId>com.fengluan</groupId>
    <artifactId>goods-store-common</artifactId>
    <version>1.0.0</version>
</dependency>
```

> loadbalancer 已在 web POM，无需重复。

**7b. 启动类开启 Feign 扫描**：编辑 [GoodsStoreWebApplication.java](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-web/src/main/java/com/fengluan/GoodsStoreWebApplication.java)：

```java
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.fengluan")   // 仅注册带 @FeignClient 的接口；纯契约自动忽略
public class GoodsStoreWebApplication {
    public static void main(String[] args) {
        SpringApplication.run(GoodsStoreWebApplication.class, args);
    }
}
```

> 用 `basePackages = "com.fengluan"`：`AuthFeignClient` 在 web 包内（`com.fengluan.web.auth`），而 spi 契约接口在 `com.fengluan.spi` 且无 `@FeignClient`，扫描两者只会把带注解的客户端注册成 Feign。

**7c. web 端口改为 8090**：编辑 [application.yaml](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-web/src/main/resources/application.yaml)：

```yaml
server:
  port: 8090
```

**7d. web 认证三件套**：

`src/main/java/com/fengluan/web/auth/AuthFeignClient.java`：

```java
@FeignClient(name = "goods-store-auth-api", path = "/auth/api")
public interface AuthFeignClient extends AuthApi {}        // 复用 spi 契约
```

`src/main/java/com/fengluan/web/auth/WebAuthService.java`：

```java
@Service
@RequiredArgsConstructor
public class WebAuthService {
    private final AuthFeignClient authFeignClient;

    public LoginResponse login(LoginRequest req) { return authFeignClient.login(req); }
}
```

`src/main/java/com/fengluan/web/auth/WebAuthController.java`：

```java
@RestController
@RequestMapping("/app/api/auth")
@RequiredArgsConstructor
public class WebAuthController {
    private final WebAuthService webAuthService;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return webAuthService.login(request);
    }
}
```

#### 步骤 8：网关前端收口路由（任务 3.9 配套）

编辑 [gateway/application.yaml](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-gateway/src/main/resources/application.yaml)，在 `server.webflux.routes` 顶部加 **web 收口路由**（顺序在前，避免被 `/auth/api/**` 等吞掉前缀冲突；二者路径不同不会冲突，但放前面更清晰）：

```yaml
server:
  webflux:
    routes:
      - id: web-frontend # 前端唯一入口：/app/api/** → web（BFF 聚合）
        uri: lb://goods-store-web
        predicates:
          - Path=/app/api/**
      - id: auth-api
        uri: lb://goods-store-auth-api
        predicates:
          - Path=/auth/api/**
      # ……其余 api 路由保持不变（供内部 Feign / 排障）
```

> 再把登录/注册/刷新加入网关白名单（Day2 的 `WhiteListConfig` 里追加）：`/app/api/auth/login`、`/app/api/auth/register`、`/app/api/auth/refresh`。

---

### Day 4 操作步骤

#### 步骤 9：JwtUtil 完善 + TokenService（任务 4.1/4.5）

在 Day3 JwtUtil 基础上，补充 `validateToken()`（校验签名+过期）；新增 `TokenService` 封装 Redis：

```java
@Service
@RequiredArgsConstructor
public class TokenService {
    private final StringRedisTemplate redis;

    /** 校验未过期 + 校验签名 */
    public boolean validate(String token) { /* parseToken 抛异常则 false */ }

    /** Refresh Token 落 Redis，7 天 */
    public void saveRefresh(String userId, String role, String refreshToken) {
        redis.opsForValue().set("token:refresh:" + userId + ":" + role,
                refreshToken, 7, TimeUnit.DAYS);
    }

    /** 登出：Access 写黑名单（TTL=剩余有效期），删 Refresh */
    public void blacklist(String accessToken) {
        Claims c = jwtUtil.parseToken(accessToken);
        long remain = c.getExpiration().getTime() - System.currentTimeMillis();
        if (remain > 0)
            redis.opsForValue().set("token:blacklist:" + c.getSubject() + ":" + c.get("type"),
                    accessToken, remain, TimeUnit.MILLISECONDS);
        redis.delete("token:refresh:" + c.getSubject() + ":" + c.get("type"));
    }

    public boolean isBlacklisted(String token) {
        Claims c = jwtUtil.parseToken(token);
        return Boolean.TRUE.equals(
                redis.hasKey("token:blacklist:" + c.getSubject() + ":" + c.get("type")));
    }
}
```

#### 步骤 10：Aaauth 接口补刷新/登出/密码（任务 4.2/4.3/4.4）

在 `AuthController`（或独立 `AuthTokenController`）追加：

```java
@PostMapping("/refresh")
public LoginResponse refresh(@RequestBody RefreshRequest req) {
    // 校验 Redis 中的 token:refresh:uid:role 与传来的 refreshToken 一致，再发新 Access
    return authService.refresh(req.getRefreshToken());
}

@PostMapping("/logout")
public Void logout(@RequestHeader("Authorization") String auth) {
    tokenService.blacklist(auth.replace("Bearer ", ""));
    return null;
}

@PutMapping("/password")
public Void changePassword(@RequestHeader("Authorization") String auth,
                           @RequestBody ChangePwdRequest req) {
    // 旧密码验签 → 新密码 BCrypt 加密落库
    authService.changePassword(auth.replace("Bearer ", ""), req);
    return null;
}
```

**账号锁定**：login 失败计数用 Redis 自增，Key `login:fail:{account}`，达 3 次写入 `login:lock:{account}`（TTL 15min）并拒绝登录。

#### 步骤 11：网关 JWT 过滤器黑名单对接（任务 4.6）

在 Day2 已建的 [JwtAuthFilter.java](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-gateway/src/main/java/com/fengluan/gateway/filter/JwtAuthFilter.java) 中，白名单校验通过后的鉴权逻辑里追加黑名单检查：

```java
// 在拿到 accessToken 并 parse 成功后：
if (redis.hasKey("token:blacklist:" + subject + ":" + type)) {
    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
    return exchange.getResponse().setComplete();   // 已登出的 token 拒绝
}
```

> 使网关能拿到 subject/type，只需在过滤器里先解析 JWT（jjwt 已在网关 Day2 引入）；Redis 客户端网关也有（Day2 已加 data-redis）。

#### 步骤 12：web 认证聚合完型（任务 4.7）

`WebAuthController` 补齐 refresh/logout/password 、`WebAuthService` 对应转调 `AuthFeignClient`：

```java
@RestController
@RequestMapping("/app/api/auth")
@RequiredArgsConstructor
public class WebAuthController {
    private final WebAuthService webAuthService;

    @PostMapping("/login")    public LoginResponse login(@RequestBody LoginRequest r) { return webAuthService.login(r); }
    @PostMapping("/register") public Void register(@RequestBody RegisterRequest r) { return webAuthService.register(r); }
    @PostMapping("/refresh")  public LoginResponse refresh(@RequestBody RefreshRequest r) { return webAuthService.refresh(r); }
    @PostMapping("/logout")   public Void logout(@RequestHeader("Authorization") String auth) { return webAuthService.logout(auth); }
    @PutMapping("/password")  public Void password(@RequestHeader("Authorization") String auth, @RequestBody ChangePwdRequest r) { return webAuthService.changePassword(auth, r); }
}
```

> refresh/logout/password 需进 `AuthApi` 契约（跨服务/跨端复用）：Day4 在 `AuthApi` 增加 `@PostMapping("/refresh")`、`@PostMapping("/logout")`、`@PutMapping("/password")`，`AuthController implements` + `AuthFeignClient extends` 自动补齐。

---

## 五、验收标准

### Day 3 验收

- [ ] `goods-store-auth-spi` 建好，`AuthApi` **无** `@FeignClient`；`spi/pom.xml` `<modules>` 已注册
- [ ] 启动 auth-api 注册到 Nacos（服务名 `goods-store-auth-api`）；`AuthController implements AuthApi` 编译通过
- [ ] `POST /auth/api/register` 注册新会员，`member` 表写入 BCrypt 加密密码
- [ ] `POST /auth/api/login` 会员登录返回 Access + Refresh Token
- [ ] 密码错误登录返回 1003 错误码
- [ ] `POST /auth/api/admin/login` 管理员登录返回 JWT + 角色权限列表
- [ ] 未登录访问 auth 受保护接口返回 401
- [ ] web 启动于 **8090**，`@EnableFeignClients` 生效
- [ ] 网关新增 `/app/api/**` → web 路由；白名单含 `/app/api/auth/login|register|refresh`
- [ ] **web 收口首个链路**：`POST /app/api/auth/login`（经网关）→ web → Feign → auth-api 返回 Token

### Day 4 验收

- [ ] 用 Refresh Token 调 `/auth/api/refresh` 返回新 Access Token（Refresh 不变）
- [ ] 过期 Refresh Token 刷新返回 401
- [ ] 登出后用旧 Access Token 访问受保护接口返回 401（黑名单生效）
- [ ] 密码修改后旧密码登录失败、新密码登录成功
- [ ] 连续 3 次密码错误，账号临时锁定 15 分钟
- [ ] 网关 `JwtAuthFilter` 黑名单检查生效
- [ ] **web 收口**：`/app/api/auth/refresh|logout|password` 经 `WebAuthService` 代理成功，前端全程未直连 `/auth/api/**`

---

## 六、端口分配表

| 服务                 | 端口 | 说明                                  |
| -------------------- | ---- | ------------------------------------- |
| goods-store-gateway  | 8888 | 网关，前端唯一入口                    |
| goods-store-web      | 8090 | BFF 聚合层（**Day2 由 8083 改到此**） |
| goods-store-auth-api | 8083 | 认证服务                              |
| Nacos                | 8848 | 配置中心                              |
| Nacos 控制台         | 8848 | http://localhost:8848/nacos           |
| Sentinel Dashboard   | 8080 | 限流控制台                            |

> web 若此前与 auth 同为 8083，务必按本手册改到 8090；同步检查 web `application.yaml` 的 `server.port`。

---

## 七、常见问题 / 回滚

| 现象                                             | 处理                                                                                                                   |
| ------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------- |
| auth-api 启动报缺 `DataSource`/Druid             | auth 暂不连库则从 `goods-store-common.yaml` 移除其 druid 数据源配置；连库则按 Day2 手写 `DruidDataSourceConfig`        |
| web Feign 客户端不生效（404 / Attempt to proxy） | 确认 `@EnableFeignClients(basePackages="com.fengluan")`；`AuthFeignClient` 必在 `com.fengluan` 包下且带 `@FeignClient` |
| spi 契约被误注册成 Feign                         | 契约接口不加 `@FeignClient`；只有 `XxxFeignClient extends XxxApi` 才加                                                 |
| `/app/api/auth/login` 经网关 404                 | 先直查 auth-api `/auth/api/login`；再确认网关路由顺序与 web 已注册 Nacos                                               |
| Security 拦截了 Feign/内部调用                   | 确认白名单含 refresh/logout；或对内部 Feign 头透传（Day14 统一）                                                       |
| 回滚                                             | 还原 `spi/pom.xml`、`auth-api/pom.xml`、web POM/启动类/端口、网关路由新增行即可；新模块文件删除即可                    |
