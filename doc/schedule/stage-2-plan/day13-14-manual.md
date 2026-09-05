# Day 13-14 操作执行手册 — 秒杀服务（下）：Lua 抢购 + MQ 异步下单 + 补偿；集成收尾：异常增强 + 链路追踪 + 文档聚合 + 集成测试

> 对应计划：[14-day-implementation-plan.md](file:///d:/.workspace/javaproject/goods-store-management-system-parent/doc/schedule/14-day-implementation-plan.md) Day 13（秒杀·下：抢购）/ Day 14（集成收尾）
> 版本：v1.0　编写日期：2026-09-02　执行日期：待填
> 主题：Day13 Redis-Lua 原子抢购 + MQ 异步创建秒杀订单 + 失败补偿 + web 抢购/结果轮询聚合；Day14 Feign 异常解码 + 参数校验异常 + TraceId 手动透传 + Knife4j 文档聚合 + 全链路集成测试（一律经 `/app/api/**` 收口）
> 依赖：Day 11-12 已完成且编译通过（trade 订单管理 + 两个 MQ 消费者；seckill-spi 纯契约重构 + seckill-api 活动/商品 CRUD + 库存预热 + web 秒杀浏览 `/app/api/seckill/list`）
> **架构前提**：遵循「分层架构规范」——spi 纯 HTTP 契约（禁 `@FeignClient`）、controller `implements` 契约、消费方 Feign `extends` 契约、web 是 BFF 聚合层（前端唯一入口 `/app/api/**`）、spi 不依赖 common（不用 `ApiResult`/mybatis 类型）。

> ⚠️ **本手册以「当前已落地架构」为准，对计划书 Day 13/14 示例做如下修正**：
>
> 1. **`GoodVO` 无 `isTakeDown`，只有 `isDel`**；秒杀 `seckill_good` 表**无 `stock`/秒杀价列** → 不做「DB 乐观锁扣库存」，而是信任 Redis Lua 预减（抢购原子性），MQ 消费者只负责**幂等建单 + 失败补偿 Redis**；
> 2. **SEKIILL 消息已存在**：`com.fengluan.common.mq.SeckillOrderMessage`（字段 `seckillId/seckillGoodId/goodId/memberId/seckillPrice(Long)/orderNo`）[已落地](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-common/src/main/java/com/fengluan/common/mq/SeckillOrderMessage.java)，直接复用，不再新建；
> 3. **计划书消费者注入 `orderMapper/goodMapper`** → 现况 seckill-api 不自建 `good` 映射，改注入 `SeckillProductClient`（`extends ProductApi`）取商品 name/pic/price；秒杀订单落 `order`/`order_item` 表则**在 seckill-api 内自建轻量实体 `SeckillOrderEntity`(@TableName("order"))/`SeckillOrderItemEntity`(@TableName("order_item")) + 对应 Mapper**（各模块自持共享库映射，不跨模块依赖）；
> 4. **错误码**：秒杀段现有 `4001~4004`，缺「已参与/重复抢购」→ Day13 需给 `ErrorCode` 新增 `SECKILL_ALREADY(4005,"您已参与过该秒杀")`；
> 5. **当前会员**：seckill 侧没有 trade 的 `CurrentUserUtil` → 在 `com.fengluan.seckill.util` 复制一份（读入站 `X-User-Id`，缺头抛 401），`SeckillOrderController` 用它取 memberId；
> 6. **`GlobalExceptionHandler` 当前全部注释**（[GlobalExceptionHandler.java](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-common/src/main/java/com/fengluan/common/exception/GlobalExceptionHandler.java)）→ Day14 需启用 BusinessException + 参数校验异常 + 兜底 Handler；且 common 走 **Jackson 3**（`tools.jackson.*`），Feign/异常解码器用 `tools.jackson.databind.ObjectMapper`；
> 7. **TraceId 手动透传（不引 Sleuth/Zipkin）**：网关(WebFlux) `TraceIdFilter` 生成放进站请求头 `X-Trace-Id` → 下游 Servlet 服务用 `common` 版 `TraceIdWebFilter`（`OncePerRequestFilter`）读入 MDC → Feign 用 `FeignTraceInterceptor`（`RequestInterceptor`）透传。**需要给 common 补 `feign-core` 依赖**（当前 common 无 openfeign/feign）；
> 8. **Knife4j 与 Boot 4 兼容性存疑**：优先尝试 `knife4j-openapi3-jakarta-spring-boot-starter`；若不兼容则以 `springdoc-openapi` 兜底，文档聚合功能优先级最低、可最后验证。

---

## 一、Day 13-14 目标与产出物

### Day 13 目标

1. **原子抢购**：Redis Lua 脚本一次完成「查库存 + 防重 + 扣减」，返回 -1(售罄)/-2(重复)/剩余库存；比计划的 Lua 多返回语义对齐现有 `RedisConfig.BUY_LUA`
2. **MQ 异步下单**：抢购成功后发 `SeckillOrderMessage` 到 `SECKILL_ORDER_KEY`，`SeckillOrderConsumer` 幂等建单（`order`+`order_item`），失败补偿 Redis 库存 + 清防重标记
3. **结果轮询**：`GET /seckill/api/order/{orderNo}/result`——有单=已抢到，无单=排队中（归属校验）
4. **web 收口**：抢购 `POST /app/api/seckill/order` + 结果轮询 `GET /app/api/seckill/order/{orderNo}/result`

### Day 14 目标

1. **异常处理增强**：跨服务 Feign 解码下游 `ApiResult` 为 `BusinessException`（保留原始 code/message，不再吞成 500）；`MethodArgumentNotValidException` 参数校验友好提示；启用全局 Handler
2. **TraceId 手动透传**：网关生成 `X-Trace-Id`，web/api 各服务写入 MDC 并可经 Feign 依次透传，日志 `[TraceId:xxx]` 贯穿调用链；响应头带 `X-Trace-Id`
3. **文档聚合**：web 作为前端文档唯一入口聚合 `/app/api/**`（示范 Knife4j；不兼容则 springdoc 兜底，可后置）
4. **集成测试 + 压测**：`test/` 脚本 3 链路 12 用例，**一律从 `/app/api/**` 发起**验证 web BFF 收口；`test/jmeter` 秒杀 1000 并发压测脚本

### 产出物清单

| 模块        | Day13 产出物                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 | Day14 产出物                                                                                                                                                                                                                          |
| ----------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| common      | ——                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           | `ErrorCode` 增 `SECKILL_ALREADY(4005)`（若 Day13 未加）；`exception/FeignErrorDecoder`；启用 `GlobalExceptionHandler`(+参数校验)；`feign/FeignTraceInterceptor`；`filter/TraceIdWebFilter`；pom 补 `feign-core`+`jakarta.servlet-api` |
| seckill-spi | 新增 `SeckillOrderApi`（抢购+结果轮询）、`SeckillOrderResponse`（orderNo/status/message）、`SeckillOrderResultVO`（orderNo/status）；`SeckillGoodApi` 保持、web 复用                                                                                                                                                                                                                                                                                                                         | ——                                                                                                                                                                                                                                    |
| seckill-api | `config/RedisConfig` 加 Lua script Bean（复用 BUY_LUA）；`config/SeckillMqConfig`（声明秒杀队列/绑定 + Jackson 转换器）；`util/CurrentUserUtil`；`remote/SeckillMemberClient`（extends MemberApi）；`entity/SeckillOrderEntity`+`SeckillOrderItemEntity`+`repository/SeckillOrderMapper`(+existsByOrderNo)+`SeckillOrderItemMapper`；`service/SeckillOrderService`(+Impl)；`mq/SeckillMessageProducer`+`mq/SeckillOrderConsumer`；`api/SeckillOrderController implements SeckillOrderApi` —— |
| web         | `SeckillFeignClient extends SeckillGoodApi, SeckillOrderApi`；`WebSeckillService` 加 seckill/result；`WebSeckillController` 加 `POST /order`、`GET /order/{orderNo}/result`                                                                                                                                                                                                                                                                                                                  | `config/SwaggerDocConfig`(可选 Knife4j)                                                                                                                                                                                               |
| gateway     | 路由 `/seckill/api/**`、`/app/api/**` 已存在，无需改动                                                                                                                                                                                                                                                                                                                                                                                                                                       | `filter/TraceIdFilter`（GlobalFilter，WebFlux）                                                                                                                                                                                       |
| test/       | ——                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           | 集成测试脚本（3 链路 12 用例）+ `jmeter/` 秒杀压测脚本                                                                                                                                                                                |

---

## 二、技术要点

### Day 13 技术要点

| 要点         | 说明                                                                                                                                                                                 |
| ------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 抢购路径     | `POST /seckill/api/order/{seckillGoodId}`（实现方）；web 聚合 `POST /app/api/seckill/order?seckillGoodId=` 收口                                                                      |
| Lua 原子操作 | `RedisConfig.BUY_LUA`：`KEYS[1]=seckill:stock:{sgId}`，`KEYS[2]=seckill:order:{memberId}:{sgId}`；返回 -1 售罄 / -2 重复 / 1 成功；用 `DefaultRedisScript<Long>` 注入执行            |
| 库存键       | 预热已写 `seckill:stock:{seckillGoodId}`（值=good.qty）[D12-5]；抢购脚本对**秒杀商品 id（sg.id）**，与预热一致                                                                       |
| 校验顺序     | 先 `seckillGoodMapper.selectById(seckillGoodId)`（无→4001）→ 活动存在→窗口校验（未开始 4002/已结束 4003）→ Lua 预减（售罄 4004/重复 4005）                                           |
| 异步建单     | 抢购成功即发 `SeckillOrderMessage` 到 `SECKILL_ORDER_KEY` → 消费者幂等建单；返回 `{orderNo, status:"PENDING", message:"排队中"}`                                                     |
| 幂等         | `SeckillOrderMapper.existsByOrderNo(orderNo)` → 已存在直接 ack；配合 `order_no`/`seckill_no` 唯一索引双保险                                                                          |
| 订单落库     | `order` 表写 `orderNo/memberAccount(=account)/totalPay(商品现价)/status=PENDING("10")/seckillNo=orderNo/checkoutTime=now`；`order_item` 写 goodId/dealPrice/count=1/goodName/goodPic |
| 失败补偿     | 建单异常→补偿 Redis：`increment(stockKey)` 恢复 + `delete(orderKey)` 清防重；消费异常 ack 不再重试（避免死循环）                                                                     |
| 手动确认     | `@RabbitListener(queues=SECKILL_ORDER_QUEUE)` 方法带 `Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag`；成功 ack，失败补偿后 ack                                         |
| 队列声明     | seckill-api 自建 `SeckillMqConfig` 声明 `SECKILL_ORDER_QUEUE` 队列 + 绑定 `SECKILL_ORDER_KEY` + `Jackson2JsonMessageConverter`（RabbitMQ 同名声明安全，与 trade 共用 exchange）      |
| 时隔/过期    | 秒杀订单不做 TTL 超时（抢购即时成交语义）；如需取消可后续接 ORDER_CANCEL 链路                                                                                                        |
| 归属校验     | result 查询按 `orderNo` 查 `order`，断言 `order.memberAccount.equals(当前账号)`，否则 `ORDER_NOT_FOUND(3001)`（跨模块复用交易错误码）                                                |

> 错误码：Day13 需给 `ErrorCode` 补 `SECKILL_ALREADY(4005,"您已参与过该秒杀")`；其余复用 4001~4004、3001。

### Day 14 技术要点

| 要点             | 说明                                                                                                                                                                                                                       |
| ---------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Feign 异常解码   | `FeignErrorDecoder implements feign.codec.ErrorDecoder`：读下游响应体 `ApiResult{code,message}` → `BusinessException(code,message)`；JSON 用 Jackson3 `tools.jackson.databind.ObjectMapper`；解析失败回退 `INTERNAL_ERROR` |
| 参数校验异常     | `GlobalExceptionHandler` 补 `@ExceptionHandler(MethodArgumentNotValidException.class)` → 返回 400 + 首个字段错误信息 `"字段: 错误消息"`                                                                                    |
| 启用全局 Handler | 取消 `globalExceptionHandler` 内 BusinessException / Exception 的注释；`@RestControllerAdvice` 生效                                                                                                                        |
| TraceId 生成     | 网关 `TraceIdFilter(GlobalFilter)`：`UUID` 去横线作 traceId，改写入站请求头 `X-Trace-Id`，并加进响应头                                                                                                                     |
| TraceId 落 MDC   | common `TraceIdWebFilter(OncePerRequestFilter)`：读上游 `X-Trace-Id`（无则生成），`MDC.put("TraceId",traceId)`，响应回写头；`finally MDC.remove`                                                                           |
| Feign 透传       | common `FeignTraceInterceptor implements RequestInterceptor`：`MDC.get("TraceId")` 非空则 `template.header("X-Trace-Id",traceId)`                                                                                          |
| 日志格式         | 推荐 `%X{TraceId}` 占位打印 `[TraceId:xxx]`；各服务须在 web 侧开启（web/api 都依赖 common，自动生效）                                                                                                                      |
| 文档聚合         | web 配 Knife4j 网关聚合策略聚合 `/app/api/**`；Boot4 不兼容则 springdoc 兜底；此任务优先级最低，可最后验证                                                                                                                 |
| 集成测试         | `test/` 脚本 3 链路 12 用例，全部 `curl http://localhost:9000/app/api/**` 发起（网关域名/端口见下）                                                                                                                        |
| 压测             | `test/jmeter` 秒杀 1000 并发，断言 Redis 库存 = DB 库存、无超卖                                                                                                                                                            |

---

## 三、Day 13-14 实施状态盘点

> 基于当前工程实测（Day 11-12 完成，编译通过）。

### Day 13 现状

| 计划任务          | 内容                                                                                                                                                                                                                                                        | 当前状态                                                                                                                                                                                                                            | 手册步骤 |
| ----------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------- |
| 13.1 Lua 脚本     | `RedisConfig.BUY_LUA` 常量已存在[已落地](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-service/goods-store-seckill-api/src/main/java/com/fengluan/seckill/config/RedisConfig.java)，缺 `DefaultRedisScript<Long>` Bean | D13-1                                                                                                                                                                                                                               |
| 13.2/13.6/13.7    | 抢购/列表/结果 Controller                                                                                                                                                                                                                                   | ❌ seckill-spi 无订单契约；[SeckillGoodApi](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-spi/goods-store-seckill-spi/src/main/java/com/fengluan/spi/seckill/SeckillGoodApi.java) 仅 list/关联 | D13-4    |
| 13.3 抢购 Service | ❌ 无 `SeckillOrderService`                                                                                                                                                                                                                                 | D13-3                                                                                                                                                                                                                               |
| 13.4 MQ 生产者    | ❌ 无；`rabbitTemplate` + `RabbitMqConfig.SECKILL_ORDER_KEY` 可用                                                                                                                                                                                           | D13-2                                                                                                                                                                                                                               |
| 13.5 MQ 消费者    | ❌ 无；`SeckillOrderMessage`(common) 已存在                                                                                                                                                                                                                 | D13-5                                                                                                                                                                                                                               |
| 13.8 web 收口     | ⚠️ [`WebSeckillController`](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-web/src/main/java/com/fengluan/web/seckill/WebSeckillController.java) 仅 `GET /list`                                                         | D13-6                                                                                                                                                                                                                               |

**Day13 现状基石**（已落地，直接复用）：

- `SeckillOrderMessage`：`seckillId/seckillGoodId/goodId/memberId/seckillPrice(Long)/orderNo`（common.mq）
- `RabbitMqConfig.SECKILL_ORDER_QUEUE/KEY`、`ORDER_EXCHANGE` 常量已声明（trade-api，常量在静态类，seckill 可 import 或本地声明等价字符串）
- `RedisConfig.BUY_LUA` + `STOCK_KEY_PREFIX` + `ORDER_KEY_PREFIX`（seckill config）
- `StockPreheatJob` 已预热 `seckill:stock:{sgId}`（D12-5）
- seckill-api 依赖齐全：amqp/data-redis/redisson 核心包/feign/loadbalancer/product-spi/member-spi
- `ProductApi.getById(id)->GoodVO(isDel,name,pic,price,qty)`、`MemberApi.getProfile(id)->MemberVO(account)`、`SnowflakeUtil.nectIdStr()`
- `order`/`order_item` 表结构：[order](file:///d:/.workspace/javaproject/goods-store-management-system-parent/sql/create_table_drop.sql#L176)? 实为[table_structure_export.sql](file:///d:/.workspace/javaproject/goods-store-management-system-parent/sql/table_structure_export.sql#L176)：`seckill_no` 唯一索引、`member_account`、`total_pay`、`status`、`checkout_time`；`order_item`：`order_id/good_id/deal_price/count/good_name/good_pic`

### Day 14 现状

| 计划任务  | 内容                 | 当前状态                                                                                                                                                                                                            | 手册步骤 |
| --------- | -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------- |
| 14.1      | FeignErrorDecoder    | ❌ 无；common 未依赖 feign-core                                                                                                                                                                                     | D14-1    |
| 14.2      | 参数校验异常         | ❌ `GlobalExceptionHandler` 全注释（[文件](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-common/src/main/java/com/fengluan/common/exception/GlobalExceptionHandler.java)短文） | D14-2    |
| 14.3      | TraceIdFilter        | ❌ 网关仅 `JwtAuthFilter`（[filter](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-gateway/src/main/java/com/fengluan/gateway/filter)）                                         | D14-3    |
| 14.4      | Feign TraceId 透传   | ❌ 无                                                                                                                                                                                                               | D14-4    |
| 14.5-14.6 | Knife4j 文档聚合     | ❌ 无；Boot4 兼容存疑，可后置/springdoc 兜底                                                                                                                                                                        | D14-5    |
| 14.7      | 集成测试（/app/api） | ❌ 无 `test/` 目录                                                                                                                                                                                                  | D14-6    |
| 14.8      | JMeter 压测脚本      | ❌ 无 `test/jmeter`                                                                                                                                                                                                 | D14-7    |

**Day14 现状基石**：

- gateway 为 **WebFlux（响应式）**：TraceId 用 `GlobalFilter`（WebFlux），不能复用 Servlet 的 `OncePerRequestFilter`
- common 用 **Jackson 3**（`tools.jackson.databind.ObjectMapper` 与自定义 `JsonMapperBuilderCustomizer`）
- 各 api/web 为 Servlet（spring-webmvc 或 spring-boot-starter-web\*），可放 `JwtAuthFilter` 风格 `OncePerRequestFilter`
- web 依赖 common/各 spi + openfeign，天然可放 Feign `RequestInterceptor`
- 网关端口：见网关 `application.yaml`（前端经网关 `9000` 进入 web；web/api 反向路由内部）——**集成测试一律走 `/app/api/**`\*\*

**环境前置**：RabbitMQ + Redis + Nacos 在跑；秒杀抢购前需先 `StockPreheatJob` 预热成功（或手动 `SET seckill:stock:{sgId} 库存`）。

---

## 四、详细操作步骤

### 前置约定

- **spi 纯净**：seckill-spi 用 `spring-web`+`jakarta.validation-api`；新增 `SeckillOrderApi` + DTO/VO 放 seckill-spi；web 复用 `SeckillFeignClient extends SeckillGoodApi, SeckillOrderApi`（一个契约、一个 Feign）。
- **当前会员**：seckill 抢购/结果由 `com.fengluan.seckill.util.CurrentUserUtil.currentUserId()` 取（读 `X-User-Id`，缺 401）——与 trade 同款逻辑复制。
- **订单归属 account**：`memberClient.getProfile(memberId).getAccount()`，渲染到 `order.member_account`，结果查询据此校验。
- **MQ**：seckill 消费者放 `com.fengluan.seckill.mq`；`@EnableRabbit` 由 starter 默认开启。
- **队列声明**：seckill 自建 `SeckillMqConfig`（Spring 会自动确保 exchange/queue/binding 幂等声明，与 trade 同名安全）；须引 `spring-boot-starter-amqp`（已具备）。
- **错误码**：先给 `ErrorCode` 加 `SECKILL_ALREADY(4005)`。

---

### Day 13 操作步骤

#### D13-0　（公共）ErrorCode 新增秒杀重复码

`ErrorCode` 秒杀段追加：

```java
SECKILL_ALREADY(4005, "您已参与过该秒杀"),
```

#### D13-1　seckill-api：Redis Lua 脚本 Bean

在 `config/RedisConfig.java`（已含 `BUY_LUA`）追加：

```java
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.context.annotation.Bean;

@Bean
public DefaultRedisScript<Long> seckillScript() {
    DefaultRedisScript<Long> script = new DefaultRedisScript<>();
    script.setScriptText(RedisConfig.BUY_LUA);
    script.setResultType(Long.class);
    return script;
}
```

#### D13-2　seckill-api：`SeckillMqConfig`（秒杀队列 + Jackson 转换器）

新建 `config/SeckillMqConfig.java`（自声明秒杀队列/绑定，与 trade 的 `ORDER_EXCHANGE` 共用）：

```java
package com.fengluan.seckill.config;

import com.fengluan.seckill.mq.SeckillMqConstants; // 或用常量字面量
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SeckillMqConfig {

    public static final String ORDER_EXCHANGE = "goods.order.exchange"; // 与 trade RabbitMqConfig 同名共用
    public static final String SECKILL_ORDER_QUEUE = "goods.seckill.order.queue";
    public static final String SECKILL_ORDER_KEY = "seckill.order.create";

    @Bean
    public TopicExchange seckillOrderExchange() {
        return new TopicExchange(ORDER_EXCHANGE, true, false);
    }

    @Bean
    public Queue seckillOrderQueue() {
        return QueueBuilder.durable(SECKILL_ORDER_QUEUE).build();
    }

    @Bean
    public Binding seckillOrderBinding() {
        return BindingBuilder.bind(seckillOrderQueue()).to(seckillOrderExchange()).with(SECKILL_ORDER_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter seckillMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
```

> RabbitMQ 对同名「exchange/queue/binding」的二次声明是幂等安全的（参数一致时无副作用），所以 seckill-api 与 trade-api 同时声明 `goods.order.exchange` 与 `goods.seckill.order.queue` 不冲突。`Jackson2JsonMessageConverter` 各服务独立声明亦可（Bean 名冲突仅限同服务内）。

#### D13-3　seckill-api：抢购 Service + 实体/映射

**实体与映射**（各模块自持共享库表映射，不跨模块依赖）：

- `entity/SeckillOrderEntity.java`：`@TableName("order")`，字段照 trade 的 `OrderEntity`（id Long 自增/orderNo/seckillNo/memberAccount/totalPay(BigDecimal)/status(String)/checkoutTime(LocalDateTime)/isDel/createdTime/updatedTime…）
- `entity/SeckillOrderItemEntity.java`：`@TableName("order_item")`，字段 id Long/orderId(Integer)/goodId(Integer)/dealPrice(BigDecimal)/count(Integer)/goodName/goodPic
- `repository/SeckillOrderMapper.java`：`extends BaseMapper<SeckillOrderEntity>`，加幂等查询：
  ```java
  @Select("SELECT COUNT(1) FROM `order` WHERE order_no = #{orderNo}")
  long existsByOrderNo(@Param("orderNo") String orderNo);
  ```
- `repository/SeckillOrderItemMapper.java`：`extends BaseMapper<SeckillOrderItemEntity>`

**remote**：`remote/SeckillMemberClient.java`：

```java
@FeignClient(name = "goods-store-member-api", contextId = "seckillMemberClient", path = "/member/api")
public interface SeckillMemberClient extends MemberApi { }
```

**`util/CurrentUserUtil.java`**：照抄 trade 版（`currentUserId()` 读 `X-User-Id`，缺抛 401）。

**Service 接口** `service/SeckillOrderService.java`：

```java
public interface SeckillOrderService {
    SeckillOrderResponse seckill(Long seckillGoodId, Long memberId);
    SeckillOrderResultVO result(String orderNo, Long memberId);
}
```

**`service/impl/SeckillOrderServiceImpl.java`** 核心逻辑：

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class SeckillOrderServiceImpl implements SeckillOrderService {
    private final SeckillGoodMapper seckillGoodMapper;
    private final SeckillActivityService seckillActivityService;
    private final SeckillMemberClient memberClient;
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> seckillScript;
    private final RabbitTemplate rabbitTemplate;
    private final SnowflakeUtil snowflakeUtil;

    @Override
    public SeckillOrderResponse seckill(Long seckillGoodId, Long memberId) {
        // 1. 秒杀商品存在性
        SeckillGoodEntity sg = seckillGoodMapper.selectById(seckillGoodId);
        if (sg == null) throw new BusinessException(ErrorCode.SECKILL_NOT_FOUND);
        // 2. 活动窗口校验
        SeckillEntity seckill = seckillActivityService.getById(sg.getSeckillId().longValue());
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(seckill.getStartTime())) throw new BusinessException(ErrorCode.SECKILL_NOT_STARTED);
        if (now.isAfter(seckill.getEndTime())) throw new BusinessException(ErrorCode.SECKILL_ENDED);
        // 3. Lua 原子预减 + 防重
        String stockKey = RedisConfig.STOCK_KEY_PREFIX + seckillGoodId;
        String orderKey = RedisConfig.ORDER_KEY_PREFIX + memberId + ":" + seckillGoodId;
        Long result = redisTemplate.execute(seckillScript, List.of(stockKey, orderKey));
        if (Long.valueOf(-1).equals(result)) throw new BusinessException(ErrorCode.SECKILL_STOCK_EMPTY);
        if (Long.valueOf(-2).equals(result)) throw new BusinessException(ErrorCode.SECKILL_ALREADY);
        // 4. 发 MQ 异步建单
        String orderNo = snowflakeUtil.nectIdStr();
        SeckillOrderMessage msg = SeckillOrderMessage.builder()
                .seckillId(sg.getSeckillId().longValue())
                .seckillGoodId(seckillGoodId)
                .goodId(sg.getGoodId().longValue())
                .memberId(memberId)
                .orderNo(orderNo)
                .build();
        rabbitTemplate.convertAndSend(SeckillMqConfig.ORDER_EXCHANGE,
                SeckillMqConfig.SECKILL_ORDER_KEY, msg);
        return new SeckillOrderResponse(orderNo, "PENDING", "排队中，请稍后查询结果");
    }

    @Override
    public SeckillOrderResultVO result(String orderNo, Long memberId) {
        SeckillOrderEntity order = seckillOrderMapper.selectOne(
                new LambdaQueryWrapper<SeckillOrderEntity>()
                        .eq(SeckillOrderEntity::getOrderNo, orderNo));
        if (order == null) {
            return new SeckillOrderResultVO(orderNo, "PROCESSING", "排队中"); // 消费者尚未建单
        }
        String account = memberClient.getProfile(memberId).getAccount();
        if (!account.equals(order.getMemberAccount())) throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        return new SeckillOrderResultVO(orderNo, "SUCCESS", "已抢到");
    }
}
```

> 说明：抢购成功即已由 Lua 预扣 Redis 库存并建防重标记；若消费者后续建单失败会**补偿** Redis（+1 库存、删防重），保证最终一致性。`SeckillOrderServiceImpl` 需注入 `seckillOrderMapper`。`result` 中「无单=排队中」「有单=已抢到」即轮询终态。

#### D13-4　seckill-spi：`SeckillOrderApi` 契约 + DTO/VO

新建 `spi/seckill/SeckillOrderApi.java`：

```java
public interface SeckillOrderApi {
    /** 抢购：成员自入站 X-User-Id 解析 */
    @PostMapping("/order/{seckillGoodId}")
    SeckillOrderResponse seckill(@PathVariable Long seckillGoodId);

    /** 秒杀结果轮询 */
    @GetMapping("/order/{orderNo}/result")
    SeckillOrderResultVO result(@PathVariable String orderNo);
}
```

新建 `dto/SeckillOrderResponse.java`（orderNo/status/message）、`dto/SeckillOrderResultVO.java`（orderNo/status/message），均用 `@Data`，字段见 D13-3 用法。

**`api/SeckillOrderController.java`**（实现方，seckill-api）：

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/seckill/api")
public class SeckillOrderController implements SeckillOrderApi {
    private final SeckillOrderService seckillOrderService;

    @Override
    public SeckillOrderResponse seckill(@PathVariable Long seckillGoodId) {
        return seckillOrderService.seckill(seckillGoodId, CurrentUserUtil.currentUserId());
    }

    @Override
    public SeckillOrderResultVO result(@PathVariable String orderNo) {
        return seckillOrderService.result(orderNo, CurrentUserUtil.currentUserId());
    }
}
```

#### D13-5　seckill-api：`SeckillMessageProducer` + `SeckillOrderConsumer`

**`mq/SeckillMessageProducer.java`**（正交：可直接在 service 用 `rabbitTemplate`，也可封装）：

```java
@Component
@RequiredArgsConstructor
public class SeckillMessageProducer {
    private final RabbitTemplate rabbitTemplate;

    public void sendSeckillOrder(SeckillOrderMessage message) {
        rabbitTemplate.convertAndSend(SeckillMqConfig.ORDER_EXCHANGE,
                SeckillMqConfig.SECKILL_ORDER_KEY, message);
    }
}
```

**`mq/SeckillOrderConsumer.java`**（幂等建单 + 失败补偿）：

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class SeckillOrderConsumer {
    private final SeckillOrderMapper orderMapper;
    private final SeckillOrderItemMapper orderItemMapper;
    private final SeckillProductClient productClient;
    private final SeckillMemberClient memberClient;
    private final StringRedisTemplate redisTemplate;

    @RabbitListener(queues = SeckillMqConfig.SECKILL_ORDER_QUEUE)
    public void handle(SeckillOrderMessage msg, Channel channel,
                       @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {
            // 幂等：订单已存在（如超时重投）直接 ack
            if (orderMapper.existsByOrderNo(msg.getOrderNo()) > 0) {
                channel.basicAck(tag, false);
                return;
            }
            GoodVO good = productClient.getById(msg.getGoodId());
            if (good == null || Boolean.TRUE.equals(good.getIsDel())) {
                compensate(msg);
                channel.basicAck(tag, false);
                return;
            }
            String account = memberClient.getProfile(msg.getMemberId()).getAccount();

            SeckillOrderEntity order = new SeckillOrderEntity();
            order.setOrderNo(msg.getOrderNo());
            order.setSeckillNo(msg.getOrderNo());
            order.setMemberAccount(account);
            order.setTotalPay(good.getPrice());
            order.setStatus("10"); // PENDING
            order.setCheckoutTime(LocalDateTime.now());
            order.setCreatedTime(LocalDateTime.now());
            orderMapper.insert(order);

            SeckillOrderItemEntity item = new SeckillOrderItemEntity();
            item.setOrderId(order.getId().intValue());
            item.setGoodId(msg.getGoodId().intValue());
            item.setDealPrice(good.getPrice());
            item.setCount(1);
            item.setGoodName(good.getName());
            item.setGoodPic(good.getPic());
            orderItemMapper.insert(item);

            channel.basicAck(tag, false);
            log.info("秒杀订单创建成功：{}", msg.getOrderNo());
        } catch (Exception e) {
            log.error("秒杀建单失败，补偿 Redis：orderNo={}", msg.getOrderNo(), e);
            compensate(msg);
            channel.basicAck(tag, false); // 补偿后确认，不无限重试
        }
    }

    private void compensate(SeckillOrderMessage msg) {
        String stockKey = RedisConfig.STOCK_KEY_PREFIX + msg.getSeckillGoodId();
        String orderKey = RedisConfig.ORDER_KEY_PREFIX + msg.getMemberId() + ":" + msg.getSeckillGoodId();
        redisTemplate.opsForValue().increment(stockKey);
        redisTemplate.delete(orderKey);
        log.info("秒杀补偿：恢复库存+清防重 seckillGoodId={} memberId={}", msg.getSeckillGoodId(), msg.getMemberId());
    }
}
```

> 要点：`@Header(AmqpHeaders.DELIVERY_TAG)` 需 import `com.rabbitmq.client.Channel` + `org.springframework.amqp.support.AmqpHeaders`。`GoodVO.price` 为 `BigDecimal`。秒杀不做 TTL 超时（即时成交）；如需可后续接入取消链路。

#### D13-6　web：抢购 + 结果轮询聚合

**`SeckillFeignClient`** 改为同时继承两个契约：

```java
@FeignClient(name = "goods-store-seckill-api", contextId = "seckillBrowseFeignClient", path = "/seckill/api")
public interface SeckillFeignClient extends SeckillGoodApi, SeckillOrderApi { }
```

**`WebSeckillService`** 追加：

```java
public SeckillOrderResponse seckill(Long seckillGoodId) {
    return seckillFeignClient.seckill(seckillGoodId);
}
public SeckillOrderResultVO result(String orderNo) {
    return seckillFeignClient.result(orderNo);
}
```

**`WebSeckillController`** 追加：

```java
@PostMapping("/order")
public ApiResult<SeckillOrderResponse> order(
        @RequestParam Long seckillGoodId) {
    return ApiResult.success(webSeckillService.seckill(seckillGoodId));
}

@GetMapping("/order/{orderNo}/result")
public ApiResult<SeckillOrderResultVO> result(@PathVariable String orderNo) {
    return ApiResult.success(webSeckillService.result(orderNo));
}
```

> 网关 `/app/api/**` 已收口到 web；抢占接口经 `X-User-Id`（网关透传）传给 seckill-api。前端抢购成功轮询 `/app/api/seckill/order/{orderNo}/result` 直到 `SUCCESS`（已抢到）或出现 4004/4005 错误。

---

### Day 14 操作步骤

#### D14-1　common：Feign 异常解码器 + 依赖

**common pom 追加**：

```xml
<dependency>
    <groupId>io.github.openfeign</groupId>
    <artifactId>feign-core</artifactId>
</dependency>
<dependency>
    <groupId>jakarta.servlet</groupId>
    <artifactId>jakarta.servlet-api</artifactId>
    <scope>provided</scope>
</dependency>
```

**`exception/FeignErrorDecoder.java`**（用 Jackson 3）：

```java
@Component
public class FeignErrorDecoder implements feign.codec.ErrorDecoder {
    private final ObjectMapper mapper = JsonMapper.builder().build(); // tools.jackson
    // 或 @Autowired jackson 3 ObjectMapper

    @Override
    public Exception decode(String methodKey, feign.Response response) {
        try {
            String body = new String(response.body().asInputStream().readAllBytes());
            JsonNode node = mapper.readTree(body);
            int code = node.path("code").asInt(ErrorCode.INTERNAL_ERROR.getCode());
            String message = node.path("message").asText("服务调用失败");
            return new BusinessException(new ErrorCode??...) // 见下
        } catch (Exception e) {
            return new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }
}
```

> `ErrorCode` 是枚举非可动态构造的 code，故回 `new BusinessException(message, code)`（该构造器已存在）。**配置**：为使各消费者自动装配，可在 common 用 `@Bean FeignErrorDecoder` 或各服务显式 `feign.errorDecoder()`。最简单为一处 `@Component`（需被扫描，各 api/web 已 `scanBasePackages="com.fengluan"`）。

#### D14-2　common：启用 GlobalExceptionHandler + 参数校验异常

取消 `handleBusinessException`/`handleException` 注释，并新增：

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ApiResult<?> handleValid(MethodArgumentNotValidException e) {
    String msg = e.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .orElse("参数校验失败");
    return ApiResult.error(ErrorCode.BAD_REQUEST.getCode(), msg);
}
```

> 相关 import：`org.springframework.web.bind.MethodArgumentNotValidException`。此 Handler 在 common，所有依赖 common 的服务自动生效。

#### D14-3　gateway：`TraceIdFilter`（WebFlux GlobalFilter）

`gateway/filter/TraceIdFilter.java`：

```java
@Component
@Order(-100)
public class TraceIdFilter implements GlobalFilter {
    private static final String TRACE_ID = "X-Trace-Id";
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest req = exchange.getRequest();
        String traceId = req.getHeaders().getFirst(TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        exchange.getResponse().getHeaders().set(TRACE_ID, traceId);
        ServerHttpRequest mutated = req.mutate().header(TRACE_ID, traceId).build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }
}
```

#### D14-4　common：TraceId 落 MDC + Feign 透传

**`filter/TraceIdWebFilter.java`**（Servlet 服务 web/api 用；`OncePerRequestFilter`）：

```java
@Component
public class TraceIdWebFilter extends OncePerRequestFilter {
    private static final String TRACE_ID = "X-Trace-Id";
    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_ID);
        if (traceId == null || traceId.isBlank()) traceId = UUID.randomUUID().toString().replace("-", "");
        MDC.put("TraceId", traceId);
        try {
            chain.doFilter(request, response);
            response.setHeader(TRACE_ID, traceId);
        } finally {
            MDC.remove("TraceId");
        }
    }
}
```

**`feign/FeignTraceInterceptor.java`**：

```java
@Component
public class FeignTraceInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        String traceId = MDC.get("TraceId");
        if (traceId != null) template.header("X-Trace-Id", traceId);
    }
}
```

> 日志打印 `[TraceId:%X{TraceId}]`（在 logback pattern 加 `%X{TraceId}`）。web 与各 api 都依赖 common → 自动生效；网关本身（WebFlux/非 Servlet）不用 `TraceIdWebFilter`。

#### D14-5　web：Knife4j 文档聚合（可后置，Boot4 兼容风险）

在 `web` 引入 `knife4j-openapi3-jakarta-spring-boot-starter` 并聚合 `/app/api/**`；若 Boot4 不兼容，改用 `springdoc-openapi-starter-webmvc-ui` 并在 `WebSeckillController` 等加 OpenAPI 注解即可。此任务优先级最低，失败不阻塞 Day14 主体（异常/TraceId/测试）。

#### D14-6/14-7　集成测试 + JMeter 压测（`test/`）

新建 `test/`（非 Java 工程，放可执行脚本与说明）：

- `test/integration/run-tests.sh`：curl 脚本按 **3 链路 12 用例** 逐条发起，入口一律 `http://localhost:9000/app/api/**`（网关）。用例（沿用计划书 TC-01~TC-12）：注册→登录→浏览→加购→下单→取消→重复下单防重；秒杀抢购/防重/结果轮询；品牌服务停用降级/超时自动取消。
- `test/jmeter/seckill-1000.jmx`：BeanShell/Sampler 打压 `POST /app/api/seckill/order?seckillGoodId=1`，1000 并发；断言 Redis `seckill:stock` + DB 库存一致、无超卖。

> 压测前务必确认：预热已写库存；`seckill_good` 有可抢商品；网关/秒杀服务配额足够。

---

## 五、验收标准

### Day 13

- [ ] `POST /seckill/api/order/{seckillGoodId}`：排队成功返回 `{orderNo,status:"PENDING"}`
- [ ] 同一用户重复抢购返回 4005「已参与过该秒杀」
- [ ] 库存为 0 返回 4004「秒杀库存已空」
- [ ] Redis `seckill:stock:{sgId}` 正确递减，防重键 `seckill:order:{member}:{sgId}` 生成
- [ ] MQ 消费成功，`order`/`order_item` 各一条，状态 PENDING
- [ ] 建单失败自动补偿 Redis（库存回补、防重清除）
- [ ] `GET /seckill/api/order/{orderNo}/result`：轮询「排队中→已抢到」
- [ ] **web 收口**：`POST /app/api/seckill/order` 与 `GET /app/api/seckill/order/{orderNo}/result` 均可用，前端不直连 seckill-api

### Day 14

- [ ] Feign 调用下游异常抛 `BusinessException`（保留原始 code/message，非 500）
- [ ] 参数校验失败返回 400 + 字段错误信息
- [ ] 响应 Header 含 `X-Trace-Id`；日志 `[TraceId:xxx]` 贯穿 web→api 链路
- [ ] 浏览器/web 文档聚合入口可查 `/app/api/**`（Knife4j，不兼容则 springdoc）
- [ ] 12 个集成用例全部通过，入口均为 `/app/api/**`
- [ ] JMeter 秒杀 1000 并发无超卖，Redis 与 DB 库存一致

---

## 六、端口与服务表

| 服务                    | 端口 | 关键路由/备注                                      |
| ----------------------- | ---- | -------------------------------------------------- | ----- | ------- |
| gateway                 | 9000 | 前端唯一入口 `/app/api/**`；`/seckill/api/**` 内部 |
| goods-store-web         | 8080 | BFF 聚合：`/app/api/seckill/list                   | order | result` |
| goods-store-seckill-api | 8087 | `/seckill/api/order/**` 抢购/结果                  |
| member/product/trade    | 80xx | 被 seckill Feign 消费                              |

---

## 七、FAQ / 常见坑

1. **秒杀重复建单**：消费者幂等用 `existsByOrderNo` + `order_no`/`seckill_no` 唯一索引双保险；`DuplicateKeyException` 捕获后 ack 跳过。
2. **补偿注意**：`seckill:stock` 的键用 `seckillGoodId`（不是 goodId），补偿用 `msg.getSeckillGoodId()`，与抢购 Lua 一致。
3. **防重键生命周期**：`BUY_LUA` 用 `setnx` 记一人一单但需定时过期，否则库存耗尽后补单用户一直「已参与」。可在抢购成功后 `expire(orderKey, 24h)`，或扩展 Lua 用 `SETEX`。
4. **Lua null**：库存键缺失（未预热）时 `redis.call('get', key)` 返回 false → 脚本返回 -1 售罄；属安全兜底，最好预热后才有库存。
5. **TraceId**：网关用 WebFlux `GlobalFilter`，web/api 用 Servlet `OncePerRequestFilter`，二者不可混用。
6. **feign-core**：common 增加 `feign-core` 才可用 `RequestInterceptor`/`ErrorDecoder`；`javax`→`jakarta` 注意 servlet 包。

---

## 八、执行清单（建议顺序）

1. **D13-0** ErrorCode 加 `SECKILL_ALREADY(4005)`。
2. **D13-1** Redis Lua `DefaultRedisScript` Bean。
3. **D13-2** `SeckillMqConfig`（队列/绑定/转换器）。
4. **D13-3** 实体/映射/`SeckillMemberClient`/`CurrentUserUtil`/`SeckillOrderService(+Impl)`。
5. **D13-4** seckill-spi `SeckillOrderApi`+DTO/VO，seckill-api `SeckillOrderController implements`。
6. **D13-5** `SeckillMessageProducer` + `SeckillOrderConsumer`（幂等 + 补偿）。
7. **D13-6** web：`SeckillFeignClient` extends 双契约 + `WebSeckillService`/`WebSeckillController` 抢购/结果。
8. **D14-1/D14-2** common：feign 依赖 + `FeignErrorDecoder` + 启用全局 Handler + 参数校验异常。
9. **D14-3/D14-4** 网关 `TraceIdFilter` + common `TraceIdWebFilter`/`FeignTraceInterceptor`。
10. **D14-5**（可后置）Knife4j 文档聚合/springdoc 兜底。
11. **D14-6/D14-7** `test/` 集成脚本 + jmeter 压测。
12. 编译验证（用户手动 `mvn clean compile`）；启动 RabbitMQ/Redis/Nacos 后跑通秒杀链路与集成用例。

---

> 执行完成后请将 Day13-14 的变更与验证结果回填到本手册「现状基石」区，并同步 `doc/summary.md` 问题记录。
