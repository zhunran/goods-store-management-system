# 2026年8月28日11:09:27

近期项目（goods-store-management-system）排查的问题与解决方案汇总。

---

## 一、环境与基础设施

### 1. Maven 本地仓库配置

- **问题**：需要将 Maven 本地仓库指向固定位置，加快依赖下载。
- **方案**：在 `D:\maven-repo\settings.xml` 中配置：
  - `<localRepository>D:/myrepository</localRepository>`
  - 添加阿里云镜像 `https://maven.aliyun.com/repository/central`

### 2. 端口冲突

- **问题**：`Port 8082 was already in use`。
- **排查**：`Get-NetTCPConnection` 发现 8082 被 QQ 进程占用。
- **方案**：goods-store-web 端口改为 **8083**。

---

## 二、数据库与 Nacos

### 3. 数据库连接字符集错误

- **现象**：`java.sql.SQLException: Unsupported character encoding 'utf8mb4'`
- **原因**：`utf8mb4` 是 MySQL 字符集名，而 MySQL Connector/J 9.x 的 `characterEncoding` 参数要求 **Java 编码名**。
- **方案**：JDBC URL 中 `characterEncoding=utf8mb4` 改为 `characterEncoding=UTF-8`。

### 4. Nacos namespace 配置

- **要点**：`namespace:` 必须填 **命名空间 ID**（UUID 形式），不是名称；不填默认走 `public`。
- **注意**：`discovery` 和 `config` 的 namespace 是分开配置的；只有 config 配 namespace 时，服务仍注册到 public。

---

## 三、MyBatis-Plus 与 Spring Boot 4 兼容性

### 5. SqlSessionFactory 创建失败

- **现象**：`Property 'sqlSessionFactory' or 'sqlSessionTemplate' are required`
- **原因**：`mybatis-plus-spring-boot3-starter` 与 Spring Boot 4.x 自动装配不兼容。
- **方案**：改用 Boot 4 专用依赖组合（版本 3.5.17）：
  - `mybatis-plus-spring-boot4-starter`
  - `mybatis-plus-extension`（`MybatisPlusInterceptor`、分页插件）
  - `mybatis-plus-jsqlparser`（`PaginationInnerInterceptor`，3.5.9+ 已拆分）

### 6. IService / ServiceImpl 包路径变更

- **现象**：`找不到符号：类 IService / ServiceImpl`
- **原因**：3.5.17 中 `IService`/`ServiceImpl` 从 `com.baomidou.mybatisplus.extension.service` 迁移到 **`com.baomidou.mybatisplus.spring.service`**。
- **方案**：更新 import 为 `com.baomidou.mybatisplus.spring.service.IService` / `...impl.ServiceImpl`。

### 7. Mapper Bean 未注册

- **现象**：`No qualifying bean of type '...BrandMapper'`
- **方案**：启动类加 `@MapperScan("...repository")`。
- **注意**：包路径是 `org.mybatis.spring.annotation.MapperScan`，不是 `org.apache.ibatis.annotations.MapperScan`。

---

## 四、Spring Cloud 服务调用

### 8. 负载均衡器缺失（RestTemplate / Feign 通用）

- **现象**：
  - RestTemplate：`UnknownHostException: goods-store-brand-api`（服务名被当域名解析）
  - Feign：`No Feign Client for loadBalancing defined. Did you forget to include spring-cloud-starter-loadbalancer?`
- **原因**：`spring-cloud-starter-alibaba-nacos-discovery` 与 `spring-cloud-starter-openfeign` 对 `spring-cloud-loadbalancer` 的依赖都是 **`<optional>true</optional>`，不会传递**。
- **方案**：凡用到「服务名调用」的模块，**显式**添加 `spring-cloud-starter-loadbalancer`。

### 9. Feign 客户端定义问题

- **问题 1**：`@FeignClient` 缺少 `name` → 无法定位服务。
  - **方案**：`@FeignClient(name = "goods-store-brand-api")`。
- **问题 2**：返回类型用 `IPage<BrandVO>` → Jackson 无法反序列化接口（`IPage` 无 `@JsonDeserialize`）。
  - **方案**：改用具体类 `Page<BrandVO>`。
- **问题 3**：GET 请求 POJO 参数未正确绑定。
  - **方案**：参数加 `@SpringQueryMap`，将对象字段展开为 query 参数。

### 10. Spring Cloud 2025.x 依赖名变化

- **问题**：`spring-cloud-starter-gateway` 报 `version is missing`。
- **原因**：2025.x 将 Gateway 拆分为 `-server-webflux` / `-server-webmvc` 两个 starter，旧名不再被 BOM 管理。
- **方案**：使用 `spring-cloud-starter-gateway-server-webflux`。

---

## 五、工程结构与模块配置

### 11. 多模块架构重组

- **调整**：
  - `goods-store-api` 更名 `goods-store-service`（业务服务聚合，下辖 brand/product/member/trade/seckill-api）
  - `goods-store-web` 上移到根目录（与 service 平级，作为 BFF）
  - 新增 `goods-store-common`（统一返回 `ApiResult`、DTO/VO、entity）与 `goods-store-gateway`（网关）
  - 新增 `goods-store-spi`（服务契约聚合层）

### 12. goods-store-spi 模块配置异常

- **现象**：IDEA 无法正确识别根目录结构。
- **原因**：
  1. 目录存在但未注册到根 pom 的 `<modules>`
  2. `<packaging>pom</packaging>` 却残留应用模板配置（`spring-boot-maven-plugin`、webmvc/devtools 依赖、`application.yaml`、`@SpringBootTest` 测试类），且无 `<modules>`
- **方案**：
  1. 根 pom `<modules>` 补 `goods-store-spi`
  2. 重写 spi pom 为干净聚合 pom（仅 lombok + compiler 插件）
  3. 删除 `application.yaml` 与 `@SpringBootTest` 测试类

### 13. 启动类 main 方法可见性

- **问题**：`static void main(...)` 缺少 `public`，`java -jar` 方式会报 Main method not found。
- **方案**：改为 `public static void main(String[] args)` 并透传 `args`。

### 14. Redisson 使用最终方案：核心包 + 手写 `RedissonConfig`（boot4-starter 运行期不兼容）

- **背景**：父 POM `<dependencyManagement>` 只锁定了 starter `redisson-spring-boot-starter`（`3.50.0`），未管理核心包 `redisson`。
- **问题 1（构建期）**：直接引核心包 `<artifactId>redisson</artifactId>` 不带版本 → `mvn` 报 `'dependencies.dependency.version' for org.redisson:redisson:jar is missing`。
- **问题 2（运行期）**：该 starter 的 `RedissonAutoConfiguration` 引用了 Spring Boot 4.1 已删除的 `org.springframework.boot.autoconfigure.data.redis.RedisProperties` → `Could not find class [RedisProperties]`，启动失败。
- **方案**：trade-api 改引核心包并**显式锁定版本** `<version>${redisson.version}</version>`；手写 `config/RedissonConfig`（读 `spring.data.redis.host/port`）构建 `RedissonClient`，绕过 starter 自动装配。`spring-boot-starter-data-redis`（+ `commons-pool2`）另供 RedisTemplate/Lettuce，与 Redisson 并存。
- **要点**：`redisson-spring-boot-starter` 仅兼容传统 Spring Boot 3；**Boot 4 环境用核心包手动装配**才不会踩 `RedisProperties` 缺失。

### 15. MyBatis-Plus 3.5.9+ 的 IService / ServiceImpl 迁移到 `spring` 模块

- **现象**：编译报「找不到符号 类 IService / ServiceImpl」。
- **原因**：3.5.9+ 为解耦 mybatis-spring，将 `IService`/`ServiceImpl` 自 `mybatis-plus-extension` 拆到新的 **`mybatis-plus-spring`** 模块，包路径从 `com.baomidou.mybatisplus.extension.service` 变为 **`com.baomidou.mybatisplus.spring.service`** / `.impl.ServiceImpl`。
- **方案**：改引 `com.baomidou.mybatisplus.spring.service.IService` 与 `*.impl.ServiceImpl`；若需显式依赖，加 `<artifactId>mybatis-plus-spring</artifactId>`（版本 `${mybatis-plus.version}`）。`extension` 现仅剩 `IRepository`/`AbstractRepository` 等。

### 16. 同一服务的多个 Feign 客户端必须加唯一 `contextId`

- **现象**：`The bean 'goods-store-trade-api.FeignClientSpecification' could not be registered. A bean with that name has already been defined`（web 模块购物车 + 下单两个 Feign 客户端指向同一服务）。
- **原因**：Feign 的 `contextId` 默认取 `name`；两个 `@FeignClient(name="goods-store-trade-api")` 会注册同名 `FeignClientSpecification` Bean，且 `spring.main.allow-bean-definition-overriding` 默认关闭。
- **方案**：为每个客户端加唯一 `contextId`，如 `@FeignClient(name="goods-store-trade-api", contextId="tradeCartFeignClient", path="/trade/api")`。
- **要点**：同服务多个 Feign 客户端（购物车/下单等）必须用 `contextId` 区分，否则启动冲突。

### 17. Feign 客户端接口不支持多继承（Only single inheritance supported）

- **现象**：`java.lang.IllegalStateException: Only single inheritance supported: SeckillFeignClient`，web 模块启动时 `SeckillFeignClient` 的 FactoryBean 创建失败，进而 `webSeckillController` 依赖注入失败。
- **原因**：一个 `@FeignClient` 接口 `extends` 了两个 SPI 契约接口（`SeckillGoodApi, SeckillOrderApi`），Feign 的 `Contract$BaseContract.parseAndValidateMetadata` 仅支持接口**单一继承**。
- **方案**：拆成两个独立 `@FeignClient`，各自单继承一个契约；`name` 相同（同一服务 `goods-store-seckill-api`）、`contextId` 唯一区分（`seckillGoodFeignClient` / `seckillOrderFeignClient`）。
- **要点**：Feign 客户端接口**只能单继承一个父契约**；需要聚合多个契约时拆分为多个 client（同服务用不同 `contextId`）。

---

## 六、管理端 BFF（goods-store-web）

### 18. 管理端登录被拦截返回 403「无权限访问」

- **现象**：调用 `/app/api/auth/admin/login` 直接返回 `{"code":403,"message":"无权限访问"}`，无法登录。
- **原因**：`AdminRoleInterceptor` 的路径模式 `/app/api/**/admin/**` 把登录端点也命中了；而登录请求尚未携带网关透传的 `X-User-Roles` 头，被判定为非管理员。
- **方案**：在 `AdminWebMvcConfig` 中放行登录端点：
  ```java
  registry.addInterceptor(new AdminRoleInterceptor())
          .addPathPatterns("/app/api/**/admin", "/app/api/**/admin/**")
          .excludePathPatterns("/app/api/auth/admin/login");
  ```
- **要点**：角色校验拦截器必须**排除登录/注册等未鉴权端点**，否则登录请求因缺少 `X-User-Roles` 被误拦。

### 19. Feign 反序列化 LoginResponse 失败（no Creators）

- **现象**：仅 `goods-store-web` 报错，auth 与 gateway 正常：
  ```
  feign.codec.DecodeException: Type definition error: [simple type, class com.fengluan.spi.auth.dto.LoginResponse]
  Caused by: InvalidDefinitionException: Cannot construct instance of com.fengluan.spi.auth.dto.LoginResponse
  (no Creators, like default constructor, exist): cannot deserialize from Object value
  ```
- **原因**：`LoginResponse` 及其内部类 `UserInfo` 只标注 `@Builder`，未生成无参构造；Jackson 3（tools.jackson）反序列化需要默认构造或 `@JsonCreator`。auth-api 只做**序列化**（用 `@Data` 的 getter）故不报错；web 层通过 Feign 解码时需**反序列化**才失败。
- **方案**：给 `LoginResponse` 与 `UserInfo` 补 `@NoArgsConstructor` + `@AllArgsConstructor`：

  ```java
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public class LoginResponse { ... }
  ```

  - `@NoArgsConstructor`：供 Jackson 反序列化时实例化；
  - `@AllArgsConstructor`：供 `@Builder` 使用；
  - `@Data` 的 setter 完成字段注入。

- **要点**：Lombok `@Builder` 类若要作为 Feign 返回类型被反序列化，**必须显式提供无参构造**（`@NoArgsConstructor` + `@AllArgsConstructor` 或 `@Jacksonized`）。

---

## 七、用户端与秒杀联调缺陷排查（2026-09-05）

### 20. 用户端注册 500：实体映射了表里不存在的列

- **现象**：`POST /app/api/auth/register` 返回 500，日志报 `Unknown column 'status' in 'field list'`。
- **原因**：`AuthUserEntity` 定义了 `Integer status`，但 member 表实际列为 `enabled bit(1)`（MyBatis-Plus 将 bit(1) 映射为 Boolean），实体与表结构不一致导致 INSERT 失败。
- **方案**：实体改为 `private Boolean enabled;`；注册置 `true`，登录判 `!Boolean.TRUE.equals(u.getEnabled())`（同时修复登录同类问题）。
- **要点**：实体字段必须与真实表结构核对，尤其 `bit(1)` → `Boolean`、列名不一致（status/enabled）两类坑。

### 21. 管理端登录 500：业务码被误映射为 HTTP 500

- **现象**：密码错误等业务异常（如 `AUTH_PASSWORD_ERROR=6002`）统一返回 HTTP 500，前端只见「服务器异常」，真实原因被掩盖。
- **原因**：`GlobalExceptionHandler` 旧逻辑 `code >= 500` 映射 HTTP 500，而本项目业务码为四位（1xxx~6xxx），全部误中。
- **方案**：区分两套码——`code < 1000` 视为 HTTP 状态码透传（如 401/403），`>= 1000` 统一按 400 返回。
- **旁证推理**：admin/refresh 返回 401（正常业务响应）而非 500，证明 Redis/DB/Feign 链路正常，锁定异常处理器映射逻辑。
- **要点**：业务码（四位）与 HTTP 状态码（三位）是两套体系，全局异常处理器必须分支处理。

### 22. 管理端秒杀商品列表不更新：误用用户端接口

- **现象**：后台添加秒杀商品提示成功，前端列表却不显示新商品。
- **原因**：前端调用的 `/seckill/list` 是用户端接口，SQL 过滤 `s.enabled=1 AND start_time<=now AND end_time>=now`——未来时段的活动查不到，也不按活动过滤。
- **方案**：新增管理端专用接口 `GET /seckill/admin/activity/{id}/goods` 全链路（SPI → Mapper `selectByActivity`（join 不限时间窗口与启用状态）→ Service → BFF）。
- **要点**：管理端与用户端列表口径天然不同（时间窗口、启用状态、按活动过滤），不能复用同一接口。

### 23. 秒杀误报「库存已空」：Redis 库存键从未预热

- **现象**：数据库库存充足（count != 0），点击抢购却提示「手速太慢」，后端报秒杀库存已空——属**后端问题**而非数据问题。
- **原因**：`StockPreheatJob` 只预热「未来 5 分钟内开始」的活动；活动开始后才添加的商品永远不在预热范围 → Redis 键不存在 → Lua 返回 -1 被当售空。另旧实现「先占位 0 再写真值」两步间存在误判售罄窗口。
- **方案**：
  1. 预热范围扩大到进行中活动（`startTime <= now+5min AND endTime >= now`）；
  2. 一步 `setIfAbsent(key, stock)` 写入真值，消除误判窗口；
  3. 抢购时 Lua 返回 -1 即时回源：查商品真实库存 setnx 写入 + 过期时间对齐 job + 重试一次。
- **要点**：预热 job 的覆盖范围要与业务操作时序对齐（商品可在活动进行中添加）；「占位再写真值」会制造中间态，能原子写入就不要拆两步。

### 24. 抢购建单失败：order 保留字 + MQ 线程无鉴权上下文（双缺陷叠加）

- **现象**：抢购进入队列后建单失败，触发 Redis 补偿（恢复库存 + 清防重键），结果轮询一直「排队中」。
- **根因 A（SQL 语法错误）**：`SeckillOrderEntity` 的 `@TableName("order")` 缺反引号，`order` 是 MySQL 保留字，`SELECT ... FROM order WHERE ...` 直接语法报错，轮询查询与建单插入全部失败。
- **根因 B（MQ 线程 401）**：消费者建单时调 `memberClient.getProfile()`，该方法内 `CurrentUserUtil.assertOwned(id)` 依赖 `RequestContextHolder` 读 `X-User-Id` 头；MQ 消费线程没有 HTTP 上下文，必抛「未登录」401。
- **方案**：
  1. `@TableName("`order`")` 加反引号（与 trade-api 的 OrderEntity 对齐）；
  2. member 新增内部接口 `getAccount(id)`（SPI + Service + Controller 全链路，无越权校验，参照 `getDefaultAddress` 惯例——调用方经网关层已有会话鉴权），MQ 消费者与 result() 归属校验均改调它。
- **要点**：
  - MySQL 保留字（order/desc/rank 等）作表名必须在 `@TableName` 与手写 SQL 中反引号转义；
  - MQ 消费者、定时任务等无 HTTP 上下文的线程不能调用带越权校验（assertOwned）的接口，应提供信任调用方的内部接口。

### 25. 秒杀「已抢到 / 已售空」状态展示（联调优化）

- **问题**：列表无法区分「用户已抢购」与「已售空」，只能点击抢购被 4004/4005 拒绝后才知道结果。
- **方案**：
  - `SeckillGoodVO` 新增 `stockLeft`（读 Redis 库存键；未预热返回 null，不算售空）与 `robbed`（防重键 `seckill:order:{memberId}:{sgId}` 存在即已抢到——建单失败时消费者删键回滚，语义准确）；
  - `activeList()` 统一填充；新增 `CurrentUserUtil.currentUserIdOrNull()` 兼容匿名浏览（未登录 robbed 全 false）；
  - 前端按钮三态（已抢到 / 已售空 / 立即抢购）+ 显示「仅剩 X 件」+ 抢购被拒后自动刷新列表。
- **要点**：Redis 防重键可同时充当「已抢到」状态源；库存键缺失时返回 null 而非 0，避免把「未预热」误判为「售空」；键过期（活动结束后 1 小时）后历史状态消失属预期。

---

## 关键经验总结

1. **Spring Boot 4 + MyBatis-Plus 必须用 `mybatis-plus-spring-boot4-starter`**，并补 `extension`、`jsqlparser`。
2. **服务名调用（RestTemplate/Feign）必须显式引入 `spring-cloud-starter-loadbalancer`**，不能依赖传递。
3. **MySQL Connector/J 9.x 的 `characterEncoding` 用 Java 编码名（UTF-8），不是 MySQL 字符集名（utf8mb4）。**
4. **Feign 返回类型避免接口（如 `IPage`），用具体实现类（如 `Page`）。**
5. **新增 Maven 子模块必须同步注册到父 pom 的 `<modules>`**，否则 IDEA 无法识别。
6. **Feign 客户端接口只能单继承一个契约接口**；聚合多个契约需拆成多个 `@FeignClient`（同名服务用不同 `contextId` 区分）。
7. **Lombok `@Builder` 类作为 Feign 返回类型被反序列化时，必须显式提供 `@NoArgsConstructor`（+`@AllArgsConstructor`）**，否则 Jackson 3 报 `no Creators, like default constructor, exist`。
8. **管理端角色校验拦截器必须排除登录/注册等未鉴权端点**，否则登录请求因缺少透传的 `X-User-Roles` 头被误判 403。
9. **MySQL 保留字表名（如 `order`）必须在 `@TableName` 与手写 SQL 中反引号转义**，否则生成 SQL 直接语法错误。
10. **MQ 消费者/定时任务等无 HTTP 上下文的线程，不能调用带 `assertOwned` 越权校验的接口**（`RequestContextHolder` 无请求必抛 401）；跨服务内部调用应提供信任调用方的无校验接口（调用方经网关已鉴权）。
11. **全局异常处理器必须区分业务码（四位 1xxx~6xxx）与 HTTP 状态码（三位）**：`<1000` 按 HTTP 语义透传，`>=1000` 统一 400，否则业务异常全部被误报为 500。
12. **实体映射前先核对真实表结构**：`bit(1)` 对应 Boolean；列名以导出的表结构为准（如 member 表是 `enabled` 而非 `status`）。
13. **Redis 预热 job 的覆盖范围要与业务时序对齐**（商品可能在活动进行中才添加），键不存在时抢购链路需即时回源兜底，而非直接判售空。
