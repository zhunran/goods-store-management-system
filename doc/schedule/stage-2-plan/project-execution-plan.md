# 并发商城系统 - 项目执行计划书

> 版本：v1.0
> 更新日期：2026-08-28
> 基于需求规格书 v1.0 + 当前项目骨架现状

---

## 一、现状总结

### 1.1 已完成

| 项目             | 状态     | 说明                                                          |
| ---------------- | -------- | ------------------------------------------------------------- |
| 项目骨架         | 已完成   | 父 POM + 7 个子模块 + SPI + Common + Gateway + Web            |
| Nacos 注册发现   | 已完成   | 所有服务已配置 `@EnableDiscoveryClient`，已注册到 Nacos       |
| Nacos 配置中心   | 已完成   | 所有服务已配置 `spring.cloud.nacos.config`，读取共享配置       |
| 实体类           | 已完成   | 所有 30 张表的 Entity 已生成（MyBatis-Plus 注解）             |
| SPI Feign 契约   | 已完成   | 5 个 SPI 模块（brand/product/member/trade/seckill）已定义接口 |
| 网关骨架         | 已完成   | Spring Cloud Gateway 已配置 brand-api 路由                    |
| 公共模块         | 已完成   | ApiResult / BusinessException / GlobalExceptionHandler / ErrorCode |
| 品牌服务基础 CRUD | 部分完成 | Controller + Service + Mapper 已搭建，仅分页查询已实现        |

### 1.2 待完成

| 项目                 | 说明                                          |
| -------------------- | --------------------------------------------- |
| auth-api 服务        | 模块目录已创建但未注册到父 POM，无业务代码    |
| 数据库连接配置       | 所有服务的 YAML 中缺少数据源配置              |
| Redis 集成           | 未引入依赖，Token 黑名单/秒杀库存需 Redis     |
| RabbitMQ 集成        | 未引入依赖，秒杀削峰需 MQ                     |
| JWT 鉴权             | 网关未集成 JWT 校验                           |
| 各服务业务逻辑       | 除品牌分页查询外，其余均为空实现或未实现       |
| 网关路由/限流        | 仅配置了 brand-api 一条路由                   |
| 全局异常处理        | 已定义但未覆盖 Feign 调用异常/参数校验异常    |

---

## 二、技术栈补充与确认

### 2.1 已确定技术栈

| 技术            | 版本                 | 用途                       |
| --------------- | -------------------- | -------------------------- |
| Java            | 25                   | 运行环境                   |
| Spring Boot     | 4.1.1                | 基础框架                   |
| Spring Cloud    | 2025.1.3             | 微服务治理                 |
| Spring Cloud Alibaba | 2025.1.0.0       | Nacos 服务发现 + 配置中心  |
| MyBatis-Plus    | 3.5.17               | ORM                        |
| MySQL           | 8.x                  | 关系型数据库               |
| Lombok          | -                    | 模板代码消除               |

### 2.2 新增/补充技术栈

| 技术               | 版本建议                          | 用途                             | 引入位置                         |
| ------------------ | --------------------------------- | -------------------------------- | -------------------------------- |
| **RabbitMQ**       | 3.13.x（服务端）                  | 秒杀削峰、订单异步处理、死信队列 | seckill-api, trade-api           |
| **Spring AMQP**    | 与 Spring Boot 4.1.1 自动匹配     | RabbitMQ 客户端                  | seckill-api, trade-api           |
| **Redis**          | 7.x（服务端）                     | Token 黑名单、秒杀库存预热、缓存 | auth-api, seckill-api, gateway   |
| **Spring Data Redis** | 与 Spring Boot 4.1.1 自动匹配 | Redis 客户端（Lettuce）          | 按需引入                         |
| **Redisson**       | 3.40.x                            | 分布式锁（秒杀防超卖兜底）       | seckill-api                      |
| **jjwt**           | 0.12.x                            | JWT Token 生成与解析             | gateway, auth-api                |
| **Spring Security** | 与 Spring Boot 4.1.1 自动匹配    | 认证鉴权框架（仅 auth-api 使用） | auth-api                         |
| **Hutool**         | 5.8.x                             | 通用工具（雪花算法、JSON 等）    | common                           |
| **Knife4j**        | 4.x                               | API 文档自动生成                 | gateway（聚合文档）              |
| **Micrometer + Prometheus** | -                         | 监控指标采集                     | 所有服务                         |
| **Druid**          | 1.2.x                             | 数据库连接池（监控 SQL 性能）    | 所有服务                         |

### 2.3 未引入技术（暂缓）

| 技术            | 原因                                      |
| --------------- | ----------------------------------------- |
| Sentinel        | 限流由网关层 + Guava RateLimiter 先实现    |
| Seata           | 分布式事务先通过 MQ 最终一致性解决        |
| Elasticsearch   | 商品搜索先用 MySQL LIKE，后续按需引入     |
| OSS（阿里云）   | 图片存储先用本地文件系统，后续按需引入    |

---

## 三、架构设计细节补充

### 3.1 数据库连接配置（Nacos 共享配置）

在 Nacos 中创建 `goods-store-common.yaml` 共享配置，所有服务通过 `spring.config.import` 引用：

```yaml
# Nacos: goods-store-common.yaml (namespace=public, group=DEFAULT_GROUP)
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/shoplook2026_0324?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
    username: root
    password: root
    type: com.alibaba.druid.pool.DruidDataSource
    druid:
      initial-size: 5
      min-idle: 5
      max-active: 20
      max-wait: 60000
  data:
    redis:
      host: localhost
      port: 6379
      password:
      database: 0
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
    listener:
      simple:
        acknowledge-mode: manual    # 手动确认
        prefetch: 1                 # 每次取1条，公平分发
        retry:
          enabled: true
          max-attempts: 3
          initial-interval: 2000ms

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
```

### 3.2 服务端口分配（修正）

> 当前已分配端口与需求规格书不一致，以已实现代码为准。

| 服务                    | 端口 | 说明          |
| ----------------------- | ---- | ------------- |
| goods-store-gateway     | 8080 | 网关          |
| goods-store-brand-api   | 8081 | 品牌服务      |
| goods-store-auth-api    | 8082 | 认证服务（调整） |
| goods-store-product-api | 8084 | 商品服务      |
| goods-store-member-api  | 8085 | 会员服务      |
| goods-store-trade-api   | 8086 | 交易服务      |
| goods-store-seckill-api | 8087 | 秒杀服务      |

### 3.3 RabbitMQ 消息队列设计

#### 3.3.1 交换机与队列定义

```
Exchange: goods.order.exchange (topic)
├── Queue: goods.order.create.queue
│   Routing Key: order.create
│   └── 用途: 下单后异步扣减库存、生成订单快照
│
├── Queue: goods.seckill.order.queue
│   Routing Key: seckill.order.create
│   └── 用途: 秒杀下单削峰，异步创建秒杀订单
│
├── Queue: goods.order.timeout.queue
│   Routing Key: order.timeout
│   └── 用途: 延迟队列（通过死信实现），30分钟未支付自动取消订单
│   └── TTL: 1800000ms (30分钟)
│   └── DLX: goods.order.exchange
│   └── DLK: order.cancel
│
└── Queue: goods.order.cancel.queue
    Routing Key: order.cancel
    └── 用途: 处理订单取消，恢复库存
```

#### 3.3.2 消息结构

```java
// 订单创建消息
public class OrderCreateMessage {
    private String orderNo;          // 订单号
    private Long memberId;           // 会员ID
    private List<OrderItemMsg> items; // 订单项
    private LocalDateTime createTime; // 创建时间
}

// 秒杀订单消息
public class SeckillOrderMessage {
    private Long seckillId;          // 秒杀活动ID
    private Long goodId;             // 商品ID
    private Long memberId;           // 会员ID
    private String orderNo;          // 订单号
    private BigDecimal price;        // 秒杀价格
}
```

### 3.4 Redis 缓存设计

| Key 格式                          | 类型   | 用途                       | TTL          |
| --------------------------------- | ------ | -------------------------- | ------------ |
| `token:access:{userId}`           | String | Access Token 黑名单        | 30 min       |
| `token:refresh:{userId}`          | String | Refresh Token 存储         | 7 天         |
| `seckill:stock:{seckillGoodId}`   | String | 秒杀库存预热（原子递减）   | 活动结束     |
| `seckill:order:{userId}:{goodId}` | String | 防重复秒杀下单             | 活动结束     |
| `lock:order:{orderNo}`            | String | Redisson 分布式锁（下单）  | 30s 自动释放 |

### 3.5 JWT 鉴权流程

```
[客户端] → [Gateway] → [微服务]
              │
              ├─ 1. 解析 Authorization Header 获取 JWT
              ├─ 2. 验证签名（jjwt）
              ├─ 3. 检查 Redis 黑名单（是否已登出）
              ├─ 4. 解析 userId + roles 写入 Header
              └─ 5. 转发到下游微服务
```

**白名单路径（无需 Token）：**
- `POST /auth/api/login`
- `POST /auth/api/register`
- `GET /product/api/**`（商品浏览）
- `GET /brand/api/**`（品牌浏览）

### 3.6 秒杀完整流程（四层防护实现细节）

```
1. [预热阶段] 秒杀开始前，将 seckill_good 库存加载到 Redis
   └─ Key: seckill:stock:{seckillGoodId}

2. [请求到达] 网关层 IP + 接口限流（Guava RateLimiter 或 Redis 令牌桶）
   └─ 每个 IP 每秒最多 10 次秒杀请求

3. [Redis 预减] 服务层执行 Lua 脚本原子操作：
   └─ DECR seckill:stock:{seckillGoodId}
   └─ 若结果 < 0，返回"已售罄"
   └─ 若结果 >= 0，SETNX seckill:order:{userId}:{goodId} 防重复

4. [MQ 削峰] 发送消息到 goods.seckill.order.queue
   └─ 异步消费：校验库存（DB乐观锁）→ 创建订单 → 扣减DB库存

5. [兜底] DB 乐观锁：UPDATE seckill_good SET stock = stock - 1
   └─ WHERE id = ? AND stock > 0
```

---

## 四、各模块实现细节补充

### 4.1 认证服务（goods-store-auth-api）

**新增依赖：** `spring-boot-starter-security`, `jjwt-api`, `jjwt-impl`, `jjwt-jackson`, `spring-boot-starter-data-redis`

**实现要点：**

| 要点               | 方案                                                         |
| ------------------ | ------------------------------------------------------------ |
| 密码加密           | BCryptPasswordEncoder（Spring Security 提供）                |
| JWT 生成           | Access Token（30min）+ Refresh Token（7天），jjwt 0.12.x     |
| Token 存储         | Access Token 黑名单存 Redis String（登出时写入），Refresh Token 存 Redis Hash |
| 会员注册           | 手机号/邮箱唯一性校验，BCrypt 加密后写入 member 表           |
| 管理员登录         | 后台管理使用 admin_user 表 + 新版 RBAC 表（role/permission/admin_user_role/role_permission） |
| 会员登录           | 使用 member 表，返回 JWT                                     |
| 权限模型           | 后台：admin_user → role → permission（新版RBAC 5表）         |
| Security 配置      | 仅 auth-api 引入 Security，其他服务无状态，网关统一鉴权     |

**表选择决策：**
- 前端会员：使用 `member` 表（已有数据）
- 后台管理员：使用新版 `admin_user` + `role` + `permission` + `admin_user_role` + `role_permission`（5 张表）
- 旧版 RBAC 表（`user`/`t_rbac_*`）不启用，保留兼容

### 4.2 品牌服务（goods-store-brand-api）

**当前状态：** Controller + Service + Mapper 已搭建，分页查询已实现。

**待补充：**

| 功能           | 实现细节                                          |
| -------------- | ------------------------------------------------- |
| 品牌详情       | `GET /brand/api/{id}`，按 ID 查询                 |
| 品牌新增       | `POST /brand/api`，校验 name 唯一性               |
| 品牌编辑       | `PUT /brand/api/{id}`，更新非空字段               |
| 品牌删除       | `DELETE /brand/api/{id}`，逻辑删除（MyBatis-Plus @TableLogic） |
| 品牌 Logo 上传 | `POST /brand/api/upload`，存储到本地 /static/upload/brand/ |
| 参数校验       | `@Valid` + `jakarta.validation` 注解              |

### 4.3 商品服务（goods-store-product-api）

**当前状态：** Entity 已完成，Controller 为 stub，Service 接口已定义。

**待补充：**

| 功能           | 实现细节                                                      |
| -------------- | ------------------------------------------------------------- |
| 分类树         | 递归查询 `category` 表（parent_id 自关联），返回树形 JSON     |
| 商品列表       | 分页查询，支持按分类/品牌/关键词/上下架/热销状态筛选           |
| 商品详情       | 查询 `good` + `good_detail_pics`（一对多），组装品牌名称（Feign 调用 brand-api） |
| 商品新增/编辑  | 操作 `good` 表，支持同时维护 `good_detail_pics`              |
| 上下架         | 更新 `good.is_take_down` 字段                                 |
| 库存扣减       | 下单时通过 Feign 被 trade-api 调用，使用乐观锁扣减库存         |
| 库存恢复       | 取消订单时通过 Feign 被 trade-api 调用，恢复库存               |
| 详情图管理     | 多图上传 + 排序，操作 `good_detail_pics` 表                    |

**跨服务调用：**
- `product-api → brand-api`：组装品牌名称
- `product-api → seckill-api`：查询商品是否参与秒杀

### 4.4 会员服务（goods-store-member-api）

**当前状态：** Entity 已完成，Application 启动类已配置。

**待补充：**

| 功能           | 实现细节                                                  |
| -------------- | --------------------------------------------------------- |
| 会员信息查询   | `GET /member/api/{id}`，仅返回脱敏后信息（手机号中间4位*） |
| 会员信息编辑   | `PUT /member/api/{id}`，校验当前用户只能修改自己的信息    |
| 收货地址 CRUD  | 增删改查 `member_address` 表，默认地址互斥逻辑            |
| 会员列表       | 管理端分页查询，支持按账号/手机号/姓名筛选                |
| 启用/禁用      | 更新 `member.enabled` 字段                                |

### 4.5 交易服务（goods-store-trade-api）

**新增依赖：** `spring-boot-starter-amqp`, `spring-boot-starter-data-redis`, `redisson-spring-boot-starter`

**待补充：**

| 功能           | 实现细节                                                      |
| -------------- | ------------------------------------------------------------- |
| 购物车列表     | 查询 `cart` 表（member_id），关联商品信息（Feign 调用 product-api） |
| 添加购物车     | `INSERT ... ON DUPLICATE KEY UPDATE qty = qty + ?`（利用唯一索引 uq_member_good） |
| 修改数量       | 更新 `cart.qty`，校验不能超过库存                             |
| 删除购物车     | 删除 `cart` 记录，支持批量删除                                |
| 下单           | 1. 查询购物车 + 校验库存（Feign 调用 product-api）<br>2. Redisson 分布式锁（orderNo）<br>3. 生成订单号（雪花算法）<br>4. 创建订单 + 订单明细<br>5. 发送 MQ 消息扣减库存<br>6. 清空购物车<br>7. 发送延迟消息（30分钟未支付取消） |
| 订单列表       | 分页查询 `order` 表，按 member_account 过滤                   |
| 订单详情       | 查询 `order` + `order_item`                                   |
| 取消订单       | 1. 校验订单状态（仅 PENDING）<br>2. 更新状态为 CANCELLED<br>3. 发送 MQ 消息恢复库存 |
| 确认收货       | 更新订单状态 SHIPPED → COMPLETED，记录 accept_time            |
| 订单超时取消   | 消费死信队列 `goods.order.cancel.queue`，检查订单是否仍为 PENDING |

**订单状态枚举：**
```java
public enum OrderStatus {
    PENDING(0, "待支付"),
    PAID(1, "已支付"),
    SHIPPED(2, "已发货"),
    COMPLETED(3, "已完成"),
    CANCELLED(4, "已取消");
}
```

### 4.6 秒杀服务（goods-store-seckill-api）

**新增依赖：** `spring-boot-starter-amqp`, `spring-boot-starter-data-redis`, `redisson-spring-boot-starter`

**待补充：**

| 功能             | 实现细节                                                      |
| ---------------- | ------------------------------------------------------------- |
| 秒杀商品列表     | 查询 `seckill` + `seckill_good`，过滤当前时间在 start_time~end_time 之间且 enabled=1 |
| 秒杀商品详情     | 关联 `good` 表获取商品信息（Feign 调用 product-api）           |
| 秒杀抢购         | 见下方秒杀流程详细设计                                        |
| 秒杀订单查询     | 查询 `order` 表（seckill_no 不为空），按 member_account 过滤  |
| 秒杀活动管理     | 管理端 CRUD `seckill` + `seckill_good` 表                     |
| 库存预热         | 定时任务（@Scheduled）在秒杀开始前 5 分钟将库存预热到 Redis    |

**秒杀抢购详细流程：**

```
POST /seckill/api/{seckillGoodId}/order

1. 校验秒杀活动状态（enabled + 时间窗口）
2. 校验用户是否已抢购（Redis SETNX seckill:order:{userId}:{goodId}）
3. 执行 Lua 脚本预减 Redis 库存
   └─ 库存不足 → 返回"已售罄"
4. 发送 SeckillOrderMessage 到 MQ
5. 返回"排队中"（异步处理结果）
6. MQ Consumer 异步处理：
   ├─ DB 乐观锁扣减库存
   ├─ 生成订单（order 表，seckill_no 字段标识秒杀单）
   └─ 失败 → 补偿 Redis 库存 + 删除防重标记
```

**Lua 脚本（Redis 预减库存）：**
```lua
local stockKey = KEYS[1]
local orderKey = KEYS[2]
local stock = redis.call('GET', stockKey)
if not stock or tonumber(stock) <= 0 then
    return -1
end
local exists = redis.call('EXISTS', orderKey)
if exists == 1 then
    return -2
end
redis.call('DECR', stockKey)
redis.call('SETEX', orderKey, 3600, '1')
return tonumber(stock) - 1
```

### 4.7 网关（goods-store-gateway）

**新增依赖：** `jjwt-api`, `jjwt-impl`, `jjwt-jackson`, `spring-boot-starter-data-redis-reactive`

**待补充：**

| 功能             | 实现细节                                                      |
| ---------------- | ------------------------------------------------------------- |
| 全局 JWT 过滤器  | `GlobalFilter` 解析 Authorization Header，验证签名，检查黑名单 |
| Token 刷新       | 网关透传 `/auth/api/refresh` 到 auth-api                       |
| 路由规则         | 补充所有 6 个服务的路由配置                                    |
| 限流             | `RequestRateLimiter` + Redis 实现令牌桶限流，按 IP+接口维度    |
| 跨域             | `CorsConfiguration` 全局配置                                   |
| 白名单           | 登录/注册/商品浏览/品牌浏览 不校验 Token                       |
| 请求日志         | 记录请求耗时、来源 IP、目标服务                                |

**路由配置：**

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
```

---

## 五、执行阶段与任务拆分

### 阶段一：基础设施完善（P0，预计 1-2 天）

| 序号 | 任务                                 | 涉及模块                          | 说明                                             |
| ---- | ------------------------------------ | --------------------------------- | ------------------------------------------------ |
| 1.1  | Nacos 共享配置创建                   | 所有服务                          | 创建 `goods-store-common.yaml`，含数据源/Redis/MQ |
| 1.2  | 父 POM 补充依赖管理                  | 父 POM                            | 添加 Redis/RabbitMQ/JWT/Redisson/Hutool 版本管理  |
| 1.3  | Common 模块补充                      | goods-store-common                | 添加 Hutool 雪花算法工具类、MQ 消息体 DTO         |
| 1.4  | 各服务引入 Druid 数据源              | 所有服务                          | 添加 Druid 依赖，替换默认 HikariCP                |
| 1.5  | 网关路由完善 + JWT 过滤器            | goods-store-gateway               | 6 条路由 + JWT 校验 + 白名单 + 限流              |
| 1.6  | auth-api 模块注册到父 POM           | goods-store-service/pom.xml       | 将 auth-api 加入 modules 列表                    |

### 阶段二：认证服务（P0，预计 2-3 天）

| 序号 | 任务                                 | 涉及模块                          | 说明                                             |
| ---- | ------------------------------------ | --------------------------------- | ------------------------------------------------ |
| 2.1  | auth-api 项目搭建                    | auth-api                          | pom.xml 依赖 + application.yaml + 启动类         |
| 2.2  | 管理员 RBAC Entity                   | auth-api                          | admin_user/role/permission/admin_user_role/role_permission |
| 2.3  | 登录/注册接口                        | auth-api                          | 会员登录 + 管理员登录 + 注册                     |
| 2.4  | JWT 双 Token 签发                    | auth-api                          | Access Token + Refresh Token + Redis 存储         |
| 2.5  | Token 刷新/登出                      | auth-api                          | Refresh 换 Access + 黑名单                       |
| 2.6  | 密码修改/重置                        | auth-api                          | 旧密码验证 + BCrypt 加密                         |

### 阶段三：品牌服务 + 商品服务（P0，预计 3-4 天）

| 序号 | 任务                                 | 涉及模块                          | 说明                                             |
| ---- | ------------------------------------ | --------------------------------- | ------------------------------------------------ |
| 3.1  | 品牌 CRUD 完善                       | brand-api                         | 详情/新增/编辑/删除/Logo 上传                    |
| 3.2  | 商品分类树接口                       | product-api                       | 递归查询 category 表                             |
| 3.3  | 商品列表/详情/搜索                   | product-api                       | 分页 + 多条件筛选 + 品牌名称组装（Feign）        |
| 3.4  | 商品 CRUD + 上下架                   | product-api                       | 新增/编辑/删除/上下架/详情图管理                 |
| 3.5  | 商品 Feign 接口（库存扣减/恢复）     | product-api + product-spi         | 供 trade-api 调用的库存操作接口                  |

### 阶段四：会员服务（P0，预计 2-3 天）

| 序号 | 任务                                 | 涉及模块                          | 说明                                             |
| ---- | ------------------------------------ | --------------------------------- | ------------------------------------------------ |
| 4.1  | 会员信息查询/编辑                    | member-api                        | 脱敏返回 + 自助修改                              |
| 4.2  | 收货地址 CRUD                        | member-api                        | 默认地址互斥逻辑                                 |
| 4.3  | 会员 Feign 接口                      | member-api + member-spi           | 供 trade-api 获取默认地址                        |

### 阶段五：交易服务（P0，预计 3-4 天）

| 序号 | 任务                                 | 涉及模块                          | 说明                                             |
| ---- | ------------------------------------ | --------------------------------- | ------------------------------------------------ |
| 5.1  | 购物车 CRUD                          | trade-api                         | 增删改查 + 库存校验（Feign 调用 product-api）     |
| 5.2  | 下单接口                             | trade-api                         | 分布式锁 + 订单生成 + MQ 扣库存 + 清空购物车     |
| 5.3  | 订单列表/详情/取消/确认收货          | trade-api                         | 状态机流转 + 恢复库存                            |
| 5.4  | MQ 消费者（库存扣减/恢复）           | trade-api                         | RabbitMQ Listener + 手动确认                     |
| 5.5  | 延迟队列（订单超时取消）             | trade-api                         | TTL + DLX + 死信队列                             |

### 阶段六：秒杀服务（P0，预计 3-4 天）

| 序号 | 任务                                 | 涉及模块                          | 说明                                             |
| ---- | ------------------------------------ | --------------------------------- | ------------------------------------------------ |
| 6.1  | 秒杀活动/商品管理                    | seckill-api                       | CRUD + 时间窗口校验                              |
| 6.2  | 库存预热定时任务                     | seckill-api                       | @Scheduled 预热到 Redis                          |
| 6.3  | 秒杀抢购接口                         | seckill-api                       | Lua 脚本预减库存 + MQ 削峰 + 防重复下单          |
| 6.4  | MQ 消费者（秒杀订单创建）            | seckill-api                       | DB 乐观锁 + 订单生成 + 补偿机制                  |
| 6.5  | 秒杀订单查询                         | seckill-api                       | 我的秒杀订单列表                                 |

### 阶段七：集成与优化（P1，预计 2-3 天）

| 序号 | 任务                                 | 涉及模块                          | 说明                                             |
| ---- | ------------------------------------ | --------------------------------- | ------------------------------------------------ |
| 7.1  | 全局异常处理增强                     | common                            | Feign 异常解码 + 参数校验异常                    |
| 7.2  | Knife4j 文档聚合                     | gateway                           | 网关聚合各服务 API 文档                          |
| 7.3  | 日志链路追踪                         | 所有服务                          | MDC + TraceId 透传                               |
| 7.4  | 集成测试                             | 所有服务                          | 核心业务流程端到端测试                           |
| 7.5  | 性能压测（秒杀）                     | seckill-api                       | JMeter 并发测试秒杀接口                          |

---

## 六、模块依赖关系图

```
goods-store-gateway  ──→ 路由到所有服务
goods-store-auth-api  ──→ Redis (Token)
goods-store-brand-api  ──→ MySQL (brand)
goods-store-product-api ──→ MySQL (category/good/good_detail_pics) + Feign→brand-api
goods-store-member-api  ──→ MySQL (member/member_address)
goods-store-trade-api   ──→ MySQL (cart/order/order_item) + Redis (分布式锁)
                              + RabbitMQ (库存扣减/延迟取消) + Feign→product-api/member-api
goods-store-seckill-api ──→ MySQL (seckill/seckill_good) + Redis (库存预热/防重)
                              + RabbitMQ (削峰) + Feign→product-api
```

---

## 七、风险与注意事项

| 风险                  | 影响     | 缓解措施                                          |
| --------------------- | -------- | ------------------------------------------------- |
| 秒杀超卖              | 严重     | 四层防护：Redis 预减 + MQ 削峰 + DB 乐观锁 + 限流 |
| 订单重复创建          | 中等     | Redisson 分布式锁（orderNo）+ 唯一索引            |
| MQ 消息丢失           | 中等     | 手动 ACK + 消息持久化 + 死信队列                  |
| 分布式事务            | 中等     | 最终一致性 + 补偿机制（暂不引入 Seata）           |
| 数据库连接池耗尽      | 中等     | Druid 监控 + 合理配置连接池                       |
| 服务间调用超时        | 低       | Feign 超时配置 + 熔断降级（后续引入 Sentinel）    |
| 表结构与 Entity 不一致 | 低      | entity 使用 `@TableField` 映射下划线命名          |

---

## 八、关键约定

1. **代码规范：** Controller 层路径统一为 `/服务名/api/**`，如 `/brand/api/page`
2. **统一返回：** 所有接口返回 `ApiResult<T>`，code=200 表示成功
3. **异常处理：** 业务异常抛 `BusinessException`，由 `GlobalExceptionHandler` 统一处理
4. **分页查询：** 使用 MyBatis-Plus `IPage`，请求参数继承 `PageQuery`
5. **雪花算法：** 订单号/秒杀编号使用 Hutool `IdUtil.getSnowflake()` 生成
6. **时间格式：** 统一使用 `LocalDateTime`，序列化格式 `yyyy-MM-dd HH:mm:ss`
7. **日志级别：** 生产环境 INFO，开发环境 DEBUG