# Stage 5 —— 支付闭环与业务补齐迭代计划

> 阶段：Stage 5 —— 在「后端微服务全链路 + 双前端交付 + SkyWalking 可观测」基础上，补齐模拟支付/退款闭环，完善秒杀、购物车、会员、RBAC、行政区划等业务操作完整性。
> 依据：对照《基于SpringCloud的B2C微服务秒杀商城-详细设计说明书》的差距分析 + 2026-09-06 支付方案讨论结论。
> 决策：支付采用「**状态机模拟 + 支付流水表**」；退款新增 **REFUNDED 状态**；不做钱包/充值/异步验签。

---

## 一、目标

1. **订单状态机全可达**：`PENDING(10) → PAID(20) → SHIPPED(30) → COMPLETED(40)` 打通模拟支付；`PAID → REFUNDED(60)` 打通退款，彻底消灭「订单永远停留在待付款」。
2. **支付三道防线**：接口幂等（CAS 条件更新）、支付与超时取消的状态竞争（谁后到谁让步）、收银台 30 分钟倒计时联动。
3. **秒杀订单超时关单**：30 分钟未支付自动取消 + Redis 库存回补 + 防重键清理。
4. **业务操作补齐**：购物车（勾选/批量删除/按选中结算）、会员管理（编辑/启禁用）。
5. **Redis 深度实践**：商品详情热点缓存 + 缓存击穿防护（逻辑过期方案），与秒杀库存实践串成一条叙事。
6. **账号安全**：登录图形验证码（Redis 5 分钟一次性）。
7. **RBAC 两角色落地**：角色管理 + 菜单按权限过滤 + 权限码判定。
8. **行政区划三级联动**：`t_cn_region_info` 表启用，挂收货地址。

---

## 二、决策记录（2026-09-06 讨论）

### 2.1 模拟支付的本质

**模拟支付模拟的不是钱，是「支付事件对系统的影响」。** 真实链路中钱始终在支付宝/银行账上，平台从来不经手（担保交易也只是冻结在渠道侧）；商城系统里本来就没有钱，只有资金事件产生的**状态与记录**。模拟支付 = 用一次本地接口调用替代「用户与渠道的资金转移 + 异步回调确认」，同时保留后者对系统的全部影响。

### 2.2 方案对比与结论

| 方案            | 模拟对象             | 钱放哪        | 需要充值   | 验签/异步回调      | 工作量 | 结论                |
| --------------- | -------------------- | ------------- | ---------- | ------------------ | ------ | ------------------- |
| A. 状态机模拟   | 「支付成功」这个事件 | 不存在        | 否         | 无防御对象，不需要 | 小     | **采纳（+流水表）** |
| B. 平台钱包     | 账户与资金流         | member 钱包表 | 是，绕不开 | 仍无外部对象       | 中     | 否决                |
| C. 模拟渠道网关 | 假支付宝             | 渠道账户      | 视设计     | 自签自验，纯仪式   | 大     | 否决                |

- **充值为什么不做**：只有钱包模式才需要；模拟充值 = 无中生有加钱，且 B2C 主流是渠道支付，平台余额业务不典型。
- **异步验签为什么不做**：验签防的是「伪造的外部通知」，自己通知自己没有防御对象；回调幂等已由 MQ 消费幂等覆盖。支付宝沙箱（内网穿透 + 异步通知 + 验签）项目一已完整实现，项目二框架优先，重复练习无增量。
- **将来接真支付**：pay 入口收敛在 BFF 一处，订单域只认「置 PAID 的入口」不认渠道——届时在 pay 背后加「预下单 + 处理异步通知」适配层即可，订单域零改动（防腐层思想）。

### 2.3 退款形态

新增 `REFUNDED("60")` 终态，仅 **PAID(20) 且未发货** 可退。语义清晰、列表可显示「已退款」标签、报表可区分取消与退款。退款 = 状态翻转 + 流水记录（无钱包则无真钱要退）。

### 2.4 明确不做清单（Out of Scope）

| #   | 不做项                 | 理由                                     |
| --- | ---------------------- | ---------------------------------------- |
| 1   | 平台钱包 / 余额 / 充值 | 模拟充值无中生有，B2C 不典型，做半截露怯 |
| 2   | 支付宝沙箱对接         | 项目一已完成（内网穿透），项目二框架优先 |
| 3   | 异步回调 / 验签        | 无外部回调方，无防御对象                 |
| 4   | 动态菜单 9 表体系      | 两角色场景用菜单过滤 + 权限码判定足够    |
| 5   | Nginx / 连接池压测调参 | 归入压测与生产部署专项，不抢功能         |
| 6   | SKU/规格体系裁剪       | 反向超出规格书，保留现状                 |

---

## 三、任务总览与排期（Day 1 – Day 10）

```
Day 1     ██████░░░░  支付后端：REFUNDED 状态 + pay_log 表 + trade pay/refund 接口 + 超时竞争
Day 2     ████████░░  支付前端：BFF 聚合 + owner-web 收银台/倒计时 + 双端状态标签与操作
Day 3     █████████░  秒杀订单超时关单 + Redis 库存回补 + 防重键清理
Day 4     ██████████  购物车编辑：勾选 / 批量删除 / 按选中结算
Day 5     ██████████  会员管理操作 + BigDecimal 收尾（SeckillOrderMessage）
Day 6     ██████████  热点缓存：商品详情 Redis + 击穿防护（逻辑过期）
Day 7     ██████████  登录图形验证码（Redis 一次性）
Day 8     ██████████  RBAC 两角色：角色管理页 + 菜单过滤 + 权限判定
Day 9     ██████████  行政区划三级联动（收货地址）
Day 10    ██████████  全链路回归 + 文档同步（业务状况 / 亮点与难点 / README）
```

| #    | 任务                | 优先级   | 涉及模块                                                 |
| ---- | ------------------- | -------- | -------------------------------------------------------- |
| 4.1  | 模拟支付 + 退款闭环 | **P0**   | trade-spi / trade-api / web(BFF) / owner-web / admin-web |
| 4.2  | 秒杀订单超时关单    | **P0**   | seckill-api                                              |
| 4.3  | 购物车编辑          | P1       | trade-spi / trade-api / web / owner-web                  |
| 4.4  | 会员管理操作        | P1       | member-spi / member-api / auth-api / web / admin-web     |
| 4.5  | 热点缓存            | P1       | product-api / web                                        |
| 4.6  | 登录验证码          | P2       | web(BFF) / gateway / owner-web                           |
| 4.7  | RBAC 两角色         | P2       | auth-api / web / admin-web                               |
| 4.8  | 行政区划            | P2       | member-spi / member-api / web / owner-web                |
| 4.9  | BigDecimal 收尾     | P2       | common / seckill-api                                     |
| 4.10 | 连接池 / Nginx      | 部署阶段 | 压测与生产化专项                                         |

---

## 四、任务详细设计

### 4.1 模拟支付 + 退款闭环（Day 1-2，P0）

#### 现状（已核实）

- 订单五态 `PENDING(10)/PAID(20)/SHIPPED(30)/COMPLETED(40)/CANCELLED(50)`，[OrderStatus.java](../../../goods-store-service/goods-store-trade-api/src/main/java/com/fengluan/trade/enums/OrderStatus.java)。
- **无任何支付/退款接口**：[OrderApi.java](../../../goods-store-spi/goods-store-trade-spi/src/main/java/com/fengluan/spi/trade/OrderApi.java) 仅 create/page/detail/cancel/confirm/adminPage/adminDetail/ship。
- [OrderEntity.java](../../../goods-store-service/goods-store-trade-api/src/main/java/com/fengluan/trade/entity/OrderEntity.java) 已备字段：`payType`、`alipayTradeNo`、`payTime`、`totalPay(BigDecimal)` —— **支付痕迹可全部复用，无需加列**。
- [OrderCancelConsumer.java](../../../goods-store-service/goods-store-trade-api/src/main/java/com/fengluan/trade/mq/OrderCancelConsumer.java) 超时取消**已做状态校验**：`非 PENDING 直接 ack 跳过` —— 支付/超时竞争的后半段天然已成立，只需补 pay 侧 CAS。
- 秒杀订单与普通订单**共用 `order` 表**（[SeckillOrderEntity](../../../goods-store-service/goods-store-seckill-api/src/main/java/com/fengluan/seckill/entity/SeckillOrderEntity.java) 同映射 `order`，以 `seckillNo` 区分），trade 订单列表天然包含秒杀订单 → **支付能力对秒杀订单自动生效**。

#### Day 1：后端

**Step 1 状态机扩展**：`OrderStatus` 增加 `REFUNDED("60", "已退款")`。

**Step 2 pay_log 表**（对齐项目建表风格，唯一索引兜底幂等）：

```sql
CREATE TABLE `pay_log` (
  `id`            bigint         NOT NULL COMMENT '雪花ID',
  `order_id`      bigint         NOT NULL COMMENT '订单ID',
  `order_no`      varchar(32)    NOT NULL COMMENT '订单编号',
  `member_account` varchar(32)  NULL     COMMENT '会员账号',
  `biz_type`      varchar(10)    NOT NULL COMMENT '业务类型：PAY-支付 / REFUND-退款',
  `channel`       varchar(20)    NOT NULL COMMENT '渠道：ALIPAY/WECHAT（均模拟）',
  `trade_no`      varchar(64)    NOT NULL COMMENT '模拟渠道流水号（雪花生成）',
  `amount`        decimal(10, 2) NOT NULL COMMENT '金额',
  `created_time`  datetime       NOT NULL,
  UNIQUE KEY `uk_order_biz` (`order_id`, `biz_type`),
  KEY `idx_order_no` (`order_no`)
) COMMENT '支付/退款流水（模拟）';
```

**Step 3 SPI + 实现接口**（[OrderApi.java](../../../goods-store-spi/goods-store-trade-spi/src/main/java/com/fengluan/spi/trade/OrderApi.java) / [OrderController.java](../../../goods-store-service/goods-store-trade-api/src/main/java/com/fengluan/trade/api/OrderController.java)）：

| HTTP | 路径                           | 语义                                                                                                                                                                                                   |
| ---- | ------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| PUT  | `/trade/api/order/{id}/pay`    | body `{payType}`；CAS 更新 `status='20', pay_time=now, pay_type, alipay_trade_no=雪花` **where id=? and status='10'**；影响行数=0 抛「订单状态已变更（已支付/已超时取消）」；事务内插入 `pay_log(PAY)` |
| PUT  | `/trade/api/order/{id}/refund` | CAS 更新 `status='60'` **where id=? and status='20'**；置 REFUNDED + 插入 `pay_log(REFUND)`；非 PAID 拒绝（已发货走线下）                                                                              |

**Step 4 竞争与幂等语义自检**：

| 场景                                   | 结果                                                  |
| -------------------------------------- | ----------------------------------------------------- |
| 用户第 29 分 59 秒点支付，超时消息后到 | 消费者查到非 PENDING → ack 跳过（**现有逻辑**）       |
| 超时取消先落库（status=50），支付后到  | pay CAS `where status='10'` 影响行数 0 → 返回业务错误 |
| 收银台重复点击 / 网络重试              | 第二次 CAS 失败 + `uk_order_biz` 唯一索引双保险       |
| 退款重复提交                           | 同上 CAS + 唯一索引                                   |

#### Day 2：BFF + 双端

**Step 1 BFF**（[WebOrderController.java](../../../goods-store-web/src/main/java/com/fengluan/web/trade/WebOrderController.java) / WebOrderMgrService）：新增 `PUT /app/api/order/{id}/pay`、`PUT /app/api/order/{id}/refund` 透传。网关无需动（登录态接口，不进白名单）。

**Step 2 owner-web 收银台**：

- 路由 `/cashier/:id`（新建 `views/cashier/index.vue`）；`checkout/index.vue` 提交成功后由 `router.push("/order")` 改为跳收银台。
- 页面元素：订单摘要（编号/金额/商品数）+ 支付方式单选（支付宝/微信，卡片上标注「模拟」）+ **剩余支付时间倒计时**（`checkoutTime + 30min - now`，归零置灰并提示「订单已超时」）+「确认支付」按钮（loading 防重复提交）。
- 支付成功 → 跳订单详情（状态已 PAID）；支付失败/超时 → 停留并提示，可回订单列表。

**Step 3 owner-web 订单状态与操作**（[types.ts](../../../goods-store-frontend/owner-web/src/api/types.ts) `ORDER_STATUS_TEXT`/`ORDER_STATUS_TAG` 加 `60 已退款/danger`；`order/list.vue`、`order/detail.vue`）：

- 状态 Tab 增加「已退款」；`OrderStatus` 类型联合加 `'60'`。
- 待付款(10) 加「去支付」按钮 → 收银台；已支付(20) 加「申请退款」按钮（确认框提示模拟退款即时到账状态翻转）。
- 详情页 `el-steps` 对 REFUNDED 显示退款提示条（对齐已取消样式）。

**Step 4 admin-web**（`views/order/index.vue`）：状态下拉与 tag 映射加 60；操作列对 `status==='20'` 增加「退款」（挂 `v-permission="'order:refund'"`，与现有 `order:ship` 同款指令）。

#### 验收标准

- [ ] 全链路：下单 → 收银台（倒计时可见）→ 支付 → 订单 PAID → 管理端发货 → 确认收货 → COMPLETED
- [ ] PAID 未发货申请退款 → REFUNDED，`pay_log` 有 PAY + REFUND 两条流水
- [ ] 倒计时归零后订单被 `order.timeout` 取消，收银台支付按钮置灰；取消后点支付返回业务错误
- [ ] 支付/退款接口重复调用只生效一次
- [ ] 秒杀订单同样可完成「支付 → 发货 → 收货」全流程（共用 order 表天然生效）

#### 完成状态（2026-09-06）

Day 1（后端）与 Day 2（BFF + 双端）代码已全部实现，双端构建验证通过（`vue-tsc` / `mvn` 均 exit 0）：

- ✅ `OrderStatus.REFUNDED("60", "已退款")`；`pay_log` 表（`PayLogEntity` / `PayLogMapper`，`uk_order_biz(order_id, biz_type)` 唯一索引兜底幂等）；`OrderPayRequest`
- ✅ `OrderApi` 新增 `pay` / `refund` / `adminRefund`；trade-api `OrderServiceImpl` 实现（CAS 条件更新 + 事务内落 `pay_log` 流水）
- ✅ BFF：`PUT /app/api/order/{id}/pay`、`PUT /app/api/order/{id}/refund`（会员）、`PUT /app/api/order/admin/{id}/refund`（管理端）透传
- ✅ owner-web 收银台 `/cashier/:id`（订单摘要 + 模拟支付方式选择 + 30 分钟倒计时 + 确认支付）；`checkout` 提交后跳收银台
- ✅ 双端订单状态 `60`「已退款」标签 + 去支付 / 申请退款 / 管理端退款按钮（`order:refund` 权限码）

待办（运行时验收，受商品查询 500 阻断，暂未执行）：

- 手动执行 `sql/table_structure_export.sql` 第 31 节 `pay_log` 建表 SQL
- 重启 trade-api / auth-api / BFF 使新代码生效
- 修复 product-api 商品查询 500（`GET /good/api/{id}`、`/good/api/page`、`/category/api/page` 均返回「服务器繁忙」）

### 4.2 秒杀订单超时关单（Day 3，P0）

#### 现状（已核实）

- [SeckillOrderConsumer](../../../goods-store-service/goods-store-seckill-api/src/main/java/com/fengluan/seckill/mq/SeckillOrderConsumer.java) 建单后**不发任何超时消息** → 秒杀订单永远 PENDING。
- Redis key（[RedisConfig.java](../../../goods-store-service/goods-store-seckill-api/src/main/java/com/fengluan/seckill/config/RedisConfig.java)）：库存 `seckill:stock:{seckillGoodId}`、防重 `seckill:order:{memberId}:{seckillGoodId}`。
- `compensate()` 已实现库存 `increment` + 防重键 `delete` —— 回补逻辑现成，关单可复用。

#### 实现内容

| 序号 | 任务                 | 涉及文件                                              | 备注                                                                                                                                          |
| ---- | -------------------- | ----------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------- |
| 1    | TTL 队列 + 死信绑定  | `seckill/config/SeckillMqConfig.java`                 | 对齐 trade 的 `order.timeout` 模式：TTL 30 分钟 → DLX → 关单队列                                                                              |
| 2    | 建单成功后发超时消息 | `seckill/mq/SeckillOrderConsumer.java`                | 事务提交后发（对齐 trade 下单的 afterCommit 风格），消息体 orderNo                                                                            |
| 3    | 关单消费者           | `seckill/mq/SeckillOrderTimeoutConsumer.java`（新建） | 按 orderNo 查 `SeckillOrderEntity`，**非 "10" 直接 ack**；是则置 CANCELLED("50") + 库存 incr + 删防重键（抽公共方法，与 `compensate()` 共用） |

#### 设计要点

- 秒杀订单与 trade 订单的关单队列**各自独立**（trade 只对自己的 orderNo 发 `order.timeout`；seckill 同理），互不干扰，orderNo 全局唯一由雪花保证。
- 秒杀**不扣 DB 库存**（Redis 是唯一扣减点），所以关单回补只操作 Redis，不调 `restoreStock`。
- 防重键删除后该会员可再次参与同一场秒杀——超时未支付本就不应占用资格，语义正确。
- 与 4.1 衔接：秒杀订单支付走收银台，若 30 分钟内支付成功，关单消费者查到非 "10" 直接 ack（与订单超时取消同一套竞争语义）。

#### 验收标准

- [ ] 抢购成功不支付 → 30 分钟后订单自动 CANCELLED，Redis 库存 +1、防重键消失
- [ ] 抢购后 29 分钟支付 → 关单消息到达后 ack 跳过，库存不被误回补
- [ ] 回补后库存可再次被抢购（连续两次抢同一场，第一次超时回补后第二次成功）

### 4.3 购物车编辑（Day 4，P1）

#### 现状（已核实）

- DB 表 `cart`（唯一索引 `uq_member_good`），已有：查列表 / 加购 / 改数量 `PUT /cart/{cartId}?qty=` / 单删 `DELETE /cart/{cartId}`（[CartApi.java](../../../goods-store-spi/goods-store-trade-spi/src/main/java/com/fengluan/spi/trade/CartApi.java)）。
- **无勾选字段、无批量删除**；结算是对购物车全部商品整单结算（`checkout/index.vue`）。

#### 实现内容

| 序号 | 任务                                       | 涉及文件                                            | 备注                                                                                                      |
| ---- | ------------------------------------------ | --------------------------------------------------- | --------------------------------------------------------------------------------------------------------- |
| 1    | `cart` 表加 `selected bit(1) DEFAULT b'1'` | `sql/table_structure_export.sql` + ALTER 语句       | `CartEntity`/`CartVO` 同步加字段                                                                          |
| 2    | 勾选接口                                   | `CartApi` + `CartController` + `CartServiceImpl`    | `PUT /trade/api/cart/selected`（body：cartId 列表 + selected 布尔，一次请求支持全选/全不选）              |
| 3    | 批量删除接口                               | 同上                                                | `DELETE /trade/api/cart/batch`（body：cartId 列表），事务内逐条/批量删                                    |
| 4    | BFF 转发                                   | `WebCartController` / `WebCartService`              | `PUT /app/api/cart/selected`、`DELETE /app/api/cart/batch`                                                |
| 5    | 下单按选中过滤                             | `web/trade/WebOrderService.submit` 及 checkout 聚合 | 结算页只读选中项；**实现时核实 OrderCreateRequest 是否含商品清单**，若 BFF 内部取全量购物车，改为取选中项 |
| 6    | owner-web 购物车页                         | `views/cart/index.vue`                              | 行勾选框 + 表头全选 + 「批量删除」按钮 + 底部结算条只统计选中项；无选中时「去结算」禁用                   |

#### 验收标准

- [ ] 勾选/全选/取消全选即时生效并持久化（刷新后保留）
- [ ] 批量删除一次清掉多行；结算金额只含选中商品
- [ ] 全不选时无法结算；下单后购物车中被结算项清除（沿用现有下单清车逻辑，若有则回归验证）

### 4.4 会员管理操作 + BigDecimal 收尾（Day 5，P1）

#### 会员管理现状（已核实）

- [MemberEntity](../../../goods-store-service/goods-store-member-api/src/main/java/com/fengluan/member/entity/MemberEntity.java) **已有 `enabled Boolean`（bit default 1）字段**，但无任何启用/禁用接口；管理端会员页**只有查询**。
- 已有编辑资料接口 `PUT /member/api/{id}`（`MemberProfileUpdateRequest`）。

#### 实现内容

| 序号 | 任务                 | 涉及文件                                                                                                              | 备注                                                                                                                                                                 |
| ---- | -------------------- | --------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1    | 启/禁用接口          | `MemberApi` + `MemberController` + Service                                                                            | `PUT /member/api/{id}/enabled?enabled=false`；返回脱敏 VO                                                                                                            |
| 2    | BFF 转发             | `web/member/*`（管理端聚合）                                                                                          | `PUT /app/api/member/admin/{id}/enabled`                                                                                                                             |
| 3    | 禁用生效点：登录拦截 | `auth-api` 登录链路                                                                                                   | 实现时核实 auth 登录取会员的方式（Feign member），登录校验 `enabled=false` 返回「账号已被禁用」；token 有效期内已登录用户可自然过期（不强踢，简化）                  |
| 4    | admin-web 操作列     | `views/member/index.vue`                                                                                              | 启/禁 switch（带确认）+ 「编辑」dialog（复用 `MemberProfileUpdateRequest` 字段）                                                                                     |
| 5    | BigDecimal 收尾      | [SeckillOrderMessage.java](../../../goods-store-common/src/main/java/com/fengluan/common/mq/SeckillOrderMessage.java) | `seckillPrice Long → BigDecimal`；发送处（`SeckillOrderServiceImpl`）与消费处（`SeckillOrderConsumer` 建单 totalPay/dealPrice）同步适配；JSON 序列化为数字无兼容问题 |

#### 验收标准

- [ ] 管理端禁用会员 → 该会员重新登录被拒（提示账号已禁用）；启用后恢复
- [ ] 会员资料编辑保存生效
- [ ] `SeckillOrderMessage` 全链路 BigDecimal 化，抢购下单金额分毫不差（DB decimal 与 Java 类型一致）

### 4.5 热点缓存：商品详情 + 击穿防护（Day 6，P1）

#### 现状（已核实）

- product 服务**零缓存**：详情每次打 DB；SkyWalking 已暴露商品列表存在 N+1 查询（每商品逐一调 brand-api）。

#### 实现内容

| 序号 | 任务                       | 涉及文件                      | 备注                                                                                                                                                                                                                                    |
| ---- | -------------------------- | ----------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1    | 详情缓存（Cache Aside）    | `product-api` GoodServiceImpl | key `product:good:{id}`，value `GoodVO`(JSON)；读：先缓存后 DB 回填；管理端**更新/删除商品时删缓存**（旁路一致性）                                                                                                                      |
| 2    | **击穿防护——逻辑过期方案** | 同上 + 工具方法               | 热 key 物理 TTL 设置很长（如 24h），value 内嵌逻辑过期时间（如 30min）；命中且未逻辑过期 → 直接返回；命中但逻辑过期 → `setnx` 抢互斥锁，抢到者**异步线程**回源重建并刷新，未抢到/抢失败者**先返回旧值**（牺牲一致性保可用，防 DB 打穿） |
| 3    | 列表缓存（可选轻量）       | GoodServiceImpl 分页查询      | 短 TTL（如 60s）或暂不做，重点在详情；N+1 顺带评估批量查 brand 修复                                                                                                                                                                     |
| 4    | 预热                       | 可选                          | 复用思路：热销/秒杀关联商品提前加载（对应商品 isHot 标记）                                                                                                                                                                              |

> 面试叙事：秒杀 = 「缓存三边界（不存在/超卖/防重）+ Lua 原子化」；详情 = 「击穿防护（逻辑过期 + 互斥重建）+ 旁路一致性」。两条拼成完整 Redis 实践。

#### 验收标准

- [ ] 详情页二次访问不再产生 SQL Span（SkyWalking UI 验证）
- [ ] 管理端改商品后详情即时可见新数据（删缓存生效）
- [ ] 模拟击穿：缓存逻辑过期瞬间并发 100 请求详情，DB 只承受 1 次回源（日志/Trace 计数验证），期间请求全部有返回（旧值）

### 4.6 登录图形验证码（Day 7，P2）

#### 实现内容

| 序号 | 任务                       | 涉及文件                                                                                                                                                 | 备注                                                                                                                 |
| ---- | -------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------- |
| 1    | 验证码生成接口（BFF 本地） | `web/auth/*` 新增 `GET /app/api/auth/captcha`                                                                                                            | Hutool `CaptchaUtil` 生成 base64 + uuid；Redis `captcha:{uuid}` 存 code，TTL 5 分钟，**一次性**（校验后立即删除）    |
| 2    | 网关白名单                 | [WhiteListConfig.java](../../../goods-store-gateway/src/main/java/com/fengluan/gateway/config/WhiteListConfig.java) 默认列表追加 `/app/api/auth/captcha` | 登录前接口必须放行                                                                                                   |
| 3    | 登录校验                   | `web/auth` BFF 聚合层（调 auth-api 之前）                                                                                                                | `LoginRequest` 加 `captchaId/captchaCode` 透传字段（spi DTO 变更）；BFF 校验 Redis 比对，不匹配拒绝，不透传 auth-api |
| 4    | owner-web 登录页           | `views/login/index.vue`                                                                                                                                  | 验证码输入框 + 图片展示 + 点击刷新；`api/auth.ts` login 增加 captcha 参数                                            |

管理端登录验证码可选（同机制复制即可，默认先只做用户端）。

#### 验收标准

- [ ] 验证码错误/过期均拒绝登录且不消耗登录失败计数
- [ ] 同一验证码用第二次即失效（一次性）
- [ ] 点击图片可刷新换新码

### 4.7 RBAC 两角色（Day 8，P2）

#### 现状（已核实）

- auth-api RBAC 6 实体（admin_user / role / permission + 两关联表）齐备；角色断言在 BFF 层 `AdminRoleInterceptor`。
- admin-web 菜单硬编码于 [AdminLayout.vue](../../../goods-store-frontend/admin-web/src/layouts/AdminLayout.vue) `menuItems`；**已有 `v-permission` 自定义指令**（订单发货按钮挂 `order:ship`）——前端判定机制已具雏形，本任务主要是补数据与闭环。

#### 两角色权限划分（建议）

| 权限码                                                                          | ADMIN（超级管理员） | OPERATOR（运营） |
| ------------------------------------------------------------------------------- | ------------------- | ---------------- |
| dashboard:view / product:manage / brand:manage / order:manage（含 ship/refund） | ✔                   | ✔                |
| seckill:manage / member:manage                                                  | ✔                   | ✘                |
| 角色管理（role:manage）                                                         | ✔                   | ✘                |

#### 实现内容

| 序号 | 任务           | 涉及文件                                         | 备注                                                                                                                |
| ---- | -------------- | ------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------- |
| 1    | 权限数据初始化 | `sql/auth_rbac.sql` 增补                         | permission 造权限码、role 造两角色、role_permission 绑定、admin_user 挂 OPERATOR 测试账号                           |
| 2    | 登录返回权限码 | auth-api adminLogin 响应                         | 从 role→permission 联查返回 `permissions: []`（现有若只返 roles 则扩展）                                            |
| 3    | 菜单过滤       | `AdminLayout.vue` menuItems 加 `permission` 字段 | 按 Pinia 中的权限码过滤渲染                                                                                         |
| 4    | 指令与按钮     | `v-permission` 指令扩展复用                      | 新页面操作按钮统一挂权限码（order:refund 等）                                                                       |
| 5    | 角色管理页     | `views/role/index.vue`（新建）+ 路由/菜单        | 仅 ADMIN 可见：角色列表 + 权限分配（穿梭框/树勾选）+ BFF `AdminRoleInterceptor` 语义对齐                            |
| 6    | 后端判定       | BFF 管理端聚合层                                 | 由「角色断言」升级为「权限码断言」（拦截器读 X-User-Roles + 登录时缓存权限码；两角色规模不引入网关 path+method 表） |

#### 验收标准

- [ ] OPERATOR 登录后菜单只见被授权项；直接输 URL 访问被守卫拦截
- [ ] 无权限按钮（如秒杀管理入口）不渲染，接口层同样被拒（403 语义）
- [ ] ADMIN 可在角色管理页调整 OPERATOR 权限并即时生效（重新登录后）

### 4.8 行政区划三级联动（Day 9，P2）

#### 现状（已核实）

- `t_cn_region_info` 表已就位（约 4.6 万行：`CRI_CODE/CRI_NAME/CRI_PARENT_ID/CRI_LEVEL`，含 `idx_cri_parent_id/idx_cri_level` 索引），**无任何代码引用**。

#### 实现内容

| 序号 | 任务              | 涉及文件                                                 | 备注                                                                                           |
| ---- | ----------------- | -------------------------------------------------------- | ---------------------------------------------------------------------------------------------- |
| 1    | Region SPI + 实现 | `member-spi` `RegionApi` + `member-api` RegionController | `GET /region/api/children?parentId=`；首次 parentId=0 取省级；只查 `CRI_DATA_STATE=1` 有效行   |
| 2    | BFF 转发          | `web/member` 或新建 `web/region`                         | `GET /app/api/region/children`，C 端接口（登录态即可，不进白名单——地址表单在个人中心内）       |
| 3    | 三级联动组件      | owner-web `components/RegionCascade.vue`（新建）         | 省市区三个 select 级联懒加载（选中省才加载市）；选择结果拼接存 `receiverAddrDetail` 前的区域段 |
| 4    | 接入地址表单      | `views/user/index.vue` 地址新增/编辑                     | 省/市/区联动 + 详细地址输入；已有地址回显（拆区域与详细段）                                    |

查询走 `idx_cri_parent_id` 单层孩子查询，无需 Caffeine/Redis（一次 ≤ 40 行）。管理端区域维护页**不做**（纯静态数据）。

#### 验收标准

- [ ] 新增/编辑地址时三级联动流畅（省 34 项秒开，市/区按需加载）
- [ ] 地址保存后订单收货信息含完整「省市区 + 详细地址」
- [ ] 旧地址数据（无区域拆分）不报错、可重新编辑补充

#### 完成状态（2026-09-07）

4.8 全链路代码与配置已实现，owner-web `vue-tsc -b` exit 0，后端新增文件无编译诊断：

- ✅ `member-spi` 新增 `RegionApi`（`GET /children`）+ `RegionVO`
- ✅ `member-api` 新增 `RegionEntity`（映射 `t_cn_region_info` 大写列）/ `RegionMapper` / `RegionService`+`RegionServiceImpl` / `RegionController`（`/region/api`）；查询兼容 `parentId=0` 取省级（DB 省级 `CRI_PARENT_ID IS NULL`），只查 `CRI_DATA_STATE=1`
- ✅ 网关新增 `/region/api/**` 路由（`lb://goods-store-member-api`）
- ✅ BFF 新增 `RegionFeignClient`（`contextId` 隔离同服务双客户端）+ `WebRegionController`（`GET /app/api/region/children`，登录态即可，不进白名单）
- ✅ owner-web 新增 `api/region.ts`、`components/RegionCascade.vue`（省市区三级懒加载 + 名称前缀回显）
- ✅ `views/user/index.vue` 地址新增/编辑改为「地区级联 + 详细地址」，保存拼接、回显按空格拆分（旧数据无分隔回填详细地址）

待办（运行时验收，本迭代不执行测试/验收）：

- 重启 member-api / web / gateway 使新代码生效
- 手动回归地址新增/编辑回显与订单收货地址拼接

### 4.9 连接池 / Nginx（部署阶段，不占本迭代开发日）

- **Druid**：现状每服务 `max-active: 10`（6 服务共 60 连接）。压测阶段按 QPS/慢 SQL 调参，开启 StatViewServlet 监控页（对齐 SkyWalking 慢 SQL 定位）。
- **Nginx**：生产部署时双前端静态托管 + `/app/api`、`/static/upload` 反代网关 8888 + gzip；开发期 vite proxy 已够，不提前引入。

---

## 五、总体验收（Day 10 回归清单）

| #   | 场景           | 通过标准                                                                                         |
| --- | -------------- | ------------------------------------------------------------------------------------------------ |
| 1   | 正常购物全流程 | 加购（勾选）→ 结算 → 收银台支付 → 订单 PAID → 管理端发货 → 确认收货 → COMPLETED                  |
| 2   | 超时取消       | 下单/秒杀不支付 30 分钟 → 自动 CANCELLED（普通订单回补 DB 库存、秒杀回补 Redis 库存 + 清防重键） |
| 3   | 支付竞争       | 第 29 分 59 秒支付成功后超时消息到达 → 不误取消；反向亦然                                        |
| 4   | 退款           | PAID 未发货退款 → REFUNDED + 流水；SHIPPED 后退款被拒                                            |
| 5   | 会员           | 禁用会员登录被拒；启用恢复                                                                       |
| 6   | 验证码         | 错码/过期/复用均拒绝                                                                             |
| 7   | RBAC           | OPERATOR 无秒杀/会员/角色入口（菜单 + 接口双层拦截）                                             |
| 8   | 缓存           | 详情命中无 SQL；击穿场景 DB 单次回源                                                             |
| 9   | 行政区划       | 地址三级联动保存与回显正确                                                                       |
| 10  | 可观测         | 全流程在 SkyWalking 形成 Trace；支付/退款/关单各产生独立可查链路                                 |

文档同步：`doc/业务状况.md`（进展表）、`doc/业务亮点与难点.md`（支付三道防线、击穿防护入亮点）、README 如涉及。

---

## 六、风险与对策

| #   | 风险                                        | 对策                                                                                                      |
| --- | ------------------------------------------- | --------------------------------------------------------------------------------------------------------- |
| 1   | 秒杀与普通订单共用 `order` 表，关单队列双轨 | 队列按服务隔离（trade/seckill 各自 TTL+DLX），消息体同为 orderNo，消费前状态校验统一「非 PENDING 即 ack」 |
| 2   | 勾选结算改动下单链路（现整单结算）          | Day 4 集中回归：下单成功清车、超时取消回补、并发下单锁                                                    |
| 3   | `LoginRequest` 加验证码字段属 spi 契约变更  | auth-api 与 BFF 同批发布；字段可空校验只在 BFF 生效，auth-api 不感知（避免影响管理端登录）                |
| 4   | RBAC 权限数据初始化遗漏导致菜单全隐         | 造数脚本进 `sql/auth_rbac.sql` 增补；ADMIN 兜底全量权限                                                   |
| 5   | 逻辑过期缓存返回旧值被感知为 bug            | 逻辑过期窗口设 30min（对齐业务可接受度）；管理端更新走删缓存旁路，保证写路径强一致                        |
| 6   | 旧地址无区域字段回显                        | 区域信息并入 `receiverAddrDetail` 文本存储，不做表结构迁移，兼容历史数据                                  |

---

## 七、交付物清单

- [ ] `pay_log` 表 + `OrderStatus.REFUNDED` + trade pay/refund 接口（SPI/Controller/Service）
- [ ] BFF `/app/api/order/{id}/pay|refund` + owner-web 收银台页（倒计时）
- [ ] 双端订单状态 60 标签与操作按钮
- [ ] seckill 超时关单队列 + 消费者（Redis 回补 + 防重键清理）
- [ ] cart 勾选/批量删除/选中结算全链路
- [ ] 会员启禁用接口 + admin-web 操作列 + 登录拦截
- [ ] `SeckillOrderMessage` BigDecimal 化
- [ ] 商品详情缓存（逻辑过期击穿防护）
- [ ] 登录验证码（BFF 生成 + Redis 一次性 + 白名单）
- [ ] RBAC 两角色（数据初始化 + 角色管理页 + 菜单过滤 + 权限码判定）
- [ ] 行政区划三级联动（RegionApi + 地址表单）
- [ ] 回归记录 + 三份文档同步（业务状况 / 亮点与难点 / README）
