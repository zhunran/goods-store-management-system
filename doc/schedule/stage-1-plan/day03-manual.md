# Day 3 操作执行手册 — 认证服务（上）：项目搭建 + RBAC 实体 + 登录注册

> 对应计划：[14-day-implementation-plan.md](file:///d:/.workspace/javaproject/goods-store-management-system-parent/doc/schedule/14-day-implementation-plan.md) Day 3（L303-407）
> 版本：v1.0　编写日期：2026-09-01　执行日期：待填
> 主题：auth-api 认证服务搭建 + RBAC 5 表实体/Mapper + 安全配置 + 会员/管理员登录注册
> 依赖：Day 1/2 已完成（Nacos 配置中心 + 公共模块 + 网关 JWT 鉴权 + Druid + 健康检查）

---

## 一、Day 3 目标与产出物

### 目标

1. 补齐 auth-api 服务骨架（依赖 + 启动类修正），注册为 `goods-store-auth-api`
2. 建立 **RBAC 5 表** 权限模型（仅后台管理员使用），产出 Entity + Mapper
3. 引入 Spring Security（仅 auth-api），配置 `SecurityFilterChain` 放行登录/注册
4. 实现 **会员注册/登录** 与 **管理员登录**，输出「供网关校验」的双 Token

### 产出物清单

| 类型        | 产出物                                                                    |
| ----------- | ------------------------------------------------------------------------- |
| POM 依赖    | auth-api 补 Security + jjwt×3 + data-redis + common                       |
| 启动类修复  | `AuthApiApplication` 的 `main` 补 `public`                                |
| RBAC 实体   | `AdminUser/Role/Permission/AdminUserRole/RolePermission` 5 个 Entity      |
| RBAC Mapper | 上述 5 表 Mapper（继承 `BaseMapper`）                                     |
| 会员映射    | auth 侧 `MemberEntity/MemberMapper`（读写同一 `member` 表）               |
| 安全配置    | `SecurityConfig`（BCrypt + 请求放行）                                     |
| JWT 工具    | `JwtProperties/JwtConfig/JwtUtil/TokenPairDTO`（签发侧，jjwt 0.12.6）     |
| 服务层      | `AuthService/AuthServiceImpl`、`AdminAuthService`、`AdminAuthServiceImpl` |
| 控制器      | `AuthController`（register/login）、`AdminAuthController`（admin/login）  |
| DTO         | `RegisterRequest/LoginRequest/LoginResponse/UserInfoDTO`                  |

---

## 二、Day 3 实施状态盘点（基于当前实际现状）

| 计划任务 | 内容                | 当前状态                                                                    | 手册步骤 |
| -------- | ------------------- | --------------------------------------------------------------------------- | -------- |
| 3.1      | auth-api POM 补依赖 | ❌ 仅父模块继承；**缺 Security/jjwt/redis/common**                          | 步骤 1   |
| 前置     | 启动类修复          | ⚠️ 已存在但 `main` 无 `public`，需修                                        | 步骤 0   |
| 3.2      | RBAC 5 表 Entity    | ❌ 未创建（目录均不存在）                                                   | 步骤 3   |
| 3.3      | RBAC Mapper         | ❌ 未创建                                                                   | 步骤 4   |
| 3.4      | SecurityConfig      | ❌ 未创建                                                                   | 步骤 5   |
| 3.5      | 会员注册            | ❌ 未创建（需 `MemberMapper` 写入 member 表）                               | 步骤 7   |
| 3.6      | 会员登录            | ❌ 未创建                                                                   | 步骤 7   |
| 3.7      | 管理员登录          | ❌ 未创建（需 RBAC 联查角色权限）                                           | 步骤 8   |
| 基建复用 | 公共异常兜底        | ⚠️ `GlobalExceptionHandler` 的 handler 被注释，需启用才能返回 1003 等业务码 | 步骤 6   |

> **端口/雪花分配**（沿用 Day 2 手册 §五）：auth=8083、worker-id=1。Day 2 遗留的「仓库 auth=8083 与 web=8083 撞端口」问题**仍是本日前置**，执行前须再次确认 web 已改到空闲端口（推荐 8090），否则两者同时启动会失败。

---

## 三、详细操作步骤

### 步骤 0：修正启动类（前置，必做）

当前 [AuthApiApplication.java](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-service/goods-store-auth-api/src/main/java/com/fengluan/AuthApiApplication.java) 的 `main` 缺少 `public`，会编译通过但**无法作为启动入口**。改为：

```java
package com.fengluan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class AuthApiApplication {

    public static void main(String[] args) {   // 注意补 public
        SpringApplication.run(AuthApiApplication.class, args);
    }
}
```

> 说明：`@SpringBootApplication` 扫描包根为 `com.fengluan`，因此 `com.fengluan.auth.**`（本日新增代码）与 `com.fengluan.common.**`（公共模块）都会被自动扫描到，无需额外 `@ComponentScan`。

---

### 步骤 1：auth-api 补依赖（任务 3.1）

在 [auth-api/pom.xml](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-service/goods-store-auth-api/pom.xml) 的 `<dependencies>`（当前为空）追加。父模块 service 已托管 webmvc/lombok/nacos/sentinel/druid/mysql/mybatis-plus/actuator 及 JDK25 lombok 注解处理器（子模块自动继承），故只需补 4 类：

```xml
<dependencies>
    <!-- common 公共模块：ApiResult / BusinessException / ErrorCode / SnowflakeUtil -->
    <dependency>
        <groupId>com.fengluan</groupId>
        <artifactId>goods-store-common</artifactId>
        <version>${project.version}</version>
    </dependency>

    <!-- Spring Security（仅认证服务引入），版本由 spring-boot-starter-parent 托管 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <!-- Redis：Access/Refresh Token 存储与黑名单（Day 4 刷新/登出用） -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- JWT 双令牌（版本已在根 pom dependencyManagement 管理为 0.12.6） -->
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
</dependencies>
```

> ⚠️ `goods-store-common` 使用 `${project.version}`（1.0.0）而非写死，小版本升级自动对齐。若需在父模块统一托管版本，可后续把 common 加进根 pom dependencyManagement（本日先直接引）。

---

### 步骤 2：JWT 配置与工具（签发侧）

> 与网关侧（Day 2）**共用同一 `jwt.secret`**。建议把 `jwt.secret` 放进 Nacos **共享配置 `goods-store-common.yaml`**（common.yaml 里的 `jwt` 段），这样 auth 签发与网关校验天然一致；二者读取的 prefix 均为 `jwt`。

**① `JwtProperties`** `com/fengluan/auth/config/JwtProperties.java`：

```java
package com.fengluan.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 与网关 JwtProperties(prefix=jwt) 同源：读取 goods-store-common.yaml 的 jwt.* */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    /** HS256 签名密钥，≥32 字节，与网关一致 */
    private String secret;
    /** Access Token 有效期（秒），默认 1800 */
    private long accessExpireSeconds = 1800L;
    /** Refresh Token 有效期（秒），默认 604800（7 天） */
    private long refreshExpireSeconds = 604800L;
}
```

**② `JwtUtil`（签发侧）** `com/fengluan/auth/util/JwtUtil.java`（jjwt 0.12.6 新 API）：

```java
package com.fengluan.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

/** 认证服务签发/解析 JWT（网关只解析不签发） */
public final class JwtUtil {

    private JwtUtil() {}

    private static SecretKey key(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 签发：type=member|admin，sub=用户ID，roles=角色列表，i/exp 由调用方传入 */
    public static String createToken(String secret, String subject, String type,
                                     List<String> roles, long expiresInSeconds) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(subject)                                    // sub = 用户ID
                .claims(Map.of("type", type, "roles", roles))        // 自定义 claims
                .issuedAt(new Date(now))                              // iat
                .expiration(new Date(now + expiresInSeconds * 1000))  // exp
                .signWith(key(secret))
                .compact();
    }

    /** 解析（供校验用），鉴权实际以网关为准 */
    public static Claims parseToken(String secret, String token) {
        return Jwts.parser().verifyWith(key(secret)).build()
                .parseSignedClaims(token).getPayload();
    }
}
```

---

### 步骤 3：RBAC 5 表 Entity（任务 3.2）

> 仅后台管理员使用；表名与字段请与你本地 SQL（见步骤 9）保持一致。实体统一放 `com/fengluan/auth/entity/`，用 Lombok `@Data`（JDK25 处理器已由父模块继承配好）。

**① `AdminUserEntity`** → `admin_user`

```java
package com.fengluan.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin_user")
public class AdminUserEntity {
    /** 管理员ID（雪花生成） */
    @TableId(type = IdType.INPUT)
    private Long id;
    private String username;
    private String password;        // BCrypt 加密
    private String nickname;
    private Integer enabled;        // 1=启用 0=禁用
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
```

**② `RoleEntity`** → `role`

```java
package com.fengluan.auth.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("role")
public class RoleEntity {
    @TableId
    private Long id;
    private String roleName;        // 如 ROLE_ADMIN / ROLE_OPERATOR
    private String roleDesc;
}
```

**③ `PermissionEntity`** → `permission`

```java
package com.fengluan.auth.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("permission")
public class PermissionEntity {
    @TableId
    private Long id;
    private String permName;        // 如 brand:create / order:list
    private String permDesc;
}
```

**④ `AdminUserRoleEntity`** → `admin_user_role`

```java
package com.fengluan.auth.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("admin_user_role")
public class AdminUserRoleEntity {
    @TableId
    private Long id;
    private Long adminUserId;
    private Long roleId;
}
```

**⑤ `RolePermissionEntity`** → `role_permission`

```java
package com.fengluan.auth.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("role_permission")
public class RolePermissionEntity {
    @TableId
    private Long id;
    private Long roleId;
    private Long permissionId;
}
```

**⑥ 会员映射**（放在 auth 侧复用，不依赖 member-api）

```java
package com.fengluan.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** auth 侧只关心登录相关列，其余相对 member-api 的 MemberEntity 可后续补充 */
@Data
@TableName("member")
public class MemberEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String account;
    private String password;        // BCrypt 加密
    private Integer enabled;        // 1=启用 0=禁用
    private String phone;
    private String email;
    private LocalDateTime createdTime;
    private LocalDateTime lastLoginTime;
}
```

---

### 步骤 4：Mapper（任务 3.3）

全部放在 `com/fengluan/auth/repository/`，继承 MyBatis-Plus `BaseMapper`，无需手写 XML：

- `AdminUserMapper` → `AdminUserEntity`
- `RoleMapper` → `RoleEntity`
- `PermissionMapper` → `PermissionEntity`
- `AdminUserRoleMapper` → `AdminUserRoleEntity`
- `RolePermissionMapper` → `RolePermissionEntity`
- `MemberMapper` → `MemberEntity`

以 `AdminUserMapper` 为例，其余 5 个照抄替换泛型：

```java
package com.fengluan.auth.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fengluan.auth.entity.AdminUserEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminUserMapper extends BaseMapper<AdminUserEntity> {
}
```

> ⚠️ 所有 Mapper 必须标 `@org.apache.ibatis.annotations.Mapper`，否则 MyBatis-Plus 扫描不到，注入会报 `Field xxxMapper required a bean`.

---

### 步骤 5：SecurityConfig（任务 3.4）

`com/fengluan/auth/config/SecurityConfig.java`。Boot 4.1.1 → Spring Security 7.x，仍用 `SecurityFilterChain` Bean 方式。**要点：无状态（JWT 由网关统一鉴权），仅放行登录/注册/健康检查，其余返回 401**：

```java
package com.fengluan.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private static final String[] WHITELIST = {
            "/auth/api/login",
            "/auth/api/register",
            "/auth/api/admin/login",
            "/actuator/**"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // 默认强度 10，满足计划要求
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)              // 无状态 API，关闭 CSRF
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(WHITELIST).permitAll()      // 登录/注册放行
                        .anyRequest().authenticated())              // 其余需认证
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
```

> 说明：真实权限校验已由网关 `JwtAuthFilter`（Day 2）承担，auth-api 的 Security 主要做**本地兜底**（保证不经网关直接访问受保护接口也返回 401）与密码加密注入。

---

### 步骤 6：启用公共异常兜底（前置修正）

共享模块 [GlobalExceptionHandler.java](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-common/src/main/java/com/fengluan/common/exception/GlobalExceptionHandler.java) 的 `@ExceptionHandler(BusinessException.class)` 与兜底 `Exception` 处理器**当前被注释**。若不加，会员登录密码错误抛出的 `BusinessException` 不会被转成 `{"code":1003}` 响应，导致验收第 4 项失败。请把注释掉的两个方法恢复为生效状态：

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResult<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常：code={}, message={}", new Object[]{e.getCode(), e.getMessage()});
        return ApiResult.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResult<?> handleException(Exception e) {
        log.error("系统异常：", e);
        return ApiResult.error(ErrorCode.INTERNAL_ERROR.getCode(), "服务器繁忙，请稍后重试");
    }
}
```

> 说因该文件在 `com.fengluan.common` 包，被所有服务共享。启用后对登录 401/参数异常等同样生效，属预期增强。若希望登录失败密码错误严格返回 1003（`ErrorCode.MEMBER_PASSWORD_ERROR`）、管理员密码错误返回 6002（`AUTH_PASSWORD_ERROR`），服务层对应抛对应枚举即可（见步骤 7/8）。

---

### 步骤 7：DTO 与服务层（会员注册/登录，任务 3.5/3.6）

**① DTO** 放 `com/fengluan/auth/dto/`

```java
// RegisterRequest.java
package com.fengluan.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank @Size(min = 3, max = 32) private String account;
    @NotBlank @Size(min = 6, max = 32) private String password;
    @Pattern(regexp = "^1[3-9]\\d{9}$") private String phone;
    @Email private String email;
}
```

```java
// LoginRequest.java
package com.fengluan.auth.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String account;
    private String password;
}
```

```java
// UserInfoDTO.java
package com.fengluan.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoDTO {
    private Long id;
    private String account;
    private String nickname;
}
```

```java
// LoginResponse.java
package com.fengluan.auth.dto;

import lombok.Data;

import java.util.List;

@Data
public class LoginResponse {
    private String accessToken;     // 30min
    private String refreshToken;    // 7天
    private Long expiresIn;         // 1800 秒
    private UserInfoDTO userInfo;
    private List<String> roles;     // 管理员返回角色，会员固定 ["ROLE_USER"]
    private List<String> permissions; // 管理员返回权限列表，会员为空
}
```

**② `AuthService`**（接口 + 实现）`com/fengluan/auth/service/`。注册校验 `account` 唯一性 → BCrypt 加密入库；登录校验密码 → 签发双 Token：

```java
// AuthService.java
package com.fengluan.auth.service;

import com.fengluan.auth.dto.LoginRequest;
import com.fengluan.auth.dto.LoginResponse;
import com.fengluan.auth.dto.RegisterRequest;

public interface AuthService {
    void register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
}
```

```java
// AuthServiceImpl.java
package com.fengluan.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fengluan.auth.config.JwtProperties;
import com.fengluan.auth.dto.LoginRequest;
import com.fengluan.auth.dto.LoginResponse;
import com.fengluan.auth.dto.RegisterRequest;
import com.fengluan.auth.dto.UserInfoDTO;
import com.fengluan.auth.entity.MemberEntity;
import com.fengluan.auth.repository.MemberMapper;
import com.fengluan.auth.util.JwtUtil;
import com.fengluan.common.exception.BusinessException;
import com.fengluan.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;

    @Override
    public void register(RegisterRequest request) {
        Long count = memberMapper.selectCount(
                Wrappers.<MemberEntity>lambdaQuery().eq(MemberEntity::getAccount, request.getAccount()));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.MEMBER_ACCOUNT_EXISTS);   // 1002
        }
        MemberEntity m = new MemberEntity();
        m.setAccount(request.getAccount());
        m.setPassword(passwordEncoder.encode(request.getPassword()));
        m.setEnabled(1);
        m.setPhone(request.getPhone());
        m.setEmail(request.getEmail());
        m.setCreatedTime(LocalDateTime.now());
        memberMapper.insert(m);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        MemberEntity m = memberMapper.selectOne(
                Wrappers.<MemberEntity>lambdaQuery().eq(MemberEntity::getAccount, request.getAccount()));
        if (m == null) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);        // 1001
        }
        if (m.getEnabled() != null && m.getEnabled() == 0) {
            throw new BusinessException(ErrorCode.MEMBER_DISABLED);         // 1004
        }
        if (!passwordEncoder.matches(request.getPassword(), m.getPassword())) {
            throw new BusinessException(ErrorCode.MEMBER_PASSWORD_ERROR);   // 1003
        }

        String accessToken = JwtUtil.createToken(jwtProperties.getSecret(),
                String.valueOf(m.getId()), "member", List.of("ROLE_USER"),
                jwtProperties.getAccessExpireSeconds());
        String refreshToken = JwtUtil.createToken(jwtProperties.getSecret(),
                String.valueOf(m.getId()), "member", List.of("ROLE_USER"),
                jwtProperties.getRefreshExpireSeconds());

        // 更新最后登录时间
        MemberEntity upd = new MemberEntity();
        upd.setId(m.getId());
        upd.setLastLoginTime(LocalDateTime.now());
        memberMapper.updateById(upd);

        LoginResponse resp = new LoginResponse();
        resp.setAccessToken(accessToken);
        resp.setRefreshToken(refreshToken);
        resp.setExpiresIn(jwtProperties.getAccessExpireSeconds());
        resp.setUserInfo(new UserInfoDTO(m.getId(), m.getAccount(), null));
        resp.setRoles(List.of("ROLE_USER"));
        return resp;
    }
}
```

---

### 步骤 8：管理员登录（任务 3.7）

在 `service/` 下新增 `AdminAuthService`，按 `admin_user → admin_user_role → role → role_permission → permission` 联查，返回 JWT + 角色 + 权限列表：

```java
// AdminAuthService.java
package com.fengluan.auth.service;

import com.fengluan.auth.dto.LoginRequest;
import com.fengluan.auth.dto.LoginResponse;

public interface AdminAuthService {
    LoginResponse adminLogin(LoginRequest request);
}
```

```java
// AdminAuthServiceImpl.java
package com.fengluan.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fengluan.auth.config.JwtProperties;
import com.fengluan.auth.dto.LoginRequest;
import com.fengluan.auth.dto.LoginResponse;
import com.fengluan.auth.dto.UserInfoDTO;
import com.fengluan.auth.entity.*;
import com.fengluan.auth.repository.*;
import com.fengluan.auth.util.JwtUtil;
import com.fengluan.common.exception.BusinessException;
import com.fengluan.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private final AdminUserMapper adminUserMapper;
    private final AdminUserRoleMapper adminUserRoleMapper;
    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;

    @Override
    public LoginResponse adminLogin(LoginRequest request) {
        AdminUserEntity admin = adminUserMapper.selectOne(
                Wrappers.<AdminUserEntity>lambdaQuery()
                        .eq(AdminUserEntity::getUsername, request.getAccount()));
        if (admin == null) {
            throw new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND);     // 6001
        }
        if (admin.getEnabled() != null && admin.getEnabled() == 0) {
            throw new BusinessException(ErrorCode.AUTH_USER_DISABLED);      // 6003
        }
        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new BusinessException(ErrorCode.AUTH_PASSWORD_ERROR);     // 6002
        }

        // 联查角色 + 权限
        Set<String> roleNames = new LinkedHashSet<>();
        Set<String> perms = new LinkedHashSet<>();
        List<AdminUserRoleEntity> ars = adminUserRoleMapper.selectList(
                Wrappers.<AdminUserRoleEntity>lambdaQuery()
                        .eq(AdminUserRoleEntity::getAdminUserId, admin.getId()));
        for (AdminUserRoleEntity ar : ars) {
            RoleEntity role = roleMapper.selectById(ar.getRoleId());
            if (role == null) continue;
            roleNames.add(role.getRoleName());
            List<RolePermissionEntity> rps = rolePermissionMapper.selectList(
                    Wrappers.<RolePermissionEntity>lambdaQuery()
                            .eq(RolePermissionEntity::getRoleId, ar.getRoleId()));
            for (RolePermissionEntity rp : rps) {
                PermissionEntity perm = permissionMapper.selectById(rp.getPermissionId());
                if (perm != null) perms.add(perm.getPermName());
            }
        }
        List<String> roles = new java.util.ArrayList<>(roleNames);

        String accessToken = JwtUtil.createToken(jwtProperties.getSecret(),
                String.valueOf(admin.getId()), "admin", roles,
                jwtProperties.getAccessExpireSeconds());
        String refreshToken = JwtUtil.createToken(jwtProperties.getSecret(),
                String.valueOf(admin.getId()), "admin", roles,
                jwtProperties.getRefreshExpireSeconds());

        LoginResponse resp = new LoginResponse();
        resp.setAccessToken(accessToken);
        resp.setRefreshToken(refreshToken);
        resp.setExpiresIn(jwtProperties.getAccessExpireSeconds());
        resp.setUserInfo(new UserInfoDTO(admin.getId(), admin.getUsername(), admin.getNickname()));
        resp.setRoles(roles);
        resp.setPermissions(new java.util.ArrayList<>(perms));
        return resp;
    }
}
```

---

### 步骤 9：Controller（对外出口）

> auth-api 是 HTTP 出口，Controller 直接调 service。统一返回 `ApiResult<T>`。

**① `AuthController`** `com/fengluan/auth/api/AuthController.java`

```java
package com.fengluan.auth.api;

import com.fengluan.auth.dto.LoginRequest;
import com.fengluan.auth.dto.LoginResponse;
import com.fengluan.auth.dto.RegisterRequest;
import com.fengluan.auth.service.AuthService;
import com.fengluan.common.result.ApiResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResult<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResult.success();
    }

    @PostMapping("/login")
    public ApiResult<LoginResponse> login(@RequestBody LoginRequest request) {
        return ApiResult.success(authService.login(request));
    }
}
```

**② `AdminAuthController`** `com/fengluan/auth/api/AdminAuthController.java`

```java
package com.fengluan.auth.api;

import com.fengluan.auth.dto.LoginRequest;
import com.fengluan.auth.dto.LoginResponse;
import com.fengluan.auth.service.AdminAuthService;
import com.fengluan.common.result.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/api/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/login")
    public ApiResult<LoginResponse> login(@RequestBody LoginRequest request) {
        return ApiResult.success(adminAuthService.adminLogin(request));
    }
}
```

> 网关白名单（Day 2 `WhiteListConfig`）已含 `/auth/api/login`、`/auth/api/register`，**但未含 `/auth/api/admin/login`**，需在网关白名单追加 `/auth/api/admin/login`，否则管理员登录会被网关 401 拦截。这是 Day 3 的**跨服务联动点**。

---

### 步骤 10：数据库（RBAC 表 + 种子数据）

> `member` 表已由 member 侧使用，无需重建。以下为 RBAC 5 表 DDL + 管理员种子（含 BCrypt 密码）。请按你本地库执行，**注意统一对应步骤 3 实体的表名字段**。

```sql
-- 管理员表
CREATE TABLE IF NOT EXISTS admin_user (
    id            BIGINT PRIMARY KEY,
    username      VARCHAR(50) NOT NULL,
    password      VARCHAR(100) NOT NULL,
    nickname      VARCHAR(50),
    enabled       TINYINT DEFAULT 1,
    created_time  DATETIME,
    updated_time  DATETIME,
    UNIQUE KEY uk_username (username)
);

-- 角色表
CREATE TABLE IF NOT EXISTS role (
    id         BIGINT PRIMARY KEY,
    role_name  VARCHAR(50) NOT NULL,
    role_desc  VARCHAR(200)
);

-- 权限表
CREATE TABLE IF NOT EXISTS permission (
    id         BIGINT PRIMARY KEY,
    perm_name  VARCHAR(100) NOT NULL,
    perm_desc  VARCHAR(200)
);

-- 管理员-角色关联
CREATE TABLE IF NOT EXISTS admin_user_role (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    admin_user_id BIGINT NOT NULL,
    role_id       BIGINT NOT NULL
);

-- 角色-权限关联
CREATE TABLE IF NOT EXISTS role_permission (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL
);
```

种子数据（`password` 均 `"123456"` 的 BCrypt 密文，登录时用 `123456`）：

```sql
INSERT INTO admin_user (id, username, password, nickname, enabled) VALUES
(1, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '超级管理员', 1);
-- 说明：上述密文为 "123456" 的 BCrypt（强度10），如不确定可先用代码生成后替换

INSERT INTO role (id, role_name, role_desc) VALUES
(1, 'ROLE_ADMIN', '超级管理员'),
(2, 'ROLE_OPERATOR', '运营管理员');

INSERT INTO permission (id, perm_name, perm_desc) VALUES
(1, 'brand:create', '创建品牌'),
(2, 'brand:delete', '删除品牌'),
(3, 'order:list', '查看订单'),
(4, 'order:return', '处理退货');

INSERT INTO admin_user_role (admin_user_id, role_id) VALUES (1, 1);
INSERT INTO role_permission (role_id, permission_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4),   -- ROLE_ADMIN 全权限
(2, 3);                             -- ROLE_OPERATOR 仅 order:list
```

> ⚠️ 若 BCrypt 密文不准，可临时用如下方式生成后替换第 1 条插入：
>
> ```java
> System.out.println(new BCryptPasswordEncoder().encode("123456"));
> ```

---

### 步骤 11：JWT secret 共享配置（Nacos）

在 Nacos `goods-store-common.yaml`（DEFAULT_GROUP / public）追加 `jwt` 段（Day 2 网关 `JwtProperties(prefix=jwt)` 已读此段）。密钥 ≥32 字节：

```yaml
jwt:
  secret: goods-store-jwt-secret-please-change-to-32chars-min!! # ≥32 字符
  access-expire-seconds: 1800
  refresh-expire-seconds: 604800
```

> auth 与 gateway 都靠该配置拿到同一 secret，双 Token 才能被网关验证通过。若网关已用别的 secret，**统一改成一致**。

---

### 步骤 12：编译与启动（统一次序）

> ⚠️ 约定的工作方式：**编译/启动验证由你手动执行**，本手册仅给出命令。

```powershell
# 1. common 被 auth 新增引用，若有改动需先 install
mvn -N install
mvn install -pl goods-store-common -am
# 2. 验证 auth 可编译（依赖父模块已托管 lombok 处理器）
mvn compile -pl goods-store-service/goods-store-auth-api -am
# 3. 启动顺序：Nacos → MySQL → Redis → auth
cd goods-store-service/goods-store-auth-api && mvn spring-boot:run
```

---

## 四、验收标准

> 编译/启动/接口调用验证由你手动执行。以下为待勾选项。

| #   | 验收项                | 验证方法                                                          | 预期结果                                    |
| --- | --------------------- | ----------------------------------------------------------------- | ------------------------------------------- |
| 1   | auth 编译通过         | `mvn compile -pl goods-store-service/goods-store-auth-api`        | BUILD SUCCESS                               |
| 2   | auth 启动并注册 Nacos | Nacos 控制台服务列表                                              | 出现 `goods-store-auth-api`，实例健康       |
| 3   | 会员注册              | `curl -POST localhost:8083/auth/api/register` 传 account/password | 返回 code=200；DB member 表写入加密密码     |
| 4   | 重复账号注册          | 同上再传同 account                                                | 返回 code=1002（账号已存在）                |
| 5   | 会员登录              | `curl -POST localhost:8083/auth/api/login`                        | 返回 accessToken + refreshToken + expiresIn |
| 6   | 密码错误登录          | 传错误密码                                                        | 返回 code=1003（密码错误）                  |
| 7   | 管理员登录            | `curl -POST localhost:8083/auth/api/admin/login`（admin/123456）  | 返回双 Token + roles + permissions 列表     |
| 8   | 未登录访问受保护接口  | 无 Token 直连 `localhost:8083/auth/api/admin/login` 以外接口      | 返回 401                                    |
| 9   | 网关放行登录/注册     | 经 `localhost:8888/auth/api/login` 用上面 token 调测              | 登录/注册可通；管理员登录经网关放行         |

> 第 9 项为 Day 3 跨联动验；若网关白名单未加 `/auth/api/admin/login`，请回步骤 9 补改并重启网关（无可疑时再验）。

---

## 五、端口 / 分配表（沿用 Day 2）

| 服务                                            | workerId | 端口                          | 备注                                |
| ----------------------------------------------- | -------- | ----------------------------- | ----------------------------------- |
| goods-store-auth-api                            | 1        | 8083                          | 本日主角；与 web 端口冲突需确认已解 |
| goods-store-web                                 | 7        | 8090\*                        | \*建议改此空闲端口（Day2 遗留）     |
| …（brand/product/member/trade/seckill/gateway） | 2-6 / -  | 8081/8084/8085/8086/8087/8888 | 不变                                |

---

## 六、常见问题排查

### 6.1 项目能编译但 `main` 不启动

- 检查 `AuthApiApplication` 是否 `public static void main`（步骤 0）。

### 6.2 登录报 `Field xxxMapper required a bean` / mapper 注入失败

- Mapper 接口漏标 `@Mapper`（步骤 4）。确认 MyBatis-Plus starter 依赖已由 service 父模块继承。

### 6.3 注册/登录返回 500 而非业务码

- 多因 `GlobalExceptionHandler` 仍被注释（步骤 6）。启用后 `BusinessException` 才会转成 `{code:100x}`。

### 6.4 登录成功但网关校验 401

- 确认 `jwt.secret` 在 auth 与 gateway 两处**一致**（步骤 11）。
- 确认网关重启过（WebFlux 过滤器不热更）。
- 若是管理员登录被拦，确认网关白名单已加 `/auth/api/admin/login`。

### 6.5 `java.lang.ClassNotFoundException: javax.servlet...`（安全相关）

- 与 Day 2 同理，Boot 4 用 jakarta.servlet。Security 配置不要引入任何 javax 依赖，只用 spring-boot-starter-security 官方类。

### 6.6 BCrypt 密文登不上

- 种子密码可能与密文不匹配。用 `new BCryptPasswordEncoder().encode("123456")` 重新生成后替换 DDL 种子再插入。

### 6.7 端口占用

- 确认 web 已避开 8083。`netstat -ano | findstr 8083` 查占用按 PID 释放。

---

## 七、本日产出核对表（提交前自查）

- [ ] 启动类 `main` 已补 `public`
- [ ] auth-api/pom.xml 已引 common + security + data-redis + jjwt×3
- [ ] RBAC 5 表 Entity + 6 个 Mapper（含 MemberMapper）齐全且带 `@Mapper`
- [ ] `JwtProperties`/`JwtUtil`/`SecurityConfig` 就位
- [ ] `GlobalExceptionHandler` 已启用（恢复注解方法）
- [ ] `AuthController`(register/login) + `AdminAuthController`(admin/login) 可用
- [ ] DTO 与 5 表数据库 DDL + 种子数据已导入
- [ ] Nacos `goods-store-common.yaml` 已加 `jwt` 段；网关白名单已加 `/auth/api/admin/login`
- [ ] 验收 4.1~4.9 通过（编译/启动/调用由你手动验证）
- [ ] Git 提交：`feat(day3): auth-api认证服务+RBAC实体+登录注册`

---

## 八、明日预告（Day 4 衔接）

- **双 Token 完善**：Refresh Token 存 Redis + 刷新接口 `/auth/api/refresh`，Access 过期用 Refresh 换新。
- **登出**：Access 写 Redis 黑名单 `token:blacklist:{userId}` + 删 Refresh，与 Day 2 网关过滤器第 4 步闭环。
- **密码管理**：改密（旧密码验证 + 新密码 BCrypt）、找回密码。
- **RBAC 鉴权落地**：网关按 `type=admin` + 权限做接口级鉴权（不同角色访问不同接口生效，对应计划 Day3 验收第 6 项）。
- 本日 JWT claims 已含 `sub=userId`、`type`、`roles`，Day 4 直接复用 `JwtUtil`。
