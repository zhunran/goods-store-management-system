# Day 11-12 操作执行手册 — 交易服务（下）：MQ 消费者 + 订单管理 + web 聚合；秒杀服务（上）：spi 契约 + 活动管理 + 库存预热

> 对应计划：[14-day-implementation-plan.md](file:///d:/.workspace/javaproject/goods-store-management-system-parent/doc/schedule/14-day-implementation-plan.md) Day 11（交易·下：MQ 消费者 + 超时取消 + 订单管理）/ Day 12（秒杀·上：活动管理 + 库存预热）
> 版本：v1.0　编写日期：2026-09-02　执行日期：待填
> 主题：Day11 订单列表/详情/取消/确认 + OrderStatus 枚举 + 库存扣减/超时取消两个 MQ 消费者 + web 订单管理聚合；Day12 seckill-spi 重构纯契约 + seckill-api 全栈 CRUD + Redis 库存预热 + web 秒杀浏览聚合
> 依赖：Day 9-10 已完成（购物车 + RabbitMQ 配置 + 下单 + Redisson 锁 + web 下单聚合），编译通过；trade 已连上 RabbitMQ/Redis
> **架构前提**：遵循 14 天计划「分层架构规范」——spi 纯 HTTP 契约（禁 `@FeignClient`）、controller `implements` 契约、消费方 Feign `extends` 契约、web 是 BFF 聚合层（前端唯一入口 `/app/api/**`）、spi 不依赖 common（不用 `ApiResult`/mybatis 类型）。

> ⚠️ **本手册以「当前已落地架构」为准，对计划书 Day 11/12 示例做如下修正**：
>
> 1. **计划书目用 `ApiResult<Boolean> result = productApi.deductStock(...)`** → 现况 `ProductApi.deductStock` 返回纯 JDK `Boolean`（[ProductApi.java](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-spi/goods-store-product-spi/src/main/java/com/fengluan/spi/product/ProductApi.java)），service/mq 不注入 spi 接口，而注入实现方自己的 `TradeProductClient`（`extends ProductApi`）；
> 2. **计划书目接注入 `ProductApi productApi`** → 一律注入 `com.fengluan.trade.remote.TradeProductClient`；
> 3. **计划书消费者用 `message.getOrderId()` 查订单** → 现况 `OrderCancelMessage` **无 orderId**，只有 `orderNo/reason/items`（[OrderCancelMessage.java](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-common/src/main/java/com/fengluan/common/mq/OrderCancelMessage.java)），且超时消息实际发的是 **裸 orderNo 字符串**（见 `OrderMessageProducer.sendOrderTimeout`）；统一改为按 `orderNo` 查单；
> 4. **当前会员/归属**：trade order 相关操作一律经 `CurrentUserUtil.currentUserId()` 取自入站 `X-User-Id`（网关 web 透传），不做越权；取消/确认需校验订单归属 `order.memberAccount==当前会员账号`；
> 5. **秒杀 spi 是旧 `@FeignClient` 版**（[SeckillApi.java](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-spi/goods-store-seckill-spi/src/main/java/com/fengluan/spi/seckill/SeckillApi.java)），且 `seckill-spi` pom 仍依赖 openfeign → Day12 第一步重构为纯契约（拆 `SeckillActivityApi` + `SeckillGoodApi`），去 openfeign（同 member-spi/trade-spi 先例）；
> 6. **商品下架判断**：`GoodVO` **无 `isTakeDown` 字段，只有 `isDel`**（[GoodVO.java](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-spi/goods-store-product-spi/src/main/java/com/fengluan/spi/product/vo/GoodVO.java)）；秒杀商品查询过滤用 `g.is_del = 0`（而非计划的 `is_take_down`）；
> 7. **秒杀启动类缺 Bean 扫描**：`SeckillApiApplication` 在 `com.fengluan.seckill`，需加 `@SpringBootApplication(scanBasePackages = "com.fengluan")`（否则扫不到 common 的 `SnowflakeUtil`），并加 `@EnableFeignClients(basePackages = "com.fengluan.seckill.remote")`；
> 8. **Redisson 兼容坑**：必须用核心包 `org.redisson:redisson` + 手写 `RedissonConfig`（不要用 `redisson-spring-boot-starter`，Boot 4.1 下会报 `RedisProperties` 缺失）——照抄 trade-api 现成写法；
> 9. **分页返回**：spi 不使用 common 的 `PageRecord`/`ApiResult`；订单分页在 trade-spi 内新增通用 `PageVO<T>`（records/total/pageNum/pageSize），web 列表收口直接透传。

---

## 一、Day 11-12 目标与产出物

### Day 11 目标

1. **OrderApi 契约扩展**：从仅 `createOrder`（[OrderApi.java](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-spi/goods-store-trade-spi/src/main/java/com/fengluan/spi/trade/OrderApi.java)）扩为 page/detail/cancel/confirm，controller implements、service 补齐方法
2. **订单状态机**：新建 `OrderStatus` 枚举（5 状态），取消仅待付款、确认收货仅已发货；`ORDER_STATUS_ERROR(3002)` 拦截非法流转
3. **MQ 消费者闭环**：`OrderCreateConsumer`（幂等扣库存）+ `OrderCancelConsumer`（超时取消失败回滚/恢复库存），手动 ack/nack
4. **web 订单管理聚合**：`WebOrderMgrController/WebOrderMgrService` 收口 `/app/api/order/page|detail|cancel|confirm`（磨平分页/状态，聚合规则 3）

### Day 12 目标

1. **seckill-spi 重构纯契约**：拆 `SeckillActivityApi`(活动 CRUD) + `SeckillGoodApi`(商品关联 + 用户端列表 + 抢购预留)，去 openfeign
2. **seckill-api 全栈**：pom 补依赖、启动类扫描修复、`SeckillController/SeckillActivityService` 活动 CRUD、`SeckillGoodController/SeckillGoodService` 商品关联、用户端有效活动列表、`StockPreheatJob` 库存预热、`RedisConfig`+`RedissonConfig`
3. **web 秒杀浏览聚合**：`WebSeckillController/WebSeckillService/SeckillFeignClient` 收口 `/app/api/seckill/list`（补活动窗口状态/倒计时）

### 产出物清单

| 模块        | Day11 产出物                                                                                                                                                                                                           | Day12 产出物                                                                                                                                                                                                                                                                                                                                   |
| ----------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| trade-spi   | OrderApi 加 4 方法；`PageVO<T>`；`OrderVO` 扩展；可选 `OrderDetailVO`                                                                                                                                                  | —                                                                                                                                                                                                                                                                                                                                              |
| trade-api   | `enums/OrderStatus`；`OrderMapper.existsByOrderNo`；`OrderItemMapper.selectByOrderId`；`OrderService+Impl` 加 page/detail/cancel/confirm；`OrderController` 补方法；`mq/OrderCreateConsumer`；`mq/OrderCancelConsumer` | —                                                                                                                                                                                                                                                                                                                                              |
| seckill-spi | —                                                                                                                                                                                                                      | 重构旧 SeckillApi → `SeckillActivityApi` + `SeckillGoodApi`；新增 `SeckillActivityRequest/SeckillGoodAddRequest/SeckillGoodVO/SeckillActivityVO`(可复用)；pom 去 openfeign                                                                                                                                                                     |
| seckill-api | —                                                                                                                                                                                                                      | pom 补 SPI/openfeign/amqp/redis/redisson/mybatis-plus-spring；`SeckillActivityService/SeckillGoodService(+Impl)`；`SeckillController/SeckillGoodController`；`SeckillMapper/SeckillGoodMapper(+selectActiveSeckillGoods)`；`config/RedisConfig`；`config/RedissonConfig`；`job/StockPreheatJob`；`remote/SeckillProductClient`；启动类扫描修复 |
| web         | `WebOrderMgrController` + `WebOrderMgrService`（复用 `TradeOrderFeignClient`，页面级）                                                                                                                                 | `SeckillFeignClient`(extends) + `WebSeckillController` + `WebSeckillService`                                                                                                                                                                                                                                                                   |
| gateway     | 路由 `/trade/api/**`、`/app/api/**` 已有，无需改动                                                                                                                                                                     | 路由 `/seckill/api/**` **已存在**（[application.yaml](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-gateway/src/main/resources/application.yaml#L64-L67)），无需改动；白名单按需                                                                                                                          |

---

## 二、技术要点

### Day 11 技术要点

| 要点             | 说明                                                                                                                                                                                              |
| ---------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 订单状态机       | `OrderStatus`：`PENDING("10") PAID("20") SHIPPED("30") COMPLETED("40") CANCELLED("50")`；Cancel 仅 `PENDING→CANCELLED`，Confirm 仅 `SHIPPED→COMPLETED`，否则 `ORDER_STATUS_ERROR(3002)`           |
| 归属校验         | cancel/confirm/detail 先按 `orderNo`(或 id) 查单，断言 `order.memberAccount.equals(currentUserAccount)`，否 则抛 `ORDER_NOT_FOUND(3001)`（避免越权）                                              |
| 当前会员账号     | trade 内 `CurrentUserUtil.currentUserId()` → `memberClient.getProfile(memberId).getAccount()`（`getProfile` 越权，需 X-User-Id 头，Feign 拦截器已透传）                                           |
| 列表分页         | `orderMapper.selectPage(new Page<>(pageNum,pageSize), wrapper.eq(member_account, account))`；`selectPage` 在 IService 已提供，cmp 包 `com.baomidou.mybatisplus.extension.plugins.pagination.Page` |
| MQ 幂等          | `OrderCreateConsumer` 先 `existsByOrderNo` 判重，已处理直接 ack；扣库存用 `TradeProductClient.deductStock(goodId,count)`（返回 `Boolean`）                                                        |
| MQ 手动确认      | `@RabbitListener` 方法带 `Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag`；成功 `basicAck(tag,false)`；异常 `basicNack(tag,false,true)` 重回队列                                     |
| 超时取消链路     | 消息实际为**裸 orderNo 字符串**（来自 `sendOrderTimeout`）；到 30min TTL 死信转 `ORDER_CANCEL_KEY` → `OrderCancelConsumer` 按 orderNo 查单 → PENDING 才置 CANCELLED + 恢复库存                    |
| 库存恢复         | 取消/超时取消时 `orderItemMapper.selectByOrderId(orderId)` → 逐项 `TradeProductClient.restoreStock(goodId,count)`                                                                                 |
| 契约路径（交易） | 控制器类级前缀 `/trade/api`；相对路径 `/order/page`、`/order/{id}`、`/order/{id}/cancel`、`/order/{id}/confirm`                                                                                   |
| web 聚合         | 新增 `WebOrderMgr*` 走页面级管理（当前会员），复用 `TradeOrderFeignClient`（它已 `extends OrderApi`，contextId=tradeOrderFeignClient）；不用新建 Feign                                            |

> 错误码：交易段现已有 `ORDER_NOT_FOUND(3001)/ORDER_STATUS_ERROR(3002)/CART_EMPTY(3003)`（[ErrorCode.java](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-common/src/main/java/com/fengluan/common/exception/ErrorCode.java#L30-L33)），Day11 直接复用，不新增。

### Day 12 技术要点

| 要点              | 说明                                                                                                                                                                                     |
| ----------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 秒杀 spi 契约     | 拆 `SeckillActivityApi`(活动 CRUD `/activity`) + `SeckillGoodApi`(商品 `/active/{seckillId}/goods` + 用户端列表 `/list` + 抢购 `/buy` Day13 预留)；类级前缀 `/seckill/api`；去 openfeign |
| seckill-api 启动  | `scanBasePackages="com.fengluan"` + `@EnableFeignClients(basePackages="com.fengluan.seckill.remote")`                                                                                    |
| 秒杀活动窗口      | `enabled=true` 且 `start_time <= now <= end_time` 视为有效；用户端列表仅返回有效活动内、且 `good.is_del=0` 的秒杀商品                                                                    |
| seckill_good 唯一 | `uq_seckill_good(seckill_id,good_id)`；添加商品前按活动+商品去重（`count` 校验幂等）                                                                                                     |
| 库存预热          | `@Scheduled(cron="0 * * * * ?")` 每分钟扫 5min 内将开始的活动，写入 `seckill:stock:{seckillGoodId}`（值取 `good.getQty()`），TTL=活动结束+1h，`SetterIfAbsent` 防重复预热                |
| Redis 配置        | `StringRedisTemplate`（Boot 自动装配即可，无需手动 Bean）；预热用 `opsForValue().setIfAbsent()`；Lua 脚本常量类放 `config/RedisConfig`（Day13 抢购用）                                   |
| Redisson 配置     | 核心包 + 手写 `RedissonConfig`，照抄 trade-api（读 `spring.data.redis.host/port`，`useSingleServer`）                                                                                    |
| 商品下架判断      | `GoodVO.isDel`（无 isTakeDown）；秒杀商品 SQL join `good g ON ... WHERE g.is_del = 0`                                                                                                    |
| 契约返回          | 秒杀 DTO/VO 用 spi 内类型，不用 `ApiResult`/mybatis；分页活动复用 `PageVO<T>`（从 trade-spi 复制到 seckill-spi 或提升到各 spi 独立定义）                                                 |

> 错误码：秒杀段现有 `SECKILL_NOT_FOUND(4001)/SECKILL_NOT_STARTED(4002)/SECKILL_ENDED(4003)/SECKILL_STOCK_EMPTY(4004)`，Day12 活动 CRUD 校验用 `4001`。

---

## 三、Day 11-12 实施状态盘点

> 基于当前工程实测（Day 9-10 完成，trade 编译/启动通过）。

### Day 11 现状

| 计划任务  | 内容                                 | 当前状态                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      | 手册步骤    |
| --------- | ------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------- |
| 11.1-11.4 | 订单 page/detail/cancel/confirm      | ⚠️ [OrderApi](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-spi/goods-store-trade-spi/src/main/java/com/fengluan/spi/trade/OrderApi.java) 仅 `createOrder`；[OrderController](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-service/goods-store-trade-api/src/main/java/com/fengluan/trade/api/OrderController.java) 仅实现 createOrder；[OrderService](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-service/goods-store-trade-api/src/main/java/com/fengluan/trade/service/OrderService.java) 仅 createOrder | D11-1/D11-2 |
| 11.8      | OrderStatus 枚举                     | ❌ 无；[OrderServiceImpl](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-service/goods-store-trade-api/src/main/java/com/fengluan/trade/service/impl/OrderServiceImpl.java) 硬编码 `"10"`                                                                                                                                                                                                                                                                                                                                                                                                 | D11-2       |
| 11.5      | OrderCreateConsumer（扣库存）        | ❌ 无；基础已备：`RabbitMqConfig` 全常量、`OrderMessageProducer`、`OrderCreateMessage`、`TradeProductClient.deductStock`、`OrderMapper`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       | D11-3       |
| 11.6-11.7 | OrderCancelConsumer（超时取消+恢复） | ❌ 无；`OrderMessageProducer.sendOrderTimeout` 已发裸 orderNo；`OrderCancelMessage` 有 orderNo/reason/items（不含 orderId）                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   | D11-4       |
| 11.9      | web 订单管理聚合                     | ⚠️ 已有 `TradeOrderFeignClient`(extends OrderApi)/`WebOrderService`/`WebOrderController(/app/api/order/submit)`；缺管理列表/详情/取消/确认入口                                                                                                                                                                                                                                                                                                                                                                                                                                                                                | D11-5       |

**Day11 现状基石**（已落地，直接复用）：

- `ProductApi` 含 `getById`/`deductStock(id,count)->Boolean`/`restoreStock(id,count)`（[ProductApi.java](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-spi/goods-store-product-spi/src/main/java/com/fengluan/spi/product/ProductApi.java)）；trade 内 `TradeProductClient extends ProductApi`（[remote](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-service/goods-store-trade-api/src/main/java/com/fengluan/trade/remote)）
- `TradeMemberClient` has `getProfile(memberId)`（越权）/`getDefaultAddress(memberId)`；OrderServiceImpl 已注入 `productClient/memberClient/orderMapper/orderItemMapper/snowflakeUtil`
- `OrderMessageProducer`：`sendOrderCreate(orderNo,memberId,List<CartEntity>)` → OrderCreateMessage；`sendOrderTimeout(orderNo)` → 裸字符串
- `RabbitMqConfig`：`ORDER_CREATE_QUEUE/KEY`、`ORDER_TIMEOUT_QUEUE(30min TTL)`、`ORDER_CANCEL_QUEUE/KEY`、`SECKILL_ORDER_QUEUE/KEY` 已全部声明
- `common`：`OrderCreateMessage`(orderNo/memberId/items[Item:goodId/skuId/count])、`OrderCancelMessage`(orderNo/reason/items)
- web `TradeOrderFeignClient extends OrderApi` contextId=tradeOrderFeignClient；`OrderVO` 已含 status/totalPay/checkoutTime 等

### Day 12 现状

| 计划任务  | 内容                        | 当前状态                                                                                                                                                     | 手册步骤          |
| --------- | --------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------ | ----------------- |
| 12.1      | seckill-api POM 补依赖      | ⚠️ [pom](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-service/goods-store-seckill-api/pom.xml) 仅 `goods-store-common` | D12-1             |
| 12.2-12.4 | 活动/商品 CRUD + 用户端列表 | ⚠️ 仅实体 `SeckillEntity`/`SeckillGoodEntity`；无 controller/service/mapper                                                                                  | D12-2/D12-3/D12-4 |
| 12.5      | StockPreheatJob 预热        | ❌ 无                                                                                                                                                        | D12-5             |
| 12.6/12.7 | Redis/Redisson 配置         | ❌ 无；`application.yaml` 端口 8087、snowflake worker-id=6、nacos 共享含 rabbitmq/redis                                                                      | D12-5             |
| 12.8      | web 秒杀浏览聚合            | ❌ web 无 seckill-spi / SeckillFeignClient                                                                                                                   | D12-6             |
| spi重构   | seckill-spi 纯契约          | ⚠️ 旧 `@FeignClient(name="goods-store-seckill-api",path="/seckill")` 仅 `GET /list`；pom 有 openfeign；`SeckillVO/SeckillQueryRequest` 在 spi                | D12-0             |

**Day12 现状基石**：

- `seckill` 表：`id/name/enabled(bit1)/start_time/end_time/description/created_time/created_by/updated_time/updated_by`（[sql](file:///d:/.workspace/javaproject/goods-store-management-system-parent/sql/table_structure_export.sql#L258-L270)）
- `seckill_good` 表：`id/seckill_id/good_id/description`，唯一 `uq_seckill_good(seckill_id,good_id)`（[sql](file:///d:/.workspace/javaproject/goods-store-management-system-parent/sql/table_structure_export.sql#L276-L283)）
- `SeckillEntity`/`SeckillGoodEntity` 已建，与表对应
- 网关路由 `/seckill/api/**` 已存在
- `SECKILL_*` 错误码已在 ErrorCode
- `GoodVO.isDel` 用于下架判断；seckill 预热取 `good.getQty()`
- trade-api 的 `RedissonConfig`/依赖/`@EnableFeignClients` 写法可整体照抄

**环境前置检查**：trade 需 RabbitMQ+Redis 已在跑；seckill-api 也走 nacos 共享配置（rabbitmq/redis）；执行前确认 redis-cli 可用、RabbitMQ management 正常。

---

## 四、详细操作步骤

### 前置约定

- **spi 纯净**：trade-spi/seckill-spi 用 `spring-web`+`jakarta.validation-api`，**不要** openfeign；方法返回 `void/Void/VO/List/PageVO`，不返回 `ApiResult`/mybatis 类型。
- **分页结构**：trade-spi 新建 `PageVO<T>`（records/total/pageNum/pageSize），seckill-spi 同样定义独立 `PageVO<T>`。web 端 API 返回统一包 `ApiResult`（web 属于服务端，允许用 common）。
- **当前会员**：trade 内一律 `CurrentUserUtil.currentUserId()`；service 以 `(Long memberId,...)` 参数贯穿，account 经 `memberClient.getProfile(memberId).getAccount()` 获取。
- **MQ 消费者包**：放 `com.fengluan.trade.mq`；`@EnableRabbit` 已由 starter 默认开启，无需重配。
- **实体字段**：`OrderItemEntity.goodId`/`orderId` 是 Integer；`OrderCreateMessage.Item.goodId` 是 Long（取货时 `.longValue()`）。

---

### Day 11 操作步骤

#### D11-1　trade-spi：OrderApi 契约扩展 + PageVO + OrderVO 增强

**文件1：新建** `goods-store-trade-spi/src/main/java/com/fengluan/spi/trade/dto/PageVO.java`

```java
package com.fengluan.spi.trade.dto;

import lombok.Data;

import java.util.List;

/** 通用分页返回（spi 自建，不依赖 common） */
@Data
public class PageVO<T> {
    private Long total;
    private Long pageNum;
    private Long pageSize;
    private List<T> records;
}
```

**文件2：修改** `goods-store-spi/../trade/OrderApi.java`（追 4 个方法）

```java
    /** 订单分页（当前会员） */
    @GetMapping("/order/page")
    PageVO<OrderVO> page(@RequestParam Long pageNum,
                         @RequestParam Long pageSize,
                         @RequestParam(required = false) String status);

    /** 订单详情（含明细） */
    @GetMapping("/order/{id}")
    OrderDetailVO detail(@PathVariable Long id);

    /** 取消订单（仅待付款） */
    @PutMapping("/order/{id}/cancel")
    Void cancel(@PathVariable Long id);

    /** 确认收货（仅已发货） */
    @PutMapping("/order/{id}/confirm")
    Void confirm(@PathVariable Long id);
```

> 对应 import 增加：`GetMapping/PutMapping/PathVariable/RequestParam` 及 `OrderDetailVO`。

**文件3：修改** `OrderVO.java`（补 `Integer status` 已有，仅作展示字段；新增内部可选）——**可选**，现有 `OrderVO` 已够承载列表，无需强改。

**文件4：新建** `goods-store-trade-spi/../vo/OrderDetailVO.java`

```java
package com.fengluan.spi.trade.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDetailVO {
    private String orderNo;
    private String memberAccount;
    private BigDecimal totalPay;
    private String payType;
    private Integer status;      // 建议返回字符串便于前端，见下方备注
    private LocalDateTime checkoutTime;
    private LocalDateTime payTime;
    private LocalDateTime shipTime;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddrDetail;
    private List<OrderItemVO> items;
}
```

**D11-1 备注**：`OrderVO.status` 当前为 `String`（值 "10"），建议 seckill 无关；为兼容商品列表/下单已有 status=String，**详情 VO 的 status 用 String**（把上面 `Integer status` 改为 `String status`），并与 `OrderVO` 对齐，避免前端混用。

#### D11-2　trade-api：OrderStatus 枚举 + OrderService 补方法 + OrderController 补实现

**文件1：新建** `trade-api/../enums/OrderStatus.java`

```java
package com.fengluan.trade.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 订单状态：待付款10 已支付20 已发货30 已完成40 已取消50 */
@Getter
@AllArgsConstructor
public enum OrderStatus {
    PENDING("10", "待付款"),
    PAID("20", "已支付"),
    SHIPPED("30", "已发货"),
    COMPLETED("40", "已完成"),
    CANCELLED("50", "已取消");
    private final String code;
    private final String desc;

    public static OrderStatus of(String code) {
        for (OrderStatus s : values()) if (s.code.equals(code)) return s;
        throw new IllegalArgumentException("未知订单状态: " + code);
    }
}
```

**文件2：修改** `trade-api/../service/OrderService.java`

```java
    OrderCreateResponse createOrder(Long memberId, OrderCreateRequest request);

    PageVO<OrderVO> page(Long memberId, Long pageNum, Long pageSize, String status);

    OrderDetailVO detail(Long memberId, Long id);

    void cancel(Long memberId, Long id);

    void confirm(Long memberId, Long id);
```

import 补：`com.fengluan.spi.trade.dto.PageVO; com.fengluan.spi.trade.vo.OrderDetailVO; com.fengluan.spi.trade.vo.OrderVO; java.util.List;`

**文件3：修改** `trade-api/../repository/OrderMapper.java`（幂等查询）

```java
    @Select("SELECT COUNT(1) FROM `order` WHERE order_no = #{orderNo}")
    long existsByOrderNo(@Param("orderNo") String orderNo);
```

import 补：`org.apache.ibatis.annotations.Select; org.apache.ibatis.annotations.Param;`

**文件4：修改** `trade-api/../repository/OrderItemMapper.java`

```java
    @Select("SELECT * FROM order_item WHERE order_id = #{orderId}")
    List<OrderItemEntity> selectByOrderId(@Param("orderId") Integer orderId);
```

import 补：`org.apache.ibatis.annotations.Select; org.apache.ibatis.annotations.Param; java.util.List;`

**文件5：修改** `trade-api/../service/impl/OrderServiceImpl.java`（追加实现 + 私有工具方法）

```java
    @Override
    public PageVO<OrderVO> page(Long memberId, Long pageNum, Long pageSize, String status) {
        String account = memberClient.getProfile(memberId).getAccount();
        Page<OrderEntity> p = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        LambdaQueryWrapper<OrderEntity> w = new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getMemberAccount, account)
                .eq(status != null && !status.isBlank(), OrderEntity::getStatus, status)
                .orderByDesc(OrderEntity::getId);
        Page<OrderEntity> result = orderMapper.selectPage(p, w);
        PageVO<OrderVO> vo = new PageVO<>();
        vo.setTotal(result.getTotal());
        vo.setPageNum(p.getCurrent());
        vo.setPageSize(p.getSize());
        vo.setRecords(result.getRecords().stream().map(this::toOrderVO).toList());
        return vo;
    }

    @Override
    public OrderDetailVO detail(Long memberId, Long id) {
        String account = memberClient.getProfile(memberId).getAccount();
        OrderEntity order = orderMapper.selectById(id);
        if (order == null || !order.getMemberAccount().equals(account)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        List<OrderItemEntity> items = orderItemMapper.selectByOrderId(order.getId().intValue());
        OrderDetailVO d = new OrderDetailVO();
        d.setOrderNo(order.getOrderNo());
        d.setMemberAccount(order.getMemberAccount());
        d.setTotalPay(order.getTotalPay());
        d.setPayType(order.getPayType());
        d.setStatus(order.getStatus());
        d.setCheckoutTime(order.getCheckoutTime());
        d.setPayTime(order.getPayTime());
        d.setShipTime(order.getShipTime());
        d.setReceiverName(order.getReceiverName());
        d.setReceiverPhone(order.getReceiverPhone());
        d.setReceiverAddrDetail(order.getReceiverAddrDetail());
        d.setItems(items == null ? List.of() : items.stream().map(it -> {
            OrderItemVO iv = new OrderItemVO();
            iv.setGoodId(it.getGoodId().longValue());
            iv.setGoodName(it.getGoodName());
            iv.setGoodPic(it.getGoodPic());
            iv.setDealPrice(it.getDealPrice());
            iv.setCount(it.getCount());
            return iv;
        }).toList());
        return d;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long memberId, Long id) {
        String account = memberClient.getProfile(memberId).getAccount();
        OrderEntity order = orderMapper.selectById(id);
        if (order == null || !order.getMemberAccount().equals(account)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!OrderStatus.PENDING.getCode().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }
        order.setStatus(OrderStatus.CANCELLED.getCode());
        orderMapper.updateById(order);
        // 恢复库存（用户主动取消同步恢复）
        List<OrderItemEntity> items = orderItemMapper.selectByOrderId(order.getId().intValue());
        if (items != null) {
            for (OrderItemEntity it : items) {
                productClient.restoreStock(it.getGoodId().longValue(), it.getCount());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long memberId, Long id) {
        String account = memberClient.getProfile(memberId).getAccount();
        OrderEntity order = orderMapper.selectById(id);
        if (order == null || !order.getMemberAccount().equals(account)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!OrderStatus.SHIPPED.getCode().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }
        order.setStatus(OrderStatus.COMPLETED.getCode());
        order.setAcceptTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    private OrderVO toOrderVO(OrderEntity o) {
        OrderVO v = new OrderVO();
        v.setId(o.getId());
        v.setOrderNo(o.getOrderNo());
        v.setMemberAccount(o.getMemberAccount());
        v.setTotalPay(o.getTotalPay());
        v.setPayType(o.getPayType());
        v.setStatus(o.getStatus());
        v.setCheckoutTime(o.getCheckoutTime());
        v.setPayTime(o.getPayTime());
        v.setShipTime(o.getShipTime());
        v.setCreatedTime(o.getCreatedTime());
        v.setUpdatedTime(o.getUpdatedTime());
        return v;
    }
```

> import 补：`com.baomidou.mybatisplus.extension.plugins.pagination.Page; com.fengluan.spi.trade.dto.PageVO; com.fengluan.spi.trade.vo.OrderDetailVO; com.fengluan.spi.trade.vo.OrderItemVO;`（`OrderItemVO` 需在 trade-spi 新建，见下）
> 注意：`selectPage` 分页需分页插件 `PaginationInnerInterceptor`！若 product/member 已配过则直接复用；**本号 trade 若未配会拿全量再内存分页（仍返回 records 但 total 不准）**。建议在 trade config 加 `MybatisPlusInterceptor` + `PaginationInnerInterceptor(DbType.MYSQL)`。

**文件6：新建** `trade-spi/../vo/OrderItemVO.java`

```java
package com.fengluan.spi.trade.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderItemVO {
    private Long goodId;
    private BigDecimal dealPrice;
    private Integer count;
    private String goodName;
    private String goodPic;
}
```

**文件7：修改** `trade-api/../api/OrderController.java`（implements 补 4 方法）

```java
    @Override
    public PageVO<OrderVO> page(@RequestParam Long pageNum,
                                @RequestParam Long pageSize,
                                @RequestParam(required = false) String status) {
        return orderService.page(CurrentUserUtil.currentUserId(), pageNum, pageSize, status);
    }

    @Override
    public OrderDetailVO detail(@PathVariable Long id) {
        return orderService.detail(CurrentUserUtil.currentUserId(), id);
    }

    @Override
    public Void cancel(@PathVariable Long id) {
        orderService.cancel(CurrentUserUtil.currentUserId(), id);
        return null;
    }

    @Override
    public Void confirm(@PathVariable Long id) {
        orderService.confirm(CurrentUserUtil.currentUserId(), id);
        return null;
    }
```

> import：`PageVO/OrderDetailVO/OrderVO/PathVariable/RequestParam`。

#### D11-3　trade-api：OrderCreateConsumer（幂等扣减库存）

**新建** `trade-api/../mq/OrderCreateConsumer.java`

```java
package com.fengluan.trade.mq;

import com.fengluan.common.mq.OrderCreateMessage;
import com.fengluan.trade.config.RabbitMqConfig;
import com.fengluan.trade.remote.TradeProductClient;
import com.fengluan.trade.repository.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

/**
 * 订单创建消息消费者：异步扣减库存（幂等）
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderCreateConsumer {

    private final OrderMapper orderMapper;
    private final TradeProductClient productClient;

    @RabbitListener(queues = RabbitMqConfig.ORDER_CREATE_QUEUE)
    public void handle(OrderCreateMessage msg, Channel channel,
                       @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws Exception {
        try {
            // 幂等：订单号已存在则跳过
            if (orderMapper.existsByOrderNo(msg.getOrderNo()) == 0) {
                log.warn("订单 {} 不存在或已处理，跳过扣库存", msg.getOrderNo());
                channel.basicAck(tag, false);
                return;
            }
            for (OrderCreateMessage.Item item : msg.getItems()) {
                Boolean ok = productClient.deductStock(item.getGoodId(), item.getCount());
                if (!Boolean.TRUE.equals(ok)) {
                    log.error("库存扣减失败 orderNo={}, goodId={}, count={}", msg.getOrderNo(), item.getGoodId(), item.getCount());
                }
            }
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("消费订单创建消息异常: {}", msg.getOrderNo(), e);
            channel.basicNack(tag, false, true); // 重回队列重试
        }
    }
}
```

> 说明：先 ack，扣库存单条失败仅记日志不重试（避免死循环）；同日 article 可把 `existsByOrderNo>0` 视为已处理直接 ack（当前实现里订单创建消息应在订单已入库后发出，因此必 >0，此分支为防御）。

#### D11-4　trade-api：OrderCancelConsumer（超时取消 + 恢复库存）

**新建** `trade-api/../mq/OrderCancelConsumer.java`

```java
package com.fengluan.trade.mq;

import com.fengluan.trade.entity.OrderEntity;
import com.fengluan.trade.entity.OrderItemEntity;
import com.fengluan.trade.enums.OrderStatus;
import com.fengluan.trade.remote.TradeProductClient;
import com.fengluan.trade.repository.OrderItemMapper;
import com.fengluan.trade.repository.OrderMapper;
import com.fengluan.trade.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.fengluan.trade.config.RabbitMqConfig;
import com.rabbitmq.client.Channel;

/**
 * 订单取消消息消费者：消费的是裸 orderNo 字符串（来自 orderTimeoutQueue 死信）
 * 仅取消仍为 PENDING 的订单，并恢复库存
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderCancelConsumer {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final TradeProductClient productClient;
    private final OrderService orderService;

    @RabbitListener(queues = RabbitMqConfig.ORDER_CANCEL_QUEUE)
    public void handle(String orderNo, Channel channel,
                       @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws Exception {
        try {
            OrderEntity order = orderMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderEntity>()
                            .eq(OrderEntity::getOrderNo, orderNo));
            if (order == null || !OrderStatus.PENDING.getCode().equals(order.getStatus())) {
                log.info("订单 {} 无需取消（不存在或非待付款）", orderNo);
                channel.basicAck(tag, false);
                return;
            }
            order.setStatus(OrderStatus.CANCELLED.getCode());
            orderMapper.updateById(order);
            List<OrderItemEntity> items = orderItemMapper.selectByOrderId(order.getId().intValue());
            if (items != null) {
                for (OrderItemEntity it : items) {
                    productClient.restoreStock(it.getGoodId().longValue(), it.getCount());
                }
            }
            log.info("订单超时自动取消: {}", orderNo);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("消费订单取消消息异常: {}", orderNo, e);
            channel.basicNack(tag, false, true);
        }
    }
}
```

> 该消费者消费的是裸 `orderNo`（`String`），**不要**按 String 之外类型声明。若后续改为 `OrderCancelMessage`（含 items），则说明用户主动取消走 MQ，可切换方法签名；当前用户主动取消已在 service 同步恢复库存，故 Timeout 消费者保持消费 orderNo。

#### D11-5　web：订单管理聚合

**文件1：新建** `web/../web/trade/WebOrderMgrController.java`

```java
package com.fengluan.web.trade;

import com.fengluan.common.result.ApiResult;
import com.fengluan.spi.trade.dto.PageVO;
import com.fengluan.spi.trade.vo.OrderDetailVO;
import com.fengluan.spi.trade.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/app/api/order")
public class WebOrderMgrController {

    private final WebOrderMgrService webOrderMgrService;

    @GetMapping("/page")
    public ApiResult<PageVO<OrderVO>> page(@RequestHeader(value = "X-User-Id", required = false) Long memberId,
                                           @RequestParam Long pageNum,
                                           @RequestParam Long pageSize,
                                           @RequestParam(required = false) String status) {
        return ApiResult.success(webOrderMgrService.page(memberId, pageNum, pageSize, status));
    }

    @GetMapping("/{id}")
    public ApiResult<OrderDetailVO> detail(@RequestHeader(value = "X-User-Id", required = false) Long memberId,
                                           @PathVariable Long id) {
        return ApiResult.success(webOrderMgrService.detail(memberId, id));
    }

    @PutMapping("/{id}/cancel")
    public ApiResult<Void> cancel(@RequestHeader(value = "X-User-Id", required = false) Long memberId,
                                  @PathVariable Long id) {
        webOrderMgrService.cancel(memberId, id);
        return ApiResult.success(null);
    }

    @PutMapping("/{id}/confirm")
    public ApiResult<Void> confirm(@RequestHeader(value = "X-User-Id", required = false) Long memberId,
                                   @PathVariable Long id) {
        webOrderMgrService.confirm(memberId, id);
        return ApiResult.success(null);
    }
}
```

**文件2：新建** `web/../web/trade/WebOrderMgrService.java`

```java
package com.fengluan.web.trade;

import com.fengluan.spi.trade.dto.PageVO;
import com.fengluan.spi.trade.vo.OrderDetailVO;
import com.fengluan.spi.trade.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 我的订单聚合（BFF）：磨平分页/状态 */
@Service
@RequiredArgsConstructor
public class WebOrderMgrService {

    private final TradeOrderFeignClient tradeOrderFeignClient;

    public PageVO<OrderVO> page(Long memberId, Long pageNum, Long pageSize, String status) {
        return tradeOrderFeignClient.page(pageNum, pageSize, status);
    }

    public OrderDetailVO detail(Long memberId, Long id) {
        return tradeOrderFeignClient.detail(id);
    }

    public void cancel(Long memberId, Long id) {
        tradeOrderFeignClient.cancel(id);
    }

    public void confirm(Long memberId, Long id) {
        tradeOrderFeignClient.confirm(id);
    }
}
```

> `TradeOrderFeignClient` 已 `extends OrderApi`，扩展后自动获得 4 个新方法，无需改 Feign。`PageVO` 放 trade-spi，web 直接引用。

**可选（列表补商品信息/状态文案）**：约定「聚合规则 3 磨平」，可在 `WebOrderMgrService.page` 中对每条 `OrderVO` 映射 `statusText`；若需展示明细商品，前端可调 detail。若要补状态文案，可在此新增 `WebOrderVO`（含 OrderVO + statusText）。

---

### Day 12 操作步骤

#### D12-0　seckill-spi 重构：纯契约（活动 + 商品）

**文件1：修改** `seckill-spi/pom.xml`——去 openfeign，加 spring-web + validation（照抄 product-spi/trade-spi）

```xml
    <dependencies>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-web</artifactId>
        </dependency>
        <dependency>
            <groupId>jakarta.validation</groupId>
            <artifactId>jakarta.validation-api</artifactId>
        </dependency>
    </dependencies>
```

**文件2：删除** 旧 `SeckillApi.java`，改为拆分：

**新建** `seckill-spi/../seckill/SeckillActivityApi.java`（活动管理）

```java
package com.fengluan.spi.seckill;

import com.fengluan.spi.seckill.dto.SeckillActivityRequest;
import com.fengluan.spi.seckill.dto.PageVO;
import com.fengluan.spi.seckill.vo.SeckillVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** 秒杀活动管理契约，类级前缀 /seckill/api（SeckillController） */
public interface SeckillActivityApi {

    @PostMapping("/activity")
    SeckillVO create(@Valid @RequestBody SeckillActivityRequest request);

    @PutMapping("/activity/{id}")
    SeckillVO update(@PathVariable Long id, @Valid @RequestBody SeckillActivityRequest request);

    @DeleteMapping("/activity/{id}")
    Void delete(@PathVariable Long id);

    @GetMapping("/activity/{id}")
    SeckillVO detail(@PathVariable Long id);

    @GetMapping("/activity/page")
    PageVO<SeckillVO> page(@RequestParam Long pageNum,
                           @RequestParam Long pageSize,
                           @RequestParam(required = false) String name);
}
```

**新建** `seckill-spi/../seckill/SeckillGoodApi.java`

```java
package com.fengluan.spi.seckill;

import com.fengluan.spi.seckill.dto.SeckillGoodAddRequest;
import com.fengluan.spi.seckill.vo.SeckillGoodVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 秒杀商品契约：关联商品 + 用户端有效列表（抢购 Day13 预留） */
public interface SeckillGoodApi {

    /** 给活动添加秒杀商品 */
    @PostMapping("/activity/{seckillId}/goods")
    SeckillGoodVO addGood(@PathVariable Long seckillId, @Valid @RequestBody SeckillGoodAddRequest request);

    /** 移除活动下的秒杀商品 */
    @DeleteMapping("/activity/goods/{id}")
    Void removeGood(@PathVariable Long id);

    /** 用户端：当前有效活动下的秒杀商品列表 */
    @GetMapping("/list")
    List<SeckillGoodVO> activeList();
}
```

**文件3：新建** `seckill-spi/../dto/PageVO.java`（独立于 trade-spi，同上结构）

```java
package com.fengluan.spi.seckill.dto;
import lombok.Data;
import java.util.List;
@Data
public class PageVO<T> { private Long total; private Long pageNum; private Long pageSize; private List<T> records; }
```

**文件4：新建** `seckill-spi/../dto/SeckillActivityRequest.java`

```java
package com.fengluan.spi.seckill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SeckillActivityRequest {
    @NotBlank private String name;
    @NotNull private Boolean enabled;
    @NotNull private LocalDateTime startTime;
    @NotNull private LocalDateTime endTime;
    private String description;
}
```

**文件5：新建** `seckill-spi/../dto/SeckillGoodAddRequest.java`

```java
package com.fengluan.spi.seckill.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SeckillGoodAddRequest {
    @NotNull private Long goodId;
    private String description;
}
```

**文件6：新建** `seckill-spi/../vo/SeckillGoodVO.java`（用户端展示）

```java
package com.fengluan.spi.seckill.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SeckillGoodVO {
    private Long id;
    private Long goodId;
    private String goodName;
    private String goodPic;
    private BigDecimal originalPrice; // good.price
    private String description;
    // 活动窗口信息（由 service 填充）
    private String seckillId;
    private String activityName;
    private String status;       // NOT_STARTED / IN_PROGRESS / ENDED
    private Long countdownSec;   // 倒计时（秒），NET_STARTED>0，IN_PROGRESS=0
}
```

> 秒杀商品预售价格：当前 `seckill_good` 表**无价格/秒杀库存字段**，Day13 抢购需每秒杀商品定秒杀价与秒杀量；本 Day12 仅做关联与展示（预览价=商品原价）。表结构如需存储秒杀价/秒杀库存，**需新增列**（见 D12-3 备注），本手册 Day12 不落库改表，Day13 规划。

#### D12-1　seckill-api：pom 补依赖 + 启动类修复

**文件1：修改** `goods-store-service/goods-store-seckill-api/pom.xml`

```xml
    <dependencies>
        <!-- 依赖模块 -->
        <dependency>
            <groupId>com.fengluan</groupId>
            <artifactId>goods-store-common</artifactId>
            <version>1.0.0</version>
        </dependency>
        <dependency>
            <groupId>com.fengluan</groupId>
            <artifactId>goods-store-seckill-spi</artifactId>
            <version>1.0.0</version>
        </dependency>
        <dependency>
            <groupId>com.fengluan</groupId>
            <artifactId>goods-store-product-spi</artifactId>
            <version>1.0.0</version>
        </dependency>
        <dependency>
            <groupId>com.fengluan</groupId>
            <artifactId>goods-store-member-spi</artifactId>
            <version>1.0.0</version>
        </dependency>

        <!-- Spring Cloud / 注册发现 / Feign -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-loadbalancer</artifactId>
        </dependency>

        <!-- MQ -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>

        <!-- Redis -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-pool2</artifactId>
        </dependency>

        <!-- Redisson 核心包 + 显式版本（勿用 starter，Boot4.1 兼容坑） -->
        <dependency>
            <groupId>org.redisson</groupId>
            <artifactId>redisson</artifactId>
            <version>${redisson.version}</version>
        </dependency>

        <!-- MyBatis-Plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>
    </dependencies>
```

> 先核对 trade-api pom 里究竟用了哪些 starter 名与 parent 管理的 artifactId/version，**保持一致**。`redisson.version`/`mybatis-plus.version` 需在 properties 给定（若 parent 未管理）。

**文件2：修改** `seckill-api/../SeckillApiApplication.java`

```java
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.fengluan.seckill.remote")
@MapperScan("com.fengluan.seckill.repository")
@SpringBootApplication(scanBasePackages = "com.fengluan")
public class SeckillApiApplication {
    // 不变
}
```

import 加 `org.springframework.cloud.openfeign.EnableFeignClients`。

**文件3：修改** `seckill-api/../config`（可能需本地 application.yaml 补 `spring.rabbitmq`/`spring.data.redis` 若 nacos 共享没有——参照 trade-api，已在共享配置则忽略）。

#### D12-2　seckill-api：活动管理（Mapper + Service + Controller）

**文件1：新建** `seckill-api/../repository/SeckillMapper.java`

```java
package com.fengluan.seckill.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fengluan.seckill.entity.SeckillEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SeckillMapper extends BaseMapper<SeckillEntity> {
}
```

**文件2：新建** `seckill-api/../service/SeckillActivityService.java`（接口）

```java
package com.fengluan.seckill.service;

import com.fengluan.spi.seckill.dto.PageVO;
import com.fengluan.spi.seckill.dto.SeckillActivityRequest;
import com.fengluan.spi.seckill.vo.SeckillVO;
import com.fengluan.seckill.entity.SeckillEntity;

public interface SeckillActivityService {
    SeckillVO create(SeckillActivityRequest request);
    SeckillVO update(Long id, SeckillActivityRequest request);
    void delete(Long id);
    SeckillVO detail(Long id);
    PageVO<SeckillVO> page(Long pageNum, Long pageSize, String name);
    SeckillEntity getById(Long id); // 校验活动存在，供 SeckillGoodService 用
}
```

**文件3：新建** `seckill-api/../service/impl/SeckillActivityServiceImpl.java`

```java
package com.fengluan.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.fengluan.common.exception.BusinessException;
import com.fengluan.common.exception.ErrorCode;
import com.fengluan.seckill.entity.SeckillEntity;
import com.fengluan.seckill.repository.SeckillMapper;
import com.fengluan.seckill.service.SeckillActivityService;
import com.fengluan.spi.seckill.dto.PageVO;
import com.fengluan.spi.seckill.dto.SeckillActivityRequest;
import com.fengluan.spi.seckill.vo.SeckillVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeckillActivityServiceImpl extends ServiceImpl<SeckillMapper, SeckillEntity>
        implements SeckillActivityService {

    private final SeckillMapper seckillMapper;

    @Override
    public SeckillVO create(SeckillActivityRequest request) {
        LocalDateTime now = LocalDateTime.now();
        if (request.getEndTime().isBefore(request.getStartTime()) || request.getEndTime().isBefore(now)) {
            throw new BusinessException("结束时间必须晚于开始时间且晚于当前", ErrorCode.BAD_REQUEST.getCode());
        }
        SeckillEntity e = new SeckillEntity();
        e.setName(request.getName());
        e.setEnabled(request.getEnabled());
        e.setStartTime(request.getStartTime());
        e.setEndTime(request.getEndTime());
        e.setDescription(request.getDescription());
        e.setCreatedTime(now);
        e.setUpdatedTime(now);
        seckillMapper.insert(e);
        return toVO(e);
    }

    @Override
    public SeckillVO update(Long id, SeckillActivityRequest request) {
        SeckillEntity exist = requireById(id);
        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new BusinessException("结束时间必须晚于开始时间", ErrorCode.BAD_REQUEST.getCode());
        }
        exist.setName(request.getName());
        exist.setEnabled(request.getEnabled());
        exist.setStartTime(request.getStartTime());
        exist.setEndTime(request.getEndTime());
        exist.setDescription(request.getDescription());
        exist.setUpdatedTime(LocalDateTime.now());
        seckillMapper.updateById(exist);
        return toVO(exist);
    }

    @Override
    public void delete(Long id) {
        requireById(id);
        seckillMapper.deleteById(id);
    }

    @Override
    public SeckillVO detail(Long id) { return toVO(requireById(id)); }

    @Override
    public PageVO<SeckillVO> page(Long pageNum, Long pageSize, String name) {
        Page<SeckillEntity> p = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        LambdaQueryWrapper<SeckillEntity> w = new LambdaQueryWrapper<SeckillEntity>()
                .like(name != null && !name.isBlank(), SeckillEntity::getName, name)
                .orderByDesc(SeckillEntity::getId);
        Page<SeckillEntity> r = seckillMapper.selectPage(p, w);
        PageVO<SeckillVO> vo = new PageVO<>();
        vo.setTotal(r.getTotal());
        vo.setPageNum(p.getCurrent());
        vo.setPageSize(p.getSize());
        vo.setRecords(r.getRecords().stream().map(this::toVO).toList());
        return vo;
    }

    @Override
    public SeckillEntity getById(Long id) { return requireById(id); }

    private SeckillEntity requireById(Long id) {
        SeckillEntity e = seckillMapper.selectById(id);
        if (e == null) throw new BusinessException(ErrorCode.SECKILL_NOT_FOUND);
        return e;
    }

    private SeckillVO toVO(SeckillEntity e) {
        SeckillVO v = new SeckillVO();
        v.setId(e.getId().longValue());
        v.setName(e.getName());
        v.setEnabled(e.getEnabled());
        v.setStartTime(e.getStartTime());
        v.setEndTime(e.getEndTime());
        v.setDescription(e.getDescription());
        v.setCreatedTime(e.getCreatedTime());
        v.setUpdatedTime(e.getUpdatedTime());
        return v;
    }
}
```

> 需要确认 `SeckillVO` 字段（id 类型）与 `SeckillEntity` `enabled` 类型（bit → Boolean）。`SeckillMapper` 分页同样依赖 `PaginationInnerInterceptor`——**seckill-api 需加 MybatisPlusInterceptor 配置**（或复用 trade 同款配置类复制为 `seckill.config.MybatisPlusConfig`）。

**文件4：新建** `seckill-api/../api/SeckillController.java`

```java
package com.fengluan.seckill.api;

import com.fengluan.spi.seckill.SeckillActivityApi;
import com.fengluan.spi.seckill.dto.PageVO;
import com.fengluan.spi.seckill.dto.SeckillActivityRequest;
import com.fengluan.spi.seckill.vo.SeckillVO;
import com.fengluan.seckill.service.SeckillActivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/seckill/api")
public class SeckillController implements SeckillActivityApi {

    private final SeckillActivityService seckillActivityService;

    @Override
    public SeckillVO create(@Valid @RequestBody SeckillActivityRequest request) {
        return seckillActivityService.create(request);
    }
    // update/delete/detail/page 同 pattern，实现即可
}
```

#### D12-3　seckill-api：秒杀商品关联 + 用户端有效列表

**文件1：新建** `seckill-api/../repository/SeckillGoodMapper.java`

```java
package com.fengluan.seckill.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fengluan.seckill.entity.SeckillGoodEntity;
import com.fengluan.spi.seckill.vo.SeckillGoodVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SeckillGoodMapper extends BaseMapper<SeckillGoodEntity> {

    /** 当前有效活动窗口内的秒杀商品（enabled=1 且 start<=now<=end 且商品未删） */
    @Select("""
        SELECT sg.id, sg.good_id, sg.description, g.name AS goodName, g.pic AS goodPic,
               g.price AS originalPrice,
               s.id AS seckillId, s.name AS activityName
        FROM seckill_good sg
        JOIN seckill s ON sg.seckill_id = s.id
        JOIN good g ON sg.good_id = g.id
        WHERE s.enabled = 1
          AND s.start_time <= #{now}
          AND s.end_time >= #{now}
          AND g.is_del = 0
        ORDER BY sg.id
        """)
    List<SeckillGoodVO> selectActiveSeckillGoods(@Param("now") LocalDateTime now);
}
```

> 确认 `seckill_good.good_id`/`g.id` 类型匹配；`good.is_del` 列存在。

**文件2：新建** `seckill-api/../service/SeckillGoodService.java` + `impl/SeckillGoodServiceImpl.java`

```java
public interface SeckillGoodService {
    SeckillGoodVO addGood(Long seckillId, SeckillGoodAddRequest request);
    void removeGood(Long id);
    List<SeckillGoodVO> activeList();
}
```

```java
@Service
@RequiredArgsConstructor
public class SeckillGoodServiceImpl implements SeckillGoodService {

    private final SeckillGoodMapper seckillGoodMapper;
    private final SeckillActivityService seckillActivityService;
    private final SeckillProductClient productClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeckillGoodVO addGood(Long seckillId, SeckillGoodAddRequest request) {
        seckillActivityService.getById(seckillId); // 活动必须存在，否 则 4001
        // 唯一约束 uq_seckill_good：重复添加则报错
        SeckillGoodEntity e = new SeckillGoodEntity();
        e.setSeckillId(seckillId.intValue());
        e.setGoodId(request.getGoodId().intValue());
        e.setDescription(request.getDescription());
        seckillGoodMapper.insert(e); // 冲突抛 DuplicateKeyException → 转 BAD_REQUEST
        return toVO(e);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeGood(Long id) {
        seckillGoodMapper.deleteById(id);
    }

    @Override
    public List<SeckillGoodVO> activeList() {
        List<SeckillGoodVO> list = seckillGoodMapper.selectActiveSeckillGoods(LocalDateTime.now());
        // 补活动窗口状态/倒计时（activityName 已带，status/countdown 由当前时间算）
        long now = System.currentTimeMillis();
        for (SeckillGoodVO v : list) {
            // 由 activity start/end 计算 status & countdown；这里因查询已限定窗口内，status 恒为 IN_PROGRESS
            v.setStatus("IN_PROGRESS");
            v.setCountdownSec(0L);
        }
        return list;
    }

    private SeckillGoodVO toVO(SeckillGoodEntity e) {
        SeckillGoodVO v = new SeckillGoodVO();
        v.setId(e.getId().longValue());
        v.setGoodId(e.getGoodId().longValue());
        v.setDescription(e.getDescription());
        return v;
    }
}
```

> 补充：更精确的倒计时/状态可让 `SeckillGoodMapper.selectActiveSeckillGoods` 额外返回 `s.start_time/end_time`（在 VO 加字段 `startTime/endTime`），由 service 计算 status/countdown。建议按此增强（本手册示例给基础版）。

**文件3：新建** `seckill-api/../remote/SeckillProductClient.java`

```java
package com.fengluan.seckill.remote;

import com.fengluan.spi.product.ProductApi;
import org.springframework.cloud.openfeign.FeignClient;

/** seckill 消费 product：仅 extends 契约 */
@FeignClient(name = "goods-store-product-api", contextId = "seckillProductClient", path = "/good/api")
public interface SeckillProductClient extends ProductApi {
}
```

**文件4：修改** `SeckillGoodEntity.java`——（**可选，Day12 不改**）如需落库秒杀价/秒杀库存，字段需与表列对齐；Day12 仅用现有 `id/seckillId/goodId/description`，不添加。

**文件5：新建** `seckill-api/../api/SeckillGoodController.java`

```java
package com.fengluan.seckill.api;

import com.fengluan.spi.seckill.SeckillGoodApi;
import com.fengluan.spi.seckill.dto.SeckillGoodAddRequest;
import com.fengluan.spi.seckill.vo.SeckillGoodVO;
import com.fengluan.seckill.service.SeckillGoodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/seckill/api")
public class SeckillGoodController implements SeckillGoodApi {

    private final SeckillGoodService seckillGoodService;

    @Override
    public SeckillGoodVO addGood(@PathVariable Long seckillId,
                                 @Valid @RequestBody SeckillGoodAddRequest request) {
        return seckillGoodService.addGood(seckillId, request);
    }

    @Override
    public Void removeGood(@PathVariable Long id) {
        seckillGoodService.removeGood(id);
        return null;
    }

    @Override
    public List<SeckillGoodVO> activeList() {
        return seckillGoodService.activeList();
    }
}
```

> 注意：`SeckillController`(活动) 与 `SeckillGoodController`(商品) 类级前缀都是 `/seckill/api`，但**方法路径不重叠**（`/activity*` vs `/list`），SpringMVC 映射不冲突；网关路由 `/seckill/api/**` 已覆盖。

#### D12-4　seckill-api：MybatisPlusConfig + RedisConfig + RedissonConfig

**文件1：新建** `seckill-api/../config/MybatisPlusConfig.java`

```java
package com.fengluan.seckill.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

> 若 trade-api 已有同款，可完全照抄路径/包名一致。此 Bean 必须在 seckill-api（而非只依赖传递）才生效。

**文件2：新建** `seckill-api/../config/RedisConfig.java`（声明 Lua 脚本，Day13 用；StringRedisTemplate 用 Boot 自动装配）

```java
package com.fengluan.seckill.config;

import org.springframework.context.annotation.Configuration;

/** 秒杀关键常量与 Lua 脚本（抢购原子性 Day13 使用） */
@Configuration
public class RedisConfig {

    public static final String STOCK_KEY_PREFIX = "seckill:stock:";
    public static final String ORDER_KEY_PREFIX = "seckill:order:";

    /** 抢购 Lua：校验并扣减库存 + 记每人一单，原子返回 1/0 */
    public static final String BUY_LUA = """
        local stock = redis.call('get', KEYS[1])
        if not stock then return -1 end
        local left = tonumber(stock)
        if left <= 0 then return 0 end
        local done = redis.call('setnx', KEYS[2], 1)
        if done == 0 then return -2 end
        redis.call('decrby', KEYS[1], 1)
        return 1
        """;
}
```

> 本日不需要执行 Lua；仅声明常量供 Day13 抢购消费。本步实质确保 `StringRedisTemplate` 可用（Boot 自动装配），无需额外 Bean。

**文件3：新建** `seckill-api/../config/RedissonConfig.java`（**核心包 + 手写**，照抄 trade-api）

```java
package com.fengluan.seckill.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;
    @Value("${spring.data.redis.port:6379}")
    private String port;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setConnectionPoolSize(10)
                .setConnectionMinimumIdleSize(5);
        return Redisson.create(config);
    }
}
```

> ⚠️ 不要用 `redisson-spring-boot-starter`（Boot 4.1 下自动装配引用已删除的 `RedisProperties` 报错）。此文件与 trade-api 的 `RedissonConfig` 等价，保持字段名一致即可。

#### D12-5　seckill-api：StockPreheatJob 库存预热

**新建** `seckill-api/../job/StockPreheatJob.java`

```java
package com.fengluan.seckill.job;

import com.fengluan.seckill.config.RedisConfig;
import com.fengluan.seckill.entity.SeckillEntity;
import com.fengluan.seckill.entity.SeckillGoodEntity;
import com.fengluan.seckill.repository.SeckillGoodMapper;
import com.fengluan.seckill.repository.SeckillMapper;
import com.fengluan.seckill.remote.SeckillProductClient;
import com.fengluan.spi.product.vo.GoodVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/** 秒杀库存预热：活动开始前 5 分钟将商品库存写入 Redis */
@Component
@EnableScheduling
@Slf4j
@RequiredArgsConstructor
public class StockPreheatJob {

    private final SeckillMapper seckillMapper;
    private final SeckillGoodMapper seckillGoodMapper;
    private final SeckillProductClient productClient;
    private final StringRedisTemplate redisTemplate;

    @Scheduled(cron = "0 * * * * ?")
    public void preheatStock() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fiveMinutesLater = now.plusMinutes(5);

        List<SeckillEntity> upcoming = seckillMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SeckillEntity>()
                        .eq(SeckillEntity::getEnabled, true)
                        .between(SeckillEntity::getStartTime, now, fiveMinutesLater)
        );

        for (SeckillEntity seckill : upcoming) {
            List<SeckillGoodEntity> goods = seckillGoodMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SeckillGoodEntity>()
                            .eq(SeckillGoodEntity::getSeckillId, seckill.getId())
            );
            for (SeckillGoodEntity sg : goods) {
                String key = RedisConfig.STOCK_KEY_PREFIX + sg.getId();
                Boolean absent = redisTemplate.opsForValue().setIfAbsent(key, "0");
                if (Boolean.FALSE.equals(absent)) {
                    continue; // 已预热，跳过
                }
                GoodVO good = productClient.getById(sg.getGoodId().longValue());
                if (good == null || Boolean.TRUE.equals(good.getIsDel())) {
                    redisTemplate.delete(key);
                    continue;
                }
                redisTemplate.opsForValue().set(key, String.valueOf(good.getQty() == null ? 0 : good.getQty()));
                long expire = Duration.between(now, seckill.getEndTime()).getSeconds() + 3600;
                redisTemplate.expire(key, Duration.ofSeconds(expire));
                log.info("秒杀库存预热 seckillGoodId={}, stock={}", sg.getId(), good.getQty());
            }
        }
    }
}
```

> `@EnableScheduling` 放启动类或本 Config 均可；此处放 job 类（Spring 会扫描）。预热值取商品当前库存 `good.getQty()`。

#### D12-6　web：秒杀浏览聚合 + gateway 白名单

**文件1：新建** `web/pom.xml` 添加 seckill-spi 依赖（若未加）：

```xml
        <dependency>
            <groupId>com.fengluan</groupId>
            <artifactId>goods-store-seckill-spi</artifactId>
            <version>1.0.0</version>
        </dependency>
```

**文件2：新建** `web/../web/seckill/SeckillFeignClient.java`

```java
package com.fengluan.web.seckill;

import com.fengluan.spi.seckill.SeckillGoodApi;
import org.springframework.cloud.openfeign.FeignClient;

/** web 浏览秒杀商品：仅 extends 契约 */
@FeignClient(name = "goods-store-seckill-api", contextId = "seckillBrowseFeignClient", path = "/seckill/api")
public interface SeckillFeignClient extends SeckillGoodApi {
}
```

**文件3：新建** `web/../web/seckill/WebSeckillService.java`

```java
package com.fengluan.web.seckill;

import com.fengluan.spi.seckill.vo.SeckillGoodVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** 秒杀浏览聚合（BFF） */
@Service
@RequiredArgsConstructor
public class WebSeckillService {

    private final SeckillFeignClient seckillFeignClient;

    public List<SeckillGoodVO> list() {
        return seckillFeignClient.activeList();
    }
}
```

**文件4：新建** `web/../web/seckill/WebSeckillController.java`

```java
package com.fengluan.web.seckill;

import com.fengluan.common.result.ApiResult;
import com.fengluan.spi.seckill.vo.SeckillGoodVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 秒杀浏览聚合入口：/app/api/seckill/list */
@RestController
@RequiredArgsConstructor
@RequestMapping("/app/api/seckill")
public class WebSeckillController {

    private final WebSeckillService webSeckillService;

    @GetMapping("/list")
    public ApiResult<List<SeckillGoodVO>> list() {
        return ApiResult.success(webSeckillService.list());
    }
}
```

**文件5：gateway**——`/app/api/**` 与 `/seckill/api/**` 路由均已存在，无需改动；秒杀列表为公开浏览（或登录态）按白名单业务决定，本日不强制。

---

## 五、验收标准

### Day 11

- [ ] `GET /trade/api/order/page?pageNum=1&pageSize=10` 返回当前会员订单分页（含 total/records）
- [ ] `GET /trade/api/order/{id}` 返回订单详情 + 明细 items
- [ ] `PUT /trade/api/order/{id}/cancel` 待付款订单取消，状态变 `50`，库存恢复
- [ ] 已支付订单 cancel 返回 `3002`（状态异常）
- [ ] `PUT /trade/api/order/{id}/confirm` 仅 SHIPPED 可确认 → `40`
- [ ] 越权：memberId(header) 与订单所属账号不一致，detail/cancel/confirm 返回 `3001`
- [ ] 下单后 MQ 消费者 `OrderCreateConsumer` 正常消费并扣库存（观察日志 + good.qty 减少）
- [ ] 延迟：缩短 `orderTimeoutQueue.ttl` 便于测试，30s/60s 后 `OrderCancelConsumer` 自动取消并恢复库存
- [ ] 幂等：同一 orderNo 消息消费两次，第二次直接 ack 不重复扣库存
- [ ] web：`GET /app/api/order/page`、`PUT /app/api/order/{id}/cancel` 等经 BFF 正常返回

### Day 12

- [ ] `POST /seckill/api/activity` 创建活动（结束时间校验）
- [ ] `POST /seckill/api/activity/{id}/goods` 添加秒杀商品（活动不存在 → `4001`；重复添加 → 400）
- [ ] `GET /seckill/api/list` 仅返回当前窗口内有效（enabled=1、start≤now≤end、good.is_del=0）的秒杀商品
- [ ] 活动开始前 5 分钟 `StockPreheatJob` 自动写 `seckill:stock:{seckillGoodId}`，TTL≈活动结束+1h
- [ ] 过期 Key 自动删除（活动结束后)
- [ ] web：`GET /app/api/seckill/list` 经 BFF 返回（含 goodName/goodPic/originalPrice/activityName/status）
- [ ] seckill-api 启动无 `SnowflakeUtil`/Redisson/Feign 相关报错（scanBasePackages + 手写 RedissonConfig + contextId 生效）

---

## 六、端口与服务表

| 服务                     | 端口               | 说明                       |
| ------------------------ | ------------------ | -------------------------- |
| goods-store-gateway      | 8888               | 前端唯一入口 `/app/api/**` |
| goods-store-web          | 8081?              | BFF，见当前各服务配置确定  |
| goods-store-product-api  | 8082?              | 商品                       |
| goods-store-member-api   | 8083?              | 会员                       |
| goods-store-trade-api    | 8086               | 交易（Day11 改动）         |
| goods-store-seckill-api  | **8087**           | 秒杀（Day12 新增全栈）     |
| nacos / rabbitmq / redis | 8848 / 5672 / 6379 | 基础设施                   |

> `?` 表示请按你当前各服务 `application.yaml` 实测值确认；务必用已配置端口。

---

## 七、FAQ / 常见坑

1. **`selectPage` total 恒为 1 条** → 未注册 `PaginationInnerInterceptor`，为 trade/seckill 各加 `MybatisPlusConfig`（IMPORTANT）。
2. **`IService`/`ServiceImpl` 找不到** → import 用 `com.baomidou.mybatisplus.spring.service.*`（3.5.9+ 迁移后的新包），trade/seckill 一致。
3. **`Could not find class [RedisProperties]`** → 用了 `redisson-spring-boot-starter`；改用核心包 `org.redisson:redisson` + 手写 `RedissonConfig`。
4. **`required a bean of type 'SnowflakeUtil'`** → seckill 启动类加 `scanBasePackages="com.fengluan"`。
5. **Feign `FeignClientSpecification` 已定义冲突** → 多客户端给唯一 `contextId`（seckill 产品客户端 contextId=seckillProductClient，web 秒杀=seckillBrowseFeignClient）。
6. **MQ 消费者消费裸 orderNo 却报类型不匹配** → 超时链路发的就是 `String`，方法签名用 `String orderNo`；不要按对象反序列化。
7. **商品下架判断**：用 `GoodVO.isDel`（无 `isTakeDown` 字段）。
8. **RabbitMQ 需本地启动**：Docker `rabbitmq:3-management` 或 winget，前面已说明。

---

## 八、执行清单（建议顺序）

1. **Day11-0 前置**：确认 trade 分页插件（MybatisPlusConfig）已加；无则补。
2. **D11-1** trade-spi 契约扩展 + PageVO/OrderDetailVO/OrderItemVO。
3. **D11-2** OrderStatus 枚举 + OrderService/Impl 4 方法 + Mapper 自定义 + OrderController 4 实现。
4. **D11-3/D11-4** 两个 MQ 消费者。
5. **D11-5** web 订单管理聚合。
6. 提交 → 你编译验证（不改：分页插件/枚举/归属校验/MQ 消费）。
7. **D12-0** seckill-spi 重构（删旧 SeckillApi，拆两个契约 + DTO/VO + PageVO）。
8. **D12-1** seckill-api pom + 启动类。
9. **D12-2/D12-3** 活动/商品 Mapper+Service+Controller。
10. **D12-4** MybatisPlusConfig/RedisConfig/RedissonConfig。
11. **D12-5** StockPreheatJob。
12. **D12-6** web 秒杀聚合。
13. 提交 → 你编译验证（注意：seckill 分页插件、scanBasePackages、RedissonConfig、contextId 齐全）。
