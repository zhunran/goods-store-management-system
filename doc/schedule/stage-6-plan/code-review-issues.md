# Stage 6 —— 代码审阅问题清单

> 审阅范围：公共层、网关、SPI 契约、6 个业务服务（auth/brand/member/product/trade/seckill）、BFF（web）、双前端（admin-web / owner-web）。
> 审阅时间：2026-09-07。
> 说明：下列问题均已逐条核对到具体代码位置，按严重级别分组，标注了文件相对路径与行号。

---

## 一、高危问题（安全 / 正确性 / 竞态）

### 1. 网关启动类 `main` 缺少 `public`
- 位置：`goods-store-gateway/src/main/java/com/fengluan/GatewayApplication.java:11-13`
- 现状：`static void main(String[] args)` 未声明 `public`。
- 影响：`java -jar` 与 JVM 启动器无法识别入口方法，网关无法启动。
- 修复：改为 `public static void main(String[] args)`。

### 2. 网关白名单整体放行品牌/商品写接口
- 位置：`goods-store-gateway/src/main/java/com/fengluan/gateway/config/WhiteListConfig.java:24-26`
- 现状：`/brand/api/**`、`/good/api/**`、`/actuator/**` 全路径免鉴权。
- 影响：任何人无需登录即可增删改商品/品牌、上下架、直接增减库存；actuator 泄露内部指标/配置。
- 修复：白名单收敛为「GET 查询 + 登录注册 + 静态资源」，移除 actuator 或加保护。

### 3. RBAC 角色/权限接口裸奔
- 位置：`goods-store-service/goods-store-auth-api/src/main/java/com/fengluan/auth/config/SecurityConfig.java:34-43`
- 现状：`/auth/api/role/**`、`/auth/api/permission/**` 被 `permitAll()`；auth-api 内未注册任何 JWT 解析过滤器，且无 `@EnableMethodSecurity`。
- 影响：任意人可查看/分配角色权限，权限体系可被改写。
- 修复：仅放行登录注册刷新；接入 JWT 过滤器与方法级权限拦截。

### 4. JWT 密钥明文硬编码（多处一致）
- 位置：
  - `goods-store-gateway/src/main/resources/application.yaml:5-6`
  - `goods-store-service/goods-store-auth-api/src/main/java/com/fengluan/auth/util/JwtUtil.java:27`
  - `goods-store-service/goods-store-auth-api/src/main/resources/application.yaml`
- 现状：密钥 `change-me-...` 明文写死且为公开默认值。
- 影响：任何拿到仓库的人都能伪造含 admin 的 token。
- 修复：密钥从环境变量/Nacos 注入，禁止提交明文。

### 5. `ApiResult.success()` 无参版返回失败语义
- 位置：`goods-store-common/src/main/java/com/fengluan/common/result/ApiResult.java:17-19`
- 现状：`success()` 无参返回 `new ApiResult<>()`，即 `code=null、success=false`。
- 影响：所有 Void 成功接口语义错误；admin-web 的删除/上下架/发货/退款等接口被误判失败（见下述中危 #7）。
- 修复：无参版改为 `success(null)` 或显式 `new ApiResult<>(200, true, null, null)`。

### 6. `/app/api/product/*` 误命中「新增商品」接口
- 位置：`goods-store-gateway/src/main/java/com/fengluan/gateway/config/WhiteListConfig.java:40`
- 现状：`*` 仅匹配单层段，会命中 `goods-store-web/.../WebProductAdminController.java:34-37` 的 `POST /app/api/product/admin`，导致该路径被白名单放行、无 `X-User-Roles` 注入。
- 影响：管理端「新增商品」被 `AdminRoleInterceptor` 恒 403，功能被打断。
- 修复：白名单精确到具体路径（如 `/app/api/product/list`、`/app/api/product/tree`），勿用 `*` 匹配 `admin`。

### 7. 库存一致性多处断裂（超卖主因）
- 位置：
  - `goods-store-service/goods-store-product-api/src/main/java/com/fengluan/product/service/impl/GoodServiceImpl.java:162-170`（扣/还库存不失效缓存）
  - `goods-store-service/goods-store-trade-api`（扣库存走异步 MQ，生产者无 publisher-confirm）
  - `goods-store-service/goods-store-seckill-api/src/main/java/com/fengluan/seckill/mq/SeckillOrderConsumer.java:48-76`（秒杀只扣 Redis，不扣 DB）
  - `goods-store-service/goods-store-seckill-api/src/main/java/com/fengluan/seckill/mq/SeckillRedisCompensator.java:20-24`（`increment` 对缺失键凭空 +1）
- 影响：
  - 商品详情读缓存、逻辑过期仍返回旧 qty，扣/还 DB 后旧库存仍被读到 → 超卖。
  - 下单扣库存消息可能丢失或部分失败仍 ack → 不漏扣/不重扣无法保证。
  - 秒杀与普通下单双卖同一份物理库存，最终不一致。
  - 补偿对不存在的库存键 `INCR` 会凭空制造库存。
- 修复：扣/还库存后同步失效缓存；MQ 加 confirm/return 与可靠 ack；秒杀扣 DB 库存或统一库存账本；补偿改为「键存在才回补」+ 幂等（Lua 原子）。

### 8. 秒杀建单无事务 → 孤儿订单
- 位置：`goods-store-service/goods-store-seckill-api/src/main/java/com/fengluan/seckill/mq/SeckillOrderConsumer.java:67-76`
- 现状：`order` 与 `order_item` 分开 insert，无 `@Transactional`；catch 对所有异常统一 `compensate + ack`。
- 影响：明细插入失败时残留无明细主单；建单成功后续步骤异常也会回补库存+清防重，造成「有单又可重买」。
- 修复：主表+明细同事务，区分业务失败（补偿）与临时故障（重试）。

### 9. 下单分布式锁未覆盖「读购物车」→ 并发重复下单
- 位置：`goods-store-service/goods-store-trade-api/src/main/java/com/fengluan/trade/service/impl/OrderServiceImpl.java:68-78`
- 现状：`selectList` 读购物车在 `tryLock` 之前，两个并发请求先读到同一批勾选购物车，再先后获锁各自建单。
- 修复：将「读购物车 + 校验 + 写订单」整体移入锁内。

### 10. 订单状态机竞态（非 CAS）
- 位置：`goods-store-service/goods-store-trade-api/src/main/java/com/fengluan/trade/service/impl/OrderServiceImpl.java`
  - `cancel` 204-221、`confirm` 223-234、`ship` 300-310
- 现状：均为「先读后无条件 `updateById`」，只有 `pay`/`doRefund` 用 CAS（`where status='x'`）。
- 影响：与支付/退款/超时关单并发时状态回退、库存重复回补；`cancel` 在事务内循环调 Feign 恢复库存，DB 回滚与远程副作用不一致。
- 修复：所有状态流转统一改为 CAS 条件更新；远程库存变更移出事务或改造为异步最终一致。

---

## 二、中危问题

### 1. 会员越权 + 敏感信息未脱敏
- 位置：`goods-store-service/goods-store-member-api/src/main/java/com/fengluan/member/api/MemberController.java:31-58`
- 现状：`setEnabled`/`page`/`getAccount`/`getDefaultAddress` 无管理员角色或无归属校验；`MemberAddressServiceImpl.toVO` 未对 `phone` 脱敏。
- 影响：任意登录会员可启用/禁用他人、查看全量会员、取他人账号与默认地址原始手机号（水平 + 垂直越权叠加）。

### 2. 直接信任伪造请求头
- 位置：
  - `goods-store-service/goods-store-member-api/src/main/java/com/fengluan/member/util/CurrentUserUtil.java:19-30`
  - `goods-store-web/src/main/java/com/fengluan/web/config/AdminRoleInterceptor.java:28-45`
- 现状：直接读取 `X-User-Id` / `X-User-Roles` / `X-User-Permissions` 请求头，无来源校验。
- 影响：服务不被网关隔离时，可伪造头冒充任意会员/管理员。
- 修复：内部服务网络隔离，或在网关与企业内部接口间加共享密钥/签名。

### 3. 网关 `roles` claim 未判空
- 位置：`goods-store-gateway/src/main/java/com/fengluan/gateway/filter/JwtAuthFilter.java:76`
- 现状：`String.join(",", claims.get("roles", List.class))` 未判空；refresh token 无 `roles` claim。
- 影响：refresh token 命中非白名单路径时 `String.join` 抛 NPE → 网关 500。

### 4. CORS 通配 Origin + allowCredentials(true)
- 位置：`goods-store-gateway/src/main/java/com/fengluan/gateway/config/CorsConfig.java:17-21`
- 现状：`allowedOriginPatterns("*")` 且 `allowCredentials(true)`。
- 影响：来源反射 + CSRF 风险。
- 修复：显式配置可信前端域名。

### 5. 文件上传无校验
- 位置：`goods-store-service/goods-store-brand-api/src/main/java/com/fengluan/brand/util/LocalFileUploadUtil.java:28-45`
- 现状：仅取客户端文件名后缀，不校验扩展名白名单/MIME/魔数/大小；且 `/static/upload/**` 静态映射可直接访问。
- 影响：可上传 `.html/.svg/.jsp` 等，存储型 XSS/钓鱼，甚至 RCE。
- 修复：校验扩展名/MIME/魔数、限制大小、静态目录与上传目录分离并追加安全响应头。

### 6. 参数校验 `@Valid` 未生效
- 位置：`goods-store-spi/goods-store-auth-spi/src/main/java/com/fengluan/spi/auth/AuthApi.java` 与 `goods-store-service/goods-store-auth-api/src/main/java/com/fengluan/auth/api/AuthController.java`
- 现状：DTO 上有 `@NotBlank/@Size/@Pattern/@Email`，但接口/控制器方法均未加 `@Valid`。
- 影响：密码长度、手机号/邮箱格式等校验形同虚设。

### 7. admin-web 未识别 Void 接口成功
- 位置：`goods-store-frontend/admin-web/src/utils/request.ts:34-35`
- 现状：仅 `res.code === 200 || res.success` 判成功，`ApiResult.code` 类型为 `number`（未允许 null）。
- 影响：后端 Void 接口返回 `code=null/success=false`（见高危 #5），删除/上下架/发货/退款被误判失败。
- 修复：先修 `ApiResult.success()` 根因，前端类型 `code: number | null` 并对 null 做成功兜底。

### 8. 注册未校验手机号/邮箱唯一性
- 位置：`goods-store-service/goods-store-auth-api/src/main/java/com/fengluan/auth/service/AuthService.java:35-48`
- 影响：可批量注册重复手机号/邮箱账号。

### 9. token 黑名单覆盖式存储
- 位置：`goods-store-service/goods-store-auth-api/src/main/java/com/fengluan/auth/service/TokenService.java:46-55`
- 现状：黑名单 key 为 `token:blacklist:{userId}:{type}`，value 覆盖写入。
- 影响：多设备登出时后一次覆盖前一次，旧会话可复活。

### 10. owner-web 引用未定义常量，构建失败
- 位置：`goods-store-frontend/owner-web/src/utils/request.ts:110`
- 现状：`isTokenExpiringSoon` 引用未声明常量 `REFRESH_AHEAD_MS`（TS2304），且该方法为死代码。
- 影响：`vue-tsc -b && vite build` 直接构建失败。
- 修复：删除死代码或补常量定义。

---

## 三、低危 / 其他

- `goods-store-common/.../util/SnowflakeUtil.java`：方法名拼写错误 `nectIdStr()`（应为 `nextIdStr`）；workerId/datacenterId 无 0-31 范围校验。
- 多处 `Long → intValue()` 截断：`OrderServiceImpl.java:69/117`、`SeckillOrderConsumer.java:70-71` 等，雪花 ID 超 int 范围会截断。
- 秒杀防重键无 TTL、无兜底清理：消息丢失会让用户永久「已抢到」。
- 秒杀价硬编码 `BigDecimal.ZERO`：消费者以原价 `good.getPrice()` 计价，秒杀价字段形同虚设。
- `GoodServiceImpl` 分类名逐条 `selectById` 存在 N+1；`BrandRemoteService` 每次拉全量品牌 `page(1000)` 内存过滤，降级时品牌名统一显示「-」。
- Nacos `spring.cloud.nacos.discovery.ip` 硬编码内网 IP（`goods-store-service/*/resources/application.yaml`）。
- Druid 使用废弃驱动类名 `com.mysql.jdbc.Driver`（应为 `com.mysql.cj.jdbc.Driver`）。
- `GlobalExceptionHandler` 缺 `MethodArgumentTypeMismatchException`/`MissingServletRequestParameterException` 等参数绑定异常处理，参数错误返回 500 而非 400。
- `FeignErrorDecoder` 日志打印下游完整响应体，存在敏感信息泄露。
- 网关 401 响应未设置 `Content-Type`/UTF-8、JSON 未转义。

---

## 四、建议处理优先级

1. **能否运行**：`GatewayApplication.main` 缺 `public`；owner-web `REFRESH_AHEAD_MS` 未定义。
2. **安全**：网关白名单收敛、角色/权限接口鉴权、JWT 密钥外置与轮换、文件上传校验、CORS 收敛。
3. **正确性**：`ApiResult.success()` 无参、库存一致性（缓存失效 / MQ 可靠 / 秒杀扣 DB）、订单状态机 CAS 统一、秒杀建单事务、下单锁前移覆盖读购物车。
4. **规范与健壮性**：`@Valid` 生效、注册唯一性、Long→int 截断、数据脱敏、Feign 降级、Nacos/Druid 配置清理。