# 并发商城系统 - 14 天实施计划

> 版本：v1.2（spi 契约层 + web BFF 收口重构后）
> 更新日期：2026-08-28
> 基于项目执行计划书 v1.0 + 审阅意见调整
> 起始日期：Day 1 = 2026-08-29

---

## 总体进度概览

> **实施主线**：本计划 Day 1-2 为基础，Day 3 起强制遵循「② ⭐ 分层架构规范」。
> **横向收口**：Day 3-13 每个业务域都按 **spi 契约 + api 实现 + web 聚合三件套** 落地——spi 定义契约（controller implements、Feign 复用），web 层同步建 `Web*Service`/`Web*Controller` 做 BFF 聚合，前端只在 `/app/api/**` 收口访问，绝不穿透到 api 服务。

```
Day 1-2   ████░░░░░░░░░░  基础设施（配置中心、依赖、网关鉴权、公共模块）
Day 3-4   ██████░░░░░░░░  认证服务（登录注册、JWT、RBAC）+ auth-spi 契约 + web 认证聚合脚手架
Day 5      ███████░░░░░░░  品牌服务（完整 CRUD）+ BrandApi 契约兑现 + web 品牌聚合
Day 6-7   █████████░░░░░  商品服务（分类树、商品 CRUD 、Feign 库存）+ ProductApi 契约 + web 商品聚合
Day 8      ██████████░░░░  会员服务（信息查询、地址管理）+ MemberApi 契约 + web 会员聚合
Day 9-10  ███████████░░░  交易服务（购物车、下单、Redisson 分布式锁）+ web 购物车/下单聚合
Day 11    ████████████░░  MQ 消费者 + 超时取消 + 订单管理 + web 订单聚合
Day 12-13 ██████████████  秒杀服务（活动管理、库存预热、Lua 抢购）+ SeckillApi 契约 + web 秒杀聚合
Day 14    ██████████████  集成收尾（文档、链路追踪、集成测试经 /app/api 收口）
```

> **收口说明**：Day 14 集成测试与前端一样，一律从**前端入口 `/app/api/**`** 发起，验证 web 聚合层真实承担 BFF 职责；api 服务路由（`/brand/api/\*\*` 等）仅作服务间 Feign / 排障保留。

---

## Day 1：基础设施搭建（上）— 配置中心 + 依赖管理 + 公共模块

### 技术要点

| 要点            | 说明                                                                                    |
| --------------- | --------------------------------------------------------------------------------------- |
| Nacos 共享配置  | `goods-store-common.yaml` 统一管理数据源/Redis/MQ/MyBatis-Plus 配置                     |
| 父 POM 版本管理 | `<dependencyManagement>` 统一管控 Redis、RabbitMQ、JWT、Redisson、Hutool、Druid 版本    |
| 雪花算法        | Hutool `IdUtil.getSnowflake(workerId, datacenterId)` 生成全局唯一 ID                    |
| MQ 消息体 DTO   | 定义 `OrderCreateMessage`、`SeckillOrderMessage`、`OrderCancelMessage` 等标准化消息结构 |

### 实现内容

| 序号 | 任务                                                    | 涉及文件                                   | 备注                                                                                                            |
| ---- | ------------------------------------------------------- | ------------------------------------------ | --------------------------------------------------------------------------------------------------------------- |
| 1.1  | Nacos 控制台创建 `goods-store-common.yaml`              | Nacos 控制台                               | namespace=public, group=DEFAULT_GROUP                                                                           |
| 1.2  | 父 POM 添加依赖版本管理                                 | `pom.xml`                                  | 新增 `spring-boot-starter-amqp`、`spring-boot-starter-data-redis`、`redisson`、`jjwt`、`hutool`、`druid` 版本号 |
| 1.3  | Common 模块新增雪花算法工具类                           | `common/.../util/SnowflakeUtil.java`       | 封装 `IdUtil.getSnowflake()`，支持 workerId 注入                                                                |
| 1.4  | Common 模块新增 MQ 消息体 DTO                           | `common/.../mq/OrderCreateMessage.java` 等 | 3 个消息体：订单创建、秒杀订单、订单取消                                                                        |
| 1.5  | Common 模块新增分页基类                                 | `common/.../dto/PageQuery.java`            | 含 `pageNo`、`pageSize`、`sortField`、`sortOrder`                                                               |
| 1.6  | Common 模块新增 Jackson 配置                            | `common/.../config/JacksonConfig.java`     | `LocalDateTime` 序列化格式 `yyyy-MM-dd HH:mm:ss`，Long 转 String 防止前端精度丢失                               |
| 1.7  | Nacos 共享配置 `refresh: true` + 各服务 `@RefreshScope` | 所有服务 `application.yaml`                | 支持配置热刷新，无需重启服务                                                                                    |

### 验收标准

- [ ] Nacos 控制台可查看 `goods-store-common.yaml` 配置
- [ ] Nacos 控制台修改配置后，服务能动态刷新（验证 `@RefreshScope` 生效）
- [ ] `mvn compile` 父项目 + common 模块无报错
- [ ] `SnowflakeUtil.nextId()` 可生成 19 位唯一 ID
- [ ] 消息体 DTO 可正常序列化/反序列化
- [ ] Jackson 序列化 Long 类型自动转为 String（雪花 ID 前端精度安全）

### 补充细节

**`goods-store-common.yaml` 完整内容：**

```yaml
# Nacos: goods-store-common.yaml (namespace=public, group=DEFAULT_GROUP, refresh=true)
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/shoplook2026_0324?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: root
    type: com.alibaba.druid.pool.DruidDataSource
    druid:
      initial-size: 3
      min-idle: 3
      max-active: 10 # 每个服务最多 10 个连接，6 个服务共 60 个连接，避免 MySQL 连接数超限
      max-wait: 60000
      validation-query: SELECT 1
      test-while-idle: true
      test-on-borrow: false
      test-on-return: false
      filter:
        stat:
          enabled: true
          log-slow-sql: true
          slow-sql-millis: 1000
        wall:
          enabled: true
          config:
            multi-statement-allow: true # 允许多表 JOIN 和子查询
            none-base-statement-allow: true # 允许非基础语句，避免商品查询被误拦截
  data:
    redis:
      host: localhost
      port: 6379
      password:
      database: 0
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
          max-wait: -1ms
    # Redis 内存策略：在 redis.conf 中配置 maxmemory 256mb / maxmemory-policy allkeys-lru
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
    publisher-confirm-type: correlated
    publisher-returns: true
    listener:
      simple:
        acknowledge-mode: manual
        prefetch: 1
        retry:
          enabled: true
          max-attempts: 3
          initial-interval: 2000ms
          multiplier: 2.0
        concurrency: 3 # 消费者并发数，秒杀场景可动态扩容到 10
        max-concurrency: 10

# Feign 全局超时配置
feign:
  client:
    config:
      default:
        connect-timeout: 3000
        read-timeout: 5000

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: isDel
      logic-delete-value: 1
      logic-not-delete-value: 0

# 雪花算法配置（每个服务实例不同 workerId）
snowflake:
  worker-id: ${SNOWFLAKE_WORKER_ID:1}
  datacenter-id: ${SNOWFLAKE_DATACENTER_ID:1}
```

**雪花算法工具类设计：**

```java
// common/.../util/SnowflakeUtil.java
@Component
public class SnowflakeUtil {
    private final Snowflake snowflake;

    // workerId 通过 Nacos 配置注入，不同服务实例分配不同 workerId（0-31）
    public SnowflakeUtil(@Value("${snowflake.worker-id:1}") long workerId,
                         @Value("${snowflake.datacenter-id:1}") long datacenterId) {
        this.snowflake = IdUtil.getSnowflake(workerId, datacenterId);
    }

    public long nextId() {
        return snowflake.nextId();
    }

    public String nextIdStr() {
        return snowflake.nextIdStr();
    }
}
```

---

## Day 2：基础设施搭建（下）— Druid 集成 + 网关路由 + JWT 过滤器

### 技术要点

| 要点                      | 说明                                                                                  |
| ------------------------- | ------------------------------------------------------------------------------------- |
| Druid 数据源              | 替换 HikariCP，启用 SQL 监控 + 防火墙 + 慢 SQL 告警                                   |
| Spring Cloud Gateway 路由 | 基于 `lb://` 实现 Nacos 服务发现负载均衡                                              |
| JWT 全局过滤器            | `GlobalFilter` 解析 Bearer Token，验证签名，检查 Redis 黑名单                         |
| 白名单路径                | 登录/注册/商品浏览/品牌浏览 不校验 Token                                              |
| Guava RateLimiter         | 秒杀接口限流，每 IP 每秒 10 次（**当前为单机限流，多实例部署需升级为 Redis 令牌桶**） |
| Nacos 健康检查            | 配置 `readiness-state` 健康检查，确保服务就绪后才被网关路由                           |

### 实现内容

| 序号 | 任务                       | 涉及文件                                                    | 备注                                                            |
| ---- | -------------------------- | ----------------------------------------------------------- | --------------------------------------------------------------- |
| 2.1  | 各服务 POM 引入 Druid 依赖 | 所有 service 的 `pom.xml`                                   | 在 `goods-store-service/pom.xml` 统一添加                       |
| 2.2  | 网关路由完整配置           | `gateway/.../application.yaml`                              | 6 条路由（auth/brand/product/member/trade/seckill）             |
| 2.3  | JWT 全局过滤器             | `gateway/.../filter/JwtAuthFilter.java`                     | 实现 `GlobalFilter, Ordered`                                    |
| 2.4  | 白名单配置                 | `gateway/.../config/WhiteListConfig.java`                   | 从 Nacos 读取白名单路径列表                                     |
| 2.5  | 跨域配置                   | `gateway/.../config/CorsConfig.java`                        | 允许所有来源（开发阶段）                                        |
| 2.6  | 限流配置                   | `gateway/.../config/RateLimiterConfig.java`                 | Guava RateLimiter 按 IP 限流                                    |
| 2.7  | auth-api 模块注册          | `goods-store-service/pom.xml`                               | 添加 `<module>goods-store-auth-api</module>`                    |
| 2.8  | auth-api 启动类 + 基础配置 | `auth-api/.../AuthApiApplication.java` + `application.yaml` | 端口 8082                                                       |
| 2.9  | Nacos 健康检查配置         | 所有服务 `application.yaml`                                 | `spring.cloud.nacos.discovery.metadata.readiness-state=enabled` |

### 验收标准

- [ ] 启动网关，所有服务注册到 Nacos 且网关路由可正确转发
- [ ] 服务启动后，Nacos 控制台显示实例状态为"健康"后才可被路由
- [ ] 无 Token 访问 `/brand/api/page` 返回正常（白名单通过）
- [ ] 无 Token 访问 `/trade/api/cart` 返回 401
- [ ] 携带无效 Token 返回 401
- [ ] 网关日志输出请求路径、耗时、来源 IP
- [ ] Druid 监控页面 `http://localhost:8081/druid` 可访问（品牌服务）

### 补充细节

**JWT 过滤器核心逻辑：**

```java
// gateway/.../filter/JwtAuthFilter.java
@Component
@Slf4j
public class JwtAuthFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 1. 白名单放行
        if (whiteListConfig.isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        // 2. 提取 Authorization Header
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "缺少 Token");
        }

        String token = authHeader.substring(7);

        // 3. 验证 JWT 签名 + 有效期
        Claims claims;
        try {
            claims = JwtUtil.parseToken(token);
        } catch (Exception e) {
            return unauthorized(exchange, "Token 无效或已过期");
        }

        // 4. 检查 Redis 黑名单（是否已登出）
        String userId = claims.getSubject();
        if (Boolean.TRUE.equals(redisTemplate.hasKey("token:blacklist:" + userId))) {
            return unauthorized(exchange, "Token 已注销");
        }

        // 5. 将 userId 写入 Header 传递给下游
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header("X-User-Id", userId)
                .header("X-User-Roles", String.join(",", claims.get("roles", List.class)))
                .build();

        return chain.filter(exchange.mutate().request(request).build());
    }
}
```

**网关路由完整配置：**

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-api
          uri: lb://goods-store-auth-api
          predicates:
            - Path=/auth/api/**
        - id: brand-api
          uri: lb://goods-store-brand-api
          predicates:
            - Path=/brand/api/**
        - id: product-api
          uri: lb://goods-store-product-api
          predicates:
            - Path=/product/api/**
          filters:
            - StripPrefix=0 # 不剥离前缀
        - id: member-api
          uri: lb://goods-store-member-api
          predicates:
            - Path=/member/api/**
        - id: trade-api
          uri: lb://goods-store-trade-api
          predicates:
            - Path=/trade/api/**
        - id: seckill-api
          uri: lb://goods-store-seckill-api
          predicates:
            - Path=/seckill/api/**
      default-filters:
        - AddResponseHeader=X-Response-Time, %{request.startTime}
```

---

## ⭐ 分层架构规范（本次重构核心，Day 3 起强制遵循）

> **目的**：解决「spi 悬空、web 形同虚设」两大结构问题。从 Day 3 起，所有服务的开发都按本规范落，使 **spi 成为贯穿全系统的契约层、web 成为前端唯一入口的 BFF 聚合层**。

### 五层职责（重新界定 spi / web）

| 层       | 模块                  | 职责                                                                             | 关键约束                                                                      |
| -------- | --------------------- | -------------------------------------------------------------------------------- | ----------------------------------------------------------------------------- |
| 基础     | `goods-store-common`  | 统一返回 `ApiResult`、异常、工具、MQ 消息体                                      | 不依赖任何业务                                                                |
| **契约** | `goods-store-*-spi`   | **跨服务 HTTP 契约**：纯接口 + DTO/VO                                            | **不加 `@FeignClient`**；只写 `@GetMapping` 等路径注解；不依赖 common/mybatis |
| 实现     | `goods-store-*-api`   | 业务实现；**controller `implements` 对应 spi 契约**                              | 「契约即实现」：接口签名变更编译期即可发现                                    |
| **聚合** | `goods-store-web`     | **BFF 聚合层，前端唯一入口**；用 Feign 客户端 `extends` spi 契约，聚合多服务编排 | 前端只调 web；web 不写业务，只编排                                            |
| 横切     | `goods-store-gateway` | 路由、鉴权、限流                                                                 | 新增 web 收口路由                                                             |

### spi「契约即实现」范式

同一份契约，**服务端靠 `implements` 兑现、消费方靠 Feign `extends` 复用**，不再存在两份对不上的接口。

```java
// ① spi 层：纯契约（无 @FeignClient，只写路径注解 + DTO/VO）
public interface BrandApi {
    @GetMapping("/list")
    List<BrandVO> list(BrandQueryRequest request);
}

// ② 提供方（brand-api）：controller implements 契约 → 暴露 GET /brand/api/list
@RestController
@RequestMapping("/brand/api")
public class BrandController implements BrandApi {
    private final BrandService brandService;
    @Override
    public List<BrandVO> list(BrandQueryRequest request) {
        return brandService.list(request);
    }
}

// ③ 消费方（任一下游，如 product-api / web）：Feign 客户端继承契约
//    path 与提供方 controller 类级 @RequestMapping 保持一致
@FeignClient(name = "goods-store-brand-api", path = "/brand/api")
public interface BrandFeignClient extends BrandApi {}
```

要点：

- **Spi 内禁止**出现 `@FeignClient`、`@Service`、`@RestController`；`@GetMapping/@PutMapping/...` 允许（作契约路径），返回类型用纯 DTO/VO。
- **提供方** controller 只写 `@RequestMapping(类级前缀)` + `implements 契约`；接口上声明的子路径与方法签名即对外 HTTP 契约。
- **消费方**（`*-api` 服务之间 + `goods-store-web`）统一用 `@FeignClient extends 契约` 注入，禁止再手写 URL / RestTemplate。
- 每个消费方启动类需 `@EnableFeignClients(basePackages = "com.fengluan.spi")`，并在自身包内定义一个 `XxxFeignClient extends XxxApi`。
- spi 的 DTO/VO 与返回类型是**跨服务唯一事实来源**，禁止在消费方另造同名 DTO。

### web BFF 聚合规范（前端收口）

```
[前端] ──📱──► [Gateway: /app/api/**] ──► [goods-store-web] ──Feign──► 各 api 服务
               （鉴权/限流）                    （BFF 聚合编排）
```

- 前端**只请求 `web`**。web 统一入口路由 `Path=/app/api/** → lb://goods-store-web`。
- 每个业务域在 web 侧固定一套：

  ```java
  // ① 契约客户端（继承 spi）
  @FeignClient(name = "goods-store-brand-api", path = "/brand/api")
  public interface BrandFeignClient extends BrandApi {}

  // ② 聚合 Service：按「场景」编排多个 api
  @Service
  public class WebBrandService {
      private final BrandFeignClient brandFeignClient;
      public List<BrandVO> listForHome(BrandQueryRequest request) {
          return brandFeignClient.list(request);   // 单查
      }
      // 聚合示例：首页「品牌+热销」可能同时调 brand+product，在此编排
  }

  // ③ 聚合 Controller（前端 Controller，路径挂 /app/api 下）
  @RestController
  @RequestMapping("/app/api/brand")
  public class WebBrandController {
      private final WebBrandService webBrandService;
  }
  ```

- **聚合三条规则**：补字段（列表页补名称/图片）、按场景编排（下单=校验+扣库存+建单）、磨平多服务差异（统一 `ApiResult`、脱敏、分页结构）。
- web 依赖引入：`spring-cloud-starter-openfeign` + `spring-cloud-starter-loadbalancer` + **各 `goods-store-*-spi`**；启动类 `@EnableFeignClients`。
- web `spring.application.name=goods-store-web`，端口保持不与 api 冲突（auth-api=8083，web 改用 **8090**）。

### 网关路由策略（web 收口后）

| 路由                              | 目标                     | 说明                                                                      |
| --------------------------------- | ------------------------ | ------------------------------------------------------------------------- |
| `Path=/app/api/**`                | `lb://goods-store-web`   | **前端唯一入口**，前置鉴权/限流                                           |
| `Path=/auth/api/**` 等原 api 路由 | `lb://goods-store-*-api` | **保留**，仅作内部契约/Feign 直连，前端不再直接暴露（可加请求头校验收紧） |

> 各 api 服务路由仍保留，用于服务间 Feign 调用与排障；对外文档聚合仍走 Knife4j。Day 14 集成测试一律从前端入口 `/app/api/**` 发起。

### spi 目录（以 brand 为样板，其余域同构）

```
goods-store-brand-spi/src/main/java/com/fengluan/spi/brand/
├── BrandApi.java          # 纯契约接口（含 @GetMapping/@PostMapping 等）
├── dto/ BrandQueryRequest.java, BrandCreateRequest.java, ...
└── vo/  BrandVO.java
```

---

## Day 3：认证服务（上）— 项目搭建 + RBAC 实体 + 登录注册

### 技术要点

| 要点            | 说明                                                                                                                                    |
| --------------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| Spring Security | 仅 auth-api 引入，配置 `SecurityFilterChain` 放行登录/注册接口                                                                          |
| BCrypt 加密     | 密码存储使用 `BCryptPasswordEncoder`，强度 10                                                                                           |
| 会员登录        | 使用 `member` 表（account + password），返回 JWT，**会员不参与 RBAC 权限模型**                                                          |
| 管理员登录      | 使用 `admin_user` 表（username + password），返回 JWT + 角色权限信息                                                                    |
| RBAC 权限模型   | **仅用于后台管理员**：新版 5 表（`admin_user`/`role`/`permission`/`admin_user_role`/`role_permission`），前端会员用简单的 role 字段区分 |

### 实现内容

| 序号 | 任务                  | 涉及文件                                                          | 备注                                                                                                |
| ---- | --------------------- | ----------------------------------------------------------------- | --------------------------------------------------------------------------------------------------- |
| 3.1  | auth-api POM 补充依赖 | `auth-api/pom.xml`                                                | Security + jjwt + Redis + common                                                                    |
| 3.2  | RBAC 5 表 Entity      | `auth-api/.../entity/AdminUserEntity.java` 等                     | **仅用于后台管理员**，映射 `admin_user`/`role`/`permission`/`admin_user_role`/`role_permission`     |
| 3.3  | RBAC Mapper           | `auth-api/.../repository/*Mapper.java`                            | 继承 `BaseMapper`                                                                                   |
| 3.4  | SecurityConfig 配置   | `auth-api/.../config/SecurityConfig.java`                         | 放行 `/auth/api/login`、`/auth/api/register`，其余需认证                                            |
| 3.5  | 会员注册接口          | `auth-api/.../api/AuthController.java`                            | `POST /auth/api/register`，校验 account 唯一性，BCrypt 加密                                         |
| 3.6  | 会员登录接口          | `auth-api/.../api/AuthController.java`                            | `POST /auth/api/login`，验证密码，返回 JWT 双 Token                                                 |
| 3.7  | 管理员登录接口        | `auth-api/.../api/AdminAuthController.java`                       | `POST /auth/api/admin/login`，返回 JWT + 角色权限列表                                               |
| 3.8  | **auth-spi 契约**     | `auth-spi/.../spi/auth/AuthApi.java` + dto/vo                     | 按分层规范建 `goods-store-auth-spi`，`AuthController implements AuthApi`                            |
| 3.9  | **web 聚合脚手架**    | `web/.../GoodsStoreWebApplication.java` + pom                     | `@EnableFeignClients(basePackages="com.fengluan.spi")` + openfeign/loadbalancer/spi 依赖，端口 8090 |
| 3.10 | **web 认证聚合壳**    | `web/.../web/auth/WebAuthService.java` + `WebAuthController.java` | 先落地空壳与路由 `POST /app/api/auth/login`，后续 Day4 填充逻辑                                     |

### 验收标准

- [ ] 启动 auth-api，注册到 Nacos（服务名：`goods-store-auth-api`）
- [ ] `POST /auth/api/register` 注册新会员，member 表写入加密密码
- [ ] `POST /auth/api/login` 会员登录返回 Access Token + Refresh Token
- [ ] 密码错误登录返回 1003 错误码
- [ ] `POST /auth/api/admin/login` 管理员登录返回角色+权限列表
- [ ] 管理员登录后，不同角色（超级管理员/普通管理员）访问不同接口，权限控制生效
- [ ] 未登录访问认证接口返回 401
- [ ] **契约闭环**：`AuthController implements AuthApi` 编译通过，`auth-spi` 无 `@FeignClient`
- [ ] **web 收口**：通过网关 `POST /app/api/auth/login` 可达 web（CD#3 链路通）

### 补充细节

**auth-api 模块结构：**

```
goods-store-auth-api/src/main/java/com/fengluan/auth/
├── api/
│   ├── AuthController.java       # 会员登录/注册/Token刷新/登出（implements AuthApi）
│   └── AdminAuthController.java  # 管理员登录
├── entity/
│   ├── AdminUserEntity.java
│   ├── RoleEntity.java
│   ├── PermissionEntity.java
│   ├── AdminUserRoleEntity.java
│   └── RolePermissionEntity.java
├── repository/
│   ├── AdminUserMapper.java
│   ├── RoleMapper.java
│   ├── PermissionMapper.java
│   ├── AdminUserRoleMapper.java
│   └── RolePermissionMapper.java
├── service/
│   ├── AuthService.java
│   ├── AuthServiceImpl.java
│   └── AdminAuthService.java
├── config/
│   ├── SecurityConfig.java
│   └── JwtConfig.java
└── util/
    └── JwtUtil.java
```

**auth 三件套示范（spi 契约 + api 实现 + web 聚合）：**

```java
// ① auth-spi：纯契约（无 @FeignClient）
public interface AuthApi {
    @PostMapping("/login")
    LoginResponse login(@RequestBody LoginRequest request);

    @PostMapping("/register")
    Void register(@RequestBody RegisterRequest request);
}

// ② auth-api：controller implements 契约 → 暴露 POST /auth/api/login
@RestController
@RequestMapping("/auth/api")
public class AuthController implements AuthApi {
    private final AuthService authService;
    @Override
    public LoginResponse login(LoginRequest request) { return authService.login(request); }
    @Override
    public Void register(RegisterRequest request) { authService.register(request); return null; }
}

// ③ web：契约客户端 extends 契约 + 聚合 Service/Controller
@FeignClient(name = "goods-store-auth-api", path = "/auth/api")
public interface AuthFeignClient extends AuthApi {}

@Service
public class WebAuthService {
    private final AuthFeignClient authFeignClient;
    public LoginResponse login(LoginRequest request) { return authFeignClient.login(request); }
}

@RestController
@RequestMapping("/app/api/auth")
public class WebAuthController {
    private final WebAuthService webAuthService;
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return webAuthService.login(request);
    }
}
```

> **web 路由**：前端请求 `POST /app/api/auth/login` → 网关 `/app/api/**` → `lb://goods-store-web` → `WebAuthController` → Feign `AuthFeignClient` → `lb://goods-store-auth-api`。这条链路即全项目 web 收口的首个样板，Day 3 落地脚手架、Day 4 填充汇聚。

**JWT Payload 结构：**

```json
{
  "sub": "会员ID或管理员ID",
  "type": "member|admin",
  "roles": ["ROLE_USER"],
  "iat": 1693200000,
  "exp": 1693201800
}
```

**请求/响应 DTO 设计：**

```java
// 登录请求
public class LoginRequest {
    @NotBlank private String account;      // 管理员用 username
    @NotBlank private String password;
    private String loginType;              // member / admin
}

// 登录响应
public class LoginResponse {
    private String accessToken;            // 30min 有效期
    private String refreshToken;           // 7天 有效期
    private Long expiresIn;                // 1800 秒
    private UserInfo userInfo;             // 用户基本信息
}

// 注册请求
public class RegisterRequest {
    @NotBlank @Size(min = 3, max = 32) private String account;
    @NotBlank @Size(min = 6, max = 32) private String password;
    @Pattern(regexp = "^1[3-9]\\d{9}$") private String phone;
    @Email private String email;
}
```

---

## Day 4：认证服务（下）— JWT 双 Token + 刷新登出 + 密码管理

### 技术要点

| 要点           | 说明                                                                                                           |
| -------------- | -------------------------------------------------------------------------------------------------------------- |
| 双 Token 机制  | Access Token（30min）+ Refresh Token（7天），Redis 存储                                                        |
| Token 刷新     | 用 Refresh Token 换新 Access Token，Refresh Token 保持不变                                                     |
| 登出           | Access Token 写入 Redis 黑名单（TTL=剩余有效期），删除 Refresh Token                                           |
| 密码修改       | 旧密码验证 + 新密码 BCrypt 加密                                                                                |
| Token 存储策略 | Access Token 黑名单 Key: `token:blacklist:{userId}:{role}`，Refresh Token Key: `token:refresh:{userId}:{role}` |

### 实现内容

| 序号 | 任务                 | 涉及文件                                                          | 备注                                                                                 |
| ---- | -------------------- | ----------------------------------------------------------------- | ------------------------------------------------------------------------------------ |
| 4.1  | JWT 工具类完善       | `auth-api/.../util/JwtUtil.java`                                  | `generateAccessToken()`、`generateRefreshToken()`、`parseToken()`、`validateToken()` |
| 4.2  | Token 刷新接口       | `auth-api/.../api/AuthController.java`                            | `POST /auth/api/refresh`，验证 Refresh Token，返回新 Access Token                    |
| 4.3  | 登出接口             | `auth-api/.../api/AuthController.java`                            | `POST /auth/api/logout`，Token 加入黑名单                                            |
| 4.4  | 密码修改接口         | `auth-api/.../api/AuthController.java`                            | `PUT /auth/api/password`，旧密码验证                                                 |
| 4.5  | Redis Token 存储     | `auth-api/.../service/TokenService.java`                          | 封装 Redis 操作                                                                      |
| 4.6  | 网关 JWT 过滤器对接  | `gateway/.../filter/JwtAuthFilter.java`                           | 补充 Redis 黑名单检查逻辑                                                            |
| 4.7  | **web 认证聚合完型** | `web/.../web/auth/WebAuthService.java` + `WebAuthController.java` | 封装 login/refresh/logout/password，路由挂 `/app/api/auth/*`                         |

### 验收标准

- [ ] 用 Refresh Token 调用 `/auth/api/refresh` 返回新 Access Token
- [ ] 过期 Refresh Token 刷新返回 401
- [ ] 登出后使用旧 Access Token 访问受保护接口返回 401（黑名单生效）
- [ ] 密码修改后旧密码登录失败，新密码登录成功
- [ ] 连续 3 次密码错误，账号临时锁定 15 分钟
- [ ] **web 收口**：`/app/api/auth/refresh|logout|password` 经 `WebAuthService` 代理 auth-api 成功，前端从未直接调 `/auth/api/**`

### 补充细节

**Token 刷新流程时序：**

```
[客户端]                    [Gateway]                    [Auth-API]              [Redis]
   │                           │                            │                       │
   │  POST /auth/api/refresh   │                            │                       │
   │  (Refresh Token)          │                            │                       │
   │──────────────────────────►│                            │                       │
   │                           │  白名单放行                 │                       │
   │                           │───────────────────────────►│                       │
   │                           │                            │  验证 Refresh Token    │
   │                           │                            │──────────────────────►│
   │                           │                            │  GET token:refresh:uid │
   │                           │                            │◄──────────────────────│
   │                           │                            │  比对一致               │
   │                           │                            │  生成新 Access Token    │
   │                           │◄───────────────────────────│                       │
   │◄──────────────────────────│                            │                       │
   │  { accessToken: "xxx" }   │                            │                       │
```

**JWT 工具类关键方法：**

```java
public class JwtUtil {
    private static final SecretKey KEY = Jwts.SIG.HS256.key().build();
    private static final long ACCESS_EXPIRATION = 30 * 60 * 1000L;    // 30分钟
    private static final long REFRESH_EXPIRATION = 7 * 24 * 60 * 60 * 1000L; // 7天

    public static String generateAccessToken(String userId, String type, List<String> roles) {
        return Jwts.builder()
                .subject(userId)
                .claim("type", type)
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ACCESS_EXPIRATION))
                .signWith(KEY)
                .compact();
    }

    public static String generateRefreshToken(String userId) {
        return Jwts.builder()
                .subject(userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + REFRESH_EXPIRATION))
                .signWith(KEY)
                .compact();
    }
}
```

---

## Day 5：品牌服务 — 完整 CRUD + Logo 上传

### 技术要点

| 要点                  | 说明                                                                                                               |
| --------------------- | ------------------------------------------------------------------------------------------------------------------ |
| MyBatis-Plus 逻辑删除 | `@TableLogic` 注解，删除时自动设置 `is_del = 1`                                                                    |
| 参数校验              | `jakarta.validation` 注解 `@Valid` + `@NotBlank`                                                                   |
| 文件上传              | Spring `MultipartFile`，开发环境存储到本地 `/static/upload/brand/`，通过 `@ConditionalOnProperty` 切换生产环境 OSS |
| 统一异常处理          | 品牌不存在抛 `BusinessException(ErrorCode.BRAND_NOT_FOUND)`                                                        |

### 实现内容

| 序号 | 任务                  | 涉及文件                                                                                       | 备注                                                                 |
| ---- | --------------------- | ---------------------------------------------------------------------------------------------- | -------------------------------------------------------------------- |
| 5.1  | BrandService 实现类   | `brand-api/.../service/impl/BrandServiceImpl.java`                                             | 详情/新增/编辑/删除/列表                                             |
| 5.2  | BrandController 完善  | `brand-api/.../api/BrandController.java`                                                       | 5 个 REST 接口                                                       |
| 5.3  | 文件上传工具类        | `brand-api/.../util/FileUploadUtil.java`                                                       | 存储到本地，返回 URL                                                 |
| 5.4  | 静态资源映射配置      | `brand-api/.../config/WebMvcConfig.java`                                                       | 映射 `/static/**` 到本地目录                                         |
| 5.5  | 品牌名称唯一性校验    | `brand-api/.../service/impl/BrandServiceImpl.java`                                             | 新增/编辑时检查 name 唯一                                            |
| 5.6  | BrandVO 完善          | `brand-spi/.../vo/BrandVO.java`                                                                | 补充 company/logo/site 字段                                          |
| 5.7  | **BrandApi 契约兑现** | `brand-spi/.../BrandApi.java` + `brand-api/.../api/BrandController.java`                       | `BrandController implements BrandApi`，删 `@FeignClient`，契约即实现 |
| 5.8  | **web 品牌聚合**      | `web/.../web/brand/WebBrandController.java` + `WebBrandService.java` + `BrandFeignClient.java` | BFF 聚合：首页品牌位 `GET /app/api/brand/list`                       |

### 验收标准

- [ ] `GET /brand/api/page?pageNo=1&pageSize=10&name=华为` 返回分页结果
- [ ] `GET /brand/api/1` 返回品牌详情，不存在的 ID 返回 5001
- [ ] `POST /brand/api` 新增品牌，重复 name 返回业务异常
- [ ] `PUT /brand/api/1` 修改品牌信息
- [ ] `DELETE /brand/api/1` 逻辑删除，数据库 `is_del` 字段置 1，查询不再返回
- [ ] `POST /brand/api/upload` 上传 Logo 图片，返回可访问 URL
- [ ] **契约闭环**：`BrandController implements BrandApi`，跨服务消费方只依赖 spi 不手写路径
- [ ] **web 收口**：`GET /app/api/brand/list` 经 web 聚合展示品牌位（含 logo）

### 补充细节

**品牌「契约即实现」＋ web 聚合（完整三件套）：**

```java
// ① brand-spi：纯契约（无 @FeignClient），DTO/VO 是跨服务唯一事实来源
public interface BrandApi {
    @GetMapping("/page")
    PageVO<BrandVO> page(BrandQueryRequest query);

    @GetMapping("/{id}")
    BrandVO getById(@PathVariable Long id);

    @PostMapping
    BrandVO create(@Valid @RequestBody BrandCreateRequest request);

    @PutMapping("/{id}")
    BrandVO update(@PathVariable Long id, @Valid @RequestBody BrandUpdateRequest request);

    @DeleteMapping("/{id}")
    Void delete(@PathVariable Long id);
}

// ② brand-api：controller implements 契约 → 暴露 GET/POST /brand/api/...
@RestController
@RequestMapping("/brand/api")
@RequiredArgsConstructor
public class BrandController implements BrandApi {

    private final BrandService brandService;

    @Override
    public PageVO<BrandVO> page(BrandQueryRequest query) {
        return brandService.page(query);
    }
    @Override
    public BrandVO getById(Long id) { return brandService.getById(id); }
    @Override
    public BrandVO create(BrandCreateRequest request) { return brandService.create(request); }
    @Override
    public BrandVO update(Long id, BrandUpdateRequest request) { return brandService.update(id, request); }
    @Override
    public Void delete(Long id) { brandService.delete(id); return null; }
}

// ②′ 上传接口不进契约（涉及 MultipartFile，仅 api 暴露，web 不经此转发即不入契约）
//    若前端需走 web 上传，则单独在 web 侧用 @MultipartFile 接收后转发（见 web 聚合示例）

// ③ web：契约客户端 extends 契约 + 聚合 Service/Controller（BFF 收口）
@FeignClient(name = "goods-store-brand-api", path = "/brand/api")
public interface BrandFeignClient extends BrandApi {}

@Service
@RequiredArgsConstructor
public class WebBrandService {
    private final BrandFeignClient brandFeignClient;
    // 首页品牌位：分页取前 N 条，补字段（确保 logo 完整）供前端直接渲染
    public List<BrandVO> listForHome() {
        BrandQueryRequest q = new BrandQueryRequest();
        q.setPageSize(8);
        return brandFeignClient.page(q).getRecords();
    }
}

@RestController
@RequestMapping("/app/api/brand")
@RequiredArgsConstructor
public class WebBrandController {
    private final WebBrandService webBrandService;
    @GetMapping("/list")
    public ApiResult<List<BrandVO>> listForHome() {
        return ApiResult.success(webBrandService.listForHome());
    }
}
```

> 说明：登录/下单这类**带人机交互或上传**的接口不必进契约；进契约的是「跨服务复用、需要 web 收口」的那部分。品牌 **list 进 web**，upload 保留 api 直连（或 web 单独转发）。

**BrandEntity 补充 @TableLogic：**

```java
@Data
@TableName("brand")
public class BrandEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String company;
    private String logo;
    private String site;
    private String description;
    private LocalDateTime createdTime;
    private String createdBy;
    private LocalDateTime updatedTime;
    private String updatedBy;

    @TableLogic  // 逻辑删除字段
    private Boolean isDel;
}
```

> 注意：`brand` 表当前无 `is_del` 字段，需执行 DDL：
>
> ```sql
> ALTER TABLE brand ADD COLUMN is_del BIT(1) DEFAULT b'0' COMMENT '是否逻辑删除';
> ```

**文件上传存储切换设计：**

```java
// 通过 @ConditionalOnProperty 切换本地存储/OSS
@Component
@ConditionalOnProperty(name = "upload.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileUploadUtil implements FileUploadUtil {
    // 本地存储实现
}

@Component
@ConditionalOnProperty(name = "upload.storage.type", havingValue = "oss")
public class OssFileUploadUtil implements FileUploadUtil {
    // OSS 存储实现
}
```

```yaml
# Nacos: goods-store-common.yaml 补充
upload:
  storage:
    type: ${UPLOAD_STORAGE_TYPE:local} # 开发环境默认 local，生产环境切换 oss
```

---

## Day 6：商品服务（上）— 分类树 + 商品列表/详情/搜索

### 技术要点

| 要点           | 说明                                                                                           |
| -------------- | ---------------------------------------------------------------------------------------------- |
| 分类树递归查询 | 一次性查询全表，内存中构建树形结构（避免 N+1 查询）                                            |
| 品牌名称组装   | **契约消费**：`product-api` 通过 `extends BrandApi` 的 Feign 客户端拿品牌名称，封装到 `GoodVO` |
| 多条件筛选     | MyBatis-Plus `LambdaQueryWrapper` 动态拼接查询条件                                             |
| 关键词搜索     | `LIKE` 匹配 `name`、`alias`、`summary` 字段                                                    |
| 详情图一对多   | 查询 `good_detail_pics` 表，按 `sort` 排序                                                     |

### 实现内容

| 序号 | 任务                 | 涉及文件                                                                                               | 备注                                                            |
| ---- | -------------------- | ------------------------------------------------------------------------------------------------------ | --------------------------------------------------------------- |
| 6.1  | Category 树形查询    | `product-api/.../service/CategoryService.java`                                                         | 递归构建树形 JSON                                               |
| 6.2  | CategoryController   | `product-api/.../api/CategoryController.java`                                                          | `GET /product/api/category/tree`                                |
| 6.3  | GoodService 实现类   | `product-api/.../service/impl/GoodServiceImpl.java`                                                    | 分页查询 + 多条件筛选                                           |
| 6.4  | GoodController 完善  | `product-api/.../api/GoodController.java`                                                              | 列表/详情/搜索                                                  |
| 6.5  | 品牌名称 Feign 调用  | `product-api/.../remote/BrandRemoteService.java`                                                       | 注入 `extends BrandApi` 的 `BrandRemoteClient`，异常降级返回"-" |
| 6.6  | GoodVO 完善          | `product-spi/.../vo/GoodVO.java`                                                                       | 补充 brandName/categoryName/detailPics 字段                     |
| 6.7  | GoodDetailPicsMapper | `product-api/.../repository/GoodDetailPicsMapper.java`                                                 | 按 good_id 查询详情图列表                                       |
| 6.8  | **web 商品浏览聚合** | `web/.../web/product/WebProductController.java` + `WebProductService.java` + `ProductFeignClient.java` | 分类树 + 商品列表/详情 BFF 收口，`GET /app/api/product/*`       |

### 验收标准

- [ ] `GET /product/api/category/tree` 返回完整分类树（嵌套 JSON）
- [ ] `GET /product/api/good/page?pageNo=1&pageSize=10` 返回商品分页，含品牌名称
- [ ] `GET /product/api/good/page?categoryId=10&keyword=手机` 按分类+关键词筛选
- [ ] `GET /product/api/good/1` 返回商品详情，含详情图列表
- [ ] 商品不存在返回 2001 错误码
- [ ] 品牌服务不可用时，品牌名称降级显示"-"
- [ ] 商品详情图按 `sort` 字段升序返回
- [ ] **契约消费**：`product-api` 不另造品牌接口，直接复用 `brand-spi` 契约
- [ ] **web 收口**：`GET /app/api/product/list` 经 web 聚合返回分类 + 商品分页

### 补充细节

**分类树构建算法：**

```java
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryTreeVO> getTree() {
        // 1. 一次性查询所有分类
        List<CategoryEntity> allCategories = categoryMapper.selectList(
            new LambdaQueryWrapper<CategoryEntity>().orderByAsc(CategoryEntity::getSort)
        );

        // 2. 构建 parentId → children 映射
        Map<Integer, List<CategoryTreeVO>> parentMap = new HashMap<>();
        List<CategoryTreeVO> allVOs = allCategories.stream().map(this::toVO).toList();

        for (CategoryTreeVO vo : allVOs) {
            parentMap.computeIfAbsent(vo.getParentId(), k -> new ArrayList<>()).add(vo);
        }

        // 3. 递归设置 children
        for (CategoryTreeVO vo : allVOs) {
            vo.setChildren(parentMap.getOrDefault(vo.getId(), Collections.emptyList()));
        }

        // 4. 返回顶级分类（parentId = null 或 0）
        return parentMap.getOrDefault(null, parentMap.getOrDefault(0, Collections.emptyList()));
    }
}
```

**Feign 品牌名称查询（契约消费 + 降级，两种方案）：**

> 契约在 `brand-spi`（`BrandApi`），`product-api` **不重造接口**，只定义客户端继承契约。

```java
// 方案一：Feign Fallback（推荐，门户客户端挂降级工厂）
// ① product-api 内定义继承契约的客户端
@FeignClient(name = "goods-store-brand-api", path = "/brand/api",
             fallbackFactory = BrandClientFallbackFactory.class)
public interface BrandRemoteClient extends BrandApi {}

// ② 降级工厂（返回空列表 → brandName 显示 "-"）
@Component
public class BrandClientFallbackFactory implements FallbackFactory<BrandRemoteClient> {
    @Override
    public BrandRemoteClient create(Throwable cause) {
        log.error("品牌服务调用失败，启用降级", cause);
        return new BrandRemoteClient() {
            @Override public PageVO<BrandVO> page(BrandQueryRequest q) { return PageVO.empty(); }
            @Override public BrandVO getById(Long id) { return null; }
            // ... 其余契约方法按需降级
        };
    }
}
```

```java
// 方案二：封装层降级（当前方案，作为保底），同样注入契约客户端
@Component
@RequiredArgsConstructor
public class BrandRemoteService {
    private final BrandRemoteClient brandRemoteClient;  // extends BrandApi

    public Map<Long, String> getBrandNameMap(List<Long> brandIds) {
        try {
            PageVO<BrandVO> page = brandRemoteClient.page(new BrandQueryRequest());  // 或新增契约按 id 批量查
            return page.getRecords().stream()
                .filter(b -> brandIds.contains(b.getId()))
                .collect(Collectors.toMap(BrandVO::getId, BrandVO::getName));
        } catch (Exception e) {
            log.error("查询品牌名称失败，降级返回默认值", e);
            return Collections.emptyMap();
        }
    }
}
```

> 两种方案可同时使用：Feign Fallback 处理连接超时，`BrandRemoteService` 处理业务异常。**关键点**：`BrandRemoteClient`/`BrandRemoteService` 只 `extends` 或注入 `brand-spi` 契约，绝不手写 `@GetMapping("/...")`。

---

## Day 7：商品服务（下）— 商品 CRUD + 上下架 + Feign 库存接口

### 技术要点

| 要点           | 说明                                                           |
| -------------- | -------------------------------------------------------------- |
| 库存扣减乐观锁 | `UPDATE good SET qty = qty - ? WHERE id = ? AND qty >= ?`      |
| 商品上下架     | 更新 `is_take_down` 字段，已下架商品不可下单                   |
| 详情图管理     | 前端传图片 URL 数组 + 排序，后端批量操作 `good_detail_pics`    |
| Feign 库存接口 | 定义 `ProductApi` 中的 `deductStock()` / `restoreStock()` 方法 |

### 实现内容

| 序号 | 任务                 | 涉及文件                                             | 备注                                                                                                                          |
| ---- | -------------------- | ---------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------- |
| 7.1  | 商品新增/编辑        | `product-api/.../service/impl/GoodServiceImpl.java`  | 含详情图批量操作                                                                                                              |
| 7.2  | 商品上下架           | `product-api/.../api/GoodController.java`            | `PUT /product/api/good/{id}/status`                                                                                           |
| 7.3  | 商品删除             | `product-api/.../api/GoodController.java`            | 逻辑删除 `is_del`                                                                                                             |
| 7.4  | 库存扣减接口         | `product-api/.../api/GoodController.java`            | `PUT /product/api/good/{id}/stock/deduct`                                                                                     |
| 7.5  | 库存恢复接口         | `product-api/.../api/GoodController.java`            | `PUT /product/api/good/{id}/stock/restore`                                                                                    |
| 7.6  | ProductApi 契约化    | `product-spi/.../ProductApi.java`                    | 新增 `deductStock()` / `restoreStock()`；**去掉 `@FeignClient`**，`GoodController implements ProductApi`，下游 `extends` 复用 |
| 7.7  | 商品详情图接口       | `product-api/.../api/GoodDetailPicsController.java`  | 批量上传 + 排序                                                                                                               |
| 7.8  | **web 商品管理收口** | `web/.../web/product/WebProductAdminController.java` | 上下架/库存等后台操作也经 `/app/api/product/admin/*` 收口，管理端不直连 api                                                   |

### 验收标准

- [ ] `POST /product/api/good` 新增商品，含详情图
- [ ] `PUT /product/api/good/1` 编辑商品信息
- [ ] `PUT /product/api/good/1/status` 下架商品，商品详情页返回 404
- [ ] `PUT /product/api/good/1/stock/deduct` 扣减库存，库存不足返回 2002
- [ ] `PUT /product/api/good/1/stock/restore` 恢复库存
- [ ] 并发扣减库存不超卖（乐观锁行锁生效）
- [ ] **契约闭环**：`GoodController implements ProductApi`，`trade-api`/`seckill-api` 通过 `extends ProductApi` 扣减库存

### 补充细节

**库存扣减 SQL（MyBatis-Plus 自定义 Mapper）：**

```java
@Mapper
public interface GoodMapper extends BaseMapper<GoodEntity> {

    /**
     * 乐观锁扣减库存
     * @return 影响行数，0 表示库存不足
     */
    @Update("UPDATE good SET qty = qty - #{count} WHERE id = #{goodId} AND qty >= #{count}")
    int deductStock(@Param("goodId") Long goodId, @Param("count") Integer count);

    @Update("UPDATE good SET qty = qty + #{count} WHERE id = #{goodId}")
    int restoreStock(@Param("goodId") Long goodId, @Param("count") Integer count);
}
```

**Feign 库存接口补充（契约化）：**

> 契约在 `product-spi`（`ProductApi`），**不含 `@FeignClient`**；`trade-api`/`seckill-api` 定义客户端 `extends` 复用。

```java
// ① product-spi：纯契约（无 @FeignClient）
public interface ProductApi {

    @GetMapping("/good/page")
    PageVO<GoodVO> page(GoodQueryRequest request);

    @GetMapping("/good/{id}")
    GoodVO getById(@PathVariable Long id);

    /**
     * 扣减库存（内部调用）
     * @return true=扣减成功, false=库存不足
     */
    @PutMapping("/good/{id}/stock/deduct")
    ApiResult<Boolean> deductStock(@PathVariable Long id, @RequestParam Integer count);

    /**
     * 恢复库存（内部调用）
     */
    @PutMapping("/good/{id}/stock/restore")
    ApiResult<Void> restoreStock(@PathVariable Long id, @RequestParam Integer count);
}

// ①′ product-api：GoodController implements ProductApi 兑现契约（含库存扣减/恢复）

// ② 消费方 trade-api：定义客户端继承契约（注入后直接 productApi.deductStock(...)）
@FeignClient(name = "goods-store-product-api", path = "/product/api")
public interface TradeProductClient extends ProductApi {}
```

> `trade-api`/`seckill-api` 只需 `ProductApi` 这一个事实来源，新增库存方法只改 `product-spi` 契约一处，提供方 `implements`、消费方 `extends` 编译期即同步。

---

## Day 8：会员服务 — 信息查询 + 地址管理 + Feign 接口

### 技术要点

| 要点           | 说明                                                                |
| -------------- | ------------------------------------------------------------------- |
| 数据脱敏       | 手机号中间 4 位替换为 `****`，身份证号中间 8 位替换                 |
| 默认地址互斥   | 设置新默认地址时，先将其他地址 `is_default` 置 0                    |
| 用户权限校验   | 会员只能修改自己的信息（从网关 Header `X-User-Id` 获取当前用户 ID） |
| Feign 地址查询 | 供 trade-api 下单时获取默认收货地址                                 |

### 实现内容

| 序号 | 任务             | 涉及文件                                                                                           | 备注                                                                                                                             |
| ---- | ---------------- | -------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- |
| 8.1  | 会员信息查询     | `member-api/.../api/MemberController.java`                                                         | `GET /member/api/{id}`，脱敏返回                                                                                                 |
| 8.2  | 会员信息编辑     | `member-api/.../api/MemberController.java`                                                         | `PUT /member/api/{id}`                                                                                                           |
| 8.3  | 收货地址 CRUD    | `member-api/.../api/AddressController.java`                                                        | 增删改查 + 设置默认                                                                                                              |
| 8.4  | 默认地址互斥逻辑 | `member-api/.../service/impl/AddressServiceImpl.java`                                              | 事务保证                                                                                                                         |
| 8.5  | MemberApi 契约化 | `member-spi/.../MemberApi.java`                                                                    | 新增 `getDefaultAddress()`；**去 `@FeignClient`**，`MemberController`/`AddressController` 兑现，`trade-api`/`web` `extends` 复用 |
| 8.6  | 脱敏工具类       | `member-api/.../util/DesensitizeUtil.java`                                                         | 手机号/身份证脱敏                                                                                                                |
| 8.7  | **web 会员聚合** | `web/.../web/member/WebMemberController.java` + `WebMemberService.java` + `MemberFeignClient.java` | 个人中心 / 收货地址管理 BFF 收口，`/app/api/member/*`                                                                            |

### 验收标准

- [ ] `GET /member/api/1` 返回脱敏后的会员信息
- [ ] `PUT /member/api/1` 修改昵称/邮箱成功
- [ ] `GET /member/api/1/address` 返回地址列表，默认地址排第一
- [ ] `POST /member/api/1/address` 新增地址
- [ ] `PUT /member/api/1/address/1/default` 设置默认地址，旧默认地址自动取消
- [ ] 修改其他用户信息返回 403
- [ ] **契约闭环**：`MemberController implements MemberApi`，`trade-api` 下单取默认地址时 `extends` 复用
- [ ] **web 收口**：`/app/api/member/profile` 经 web 聚合返回脱敏资料 + 地址

### 补充细节

**地址互斥逻辑（事务）：**

```java
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final MemberAddressMapper addressMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long memberId, Long addressId) {
        // 1. 取消所有旧默认地址
        addressMapper.update(
            Wrappers.<MemberAddressEntity>lambdaUpdate()
                .set(MemberAddressEntity::getIsDefault, false)
                .eq(MemberAddressEntity::getMemberAccount, memberId)
        );

        // 2. 设置新默认地址
        addressMapper.update(
            Wrappers.<MemberAddressEntity>lambdaUpdate()
                .set(MemberAddressEntity::getIsDefault, true)
                .eq(MemberAddressEntity::getId, addressId)
                .eq(MemberAddressEntity::getMemberAccount, memberId)
        );
    }
}
```

**脱敏工具类：**

```java
public class DesensitizeUtil {

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) return idCard;
        return idCard.substring(0, 4) + "********" + idCard.substring(idCard.length() - 4);
    }
}
```

---

## Day 9：交易服务（上）— 购物车 CRUD + RabbitMQ 配置

### 技术要点

| 要点           | 说明                                                                                  |
| -------------- | ------------------------------------------------------------------------------------- |
| 购物车唯一约束 | `INSERT ... ON DUPLICATE KEY UPDATE qty = qty + ?`（利用 `uq_member_good` 索引）      |
| 库存校验       | 添加购物车/修改数量时，Feign 调用 product-api 查询商品是否下架、库存是否充足          |
| RabbitMQ 配置  | 声明 Exchange、Queue、Binding，配置 `RabbitTemplate` + `Jackson2JsonMessageConverter` |
| MQ 消息确认    | `publisher-confirm-type: correlated` + `publisher-returns: true`                      |

### 实现内容

| 序号 | 任务                   | 涉及文件                                                                                     | 备注                                                                            |
| ---- | ---------------------- | -------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------- |
| 9.1  | trade-api POM 补充依赖 | `trade-api/pom.xml`                                                                          | AMQP + Redis + Redisson                                                         |
| 9.2  | RabbitMQ 配置类        | `trade-api/.../config/RabbitMqConfig.java`                                                   | 声明 Exchange/Queue/Binding                                                     |
| 9.3  | 购物车列表             | `trade-api/.../api/CartController.java`                                                      | `GET /trade/api/cart`，关联商品信息                                             |
| 9.4  | 添加购物车             | `trade-api/.../api/CartController.java`                                                      | `POST /trade/api/cart`，唯一约束处理                                            |
| 9.5  | 修改数量               | `trade-api/.../api/CartController.java`                                                      | `PUT /trade/api/cart/{id}`                                                      |
| 9.6  | 删除购物车             | `trade-api/.../api/CartController.java`                                                      | `DELETE /trade/api/cart/{id}`，支持批量                                         |
| 9.7  | CartService 实现       | `trade-api/.../service/impl/CartServiceImpl.java`                                            | 库存校验 + 商品下架校验                                                         |
| 9.8  | **web 购物车聚合**     | `web/.../web/trade/WebCartController.java` + `WebCartService.java` + `TradeFeignClient.java` | 购物车列表/增删改收口 `/app/api/cart/*`；列表时 Feign 补商品图/名（聚合规则 1） |

### 验收标准

- [ ] `GET /trade/api/cart` 返回当前会员购物车列表，含商品名称/图片/价格
- [ ] `POST /trade/api/cart` 添加商品，再次添加同一商品时数量累加
- [ ] `PUT /trade/api/cart/1` 修改数量，超过库存时返回错误
- [ ] `DELETE /trade/api/cart/1` 删除购物车项
- [ ] 已下架商品添加购物车返回错误
- [ ] RabbitMQ 控制台可看到 Exchange 和 Queue 创建成功
- [ ] **契约消费**：`trade-api` 校验库存用 `extends ProductApi` 的客户端，不手写路径
- [ ] **web 收口**：`GET /app/api/cart/list` 经 web 聚合（购物车 + 商品图/名）返回

### 补充细节

**RabbitMQ 配置类：**

```java
@Configuration
public class RabbitMqConfig {

    public static final String ORDER_EXCHANGE = "goods.order.exchange";

    // 订单创建队列
    public static final String ORDER_CREATE_QUEUE = "goods.order.create.queue";
    public static final String ORDER_CREATE_KEY = "order.create";

    // 订单超时队列（延迟队列）
    public static final String ORDER_TIMEOUT_QUEUE = "goods.order.timeout.queue";
    public static final String ORDER_TIMEOUT_KEY = "order.timeout";

    // 订单取消队列（死信队列）
    public static final String ORDER_CANCEL_QUEUE = "goods.order.cancel.queue";
    public static final String ORDER_CANCEL_KEY = "order.cancel";

    // 秒杀订单队列
    public static final String SECKILL_ORDER_QUEUE = "goods.seckill.order.queue";
    public static final String SECKILL_ORDER_KEY = "seckill.order.create";

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderCreateQueue() {
        return QueueBuilder.durable(ORDER_CREATE_QUEUE).build();
    }

    @Bean
    public Queue orderTimeoutQueue() {
        return QueueBuilder.durable(ORDER_TIMEOUT_QUEUE)
                .ttl(30 * 60 * 1000)  // 30分钟
                .deadLetterExchange(ORDER_EXCHANGE)
                .deadLetterRoutingKey(ORDER_CANCEL_KEY)
                .build();
    }

    @Bean
    public Queue orderCancelQueue() {
        return QueueBuilder.durable(ORDER_CANCEL_QUEUE).build();
    }

    @Bean
    public Queue seckillOrderQueue() {
        return QueueBuilder.durable(SECKILL_ORDER_QUEUE).build();
    }

    @Bean
    public Binding orderCreateBinding() {
        return BindingBuilder.bind(orderCreateQueue()).to(orderExchange()).with(ORDER_CREATE_KEY);
    }

    @Bean
    public Binding orderTimeoutBinding() {
        return BindingBuilder.bind(orderTimeoutQueue()).to(orderExchange()).with(ORDER_TIMEOUT_KEY);
    }

    @Bean
    public Binding orderCancelBinding() {
        return BindingBuilder.bind(orderCancelQueue()).to(orderExchange()).with(ORDER_CANCEL_KEY);
    }

    @Bean
    public Binding seckillOrderBinding() {
        return BindingBuilder.bind(seckillOrderQueue()).to(orderExchange()).with(SECKILL_ORDER_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
```

**添加购物车（利用唯一索引）：**

```java
@Override
public void addCart(CartAddRequest request) {
    // 1. 校验商品是否存在、是否下架、库存是否充足
    ApiResult<GoodVO> result = productApi.getById(request.getGoodId());
    GoodVO good = result.getData();
    if (good == null || Boolean.TRUE.equals(good.getIsTakeDown())) {
        throw new BusinessException(ErrorCode.GOOD_NOT_FOUND);
    }
    if (good.getQty() < request.getQty()) {
        throw new BusinessException(ErrorCode.GOOD_STOCK_INSUFFICIENT);
    }

    // 2. INSERT ... ON DUPLICATE KEY UPDATE（利用 uq_member_good 唯一索引）
    cartMapper.insertOrUpdate(request.getMemberId(), request.getGoodId(), request.getQty());
}
```

```java
@Mapper
public interface CartMapper extends BaseMapper<CartEntity> {

    @Insert("INSERT INTO cart (member_id, good_id, qty) VALUES (#{memberId}, #{goodId}, #{qty}) " +
            "ON DUPLICATE KEY UPDATE qty = qty + #{qty}")
    int insertOrUpdate(@Param("memberId") Long memberId,
                       @Param("goodId") Long goodId,
                       @Param("qty") Integer qty);
}
```

---

## Day 10：交易服务（中）— 下单接口 + Redisson 分布式锁

### 技术要点

| 要点           | 说明                                                                           |
| -------------- | ------------------------------------------------------------------------------ |
| 分布式锁       | Redisson `RLock`，基于订单号加锁，防止重复下单                                 |
| 雪花算法订单号 | `SnowflakeUtil.nextIdStr()` 生成唯一订单号                                     |
| 下单流程       | 锁 → 校验库存 → 创建订单 → 发送 MQ 扣库存 → 清空购物车 → 发送延迟消息 → 释放锁 |
| 事务控制       | `@Transactional` 保证订单 + 订单明细原子写入，MQ 发送在事务提交后              |

### 实现内容

| 序号 | 任务              | 涉及文件                                                             | 备注                                                                                  |
| ---- | ----------------- | -------------------------------------------------------------------- | ------------------------------------------------------------------------------------- |
| 10.1 | Redisson 配置类   | `trade-api/.../config/RedissonConfig.java`                           | 单机模式配置                                                                          |
| 10.2 | 下单接口          | `trade-api/.../api/OrderController.java`                             | `POST /trade/api/order`                                                               |
| 10.3 | OrderService 实现 | `trade-api/.../service/impl/OrderServiceImpl.java`                   | 完整下单流程                                                                          |
| 10.4 | 订单号生成        | `trade-api/.../service/impl/OrderServiceImpl.java`                   | 雪花算法                                                                              |
| 10.5 | MQ 消息发送       | `trade-api/.../mq/OrderMessageProducer.java`                         | 发送订单创建 + 延迟消息                                                               |
| 10.6 | 下单请求 DTO      | `trade-api/.../dto/OrderCreateRequest.java`                          | 收货地址 ID + 备注                                                                    |
| 10.7 | **web 下单聚合**  | `web/.../web/trade/WebOrderController.java` + `WebOrderService.java` | `POST /app/api/order/submit` 编排下单：校验购物车 → 调下单 → 返回订单号（聚合规则 2） |

### 验收标准

- [ ] `POST /trade/api/order` 下单成功，返回订单号
- [ ] 下单后购物车自动清空
- [ ] 库存不足时下单失败，返回 2002
- [ ] 同一请求并发重复下单，分布式锁生效，仅创建一单
- [ ] 订单表 `order` 和 `order_item` 同时写入（事务）
- [ ] RabbitMQ 控制台可看到订单创建消息 + 延迟消息
- [ ] **web 收口**：`POST /app/api/order/submit` 经 `WebOrderService` 编排完成下单，前端不直连 `trade-api`

### 补充细节

**下单完整流程：**

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final CartMapper cartMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductApi productApi;
    private final MemberApi memberApi;
    private final RedissonClient redissonClient;
    private final RabbitTemplate rabbitTemplate;
    private final SnowflakeUtil snowflakeUtil;

    @Override
    public OrderCreateResponse createOrder(Long memberId, OrderCreateRequest request) {
        // 1. 查询购物车
        List<CartEntity> cartItems = cartMapper.selectByMemberId(memberId);
        if (cartItems.isEmpty()) {
            throw new BusinessException(ErrorCode.CART_EMPTY);
        }

        // 2. 生成订单号
        String orderNo = snowflakeUtil.nextIdStr();

        // 3. 分布式锁防重复下单
        RLock lock = redissonClient.getLock("lock:order:" + memberId);
        try {
            if (!lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                throw new BusinessException("系统繁忙，请稍后重试", 500);
            }

            // 4. 校验库存 + 计算总价
            BigDecimal totalPay = BigDecimal.ZERO;
            List<OrderItemEntity> orderItems = new ArrayList<>();
            for (CartEntity cartItem : cartItems) {
                ApiResult<GoodVO> result = productApi.getById(cartItem.getGoodId());
                GoodVO good = result.getData();
                if (good == null || Boolean.TRUE.equals(good.getIsTakeDown())) {
                    throw new BusinessException(ErrorCode.GOOD_NOT_FOUND);
                }
                if (good.getQty() < cartItem.getQty()) {
                    throw new BusinessException(ErrorCode.GOOD_STOCK_INSUFFICIENT);
                }
                totalPay = totalPay.add(good.getPrice().multiply(BigDecimal.valueOf(cartItem.getQty())));
                orderItems.add(buildOrderItem(orderNo, good, cartItem.getQty()));
            }

            // 5. 获取默认收货地址
            AddressVO address = memberApi.getDefaultAddress(memberId).getData();
            if (address == null) {
                throw new BusinessException("请先设置收货地址", 400);
            }

            // 6. 创建订单 + 订单明细（事务）
            OrderEntity order = buildOrder(orderNo, memberId, totalPay, address, request.getComment());
            orderMapper.insert(order);
            orderItemMapper.insertBatch(orderItems);

            // 7. 事务提交后发 MQ（使用 @TransactionalEventListener 或 TransactionSynchronization）
            // 发送订单创建消息（异步扣减库存）
            sendOrderCreateMessage(orderNo, cartItems);
            // 发送延迟消息（30分钟未支付取消）
            sendOrderTimeoutMessage(orderNo);

            // 8. 清空购物车
            cartMapper.deleteByMemberId(memberId);

            return new OrderCreateResponse(orderNo, totalPay);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("系统繁忙", 500);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

**Redisson 配置：**

```java
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + redisHost + ":" + redisPort)
                .setConnectionPoolSize(10)
                .setConnectionMinimumIdleSize(5);
        return Redisson.create(config);
    }
}
```

---

## Day 11：交易服务（下）— MQ 消费者 + 超时取消 + 订单管理

### 技术要点

| 要点         | 说明                                                           |
| ------------ | -------------------------------------------------------------- |
| MQ 消息幂等  | 消费者根据订单号查 DB 判断是否已处理，防止重复消费重复创建订单 |
| 订单状态机   | PENDING → CANCELLED / PAID → SHIPPED → COMPLETED               |
| 状态流转校验 | 取消仅 PENDING 可操作，确认收货仅 SHIPPED 可操作               |
| MQ 手动确认  | `channel.basicAck()` 成功确认，`channel.basicNack()` 失败重试  |
| 死信队列     | 30 分钟超时消息自动路由到 `goods.order.cancel.queue`           |
| 库存恢复     | 取消订单时 Feign 调用 product-api 恢复库存                     |

### 实现内容

| 序号 | 任务                  | 涉及文件                                                                   | 备注                                                                        |
| ---- | --------------------- | -------------------------------------------------------------------------- | --------------------------------------------------------------------------- |
| 11.1 | 订单列表查询          | `trade-api/.../api/OrderController.java`                                   | `GET /trade/api/order/page`                                                 |
| 11.2 | 订单详情              | `trade-api/.../api/OrderController.java`                                   | `GET /trade/api/order/{id}`                                                 |
| 11.3 | 取消订单              | `trade-api/.../api/OrderController.java`                                   | `PUT /trade/api/order/{id}/cancel`                                          |
| 11.4 | 确认收货              | `trade-api/.../api/OrderController.java`                                   | `PUT /trade/api/order/{id}/confirm`                                         |
| 11.5 | MQ 消费者（库存扣减） | `trade-api/.../mq/OrderCreateConsumer.java`                                | 消费 `goods.order.create.queue`                                             |
| 11.6 | MQ 消费者（超时取消） | `trade-api/.../mq/OrderCancelConsumer.java`                                | 消费 `goods.order.cancel.queue`                                             |
| 11.7 | MQ 消费者（库存恢复） | `trade-api/.../mq/OrderCancelConsumer.java`                                | 恢复库存                                                                    |
| 11.8 | 订单状态枚举          | `trade-api/.../enums/OrderStatus.java`                                     | 5 个状态                                                                    |
| 11.9 | **web 订单管理聚合**  | `web/.../web/trade/WebOrderMgrController.java` + `WebOrderMgrService.java` | 订单列表/详情/取消/确认收口 `/app/api/order/*`（聚合规则 3：磨平分页/状态） |

### 验收标准

- [ ] `GET /trade/api/order/page` 返回当前会员订单分页
- [ ] `GET /trade/api/order/1` 返回订单详情 + 订单明细
- [ ] `PUT /trade/api/order/1/cancel` 取消待支付订单，库存恢复
- [ ] 已支付订单取消返回 3002（状态异常）
- [ ] `PUT /trade/api/order/1/confirm` 确认收货
- [ ] MQ 消费者正常消费消息，库存正确扣减
- [ ] 延迟消息 30 分钟后自动触发订单取消（可缩短 TTL 测试）
- [ ] 同一条订单创建消息消费两次，只创建一条订单（MQ 幂等验证）
- [ ] 死信队列消费后，订单状态正确更新为"已取消"
- [ ] **契约消费**：MQ 消费者扣减/恢复库存复用 `extends ProductApi` 的 `TradeProductClient`
- [ ] **web 收口**：`GET /app/api/order/page` 经 web 聚合返回（磨平分页结构）

### 补充细节

**MQ 消息幂等处理：**

```java
// 消费者根据订单号 + 业务类型判断是否已处理
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderCreateConsumer {

    private final ProductApi productApi;
    private final OrderMapper orderMapper;

    @RabbitListener(queues = RabbitMqConfig.ORDER_CREATE_QUEUE)
    public void handleOrderCreate(OrderCreateMessage message, Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            // 幂等检查：根据订单号查询是否已处理
            if (orderMapper.existsByOrderNo(message.getOrderNo())) {
                log.warn("订单已处理，跳过重复消息：{}", message.getOrderNo());
                channel.basicAck(deliveryTag, false);
                return;
            }

            log.info("处理订单创建消息：{}", message.getOrderNo());
            for (OrderItemMsg item : message.getItems()) {
                ApiResult<Boolean> result = productApi.deductStock(item.getGoodId(), item.getCount());
                if (!Boolean.TRUE.equals(result.getData())) {
                    log.error("库存扣减失败：orderNo={}, goodId={}", message.getOrderNo(), item.getGoodId());
                }
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("消费订单创建消息异常", e);
            try {
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ex) {
                log.error("消息确认失败", ex);
            }
        }
    }
}
```

**MQ 消费者（库存扣减）：**

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderCreateConsumer {

    private final ProductApi productApi;

    @RabbitListener(queues = RabbitMqConfig.ORDER_CREATE_QUEUE)
    public void handleOrderCreate(OrderCreateMessage message, Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            log.info("处理订单创建消息：{}", message.getOrderNo());
            for (OrderItemMsg item : message.getItems()) {
                ApiResult<Boolean> result = productApi.deductStock(item.getGoodId(), item.getCount());
                if (!Boolean.TRUE.equals(result.getData())) {
                    log.error("库存扣减失败：orderNo={}, goodId={}", message.getOrderNo(), item.getGoodId());
                    // 库存不足，记录异常，人工处理
                }
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("消费订单创建消息异常", e);
            try {
                channel.basicNack(deliveryTag, false, true);  // 重新入队
            } catch (IOException ex) {
                log.error("消息确认失败", ex);
            }
        }
    }
}
```

**MQ 消费者（超时取消）：**

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderCancelConsumer {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductApi productApi;

    @RabbitListener(queues = RabbitMqConfig.ORDER_CANCEL_QUEUE)
    public void handleOrderCancel(OrderCancelMessage message, Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            OrderEntity order = orderMapper.selectById(message.getOrderId());
            // 仅取消仍为 PENDING 的订单
            if (order != null && OrderStatus.PENDING.name().equals(order.getStatus())) {
                order.setStatus(OrderStatus.CANCELLED.name());
                orderMapper.updateById(order);

                // 恢复库存
                List<OrderItemEntity> items = orderItemMapper.selectByOrderId(order.getId());
                for (OrderItemEntity item : items) {
                    productApi.restoreStock(item.getGoodId(), item.getCount());
                }
                log.info("订单超时自动取消：{}", order.getOrderNo());
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("处理订单取消消息异常", e);
            try {
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ex) {
                log.error("消息确认失败", ex);
            }
        }
    }
}
```

---

## Day 12：秒杀服务（上）— 活动管理 + 库存预热

### 技术要点

| 要点           | 说明                                                       |
| -------------- | ---------------------------------------------------------- |
| 秒杀活动管理   | CRUD `seckill` + `seckill_good` 表，时间窗口校验           |
| 库存预热       | `@Scheduled` 定时任务，秒杀开始前 5 分钟将库存加载到 Redis |
| Redis 库存 Key | `seckill:stock:{seckillGoodId}`，值为总库存                |
| 防重复 Key     | `seckill:order:{userId}:{seckillGoodId}`，SETNX 原子操作   |

### 实现内容

| 序号 | 任务                     | 涉及文件                                                                                               | 备注                                                                |
| ---- | ------------------------ | ------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------- |
| 12.1 | seckill-api POM 补充依赖 | `seckill-api/pom.xml`                                                                                  | AMQP + Redis + Redisson                                             |
| 12.2 | 秒杀活动 CRUD            | `seckill-api/.../api/SeckillController.java`                                                           | 增删改查                                                            |
| 12.3 | 秒杀商品 CRUD            | `seckill-api/.../api/SeckillGoodController.java`                                                       | 关联秒杀活动                                                        |
| 12.4 | 秒杀商品列表（用户端）   | `seckill-api/.../api/SeckillController.java`                                                           | `GET /seckill/api/list`，过滤当前有效活动                           |
| 12.5 | 库存预热定时任务         | `seckill-api/.../job/StockPreheatJob.java`                                                             | `@Scheduled(cron = "...")`                                          |
| 12.6 | Redis 配置类             | `seckill-api/.../config/RedisConfig.java`                                                              | StringRedisTemplate + Lua 脚本加载                                  |
| 12.7 | Redisson 配置            | `seckill-api/.../config/RedissonConfig.java`                                                           | 同 trade-api                                                        |
| 12.8 | **web 秒杀浏览聚合**     | `web/.../web/seckill/WebSeckillController.java` + `WebSeckillService.java` + `SeckillFeignClient.java` | 活动/秒杀商品列表收口 `/app/api/seckill/list`（补当前活动窗口字段） |

### 验收标准

- [ ] `POST /seckill/api/activity` 创建秒杀活动，设置时间范围
- [ ] `POST /seckill/api/activity/1/goods` 添加秒杀商品
- [ ] `GET /seckill/api/list` 仅返回当前时间在活动窗口内的秒杀商品
- [ ] 秒杀开始前 5 分钟，Redis 中自动写入 `seckill:stock:{id}` 库存
- [ ] 秒杀结束后，库存预热 Key 自动过期
- [ ] **web 收口**：`GET /app/api/seckill/list` 经 web 聚合返回活动窗口商品（含倒计时）

### 补充细节

**库存预热定时任务：**

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class StockPreheatJob {

    private final SeckillMapper seckillMapper;
    private final SeckillGoodMapper seckillGoodMapper;
    private final GoodMapper goodMapper;
    private final StringRedisTemplate redisTemplate;

    private static final String STOCK_KEY_PREFIX = "seckill:stock:";

    /**
     * 每分钟扫描即将开始的秒杀活动，预热库存
     */
    @Scheduled(cron = "0 * * * * ?")
    public void preheatStock() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fiveMinutesLater = now.plusMinutes(5);

        // 查找 5 分钟后开始的秒杀活动
        List<SeckillEntity> upcomingSeckills = seckillMapper.selectList(
            new LambdaQueryWrapper<SeckillEntity>()
                .eq(SeckillEntity::getEnabled, true)
                .between(SeckillEntity::getStartTime, now, fiveMinutesLater)
        );

        for (SeckillEntity seckill : upcomingSeckills) {
            // 查询该活动关联的商品
            List<SeckillGoodEntity> goods = seckillGoodMapper.selectList(
                new LambdaQueryWrapper<SeckillGoodEntity>()
                    .eq(SeckillGoodEntity::getSeckillId, seckill.getId())
            );

            for (SeckillGoodEntity seckillGood : goods) {
                String stockKey = STOCK_KEY_PREFIX + seckillGood.getId();
                // 只在未预热时写入（避免重复预热）
                Boolean exists = redisTemplate.hasKey(stockKey);
                if (Boolean.FALSE.equals(exists)) {
                    GoodEntity good = goodMapper.selectById(seckillGood.getGoodId());
                    if (good != null) {
                        redisTemplate.opsForValue().set(stockKey, String.valueOf(good.getQty()));
                        // 设置过期时间 = 活动结束时间 + 1小时
                        long expireSeconds = Duration.between(now, seckill.getEndTime()).getSeconds() + 3600;
                        redisTemplate.expire(stockKey, Duration.ofSeconds(expireSeconds));
                        log.info("秒杀库存预热完成：seckillGoodId={}, stock={}", seckillGood.getId(), good.getQty());
                    }
                }
            }
        }
    }
}
```

**秒杀活动时间校验：**

```java
public List<SeckillGoodVO> getActiveSeckillGoods() {
    LocalDateTime now = LocalDateTime.now();
    return seckillGoodMapper.selectActiveSeckillGoods(now);
}
```

```java
@Mapper
public interface SeckillGoodMapper extends BaseMapper<SeckillGoodEntity> {

    @Select("""
        SELECT sg.*, g.name as goodName, g.price as originalPrice, g.pic as goodPic
        FROM seckill_good sg
        JOIN seckill s ON sg.seckill_id = s.id
        JOIN good g ON sg.good_id = g.id
        WHERE s.enabled = 1
          AND s.start_time <= #{now}
          AND s.end_time >= #{now}
          AND g.is_take_down = 0
          AND g.is_del = 0
        """)
    List<SeckillGoodVO> selectActiveSeckillGoods(@Param("now") LocalDateTime now);
}
```

---

## Day 13：秒杀服务（下）— Lua 脚本抢购 + MQ 消费者 + 补偿机制

### 技术要点

| 要点      | 说明                                                                   |
| --------- | ---------------------------------------------------------------------- |
| Lua 脚本  | Redis 原子操作：检查库存 + 防重复 + 扣减库存，3 步原子执行             |
| MQ 削峰   | 秒杀请求先入 MQ 队列，异步创建订单，前端轮询结果                       |
| DB 乐观锁 | `UPDATE seckill_good SET stock = stock - 1 WHERE id = ? AND stock > 0` |
| 补偿机制  | MQ 消费失败时，补偿 Redis 库存 + 删除防重标记                          |

### 实现内容

| 序号 | 任务                      | 涉及文件                                               | 备注                                                                                             |
| ---- | ------------------------- | ------------------------------------------------------ | ------------------------------------------------------------------------------------------------ |
| 13.1 | Lua 脚本加载              | `seckill-api/.../config/RedisConfig.java`              | 加载 `seckill.lua` 脚本                                                                          |
| 13.2 | 秒杀抢购接口              | `seckill-api/.../api/SeckillController.java`           | `POST /seckill/api/{seckillGoodId}/order`                                                        |
| 13.3 | SeckillService 实现       | `seckill-api/.../service/impl/SeckillServiceImpl.java` | Lua 预减 + MQ 发送                                                                               |
| 13.4 | MQ 消息生产者             | `seckill-api/.../mq/SeckillMessageProducer.java`       | 发送秒杀订单消息                                                                                 |
| 13.5 | MQ 消费者（秒杀订单创建） | `seckill-api/.../mq/SeckillOrderConsumer.java`         | DB 乐观锁 + 订单生成 + 补偿                                                                      |
| 13.6 | 秒杀订单查询              | `seckill-api/.../api/SeckillController.java`           | `GET /seckill/api/order/list`                                                                    |
| 13.7 | 秒杀结果查询              | `seckill-api/.../api/SeckillController.java`           | `GET /seckill/api/order/{orderNo}/result`                                                        |
| 13.8 | **web 秒杀参与聚合**      | `web/.../web/seckill/WebSeckillController.java` 扩充   | 抢购 `POST /app/api/seckill/order` + 结果轮询 `GET /app/api/seckill/order/{orderNo}/result` 收口 |

### 验收标准

- [ ] `POST /seckill/api/1/order` 秒杀抢购成功，返回"排队中"
- [ ] 同一用户重复抢购返回"已抢购"
- [ ] 库存不足返回"已售罄"
- [ ] Redis 库存正确递减
- [ ] MQ 消费成功，DB 订单创建成功
- [ ] MQ 消费失败时，Redis 库存自动补偿
- [ ] `GET /seckill/api/order/{orderNo}/result` 可查询秒杀结果（轮询：排队中 → 已抢到/已售罄）
- [ ] JMeter 模拟 1000 并发抢购，库存扣减准确，不超卖，Redis 与 DB 库存一致
- [ ] **web 收口**：抢购与结果轮询均经 `/app/api/seckill/*`，前端不直连 `seckill-api`

### 补充细节

**秒杀结果轮询设计：**

```
[前端]                    [Gateway]                  [Seckill-API]            [Redis]
  │                           │                           │                       │
  │  POST /seckill/1/order    │                           │                       │
  │──────────────────────────►│                           │                       │
  │                           │──────────────────────────►│                       │
  │                           │                           │  Lua 预减库存          │
  │                           │                           │──────────────────────►│
  │                           │                           │  发送 MQ 消息          │
  │                           │                           │◄──────────────────────│
  │  { orderNo, "排队中" }    │                           │                       │
  │◄──────────────────────────│                           │                       │
  │                           │                           │                       │
  │  GET /seckill/order/xxx/result（轮询，每 1 秒）       │                       │
  │──────────────────────────►│──────────────────────────►│                       │
  │                           │                           │  查询订单状态          │
  │                           │                           │  PENDING → "排队中"    │
  │                           │                           │  PAID → "已抢到"       │
  │                           │                           │  CANCELLED → "已售罄"  │
  │  { status: "排队中" }     │                           │                       │
  │◄──────────────────────────│                           │                       │
  │                           │                           │                       │
  │  （MQ 消费完成后，订单状态更新为 PAID）               │                       │
  │                           │                           │                       │
  │  GET /seckill/order/xxx/result（再次轮询）            │                       │
  │──────────────────────────►│──────────────────────────►│                       │
  │  { status: "已抢到" }     │                           │                       │
  │◄──────────────────────────│                           │                       │
```

> 前端抢购成功后先返回"排队中"，MQ 消费完成后更新为"已抢到"。前端需轮询 `/seckill/api/order/{orderNo}/result` 获取最终结果。

**Lua 脚本（`seckill.lua`）：**

```lua
-- KEYS[1]: seckill:stock:{seckillGoodId}
-- KEYS[2]: seckill:order:{userId}:{seckillGoodId}
-- ARGV[1]: 防重标记过期时间（秒）
-- 返回值: -1=库存不足, -2=重复抢购, >=0=剩余库存

local stockKey = KEYS[1]
local orderKey = KEYS[2]
local expireSeconds = tonumber(ARGV[1])

local stock = redis.call('GET', stockKey)
if not stock or tonumber(stock) <= 0 then
    return -1
end

local exists = redis.call('EXISTS', orderKey)
if exists == 1 then
    return -2
end

redis.call('DECR', stockKey)
redis.call('SETEX', orderKey, expireSeconds, '1')
return tonumber(stock) - 1
```

**秒杀抢购核心逻辑：**

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class SeckillServiceImpl implements SeckillService {

    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final SnowflakeUtil snowflakeUtil;
    private final RedisScript<Long> seckillScript;  // 注入 Lua 脚本

    @Override
    public SeckillOrderResponse executeSeckill(Long seckillGoodId, Long memberId) {
        // 1. 校验秒杀活动状态
        SeckillGoodEntity seckillGood = seckillGoodMapper.selectById(seckillGoodId);
        SeckillEntity seckill = seckillMapper.selectById(seckillGood.getSeckillId());
        validateSeckillStatus(seckill);

        // 2. 执行 Lua 脚本预减库存
        String stockKey = "seckill:stock:" + seckillGoodId;
        String orderKey = "seckill:order:" + memberId + ":" + seckillGoodId;
        Long result = redisTemplate.execute(
            seckillScript,
            List.of(stockKey, orderKey),
            "3600"  // 防重标记 1 小时
        );

        if (result == -1L) {
            throw new BusinessException(ErrorCode.SECKILL_STOCK_EMPTY);
        }
        if (result == -2L) {
            throw new BusinessException("您已参与过该秒杀", 4005);
        }

        // 3. 生成订单号
        String orderNo = snowflakeUtil.nextIdStr();

        // 4. 发送 MQ 消息异步创建订单
        SeckillOrderMessage message = new SeckillOrderMessage();
        message.setSeckillGoodId(seckillGoodId);
        message.setGoodId(seckillGood.getGoodId());
        message.setMemberId(memberId);
        message.setOrderNo(orderNo);
        message.setSeckillId(seckill.getId());

        rabbitTemplate.convertAndSend(
            RabbitMqConfig.ORDER_EXCHANGE,
            RabbitMqConfig.SECKILL_ORDER_KEY,
            message
        );

        return new SeckillOrderResponse(orderNo, "排队中，请稍后查询结果");
    }
}
```

**MQ 消费者（秒杀订单创建 + 补偿）：**

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class SeckillOrderConsumer {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final GoodMapper goodMapper;
    private final StringRedisTemplate redisTemplate;

    @RabbitListener(queues = RabbitMqConfig.SECKILL_ORDER_QUEUE)
    public void handleSeckillOrder(SeckillOrderMessage message, Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            // 1. DB 乐观锁扣减库存
            GoodEntity good = goodMapper.selectById(message.getGoodId());
            int rows = goodMapper.deductStock(message.getGoodId(), 1);
            if (rows == 0) {
                // 库存不足，补偿 Redis
                compensateRedis(message);
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 2. 创建订单
            OrderEntity order = new OrderEntity();
            order.setOrderNo(message.getOrderNo());
            order.setMemberAccount(String.valueOf(message.getMemberId()));
            order.setTotalPay(good.getPrice());
            order.setStatus(OrderStatus.PENDING.name());
            order.setSeckillNo(message.getOrderNo());
            orderMapper.insert(order);

            OrderItemEntity item = new OrderItemEntity();
            item.setOrderId(order.getId());
            item.setGoodId(message.getGoodId());
            item.setGoodName(good.getName());
            item.setGoodPic(good.getPic());
            item.setPrice(good.getPrice());
            item.setCount(1);
            orderItemMapper.insert(item);

            channel.basicAck(deliveryTag, false);
            log.info("秒杀订单创建成功：{}", message.getOrderNo());

        } catch (Exception e) {
            log.error("秒杀订单创建失败，执行补偿", e);
            compensateRedis(message);
            try {
                channel.basicAck(deliveryTag, false);  // 补偿后确认，不再重试
            } catch (IOException ex) {
                log.error("消息确认失败", ex);
            }
        }
    }

    /**
     * 补偿 Redis：恢复库存 + 删除防重标记
     */
    private void compensateRedis(SeckillOrderMessage message) {
        String stockKey = "seckill:stock:" + message.getSeckillGoodId();
        String orderKey = "seckill:order:" + message.getMemberId() + ":" + message.getSeckillGoodId();
        redisTemplate.opsForValue().increment(stockKey);  // 恢复库存
        redisTemplate.delete(orderKey);                   // 删除防重标记
        log.info("秒杀补偿完成：seckillGoodId={}, memberId={}", message.getSeckillGoodId(), message.getMemberId());
    }
}
```

---

## Day 14：集成收尾 — 异常处理增强 + Knife4j + 链路追踪 + 集成测试

### 技术要点

| 要点           | 说明                                                                                                 |
| -------------- | ---------------------------------------------------------------------------------------------------- |
| Feign 异常解码 | `FeignErrorDecoder` 将下游服务异常转换为 `BusinessException`                                         |
| 参数校验异常   | `MethodArgumentNotValidException` 统一处理，返回友好错误信息                                         |
| Knife4j 聚合   | **web 收口后文档入口改为 web**：聚合 `/app/api`（前端）+ 各 api 服务（内部），一个入口查看所有接口   |
| MDC TraceId    | `GlobalFilter` 生成 TraceId 写入 MDC，Feign 请求拦截器透传（**手动透传方案，不引入 Sleuth+Zipkin**） |
| 集成测试       | **从前端入口 `/app/api/**` 收口发起\*\*：注册→登录→浏览→加购→下单→秒杀 全链路验证 web BFF 聚合       |

### 实现内容

| 序号 | 任务                | 涉及文件                                           | 备注                                                   |
| ---- | ------------------- | -------------------------------------------------- | ------------------------------------------------------ |
| 14.1 | Feign 异常解码器    | `common/.../exception/FeignErrorDecoder.java`      | 解析下游返回的 ApiResult，提取错误码和消息             |
| 14.2 | 参数校验异常处理    | `common/.../exception/GlobalExceptionHandler.java` | 补充 `MethodArgumentNotValidException` 处理            |
| 14.3 | TraceId 过滤器      | `gateway/.../filter/TraceIdFilter.java`            | 生成 TraceId，写入 MDC + Response Header               |
| 14.4 | Feign TraceId 透传  | `common/.../feign/FeignTraceInterceptor.java`      | `RequestInterceptor` 将 TraceId 透传到下游             |
| 14.5 | web Knife4j 聚合    | `web/.../config/SwaggerConfig.java` + 网关聚合配置 | web 作为前端文档入口，聚合 `/app/api/**`               |
| 14.6 | 各服务 Knife4j 配置 | 所有 service 的 `pom.xml` + 配置类                 | 引入 `knife4j-openapi3`                                |
| 14.7 | 集成测试脚本        | `test/` 目录                                       | **全部经 `/app/api/**` 发起\*\*，10 个核心场景测试用例 |
| 14.8 | 性能压测准备        | `test/jmeter/`                                     | JMeter 脚本：秒杀并发 1000 用户                        |

### 验收标准

- [ ] Feign 调用下游服务异常时，返回原始错误码和消息（而非 500）
- [ ] 参数校验失败返回 400 + 具体字段错误信息
- [ ] 请求响应 Header 中包含 `X-Trace-Id`
- [ ] 日志中 `[TraceId:xxx]` 可追踪完整调用链路
- [ ] 浏览器访问 `http://localhost:8090/doc.html` 聚合 web 前端文档；api 内部文档经 api 路由可查
- [ ] 10 个集成测试用例全部通过，**入口均为 `/app/api/**`（前端唯一入口）\*\*
- [ ] JMeter 秒杀压测：1000 并发无超卖，Redis 库存与 DB 库存一致

### 补充细节

**Feign 异常解码器：**

```java
@Component
public class FeignErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Exception decode(String methodKey, Response response) {
        try {
            // 读取响应体中的 ApiResult
            String body = IOUtils.toString(response.body().asInputStream(), StandardCharsets.UTF_8);
            JsonNode jsonNode = objectMapper.readTree(body);
            int code = jsonNode.get("code").asInt();
            String message = jsonNode.get("message").asText();
            return new BusinessException(code, message);
        } catch (Exception e) {
            return new BusinessException(ErrorCode.INTERNAL_ERROR, "服务调用失败");
        }
    }
}
```

**TraceId 过滤器 + Feign 透传：**

```java
// Gateway 过滤器
@Component
@Order(-2)
public class TraceIdFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = IdUtil.fastSimpleUUID();
        MDC.put("TraceId", traceId);
        exchange.getResponse().getHeaders().add("X-Trace-Id", traceId);
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header("X-Trace-Id", traceId)
                .build();
        return chain.filter(exchange.mutate().request(request).build());
    }
}
```

```java
// Feign 拦截器
@Component
public class FeignTraceInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        String traceId = MDC.get("TraceId");
        if (traceId != null) {
            template.header("X-Trace-Id", traceId);
        }
    }
}
```

**Knife4j 网关聚合配置：**

> web 收口后，**前端文档入口为 web**（聚合 `/app/api/**`）；网关聚合各 api 服务文档仅供内部/排障查看。

```yaml
# web application.yaml
knife4j:
  gateway:
    enabled: true
    strategy: discover
    discover:
      enabled: true
      version: openapi3
```

**集成测试用例清单（3 条链路，入口统一 `/app/api/**` 前端收口）：\*\*

> 以下所有**发起入口**均指 `前端唯一入口 = `/app/api/**`**，内部再经 web → Feign → api 服务。

**链路 1：正常下单完整链路（TC-01 ~ TC-07）**

| 编号  | 场景       | 入口（前端）                     | 步骤                  | 预期结果                       |
| ----- | ---------- | -------------------------------- | --------------------- | ------------------------------ |
| TC-01 | 会员注册   | `POST /app/api/auth/register`    | 提交注册信息          | 返回成功，member 表有记录      |
| TC-02 | 会员登录   | `POST /app/api/auth/login`       | 用注册账号登录        | 返回 JWT 双 Token              |
| TC-03 | 浏览商品   | `GET /app/api/product/list`      | 带 Token 查询商品列表 | 返回分页数据，含品牌名称       |
| TC-04 | 添加购物车 | `POST /app/api/cart`             | 添加 2 件商品         | 购物车列表含 2 项              |
| TC-05 | 下单       | `POST /app/api/order/submit`     | 提交订单              | 返回订单号，购物车清空         |
| TC-06 | 取消订单   | `PUT /app/api/order/{id}/cancel` | 取消待支付订单        | 状态变更为 CANCELLED，库存恢复 |
| TC-07 | 重复下单   | `POST /app/api/order/submit`     | 并发 2 次下单         | 仅创建 1 个订单（分布式锁）    |

**链路 2：秒杀链路（TC-08 ~ TC-10）**

| 编号  | 场景     | 入口（前端）                                  | 步骤            | 预期结果                 |
| ----- | -------- | --------------------------------------------- | --------------- | ------------------------ |
| TC-08 | 秒杀抢购 | `POST /app/api/seckill/order`                 | 抢购秒杀商品    | 返回排队中，最终订单创建 |
| TC-09 | 秒杀防重 | `POST /app/api/seckill/order`                 | 同一用户抢 2 次 | 返回"已抢购"             |
| TC-10 | 秒杀结果 | `GET /app/api/seckill/order/{orderNo}/result` | 轮询查询        | 排队中 → 已抢到/已售罄   |

**链路 3：服务降级链路（TC-11 ~ TC-12）**

| 编号  | 场景         | 入口（前端）                 | 步骤                           | 预期结果                        |
| ----- | ------------ | ---------------------------- | ------------------------------ | ------------------------------- |
| TC-11 | 品牌服务降级 | `GET /app/api/product/list`  | 停止品牌服务，查询商品列表     | 商品列表仍可返回，品牌名显示"-" |
| TC-12 | 超时自动取消 | `POST /app/api/order/submit` | 创建订单后不支付，等待 30 分钟 | 订单自动取消，库存恢复          |

---

---

## 全局风险点与应对策略

| 风险                   | 影响                                                   | 应对措施                                                                                               | 所属阶段 |
| ---------------------- | ------------------------------------------------------ | ------------------------------------------------------------------------------------------------------ | -------- |
| Nacos 服务注册延迟     | 服务启动后，网关可能路由到未就绪的服务                 | 所有服务配置 `readiness-state` 健康检查，服务就绪后 Nacos 才标记为健康（Day 2 已纳入）                 | Day 1-2  |
| Feign 超时与重试       | 跨服务调用超时，可能导致下单失败                       | Feign 全局配置 `connect-timeout: 3000`、`read-timeout: 5000`（Day 1 已纳入）                           | Day 1-2  |
| MQ 消息堆积            | 秒杀场景下 MQ 消息量暴增，消费速度跟不上               | MQ 消费者并发配置 `concurrency: 3`/`max-concurrency: 10`，支持动态扩容消费者实例；消息幂等防止重复处理 | Day 9-11 |
| 数据库连接池耗尽       | 多个服务连接同一个数据库，总连接数可能超限             | Druid `max-active` 降至 10，6 个服务共 60 个连接（Day 1 已纳入）                                       | Day 1-2  |
| Redis 内存不足         | 秒杀库存 + Token 黑名单 + 缓存数据，Redis 内存可能不够 | Redis 配置 `maxmemory 256mb`，`maxmemory-policy allkeys-lru`（Day 1 已纳入）                           | Day 1-2  |
| 网关限流单机限制       | Guava RateLimiter 仅单机生效，多实例部署需升级         | 当前为单机限流，后续可升级为 Redis 令牌桶分布式限流（Day 2 已备注）                                    | Day 2    |
| 下单同步扣库存性能瓶颈 | 商品服务响应慢时下单接口被拖慢                         | 当前为同步扣库存，后续可优化为"Redis 预扣库存 + MQ 异步扣减"（Day 10 已备注）                          | Day 10   |
| 分类树大数据量递归     | 分类数据量大时递归查询性能差                           | 当前已使用"一次查全表 + 内存构建树"方案，后续可按需加 Redis 缓存（Day 6 已纳入）                       | Day 6-7  |

## 版本更新说明

| 版本 | 日期       | 更新内容                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| ---- | ---------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| v1.0 | 2026-08-28 | 初始版本，14 天实施计划                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| v1.1 | 2026-08-28 | 经可用性审查后调整：Nacos 热刷新、Druid wall 配置、数据库连接池降配、Redis 内存策略、Feign 超时、RBAC 仅管理员、Refresh Token key 规范、文件上传本地存储切换、Feign Fallback 降级、MQ 幂等、秒杀并发测试验收、轮询状态、集成测试 3 链路、TraceId 手动透传                                                                                                                                                                                                       |
| v1.2 | 2026-08-28 | **分层架构重构**：新增「⭐ 分层架构规范」（Day 3 起强制）。spi 具象化为**纯 HTTP 契约层**（去 `@FeignClient`，`controller implements`、消费方 `extends` 复用，「契约即实现」）；web 具象化为 **BFF 聚合层**（前端唯一入口 `/app/api/**`，每域 `Web*FeignClient/Web*Service/Web*Controller` 三件套 + 聚合三规则）；网关新增 `/app/api/**` 收口路由，api 路由降级为内部 Feign/排障；Day 3-13 各域补 spi 契约 + web 聚合任务，Day 14 集成测试/Knife4j 收口到 web。 |

## 附录 A：错误码汇总

| 错误码 | 说明               | 所属模块    |
| ------ | ------------------ | ----------- |
| 200    | 成功               | 公共        |
| 400    | 参数错误           | 公共        |
| 401    | 未认证             | 网关        |
| 403    | 无权限             | 网关        |
| 500    | 服务器内部错误     | 公共        |
| 1001   | 用户名或密码错误   | auth-api    |
| 1002   | 账号已存在         | auth-api    |
| 1003   | Token 无效或已过期 | auth-api    |
| 2001   | 商品不存在         | product-api |
| 2002   | 商品库存不足       | product-api |
| 2003   | 商品已下架         | product-api |
| 3001   | 购物车为空         | trade-api   |
| 3002   | 订单状态异常       | trade-api   |
| 3003   | 订单不存在         | trade-api   |
| 4001   | 秒杀活动未开始     | seckill-api |
| 4002   | 秒杀活动已结束     | seckill-api |
| 4003   | 秒杀库存不足       | seckill-api |
| 4004   | 秒杀活动已禁用     | seckill-api |
| 4005   | 已参与该秒杀       | seckill-api |
| 5001   | 品牌不存在         | brand-api   |
| 5002   | 品牌名称重复       | brand-api   |
| 6001   | 会员不存在         | member-api  |
| 6002   | 地址不存在         | member-api  |

## 附录 B：DDL 补充

```sql
-- 品牌表添加逻辑删除字段
ALTER TABLE brand ADD COLUMN is_del TINYINT(1) DEFAULT 0 COMMENT '是否逻辑删除: 0-否, 1-是';

-- 商品表添加逻辑删除字段
ALTER TABLE good ADD COLUMN is_del TINYINT(1) DEFAULT 0 COMMENT '是否逻辑删除: 0-否, 1-是';

-- 商品表添加版本号字段（乐观锁）
ALTER TABLE good ADD COLUMN version INT DEFAULT 0 COMMENT '乐观锁版本号';
```

## 附录 C：开发环境准备清单

| 软件             | 版本  | 用途              |
| ---------------- | ----- | ----------------- |
| JDK              | 25    | Java 运行环境     |
| Maven            | 3.9+  | 项目构建          |
| MySQL            | 8.0+  | 数据库            |
| Nacos            | 2.4+  | 服务注册/配置中心 |
| Redis            | 7.0+  | 缓存/分布式锁     |
| RabbitMQ         | 3.13+ | 消息队列          |
| JMeter           | 5.6+  | 性能压测          |
| Postman / Apifox | 最新  | 接口调试          |

```

```
