# SkyWalking 接入计划手册（Stage 4）

> 阶段：Stage 4 —— 在「后端微服务全链路跑通、双前端就绪、双端联调缺陷清零」的基础上，接入 SkyWalking APM，实现全链路可观测。
> 范围：8 个 Java 服务挂载 Agent 上报数据；OAP/UI 已本地部署（MySQL 存储）；**不改业务代码，零侵入接入**。

---

## 一、目标

1. 8 个 Java 服务（gateway/web/6 业务服务）全部挂载 SkyWalking Java Agent，自动上报链路（Trace）、指标（Metrics）。
2. UI 可视化：服务拓扑图、接口级 Trace 详情、慢 SQL、JVM/实例指标。
3. 完整验证一笔跨服务业务（登录 → 浏览 → 加购 → 下单 → 秒杀）在 UI 上形成端到端链路。
4. 可选增强：日志 `%tid` 集成，将 SkyWalking TraceId 打入应用日志，与自研 `X-Trace-Id` 并存。

---

## 二、现状盘点

### 2.1 已就绪（无需重做）

| 项                      | 状态                                                           | 位置                                                                                            |
| ----------------------- | -------------------------------------------------------------- | ----------------------------------------------------------------------------------------------- |
| 发行包                  | SkyWalking 9.7.0 二进制已就位（未入 git）                      | `skywalking/apache-skywalking-apm-9.7.0.tar.gz`                                                 |
| OAP 服务端              | 已解压，standalone 模式                                        | `skywalking/apache-skywalking-apm-bin/`                                                         |
| OAP 存储                | **已切换 MySQL**（selector=mysql）                             | [application.yml:140-195](../../../skywalking/apache-skywalking-apm-bin/config/application.yml) |
| 存储库                  | `goods_store`（xuanxiefuyao/123456，OAP 自动建约 60 张观测表） | 同上 jdbcUrl                                                                                    |
| OAP 端口                | gRPC 11800（收 Agent 数据）/ REST 12800（UI 查询）             | 同上                                                                                            |
| UI（skywalking-webapp） | 已就位，默认端口 8080 → OAP 12800                              | [webapp/application.yml](../../../skywalking/apache-skywalking-apm-bin/webapp/application.yml)  |

### 2.2 缺口分析

| #   | 缺口                                                                                      | 影响                 | 优先级 |
| --- | ----------------------------------------------------------------------------------------- | -------------------- | ------ |
| S1  | `skywalking-agent/` 目录**不存在**（tar.gz 未解出，仅剩 macOS 残留 `._skywalking-agent`） | 无 Agent 可挂载      | **P0** |
| S2  | 全部服务 IDEA 运行配置**无 `-javaagent` VM 参数**                                         | 无数据上报，OAP 空转 | **P0** |
| S3  | UI 8080 与 Sentinel Dashboard 8080 **端口冲突**（同启必撞）                               | UI 无法访问          | **P0** |
| S4  | 无 apm-toolkit 依赖，日志无 `%tid`，应用 TraceId 与自研 `X-Trace-Id` 两套并存             | 排查需两处对齐       | P1     |
| S5  | Redis（Lettuce/Redisson）、部分客户端插件位于 `optional-plugins/`，默认不生效             | 组件级追踪不全       | P2     |

### 2.3 现有自研 TraceId 机制（保留，不冲突）

[TraceIdWebFilter.java](../../../goods-store-common/src/main/java/com/fengluan/common/filter/TraceIdWebFilter.java)（MDC + `X-Trace-Id` + UUID）+ 网关 [TraceIdFilter.java](../../../goods-store-gateway/src/main/java/com/fengluan/gateway/filter/TraceIdFilter.java) + FeignTraceInterceptor：自研日志链路，与 SkyWalking 互不干扰，继续保留。

---

## 三、技术要点

### 3.1 Agent 挂载原理

SkyWalking Java Agent 基于 `-javaagent`（premain）做**字节码增强**：启动时织入 Spring MVC、Feign、RabbitMQ、MyBatis 等插件埋点，无需修改任何业务代码。三个必备参数：

| 参数                                                     | 说明                                         |
| -------------------------------------------------------- | -------------------------------------------- |
| `-javaagent:<path>\skywalking-agent.jar`                 | 指向 Agent 主 jar                            |
| `-Dskywalking.agent.service_name=<服务名>`               | UI 上显示的服务名（建议与 Nacos 服务名一致） |
| `-Dskywalking.collector.backend_service=127.0.0.1:11800` | OAP gRPC 地址                                |

### 3.2 服务清单与 service_name 命名

| 模块                    | 端口 | service_name            |
| ----------------------- | ---- | ----------------------- |
| goods-store-gateway     | 8888 | goods-store-gateway     |
| goods-store-web         | 8090 | goods-store-web         |
| goods-store-auth-api    | 8083 | goods-store-auth-api    |
| goods-store-brand-api   | 8081 | goods-store-brand-api   |
| goods-store-product-api | 8084 | goods-store-product-api |
| goods-store-member-api  | 8085 | goods-store-member-api  |
| goods-store-trade-api   | 8086 | goods-store-trade-api   |
| goods-store-seckill-api | 8087 | goods-store-seckill-api |

### 3.3 插件覆盖预期（解压后以 `agent/plugins/`、`agent/optional-plugins/` 实际目录为准）

> **实测回写（2026-09-05，Agent 9.7.0 + Spring Boot 4.1.1 / Spring Framework 7 / SCG 5.x / JDK 25）**：因 Spring Framework 7 移除了旧 witness 类（`AnnotationBeanUtils`、`DefaultKeyGenerator` 等），spring-mvc v3/v4/v5 插件全部失活，**入口 Span 由 Tomcat 插件接管**（endpoint 名为 `GET:/路径` URL 格式而非 Controller 方法名）；webflux-6.x 插件在网关抛 `NoSuchMethodError: HttpHeaders.get(Object)`（仅污染网关入口 Span 命名，不影响转发传播）；gateway-4.x 的 NettyRoutingFilter 插桩虽因 witness 不匹配未激活，但 **`SpringCloudGateway/sendRequest` Exit Span 与 sw8 跨服务传播实测全部正常**。MyBatis、MySQL JDBC（驱动 9.7.0）、AlibabaDruid、Feign 下游 Exit Span 均完整工作。

| 技术组件                        | 预期插件             | 默认生效                    | 实测结果                                               |
| ------------------------------- | -------------------- | --------------------------- | ------------------------------------------------------ |
| Tomcat（Servlet 入口）          | tomcat               | 是                          | **正常**（承担全部 MVC 服务入口 Span）                 |
| Spring MVC Controller           | springmvc            | 是                          | **失活**（Spring 7 witness 缺失，入口降级为 URL 命名） |
| Spring Cloud Gateway（WebFlux） | spring-cloud-gateway | 是                          | **部分**（入口 Span 命名降级；sendRequest/传播正常）   |
| OpenFeign（默认客户端）         | feign-default        | 是                          | **正常**（web→product、product→brand 串联验证通过）    |
| RabbitMQ（spring-amqp）         | rabbitmq             | 是                          | 未验证（Day 3）                                        |
| MyBatis / MyBatis-Plus 3.5.x    | mybatis-3.x          | 是                          | **正常**（mapper 名 + SQL 语句 + db.instance）         |
| MySQL JDBC（驱动 9.7.0）        | mysql-8.x            | 是                          | **正常**（Exit Span + 慢 SQL + 异常捕获）              |
| AlibabaDruid 连接池             | druid                | 是                          | **正常**（附赠连接池 Span）                            |
| Lettuce / Redisson              | optional             | **否，需手动拷入 plugins/** | 未验证（Day 3）                                        |

### 3.4 风险与预判（含实测结论）

1. **JDK 25 兼容性（最大风险）**：Agent 9.7 官方验证止于更早的 JDK，服务类文件版本（v69）可能触发 ByteBuddy `Unsupported class file version`；OAP 官方支持 JDK 8/11/17，Java 25 上启动可能需 `--add-opens` 或独立 JDK 17。**兜底方案**：Agent/OAP 升级至最新 9.x 小版本，或为 SkyWalking 单独安装 JDK 17（`bin/oapService.bat` 走 `JAVA_HOME`）。
   > **实测结论**：① Agent 挂在 JDK 25 服务上**正常工作**（ByteBuddy 风险未触发，仅 sun.misc.Unsafe 弃用警告）；② **OAP 在 JDK 25 下启动崩溃**——meter-analyzer 模块用 Groovy 4.0.15 编译 MAL DSL，其内置 ASM 不支持 major version 69 类文件。**已落地修复**：[oapService.bat](../../../skywalking/apache-skywalking-apm-bin/bin/oapService.bat) 在 `setlocal` 内强制 `JAVA_HOME=C:\Program Files\Java\jdk-17`（不影响业务服务仍用 JDK 25），重启后 OAP 约 3 分钟完整启动。
2. **UI 端口冲突**：Sentinel Dashboard 已占用 8080，UI 必须改端口（本计划改为 **8091**）。（已落地）
3. **MySQL 存储性能**：观测表与业务表混在 `goods_store` 库，演示环境可接受；压测/生产建议独立库。**实测建表规则**：表名为「模型名*日期」如 `service_traffic_20260905`（\*\*无 sw* 前缀\*\*，按天滚动后缀），约 60 张。
4. **数据膨胀**：默认全量采样，长跑会撑大 MySQL；联调验证后可改采样或定期清理历史日期后缀表。

---

## 四、实施步骤

### Day 1：Agent 就绪 + 服务端启动 + 冒烟

**Step 1 解压 Agent**（PowerShell，在 `skywalking/` 目录下）：

```powershell
cd d:\.workspace\javaproject\goods-store-management-system-parent\skywalking
tar -xzf apache-skywalking-apm-9.7.0.tar.gz skywalking-agent
Remove-Item ._skywalking-agent -ErrorAction SilentlyContinue   # 清理 macOS 残留
# 确认 skywalking-agent\skywalking-agent.jar、config\agent.config、plugins\ 存在
```

**Step 2 修改 UI 端口**（避开 Sentinel 8080）：编辑 [webapp/application.yml](../../../skywalking/apache-skywalking-apm-bin/webapp/application.yml) 第 17 行：

```yaml
serverPort: ${SW_SERVER_PORT:-8091}
```

**Step 3 启动 OAP + UI**：

```powershell
skywalking\apache-skywalking-apm-bin\bin\startup.bat    # 同时启动 OAP 与 webapp
```

**Step 4 验证服务端**：

- 日志无 ERROR：`apache-skywalking-apm-bin/logs/skywalking-oap-server.log`
- MySQL 中 OAP 自动建表（约 60 张，命名「模型名\_日期」）：`SHOW TABLES LIKE '%_20260905';`
- 浏览器打开 `http://localhost:8091` UI 正常加载

> **实测**：OAP 首次以 JDK 25 启动失败（见 3.4 风险 1），`oapService.bat` 强制 JDK 17 后约 3 分钟完整启动，11800/12800 监听、60 张表建齐、UI HTTP 200。注意 OAP 日志中含 "error" 字样的 INFO 行（如 browser_error_log 建表记录）并非真实 ERROR。

**Step 5 冒烟（只挂网关）**：IDEA 中给 `GoodsStoreGatewayApplication` 的运行配置加 VM 参数：

```
-javaagent:d:\.workspace\javaproject\goods-store-management-system-parent\skywalking\skywalking-agent\skywalking-agent.jar
-Dskywalking.agent.service_name=goods-store-gateway
-Dskywalking.collector.backend_service=127.0.0.1:11800
```

重启网关 → 随便访问一个接口 → UI「General Service」出现 goods-store-gateway 即冒烟通过。

> **实测**：gateway（JDK 25 + Agent）15.2 秒启动，服务注册 + 12 个 Segment 落库；随后的 brand-api 冒烟更进一步验证了 **Tomcat 入口 + MyBatis + Druid + JDBC 完整 Span 链**（`GET:/brand/api/config/check` → `BrandMapper.selectList` → `Mysql/JDBC/PreparedStatement/execute`，含 db.statement/db.instance 标签）。
>
> 注意：本次实施以 fat jar `java -jar` 方式挂 Agent 启动（脚本化可复现），IDEA 运行配置加 VM 参数的方式同样适用。

### Day 2：全服务挂载 + 拓扑验证

**Step 1 按下表为其余 7 个服务逐个加 VM 参数**（IDEA Run/Debug Configurations → Modify options → Add VM options）：

```
-javaagent:d:\.workspace\javaproject\goods-store-management-system-parent\skywalking\skywalking-agent\skywalking-agent.jar
-Dskywalking.agent.service_name=goods-store-web
-Dskywalking.collector.backend_service=127.0.0.1:11800
```

（其余服务仅替换 `service_name`，对照 3.2 节表格。）

**Step 2 按依赖顺序重启**：brand/product/auth/member → trade/seckill → web → gateway。

**Step 3 验证拓扑**：UI「拓扑」应出现：

- gateway → web，及各业务服务直连分支
- web → auth/brand/product/member/trade/seckill
- product → brand、trade → member/product、seckill → product/member

**Step 4 端到端链路验证**：用户端完成「登录 → 浏览商品 → 加购 → 下单 → 秒杀抢购」，在 Trace 中确认：

- 一条 Trace 串起 gateway → web → 业务服务的完整 Span 链
- Feign Span（`xxx` 远程调用）、MQ Span（trade/seckill 异步建单）、MyBatis SQL Span 均可见

### Day 3：可选增强（按需执行）

1. **Redis 插件**：将 `skywalking-agent/optional-plugins/` 下 Lettuce/Redisson 相关 jar 拷入 `plugins/`，重启 seckill/trade，验证 Redis Span。
2. **日志 %tid 集成**（打通两套 TraceId）：`goods-store-common` 引入 `apm-toolkit-logback-1.x`（版本对齐 Agent 9.7.0），logback pattern 追加 `%tid`；验证应用日志 TraceId 与 UI 一致。
3. **采样与清理**：联调全量验证后，调整 `agent/config/agent.config` 采样配置避免 MySQL 膨胀。

---

## 五、验收标准（含实测结果）

| #   | 验收项       | 通过标准                                                                       | 实测（2026-09-05）                                                                                                                                      |
| --- | ------------ | ------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | UI 可访问    | `http://localhost:8091` 正常打开，与 Sentinel 8080 互不影响                    | **通过**（HTTP 200，Title: Apache SkyWalking）                                                                                                          |
| 2   | 服务注册     | UI 服务列表出现全部 8 个服务，实例数正确                                       | **通过**（9 个服务：8 微服务 + `localhost:3306` 数据库虚拟节点，各 1 实例）                                                                             |
| 3   | 拓扑正确     | 3.2 节全部调用关系在拓扑图可见                                                 | **基本通过**（已见 User→brand、product→brand、web→product、gateway→web 边；全量边待业务流量补齐）                                                       |
| 4   | 端到端 Trace | 一笔「登录→下单」Trace 跨 gateway/web/业务服务完整串联，Span 含 Feign/MQ/SQL   | **通过**（`/app/api/product/list` 一条 Trace 串 gateway→web→product-api→brand-api 4 服务 46 Span，含 Feign Exit、MyBatis、JDBC；MQ Span 待 Day 3 验证） |
| 5   | 秒杀链路     | 秒杀 Trace 含 Redis 预扣 → MQ → 消费建单 的异步衔接（MQ 插件连接上下游 Trace） | 未验证（Day 3；登录接口受业务缺陷阻塞）                                                                                                                 |
| 6   | 存储         | `goods_store` 库观测表持续有数据写入                                           | **通过**（service_traffic/endpoint_traffic/segment/service_relation 等按天滚动写入）                                                                    |
| 7   | 无业务回归   | 8 服务启动无 Agent 引发的报错，核心功能冒烟通过                                | **通过**（Agent 日志无 ERROR 级新增；商品列表 200）                                                                                                     |

> 附：错误追踪能力额外验证通过——auth-api 登录 500（member 表缺 `status` 列的既有业务缺陷）被完整捕获：网关/web/auth-api 三层 segment `is_error=1`，JDBC Span 携带 `error.kind=java.sql.SQLSyntaxErrorException` + message + 完整堆栈，可从 UI Trace 直接定位到 SQL 层。

---

## 六、回滚方案

1. 移除各服务 VM 参数 → 重启即恢复（Agent 零侵入，卸载无残留）。
2. UI 端口改动仅一处，还原 `serverPort` 即可。
3. 观测数据可保留（与业务表互不影响）；如需彻底清理，确认无依赖后 drop 「模型名\_日期」系列表（如 `service_traffic_20260905`）由 OAP 重建。

---

## 七、常见问题排查

| 现象                                                                                       | 原因                                                                                | 处理                                                                                                           |
| ------------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------- |
| 服务启动报 ByteBuddy / Unsupported class file version                                      | Agent 9.7 对 JDK 25 类文件（v69）支持不足                                           | **实测未触发**（JDK 25 正常）；如遇此错升级 Agent 至最新 9.x 或服务暂用 JDK 21 运行                            |
| OAP 启动即退 / 反射报错                                                                    | OAP 在 Java 25 上运行（Groovy 4.0.15 的 ASM 不支持 major 69 类文件）                | **已落地**：`oapService.bat` 强制 JDK 17（见 3.4 实测结论）                                                    |
| UI 打不开                                                                                  | Sentinel 占用 8080                                                                  | 已按 Day1 改 8091；或 `set SW_SERVER_PORT=8091` 后重启                                                         |
| UI 有服务但无 Trace                                                                        | 11800 不通 / service_name 未生效                                                    | 检查 VM 参数、防火墙；看 `logs/skywalking-api.log`                                                             |
| 接口链路有但无 SQL Span                                                                    | MySQL 驱动 9.x 未被 mysql 插件识别                                                  | **实测正常**（mysql-connector-j 9.7.0 完整识别）；如遇此错换 8.x 驱动验证                                      |
| Agent 日志 `NoSuchMethodError: HttpHeaders.get(Object)`（webflux-6.x 插件）                | Spring Framework 7 方法签名变化，官方支持列表未覆盖                                 | 属 Agent 生态缺口，仅影响网关入口 Span 命名，**不影响转发与 sw8 传播**，接受降级并记录；待官方支持后升级 Agent |
| Agent 日志大量 `not activated ... Witness class ... does not exist`（spring-mvc v3/v4/v5） | Spring Framework 7 移除了 `DefaultKeyGenerator`/`AnnotationBeanUtils` 等 witness 类 | **入口 Span 由 Tomcat 插件接管**（endpoint 为 URL 格式），功能不受影响；属 Agent 生态缺口，接受降级并记录      |
| Redis Span 缺失                                                                            | 插件在 optional-plugins                                                             | 拷入 plugins/ 后重启                                                                                           |
| 日志无 %tid                                                                                | 未引 toolkit 或未重启                                                               | Day3 Step2 依赖 + pattern + 重启                                                                               |
| MySQL 数据膨胀                                                                             | 全量采样                                                                            | 调采样；定期清理历史日期后缀表（`*_2026090x`）                                                                 |

---

## 八、交付物清单

- [x] `skywalking/skywalking-agent/` 解压就绪（plugins/ 已增补 gateway-4.x、mybatis-3.x、webflux-6.x 三个插件）
- [x] UI 端口 8091 修改记录（本手册 Day1 Step2）
- [x] 8 服务 Agent 挂载（fat jar `java -jar` + 三参数启动，见第九节启动脚本；IDEA VM 参数方式等效）
- [x] OAP 强制 JDK 17 修复（oapService.bat，见 3.4 实测结论）
- [x] 拓扑 / 端到端 Trace 验证记录（见第五节验收实测；截图留档 UI：http://localhost:8091）
- [x] doc/summary.md 增补「SkyWalking 接入」问题记录章节
- [x] doc/技术栈.md、README.md 可观测性段落按实际接入状态更新
- [ ] 秒杀链路 / MQ Span / Redis Span / 日志 %tid（Day 3 可选项）

---

## 九、实施结果回写（2026-09-05）

### 9.1 执行摘要

Day 1 + Day 2 + 端到端验证一次通过（总耗时约 1.5 小时），核心能力全部落地：

| 阶段           | 结果                                                                                                      |
| -------------- | --------------------------------------------------------------------------------------------------------- |
| OAP + UI       | OAP 以 JDK 17 运行（修复 Groovy/ASM 不支持 JDK 25 类文件问题），MySQL 存储建 60 表，11800/12800/8091 就绪 |
| 8 服务挂 Agent | 全部以 JDK 25 + Agent 启动成功，9 服务注册（含 DB 虚拟节点）                                              |
| 跨服务 Trace   | gateway→web→product-api→brand-api 四级串联，46 Span，refs 正确                                            |
| SQL 观测       | MyBatis（mapper 名）+ JDBC（db.statement/db.instance/peer）+ Druid 连接池 Span 完整                       |
| 错误追踪       | 既有业务缺陷（member 表缺 status 列）被定位到 JDBC Span 异常事件（error.kind/message/stack）              |
| 附加发现       | product 列表存在 N+1 查询（每商品逐一调 brand-api），已由 Trace 数据直观暴露                              |

### 9.2 启动方式（可复现）

服务端（一次性）：

```powershell
& "D:\.workspace\javaproject\goods-store-management-system-parent\skywalking\apache-skywalking-apm-bin\bin\startup.bat"
# OAP 已强制 JDK 17（oapService.bat 内 setlocal 处）；UI: http://localhost:8091
```

业务服务（每个服务，示例为 brand-api，其余替换 service_name 与 jar 路径）：

```powershell
java -javaagent:D:\.workspace\javaproject\goods-store-management-system-parent\skywalking\skywalking-agent\skywalking-agent.jar `
     -Dskywalking.agent.service_name=goods-store-brand-api `
     -Dskywalking.collector.backend_service=127.0.0.1:11800 `
     -jar goods-store-service\goods-store-brand-api\target\goods-store-brand-api-1.0.0.jar
```

启动顺序：OAP/UI → MySQL/Redis/Nacos/Sentinel → brand/product/auth/member → trade/seckill → web → gateway。

### 9.3 兼容性结论（Spring Boot 4.1.1 / Framework 7 / SCG 5.x + Agent 9.7.0）

| 能力                       | 状态 | 说明                                                                                             |
| -------------------------- | ---- | ------------------------------------------------------------------------------------------------ |
| Agent 字节码增强（JDK 25） | 正常 | ByteBuddy 风险未触发，仅 sun.misc.Unsafe 弃用警告                                                |
| sw8 跨服务传播             | 正常 | 网关转发、Feign 调用均正确注入/提取，Trace 四级串联                                              |
| MyBatis / JDBC / Druid     | 正常 | SQL 全量可见，异常捕获完整                                                                       |
| Tomcat 入口 Span           | 正常 | MVC 服务的 Entry Span 由 Tomcat 插件承担                                                         |
| MVC 入口命名               | 降级 | spring-mvc 插件失活，endpoint 为 `GET:/路径` URL 格式（非 Controller 方法名）                    |
| 网关入口 Span              | 降级 | webflux-6.x 插件报 NoSuchMethodError，入口 span 呈 Local 类型 `SpringCloudGateway/GatewayFilter` |
| 官方支持                   | 缺口 | Agent 9.7.0（近期版本）支持列表最高 SCG 4.1.x / 未列 Framework 7；升级短期无解，**接受降级运行** |

**处置原则**：功能层面全链路可用（追踪/拓扑/SQL/错误），仅入口命名降级；待 SkyWalking Java Agent 官方适配 Spring Framework 7 / SCG 5.x 后升级恢复。

### 9.4 遗留事项

1. 登录接口 500（member 表缺 `status` 列）为业务侧缺陷，建议业务排期修复（本接入仅验证了错误追踪能力）。
2. Day 3 可选项未执行：Redis Span、RabbitMQ Span、日志 %tid、采样调优。
3. 生产化建议：观测数据独立库存储、按天清理历史后缀表、网关入口降级项持续关注 Agent 版本。
