# 商品商城管理系统（goods-store-management-system）

B2C 商城微服务系统：面向**会员/用户**的商品交易平台（秒杀、购物车、下单）+ 面向**管理员**的后台管理（品牌、商品、会员、订单、秒杀活动）。

技术形态：Spring Cloud Alibaba 微服务架构 + 双前端（管理端 / 用户端）+ BFF 聚合层。

## 一、技术栈

### 后端

| 分类            | 技术                                                 | 版本                  |
| --------------- | ---------------------------------------------------- | --------------------- |
| 语言 / JDK      | Java                                                 | 25                    |
| 基础框架        | Spring Boot                                          | 4.1.1                 |
| 微服务          | Spring Cloud / Spring Cloud Alibaba                  | 2025.1.3 / 2025.1.0.0 |
| 网关            | Spring Cloud Gateway（Server WebFlux）               | 随 Cloud 管理         |
| 注册 / 配置中心 | Nacos Discovery + Config                             | 随 SCA 管理           |
| 服务调用        | OpenFeign + LoadBalancer（SPI 契约模式）             | 随 Cloud 管理         |
| 限流熔断        | Sentinel                                             | 随 SCA 管理           |
| ORM             | MyBatis-Plus（boot4 专用 starter + jsqlparser 分页） | 3.5.17                |
| 连接池          | Druid（核心包手动注册 DataSource）                   | 1.2.25                |
| 缓存 / 分布式锁 | Redis + Redisson（核心包手动装配）                   | 3.50.0                |
| 消息队列        | RabbitMQ（spring-boot-starter-amqp）                 | 随 Boot 管理          |
| 认证            | JJWT（双令牌 access + refresh）                      | 0.12.6                |
| 工具库          | Hutool（雪花算法、加密等）                           | 5.8.38                |
| 数据库          | MySQL（goods_store 库）                              | 8.0.46                |

### 前端（Vue 3 + TypeScript + Vite + Pinia + Element Plus + Axios）

| 应用                             | 说明                                                                                                |
| -------------------------------- | --------------------------------------------------------------------------------------------------- |
| `goods-store-frontend/admin-web` | 管理端：登录、仪表盘（ECharts）、品牌、商品、会员、订单、秒杀页面                                   |
| `goods-store-frontend/owner-web` | 用户端：注册/登录、首页、商品列表/详情、购物车、结算下单、秒杀（三态按钮 + 倒计时）、订单、个人中心 |

### 可观测性

- SkyWalking 9.7.0（Agent + OAP + UI）：8 个服务零代码侵入接入，OAP 存储为 MySQL（goods_store 库），UI 端口 8091
- 已实测通过：服务拓扑、跨服务 Trace（sw8 传播，单链 4 服务 46 Span）、SQL Span（MyBatis + JDBC + Druid）、错误追踪
- OAP 需 JDK 17 启动（`skywalking/apache-skywalking-apm-bin/bin/oapService.bat`）；接入手册与实测结论见 `doc/schedule/stage-4-plan/skywalking-integration-plan.md`
- TraceId 全链路透传（网关 TraceIdFilter / 服务 TraceIdWebFilter / Feign 拦截器）

## 二、总体架构

```
浏览器（管理端 / 用户端 SPA）
    │
    ▼
Spring Cloud Gateway（8888，统一鉴权 / 路由 / 限流）
    │
    ├── /app/api/** ──────────► goods-store-web（BFF 聚合层，8090）
    │                               │ Feign（SPI 契约）
    │                               ▼
    │              ┌────────────────┴────────────────┐
    │              │ auth / brand / product / member  │
    │              │ trade / seckill                  │
    │              └──────────────────────────────────┘
    └── /auth|brand|good|member|trade|seckill/api/** ──► 各业务服务直连
                        │
        ┌───────────────┼───────────────┐
        ▼               ▼               ▼
      MySQL           Redis          RabbitMQ
    （数据存储）  （缓存/锁/秒杀预扣） （下单/取消/秒杀异步）
```

服务间调用：product → brand；trade → member、product；seckill → product、member（均经 Feign，部分含降级 FallbackFactory）。

## 三、模块清单与端口

| 模块                    | 端口 | 说明                                        |
| ----------------------- | ---- | ------------------------------------------- |
| goods-store-gateway     | 8888 | 统一入口、JWT 鉴权、路由转发、Sentinel 限流 |
| goods-store-web         | 8090 | BFF 聚合层，供前端 `/app/api/**` 调用       |
| goods-store-auth-api    | 8083 | 认证 / RBAC 权限（双令牌 JWT）              |
| goods-store-brand-api   | 8081 | 品牌 CRUD、上下架、图片上传                 |
| goods-store-product-api | 8084 | 商品 / 分类树 / SKU / 规格组                |
| goods-store-member-api  | 8085 | 会员、收货地址、信息脱敏                    |
| goods-store-trade-api   | 8086 | 购物车、订单（MQ 异步 + Redisson 防重）     |
| goods-store-seckill-api | 8087 | 秒杀（Redis 预扣库存 + Lua + MQ 异步建单）  |
| goods-store-spi         | —    | Feign 契约层（6 个 \*-spi：接口 + DTO/VO）  |
| goods-store-common      | —    | 统一返回 ApiResult、异常、MQ 消息模型、工具 |

中间件：Nacos 8848 / MySQL 3306 / Redis / RabbitMQ / Sentinel Dashboard 8080 / SkyWalking OAP 11800（gRPC）、12800（HTTP）。

## 四、当前进展（截至 2026-09-05）

| 项                                             | 状态                                     |
| ---------------------------------------------- | ---------------------------------------- |
| 后端 6 大业务服务 + 网关 + BFF 编译运行        | 全部通过                                 |
| 中间件连接（Nacos / MySQL / Redis / RabbitMQ） | 正常                                     |
| Nacos 服务注册与发现                           | 全部服务可发现                           |
| 管理端前端（admin-web）                        | 已实现并打通数据请求                     |
| 用户端前端（owner-web）                        | 已实现（含秒杀、购物车、下单全流程）     |
| 用户端与秒杀联调缺陷（2026-09-05 排查）        | 已修复（见 doc/summary.md 第七节）       |
| 可观测性（SkyWalking）                         | Agent + OAP + UI 就绪，OAP 用 MySQL 存储 |

## 五、快速开始

1. **准备中间件**：启动 MySQL（导入 `sql/` 下脚本）、Nacos、Redis、RabbitMQ；可选 Sentinel Dashboard、SkyWalking（OAP 需 JDK 17 启动，业务服务以 `-javaagent` 三参数挂载，命令见 `doc/schedule/stage-4-plan/skywalking-integration-plan.md` 第九节）。
2. **配置中心**：Nacos 中维护 `goods-store-common.yaml`、`goods-store-shared-service-config.yaml` 及各服务专属配置（数据库、Redis、MQ、Sentinel 等）。
3. **启动后端**：按需启动各服务启动类（gateway → 各业务 api → web），或通过 Maven 构建。
4. **启动前端**：

   ```bash
   cd goods-store-frontend/admin-web   # 管理端
   npm install && npm run dev

   cd goods-store-frontend/owner-web   # 用户端
   npm install && npm run dev
   ```

5. **访问**：前端经由网关（8888）访问 `/app/api/**` 走 BFF，或直连各服务路由。

## 六、文档索引

| 文档            | 内容                                     |
| --------------- | ---------------------------------------- |
| doc/架构.md     | 总体架构、网关路由规则、关键设计         |
| doc/技术栈.md   | 技术选型与版本明细、目录结构             |
| doc/业务状况.md | 业务域概览、调用关系、当前进展           |
| doc/summary.md  | 历史问题排查与解决方案（含关键经验总结） |
| doc/log.md      | 开发日志                                 |
| doc/schedule/   | 分阶段执行计划与手册                     |
