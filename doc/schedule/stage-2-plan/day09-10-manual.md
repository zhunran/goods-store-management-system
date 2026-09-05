# Day 9-10 操作执行手册 — 交易服务（购物车 + RabbitMQ + 下单 + Redisson 分布式锁）+ spi 契约 + web 聚合

> 对应计划：[14-day-implementation-plan.md](file:///d:/.workspace/javaproject/goods-store-management-system-parent/doc/schedule/14-day-implementation-plan.md) Day 9（交易·上：购物车 + MQ 配置）/ Day 10（交易·中：下单 + 分布式锁）
> 版本：v1.0　编写日期：2026-09-02　执行日期：待填
> 主题：trade-spi 纯契约（CartApi/OrderApi）；购物车 CRUD + RabbitMQ 交换队列；下单全流程 + Redisson 分布式锁 + 雪花订单号 + MQ 扣库存/延迟取消；web 购物车/下单聚合收口
> 依赖：Day 1-8 已完成（基础设施 + 认证 + 品牌 + 商品 + 会员），均编译通过并启动成功
> **架构前提**：遵循 14 天计划「⭐ 分层架构规范」——spi 是**纯 HTTP 契约层**（禁 `@FeignClient`）、controller `implements` 契约、消费方 Feign `extends` 契约、web 是 BFF 聚合层（前端唯一入口 `/app/api/**`）。

> ⚠️ **本手册以「当前已落地架构」为准，对计划书 Day 9/10 示例做三处修正**：
>
> 1. 计划书写 `@FeignClient(name="goods-store-product-api", path="/product/api")`、`ApiResult<Boolean> deductStock(...)` → 现况是 `ProductApi` 纯契约、类级前缀 `/good/api`、库存扣减返回纯 JDK `Boolean`（[ProductApi.java](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-spi/goods-store-product-spi/src/main/java/com/fengluan/spi/product/ProductApi.java)），消费方 `extends ProductApi` 客户端 `path="/good/api"`；
> 2. 计划书下单示例用 `ProductApi productApi`（注入契约）→ 实际 service 内应注入**实现方自己的 Feign 客户端**（`extends ProductApi`），不直接注入 spi 接口；
> 3. `SnowflakeUtil` 的字符串方法是 `nectIdStr()`（计划书误写 `nextIdStr()`）——按现况方法名调用。

---

## 一、Day 9-10 目标与产出物

### 目标

1. **trade-spi 从「带 `@FeignClient` 的测试接口」重构为纯契约**：拆 `CartApi`（购物车）+ `OrderApi`（下单），补 DTO/VO；去除 openfeign 依赖（同 member-spi Day8 先例）
2. **购物车能力**：增删改查 + 唯一索引 `uq_member_good`（同一商品重复添加数量累加）+ 库存/下架校验（`extends ProductApi`）
3. **RabbitMQ 基础设施**：声明订单 Exchange/Queue/Binding（含 30 分钟延迟超时 → 死信取消队列）+ `Jackson2JsonMessageConverter` + 发布确认
4. **下单全流程**：Redisson 分布式锁防重复下单 → 校验购物车/商品库存 → 雪花订单号 → 订单+明细事务写入 → MQ 发扣库存/延迟消息 → 清空购物车
5. **契约消费闭环**：trade 消费 product/member 只 `extends` 契约；web 消费 trade 只 `extends`
6. **web 聚合收口**：购物车 `/app/api/cart/*`（列表补商品图/名/价）+ 下单 `POST /app/api/order/submit`（编排）

### 产出物清单

| 模块      | 产出物                                                                                                                                                                                                                                                                                                                                                          |
| --------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| trade-spi | `pom` 去 openfeign → spring-web + validation；删旧 `TradeApi`，新建 `CartApi`/`OrderApi` 纯契约 + `CartAddRequest/CartUpdateRequest/CartVO/OrderCreateRequest/OrderCreateResponse`                                                                                                                                                                              |
| trade-api | pom 补 SPI/openfeign/loadbalancer/amqp/redis/redisson；`TradeApiApplication` 加 `@EnableFeignClients`；`RabbitMqConfig`；`RedissonConfig`；`CartMapper(insertOrUpdate)`/`OrderMapper`/`OrderItemMapper`；`CartService`/`OrderService`+Impl；`CartController`/`OrderController implements`；`OrderMessageProducer`；Feign 客户端（product/member）+ 头透传拦截器 |
| web       | pom 加 `goods-store-trade-spi`；`trade/` 四件套：`TradeCartFeignClient`/`TradeOrderFeignClient`(extends) + `WebCartService`/`WebOrderService` + `WebCartController`/`WebOrderController` `/app/api/**`                                                                                                                                                          |
| gateway   | 路由 `/trade/api/**` 已存在；`/app/api/**` 已收口；白名单无需改动                                                                                                                                                                                                                                                                                               |

---

## 二、技术要点

### Day 9 技术要点

| 要点             | 说明                                                                                                                                                 |
| ---------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- |
| 购物车唯一约束   | `INSERT ... ON DUPLICATE KEY UPDATE qty = qty + #{qty}`，复用 `cart` 表 `uq_member_good`(member_id,good_id) 唯一索引                                 |
| 库存/下架校验    | 添加/改数量时 `productFeign.getById(goodId)`：商品为空或 `isTakeDown=true` 抛 `GOOD_NOT_FOUND(2001)`；`qty< 想买` 抛 `GOOD_STOCK_INSUFFICIENT(2002)` |
| RabbitMQ 配置    | `@Configuration` 声明 `OrderExchange/Queue/Binding` + `Jackson2JsonMessageConverter`；发布确认 `publisher-confirms/correlated+returns`               |
| 当前用户         | `cart` 表用 `member_id(int)`；trade 从入站 `X-User-Id` 头取当前会员（网关或 web 经 Feign 透传，另一端兜底）→ 校验归属                                |
| 契约纯度（spi）  | `CartApi/OrderApi` 无 `@FeignClient`，只写路径注解 + DTO/VO；controller `implements`；web `extends`                                                  |
| 契约路径（交易） | 控制器类级前缀 `/trade/api`（与网关路由一致）；购物车相对 `/cart`，改数量 `/cart/{id}?qty=`，删除 `/cart/{id}`                                       |
| 头透传           | trade-api 调 member/product 的下游 Feign 需把 `X-User-Id` 透传（member `getProfile` 有越权），复用 web 的拦截器思路到 trade                          |

### Day 10 技术要点

| 要点              | 说明                                                                                                                                                                          |
| ----------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Redisson 分布式锁 | 以 `lock:order:{memberId}` 加锁，`tryLock(10,30,s)`；同会员并发重复下单仅一单成功；`finally` 内 `isHeldByCurrentThread` 释放                                                  |
| 雪花算法订单号    | `snowflakeUtil.nectIdStr()`（注意方法名拼写），写 `order.order_no`（`uq_order_no` 唯一）                                                                                      |
| 下单流程          | 锁 → 校验购物车非空(否则 `CART_EMPTY(3003)`) → 逐个商品校验库存/下架+算总价 → 取收货地址（`addressId` 或默认） → 订单+明细事务写入 → MQ 扣库存+延迟取消 → 清空购物车 → 释放锁 |
| 会员账号关联      | `order.member_account` 存账号（varchar）；trade 经 `memberFeign.getProfile(memberId).getAccount()` 获取                                                                       |
| 地址来源          | `request.addressId` 可选，缺省用 `memberFeign.getDefaultAddress(memberId)`；地址不存在抛 `MEMBER_ADDRESS_NOT_FOUND(1005)`                                                     |
| 事务 + MQ 顺序    | 订单+明细的 DB 写入用 `@Transactional`；MQ 消息在**事务提交后**发（`TransactionSynchronization` afterCommit），避免发消息后回滚                                               |
| 异步扣库存        | 下单后发送 `OrderCreateMessage`（含 orderNo/memberId/items）→ Day 11 消费者调 `ProductApi.deductStock`（乐观锁）真正扣减                                                      |
| 延迟取消          | 发送到 `ORDER_TIMEOUT_KEY` 路由到 TTL=30min 的 `orderTimeoutQueue`，到期死信 → `ORDER_CANCEL_KEY` → `orderCancelQueue`（Day 11 消费取消）                                     |

> 错误码：交易段现已有 `ORDER_NOT_FOUND(3001)/ORDER_STATUS_ERROR(3002)/CART_EMPTY(3003)`（[ErrorCode.java](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-common/src/main/java/com/fengluan/common/exception/ErrorCode.java#L30-L33)），Day 9-10 直接复用，不新增。

---

## 三、Day 9-10 实施状态盘点

> 基于当前工程实测（Day 1-8 完成）。

| 计划任务  | 内容                       | 当前状态                                                                                                                                                                                                   | 手册步骤         |
| --------- | -------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------- |
| 9.1       | trade-api POM 补依赖       | ⚠️ [trade-api/pom](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-service/goods-store-trade-api/pom.xml) 仅 `goods-store-common`，无 SPI/openfeign/amqp/redis/redisson | 步骤 D9-1        |
| 9.2       | RabbitMqConfig             | ❌ 无任何 MQ 配置类                                                                                                                                                                                        | 步骤 D9-2        |
| 9.3-9.6   | CartController CRUD        | ❌ 仅 `CartEntity`（含 `memberId/goodId/qty`），无 controller/service/mapper                                                                                                                               | 步骤 D9-3/D9-4   |
| 9.7       | CartService 库存校验       | ❌ 无                                                                                                                                                                                                      | 步骤 D9-3        |
| 9.8       | web 购物车聚合             | ❌ web 无 trade-spi / trade 三件套                                                                                                                                                                         | 步骤 D9-5        |
| 10.1      | Redisson 配置类            | ❌ 无                                                                                                                                                                                                      | 步骤 D10-1       |
| 10.2-10.6 | 下单+Service+订单号+MQ+DTO | ❌ `OrderEntity`/`OrderItemEntity` 已有，无 mapper/service/controller/mq；`common` 已有 `OrderCreateMessage`/`SnowflakeUtil`                                                                               | 步骤 D10-2/D10-3 |
| 10.7      | web 下单聚合               | ❌ 无                                                                                                                                                                                                      | 步骤 D10-4       |

**现状基石**（已落地，直接复用）：

- `ProductApi` 纯契约含 `getById`/`deductStock(id,count)->Boolean`/`updateStatus`；实现方 `GoodController` 前缀 `/good/api`（[ProductApi.java](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-spi/goods-store-product-spi/src/main/java/com/fengluan/spi/product/ProductApi.java)，[GoodController.java](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-service/goods-store-product-api/src/main/java/com/fengluan/product/api/GoodController.java)）
- `MemberApi` 纯契约含 `getProfile(id)`（越权）/`getDefaultAddress(id)`（**内部，不越权**）；实现方前缀 `/member/api`（[MemberApi.java](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-spi/goods-store-member-spi/src/main/java/com/fengluan/spi/member/MemberApi.java)）
- `goods-store-common`：`OrderCreateMessage`（orderNo/memberId/items）+ `OrderCancelMessage` + `SeckillOrderMessage`（[common/mq](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-common/src/main/java/com/fengluan/common/mq/OrderCreateMessage.java)）；`SnowflakeUtil.nectIdStr()`（字符串雪花）
- spi 纯净先例：`product-spi` 只依赖 `spring-web + jakarta.validation-api`（[pom](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-spi/goods-store-product-spi/pom.xml)）
- web Feign 透传 `X-User-Id` 拦截器已存在（[FeignHeaderPropagateInterceptor.java](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-web/src/main/java/com/fengluan/web/config/FeignHeaderPropagateInterceptor.java)）
- 网关路由已含 `/trade/api/**` 与 `/app/api/**`（[application.yaml](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-gateway/src/main/resources/application.yaml)）
- `trade-api` `application.yaml` 已配端口 **8086**、snowflake worker-id=5、import 共享 nacos 配置

**表结构**（[table_structure_export.sql](file:///d:/.workspace/javaproject/goods-store-management-system-parent/sql/table_structure_export.sql)）：

- `cart`：`member_id`(int)/`good_id`(int)/`qty`，唯一索引 `uq_member_good`（唯一约束累加的前提）
- `order`：`order_no`(uq)/`seckill_no`/`member_account`/`total_pay`/`status`/`receiver_*`/`order_comment`/`is_del` 等
- `order_item`：`order_id`/`good_id`/`deal_price`/`count`/`good_name`/`good_pic`/`good_desc`
- `member_address`：`member_account`(varchar) 关联会员（取默认地址后直接映射 receiver 收件字段）

**环境前置检查**：trade 用到 RabbitMQ 与 Redis；确认 nacos 共享配置（`goods-store-common.yaml` 或 `goods-store-shared-service-config.yaml`）中已有 `spring.rabbitmq.*`、`spring.data.redis.host/port`。若缺失，在 trade-api 本地 `application.yaml` 补齐（值按基础设施搭建时设定，如 `localhost:5672` / `localhost:6379`）。

---

## 四、详细操作步骤

### 前置约定

- **spi 纯净**：`trade-spi` 改用 `spring-web`+`jakarta.validation-api`，**不要**再依赖 `spring-cloud-starter-openfeign`。
- **契约命名**：把旧 `TradeApi` 拆成 `CartApi`（购物车）+ `OrderApi`（下单），各由一个 controller `implements`，避免单接口多实现类必须实现全部方法。
- **契约返回**：`void`/`Void`/VO/`List`，不用 `ApiResult`/mybatis 类型。
- **当前用户**：`cart`/`order` 归属 `member_id`；trade controller 从 `@RequestHeader("X-User-Id")`（或 `RequestContextHolder`）取当前会员，service 统一以 `memberId` 参数贯穿。
- **头透传**：trade 调下游 member/product 的 Feign 客户端需透传 `X-User-Id`（member `getProfile` 越权校验）。
- **Feign 客户端落位**：trade 内建的 product/member 客户端放 `com.fengluan.trade.remote`，`TradeApiApplication` 上加 `@EnableFeignClients(basePackages="com.fengluan.trade.remote")`（参照 product-api 扫 web 包的做法）。

---

### Day 9 操作步骤

#### 步骤 D9-1：trade-spi 纯契约 + trade-api 依赖（任务 9.1 + spi 重构）

**1a. 改 `trade-spi/pom.xml`**：把 `spring-cloud-starter-openfeign` 换成 `spring-web` + `jakarta.validation-api`（逐字照抄 [product-spi/pom](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-spi/goods-store-product-spi/pom.xml) 的依赖）。

**1b. 删除旧契约与占位类型**：删除带 `@FeignClient` 的 [TradeApi.java](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-spi/goods-store-trade-spi/src/main/java/com/fengluan/spi/trade/TradeApi.java)。`dto/OrderQueryRequest`、`vo/OrderVO` 保留（Day 11 订单列表复用）。

**1c. 新增 DTO/VO** `com.fengluan.spi.trade.dto` / `.vo`：

```java
// CartAddRequest.java
public class CartAddRequest {
    @NotNull private Long goodId;
    @NotNull @Min(1) private Integer qty;
}

// CartVO.java —— trade 侧只回基础字段，商品图/名由 web 聚合补齐
public class CartVO {
    private Long id;
    private Long memberId;
    private Long goodId;
    private Integer qty;
}

// OrderCreateRequest.java —— addressId 可空（缺省用默认地址）
public class OrderCreateRequest {
    private Long addressId;
    private String comment;
}

// OrderCreateResponse.java
public class OrderCreateResponse {
    private String orderNo;
    private BigDecimal totalPay;
    private String status;
}
```

**1d. 新建纯契约** `com.fengluan.spi.trade`：

```java
package com.fengluan.spi.trade;

// CartApi.java —— 当前会员由实现方从 X-User-Id 解析，契约不声明 header
public interface CartApi {
    @GetMapping("/cart") List<CartVO> listCart();
    @PostMapping("/cart") Void addCart(@Valid @RequestBody CartAddRequest request);
    @PutMapping("/cart/{cartId}") Void updateCartQty(@PathVariable Long cartId, @RequestParam Integer qty);
    @DeleteMapping("/cart/{cartId}") Void removeCart(@PathVariable Long cartId);
}

// OrderApi.java
public interface OrderApi {
    @PostMapping("/order") OrderCreateResponse createOrder(@Valid @RequestBody OrderCreateRequest request);
}
```

> 相对路径叠加 `/trade/api`；`CartController`/`OrderController` 各自 `implements`。

**1e. 改 `trade-api/pom.xml`** 追加依赖：

```xml
<!-- spi 契约 -->
<dependency><groupId>com.fengluan</groupId><artifactId>goods-store-trade-spi</artifactId><version>1.0.0</version></dependency>
<dependency><groupId>com.fengluan</groupId><artifactId>goods-store-product-spi</artifactId><version>1.0.0</version></dependency>
<dependency><groupId>com.fengluan</groupId><artifactId>goods-store-member-spi</artifactId><version>1.0.0</version></dependency>
<!-- Feign 消费 -->
<dependency><groupId>org.springframework.cloud</groupId><artifactId>spring-cloud-starter-openfeign</artifactId></dependency>
<dependency><groupId>org.springframework.cloud</groupId><artifactId>spring-cloud-starter-loadbalancer</artifactId></dependency>
<!-- RabbitMQ / Redis / Redisson -->
<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-amqp</artifactId></dependency>
<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-redis</artifactId></dependency>
<dependency><groupId>org.redisson</groupId><artifactId>redisson</artifactId></dependency>
```

**1f. 改 `TradeApiApplication`**：加 `@EnableFeignClients(basePackages="com.fengluan.trade.remote")`（保留 `@MapperScan("com.fengluan.trade.repository")`）。

#### 步骤 D9-2：Feign 客户端 + 头透传 + RabbitMqConfig（任务 9.2）

**2a. Feign 客户端**（`com.fengluan.trade.remote`，`extends` 契约，不手写路径）：

```java
// TradeProductClient.java
@FeignClient(name = "goods-store-product-api", path = "/good/api")
public interface TradeProductClient extends ProductApi { }

// TradeMemberClient.java
@FeignClient(name = "goods-store-member-api", path = "/member/api")
public interface TradeMemberClient extends MemberApi { }
```

**2b. 头透传拦截器**（`com.fengluan.trade.config`，参照 web 同名类）：

```java
public class TradeFeignHeaderInterceptor implements RequestInterceptor {
    @Override public void apply(RequestTemplate template) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return;
        String uid = attrs.getRequest().getHeader("X-User-Id");
        if (uid != null && !uid.isBlank()) template.header("X-User-Id", uid);
    }
}
```

> 加拦截器后 trade 调 `memberFeign.getProfile(memberId)` 时携带当前 `X-User-Id`，member-api 的越权校验（== 目标 id）通过。

**2c. `RabbitMqConfig`**（`com.fengluan.trade.config`）：Exchange/Queue/Binding 逐字照抄总计划 Day 9 示例（`goods.order.*` + `sECKILL_ORDER` 队列）＋ `Jackson2JsonMessageConverter`。要点：

- 超时队列 `QueueBuilder.durable(ORDER_TIMEOUT_QUEUE).ttl(30*60*1000).deadLetterExchange(ORDER_EXCHANGE).deadLetterRoutingKey(ORDER_CANCEL_KEY)`；
- 常量：`ORDER_EXCHANGE`=`goods.order.exchange`、`ORDER_CREATE_QUEUE/KEY`、`ORDER_TIMEOUT_QUEUE/KEY`、`ORDER_CANCEL_QUEUE/KEY`、`SECKILL_ORDER_QUEUE/KEY`。

#### 步骤 D9-3：CartMapper + CartService（任务 9.7）

**3a. `CartMapper`**（`com.fengluan.trade.repository`，`extends BaseMapper<CartEntity>`）加唯一索引累加 SQL：

```java
@Mapper
public interface CartMapper extends BaseMapper<CartEntity> {
    @Insert("INSERT INTO cart (member_id, good_id, qty) VALUES (#{memberId}, #{goodId}, #{qty}) " +
            "ON DUPLICATE KEY UPDATE qty = qty + #{qty}")
    int insertOrUpdate(@Param("memberId") Integer memberId,
                       @Param("goodId") Integer goodId,
                       @Param("qty") Integer qty);

    @Delete("DELETE FROM cart WHERE member_id = #{memberId}")
    int deleteByMemberId(@Param("memberId") Integer memberId);
}
```

> `cart.member_id/good_id` 是 int，编码时把 `Long memberId/goodId` 转 `Integer`（或直接用 Integer 贯穿）。

**3b. `CartService` + Impl**（`com.fengluan.trade.service`）：

```java
public interface CartService extends IService<CartEntity> {
    List<CartVO> listCart(Long memberId);
    void addCart(Long memberId, CartAddRequest request);
    void updateCartQty(Long memberId, Long cartId, Integer qty);
    void removeCart(Long memberId, Long cartId);
}
```

Impl 要点（注入 `CartMapper` + `TradeProductClient`）：

- 私有 `GoodVO checkGood(Long goodId, int need)`：`productFeign.getById(goodId)` 为空或 `isTakeDown==true` → `GOOD_NOT_FOUND(2001)`；`getQty() < need` → `GOOD_STOCK_INSUFFICIENT(2002)`；返回 good。
- `addCart`：`checkGood(goodId, qty)` 后 `cartMapper.insertOrUpdate(memberId.intValue(), goodId.intValue(), qty)`。
- `updateCartQty`：`CartEntity cart = get by id + eq(member_id)`（查不到→ `CART_EMPTY`? 建议 `throw BusinessException(GOOD_NOT_FOUND)` 或新增购物车项不存在，先用 `ORDER_NOT_FOUND` 占位无明显更好 → 用 `BusinessException(BAD_REQUEST)` 简单表达"购物车项不存在"）。`checkGood(cart.getGoodId(), qty)` 后 `cart.setQty(qty)` + `updateById`。
- `removeCart`：按 `id + member_id` 校验归属后 `deleteById`。
- `listCart`：`selectList(eq member_id)` 转 `CartVO`。

#### 步骤 D9-4：CartController implements CartApi（任务 9.3/9.4/9.5/9.6）

**4a.** `com.fengluan.trade.api.CartController`：

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/trade/api")
public class CartController implements CartApi {
    private final CartService cartService;

    @Override public List<CartVO> listCart() {
        return cartService.listCart(currentMemberId());
    }
    @Override public void addCart(@Valid @RequestBody CartAddRequest request) {
        cartService.addCart(currentMemberId(), request);
    }
    @Override public void updateCartQty(@PathVariable Long cartId, @RequestParam Integer qty) {
        cartService.updateCartQty(currentMemberId(), cartId, qty);
    }
    @Override public void removeCart(@PathVariable Long cartId) {
        cartService.removeCart(currentMemberId(), cartId);
    }
    private Long currentMemberId() {
        // @RequestHeader("X-User-Id") 或 RequestContextHolder；缺失抛 401
    }
}
```

> `Void` 返回可写作 `Void` 或 `void`；契约方法与实现用 `void` 简写亦可（实现无需 return），以 compiler 通过为准。

#### 步骤 D9-5：web 购物车聚合三件套（任务 9.8）

**5a. web pom 加 `goods-store-trade-spi`**。

**5b. Feign 客户端**（`com.fengluan.web.trade`）：

```java
@FeignClient(name = "goods-store-trade-api", path = "/trade/api")
public interface TradeCartFeignClient extends CartApi { }
```

**5c. `WebCartService`**：`list(memberId)` 先 `tradeCartFeign.listCart()`，再逐个 `productFeign.getById(item.goodId)` 用商品图/名/价补齐，组装 `CartItemVO{cartId,goodId,qty,goodName,goodPic,price}`（聚合规则 1）。

**5d. `WebCartController`**（`@RequestMapping("/app/api/cart")`，`@RequestHeader X-User-Id`）：

```
GET  /app/api/cart/list            -> ApiResult<List<CartItemVO>>（含商品图/名/价）
POST /app/api/cart/add  {goodId,qty}
PUT  /app/api/cart/{cartId}?qty=3
DELETE /app/api/cart/{cartId}
```

---

### Day 10 操作步骤

#### 步骤 D10-1：Redisson（任务 10.1 —— 用核心包 + 手写配置，避开 Boot 4 不兼容的 starter）

**1a.** 父 POM 只管理 `redisson-spring-boot-starter`，但该 starter 自动装配引用 Boot 4.1 已删除的 `RedisProperties`，运行期报 `Could not find class RedisProperties`。故 `trade-api/pom` 引核心包并显式锁定版本：

```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson</artifactId>
    <version>${redisson.version}</version>
</dependency>
```

**1b.** 手写 `com.fengluan.trade.config.RedissonConfig`（读 `spring.data.redis.host/port`）构建单节点客户端：

```java
@Configuration
public class RedissonConfig {
    @Value("${spring.data.redis.host}") private String host;
    @Value("${spring.data.redis.port}") private int port;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://" + host + ":" + port)
                .setConnectionPoolSize(10).setConnectionMinimumIdleSize(5);
        return Redisson.create(config);
    }
}
```

> `spring.data.redis.*` 取自共享 nacos 配置；若本地暂缺，在 `trade-api/application.yaml` 补 `spring.data.redis.host/port`。

#### 步骤 D10-2：Mapper + OrderMessageProducer + OrderService（任务 10.1/10.3/10.4/10.5/10.6）

**2a. Mapper**（`com.fengluan.trade.repository`）：`OrderMapper`、`OrderItemMapper`（`extends BaseMapper`，无需自定义 SQL；列表/分页 Day 11）。

**2b. `OrderMessageProducer`**（`com.fengluan.trade.mq`）：注入 `RabbitTemplate` + `RabbitMqConfig` 常量：

```java
@Component @RequiredArgsConstructor @Slf4j
public class OrderMessageProducer {
    private final RabbitTemplate rabbitTemplate;
    // 1. 订单创建（含明细，供异步扣库存）
    public void sendOrderCreate(String orderNo, Long memberId, List<CartEntity> carts) {
        List<OrderCreateMessage.Item> items = carts.stream().map(c ->
            OrderCreateMessage.Item.builder().goodId(c.getGoodId().longValue())
                .count(c.getQty()).build()).toList();
        OrderCreateMessage msg = OrderCreateMessage.builder().orderNo(orderNo)
                .memberId(memberId).items(items).build();
        rabbitTemplate.convertAndSend(RabbitMqConfig.ORDER_EXCHANGE, RabbitMqConfig.ORDER_CREATE_KEY, msg);
    }
    // 2. 延迟消息（30min 未支付超时取消）
    public void sendOrderTimeout(String orderNo) {
        rabbitTemplate.convertAndSend(RabbitMqConfig.ORDER_EXCHANGE, RabbitMqConfig.ORDER_TIMEOUT_KEY, orderNo);
    }
}
```

**2c. `OrderService` + Impl**（`com.fengluan.trade.service`）：

```java
public interface OrderService extends IService<OrderEntity> {
    OrderCreateResponse createOrder(Long memberId, OrderCreateRequest request);
}
```

Impl（注入 `CartMapper`/`OrderMapper`/`OrderItemMapper`/`TradeProductClient`/`TradeMemberClient`/`RedissonClient`/`OrderMessageProducer`/`SnowflakeUtil`）：

```java
@Override
public OrderCreateResponse createOrder(Long memberId, OrderCreateRequest request) {
    List<CartEntity> carts = cartMapper.selectList(new LambdaQueryWrapper<CartEntity>()
            .eq(CartEntity::getMemberId, memberId));
    if (carts.isEmpty()) throw new BusinessException(ErrorCode.CART_EMPTY);

    String orderNo = snowflakeUtil.nectIdStr();
    RLock lock = redissonClient.getLock("lock:order:" + memberId);
    try {
        if (!lock.tryLock(10, 30, TimeUnit.SECONDS)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "系统繁忙，请稍后重试");
        }
        // 1. 校验库存/下架 + 汇总
        BigDecimal totalPay = BigDecimal.ZERO;
        List<OrderItemEntity> items = new ArrayList<>();
        for (CartEntity c : carts) {
            GoodVO g = productFeign.getById(c.getGoodId().longValue());
            if (g == null || Boolean.TRUE.equals(g.getIsTakeDown())) throw new BusinessException(ErrorCode.GOOD_NOT_FOUND);
            if (g.getQty() < c.getQty()) throw new BusinessException(ErrorCode.GOOD_STOCK_INSUFFICIENT);
            totalPay = totalPay.add(g.getPrice().multiply(BigDecimal.valueOf(c.getQty())));
            OrderItemEntity oi = new OrderItemEntity();
            oi.setGoodId(c.getGoodId()); oi.setCount(c.getQty());
            oi.setDealPrice(g.getPrice()); oi.setGoodName(g.getName()); oi.setGoodPic(g.getPic()); oi.setGoodDesc(g.getSummary());
            items.add(oi);
        }
        // 2. 收货地址：request.addressId ?? 默认
        MemberAddressVO addr = request.getAddressId() != null
            ? memberFeign.getDefaultAddress(memberId)          // 简化：优先默认；addressId 精确查询可留 Day11
            : memberFeign.getDefaultAddress(memberId);
        if (addr == null) throw new BusinessException("请先设置收货地址", 400);

        // 3. 会员账号（order.member_account）
        String account = memberFeign.getProfile(memberId).getAccount();

        // 4. 事务内写订单 + 明细
        OrderEntity order = buildOrder(orderNo, memberId, account, totalPay, addr, request.getComment());
        insertOrderAndItems(order, items);                      // 见下（事务提交后发 MQ）

        return new OrderCreateResponse(orderNo, totalPay, order.getStatus());
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new BusinessException(ErrorCode.INTERNAL_ERROR);
    } finally {
        if (lock.isHeldByCurrentThread()) lock.unlock();
    }
}
```

> **事务 + MQ 顺序**：`insertOrderAndItems` 用 `@Transactional`（传播 REQUIRED）写 `order`+`order_item`，并在方法内用 `TransactionSynchronizationManager.registerSynchronization(afterCommit)` 里调用 `orderMessageProducer.sendOrderCreate(...)` + `sendOrderTimeout(orderNo)`，保证提交后才发消息。再于外层 `createOrder` 收尾 `cartMapper.deleteByMemberId(memberId.intValue())` 清空购物车。
> **地址精确查询**：为避免新增不必要契约，`addressId` 先简化为「非空也走默认地址」；如需按 id 取地址，可后续在 `MemberApi` 追加（Day 建议备注）。以编译通过、功能可用为先。

**2d. `buildOrder` 要点**：`order.setOrderNo(orderNo)`、`setMemberAccount(account)`、`setTotalPay(totalPay)`、`setReceiverName/Phone/AddrDetail(addr)`、`setReceiverAddrId(addr.getId())`、`setStatus("10")`（待付款）、`setCheckoutTime(now)`、`setIsDel(false)`、`setCreatedTime/UpdatedTime(now)`。

#### 步骤 D10-3：OrderController implements OrderApi（任务 10.2）

**3a.** `com.fengluan.trade.api.OrderController`：`@RequestMapping("/trade/api") implements OrderApi`，`createOrder` 取 `currentMemberId()` 后委托 `orderService.createOrder(memberId, request)`。

#### 步骤 D10-4：web 下单聚合（任务 10.7）

**4a.** `TradeOrderFeignClient`（`com.fengluan.web.trade`）：`@FeignClient(name="goods-store-trade-api", path="/trade/api") extends OrderApi`。

**4b. `WebOrderService`**：`submit(memberId, request)` → `tradeOrderFeign.createOrder(request)` 返回 `OrderCreateResponse`（编排：前端已先调购物车接口，此处只调下单生成订单号，聚合规则 2）。

**4c. `WebOrderController`**：`@RequestMapping("/app/api/order")`，`POST /submit`：入参 `@RequestHeader X-User-Id` + `OrderCreateRequest`，返回 `ApiResult<OrderCreateResponse>`。

---

## 五、验收标准

### Day 9 验收

- [ ] `trade-spi` 的 `CartApi`/`OrderApi` **无** `@FeignClient`；pom 只依赖 spring-web + validation
- [ ] `trade-api` 启动成功（端口 8086），`@EnableFeignClients` 扫描 `trade.remote` 客户端
- [ ] RabbitMQ 控制台可见 `goods.order.*` 各 Queue（含 TTL 超时队）与 Binding
- [ ] `POST /trade/api/cart` 添加购物车；同一 `(member_id,good_id)` 再添加数量**累加**（`uq_member_good` 生效）
- [ ] `GET /trade/api/cart` 返回当前会员购物车列表；跨用户不可见（按 `member_id` 隔离）
- [ ] `PUT /trade/api/cart/{id}?qty=` 超过库存返回 `2002`
- [ ] `DELETE /trade/api/cart/{id}` 删除成功；`X-User-Id` 与归属不符时拒绝
- [ ] 下架商品添加购物车返回 `2001`
- [ ] **契约消费**：trade 校验库存用 `TradeProductClient extends ProductApi`，不手写路径
- [ ] **web 收口**：`GET /app/api/cart/list` 经 web 聚合返回购物车 + 商品图/名/价

### Day 10 验收

- [ ] `POST /trade/api/order` 下单成功返回 `{orderNo,totalPay}`；`order` 与 `order_item` 同时写入（事务）
- [ ] 下单后购物车自动清空（`cart` 对应 `member_id` 行删除）
- [ ] 库存不足返回 `2002`；未设收货地址返回提示
- [ ] 同一会员并发重复请求，`lock:order:{memberId}` 分布式锁仅放行一单
- [ ] `order.order_no` 为雪花串且唯一（`uq_order_no`）
- [ ] RabbitMQ 控制台可见：1 条订单创建消息（路由 `order.create`）+ 1 条延迟消息（路由 `order.timeout`）
- [ ] **web 收口**：`POST /app/api/order/submit` 经 `WebOrderService` 完成下单并返回订单号，前端不直连 trade-api

---

## 六、端口分配表

| 服务                    | 端口 | 说明                          |
| ----------------------- | ---- | ----------------------------- |
| goods-store-gateway     | 8888 | 网关，前端唯一入口            |
| goods-store-web         | 8090 | BFF 聚合层                    |
| goods-store-trade-api   | 8086 | 交易服务（购物车/下单，现状） |
| goods-store-product-api | 8084 | 商品服务                      |
| goods-store-member-api  | 8085 | 会员服务                      |
| goods-store-brand-api   | 8081 | 品牌服务                      |
| goods-store-auth-api    | 8083 | 认证服务                      |
| RabbitMQ                | 5672 | 消息中间件（管理台 15672）    |
| Redis                   | 6379 | 缓存/Redisson 锁              |
| Nacos                   | 8848 | 配置中心 / 服务注册           |
| Sentinel Dashboard      | 8080 | 限流控制台                    |

---

## 七、常见问题 / 回滚

| 现象                                             | 处理                                                                                                                                |
| ------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------- |
| spi 出现 `@FeignClient`/`ApiResult`/mybatis 类型 | spi 只依赖 spring-web+validation；返回 `Void`/VO/`List`；Feign 客户端放 api 模块 `remote` 包 `extends` 契约                         |
| `trade-spi` 旧 `TradeApi` 被引用报错             | 已拆 `CartApi`/`OrderApi`，确保实现/消费端改用新契约                                                                                |
| trade `@EnableFeignClients` 未扫到客户端         | 加 `basePackages="com.fengluan.trade.remote"`；客户端须在该包且 `@FeignClient name/path` 与网关路由一致                             |
| 调用 member `getProfile` 报 401 越权             | trade Feign 客户端加 `X-User-Id` 透传拦截器（见 D9-2b）；保证当前用户 == 目标会员 id                                                |
| 购物车重复添加不累加反而报唯一键冲突             | 用 `INSERT ... ON DUPLICATE KEY UPDATE`（见 [CartMapper](D9-3a)），勿用先查后更                                                     |
| `order.member_account` 为空                      | 下单时 `memberFeign.getProfile(memberId).getAccount()` 取账号                                                                       |
| MQ 消息没发 / 触发回滚后仍发消息                 | 用 `TransactionSynchronization.afterCommit` 发消息（D10-2c），勿在事务中直接发                                                      |
| 延迟取消不生效                                   | 确认 `orderTimeoutQueue` 是 `ttl(30*60*1000)+deadLetterExchange+deadLetterRoutingKey(ORDER_CANCEL_KEY)`                             |
| Redisson 启动连不上                              | 确认 `spring.data.redis.host/port`（nacos 或本地 yaml）；`redis://host:port` 前缀不可省略                                           |
| 库存被买超（扣减晚于下单）                       | Day 9 校验用 `getById.qty>=`；真正防超卖靠 Day 11 MQ 消费者调 `ProductApi.deductStock`（乐观锁 `WHERE qty>=`）                      |
| 回滚                                             | 还原 trade-spi pom/TradeApi→CartApi+OrderApi/trade-api pom 与新增类/`RabbitMqConfig`/`RedissonConfig`/web trade 三件套/网关路由即可 |
