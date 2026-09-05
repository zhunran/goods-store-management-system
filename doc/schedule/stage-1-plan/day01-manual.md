# Day 1 操作执行手册 — 基础设施搭建（上）

> 对应计划：[14-day-implementation-plan.md](file:///d:/.workspace/javaproject/goods-store-management-system-parent/doc/schedule/14-day-implementation-plan.md) Day 1
> 版本：v1.0　编写日期：2026-08-28　执行日期：2026-08-29
> 主题：Nacos 共享配置 + 父 POM 依赖版本管理 + 公共模块基础能力

---

## 一、本日目标与产出物

### 目标

1. 建立 Nacos 共享配置中心，所有服务从同一份 `goods-store-common.yaml` 拉取基础设施配置
2. 父 POM 完成 Redis / RabbitMQ / JWT / Redisson / Hutool / Druid 的版本统一管控
3. `goods-store-common` 模块补齐后续 13 天都要复用的 4 类基础能力：雪花 ID、MQ 消息体、分页基类、Jackson 序列化

### 产出物清单

| 类型         | 产出物                                                                                                           |
| ------------ | ---------------------------------------------------------------------------------------------------------------- |
| Nacos 配置   | `goods-store-common.yaml`（namespace=public，group=DEFAULT_GROUP）                                               |
| 父 POM       | `pom.xml` 新增 6 个版本属性 + `<dependencyManagement>` 条目                                                      |
| Common 模块  | `SnowflakeUtil`、`PageQuery`、`JacksonConfig`、`OrderCreateMessage`、`SeckillOrderMessage`、`OrderCancelMessage` |
| 服务配置改造 | 6 个服务 `application.yaml` 补齐 Nacos config 导入与热刷新                                                       |

### 前置条件（执行前必须确认）

| 组件     | 版本要求 | 验证命令                                 | 说明                             |
| -------- | -------- | ---------------------------------------- | -------------------------------- |
| JDK      | 25       | `java -version`                          | 父 POM `java.version=25`         |
| Maven    | 3.9+     | `mvn -v`                                 | 需支持 Spring Boot 4 构建        |
| MySQL    | 8.0+     | `mysql -uroot -proot -e "select 1"`      | 库名 `shoplook2026_0324`         |
| Redis    | 6.0+     | `redis-cli ping`                         | 返回 PONG                        |
| RabbitMQ | 3.12+    | 浏览器访问 `http://localhost:15672`      | guest/guest 可登录               |
| Nacos    | 2.3+     | 浏览器访问 `http://localhost:8848/nacos` | nacos/nacos 可登录，服务列表可见 |

> 当前项目已完成 Nacos 服务注册与发现，Day 1 只新增**配置中心**能力，不改动 discovery 部分。

---

## 二、技术要点

### 2.1 注册中心与配置中心分离

当前项目 Nacos discovery（服务注册/发现）已跑通，Day 1 新增的是 Nacos config（配置中心）能力：

| 能力     | 依赖                                           | 配置入口                                             | 当前状态                                                                       |
| -------- | ---------------------------------------------- | ---------------------------------------------------- | ------------------------------------------------------------------------------ |
| 服务注册 | `spring-cloud-starter-alibaba-nacos-discovery` | `spring.cloud.nacos.discovery`                       | 已完成                                                                         |
| 配置中心 | `spring-cloud-starter-alibaba-nacos-config`    | `spring.cloud.nacos.config` + `spring.config.import` | brand/product/member/trade/seckill 已引入，**auth-api / gateway / web 待补齐** |

### 2.2 共享配置热刷新机制

- 配置需开启 `refresh: true`（Nacos 控制台创建时勾选，对应元数据 `config-refresh=true`），Nacos 客户端才会推送变更
- Bean 必须标注 `@RefreshScope` 才能在配置变更时重建；`@Value` / `@ConfigurationProperties` 均受其管辖
- 前缀 `optional:` 表示配置中心不可用时不阻断启动（本地开发友好），生产环境可去掉 optional 强依赖

### 2.3 依赖版本管理原则

父 POM 已通过 `spring-boot-dependencies` 4.1.1 导入 BOM，因此：

| 依赖                                      | 是否需手动指定版本 | 原因                                                       |
| ----------------------------------------- | ------------------ | ---------------------------------------------------------- |
| `spring-boot-starter-amqp`                | 否                 | BOM 已管理                                                 |
| `spring-boot-starter-data-redis`          | 否                 | BOM 已管理                                                 |
| `redisson-spring-boot-starter`            | 是                 | 第三方，需手动锁定                                         |
| `jjwt-api` / `jjwt-impl` / `jjwt-jackson` | 是                 | 第三方，需手动锁定                                         |
| `hutool-all`                              | 是                 | 第三方，需手动锁定                                         |
| `druid-spring-boot-3-starter`             | 是                 | 第三方，需手动锁定（Day 2 才实际引入依赖，Day 1 只锁版本） |

> 版本兼容性提醒：Spring Boot 4.1 + Spring Cloud 2025.1 属新版本线，redisson / druid starter 与其兼容性需在引入当天用 `mvn dependency:tree` 验证，若冲突则降级为直接引核心包 + 手写配置类。

### 2.4 雪花算法 workerId 分配策略

雪花 ID = 时间戳 + datacenterId(5bit) + workerId(5bit) + 序列号(12bit)。多服务共用同一 DB 时**必须保证 workerId 全局唯一**，否则可能生成重复主键。本项目按服务静态分配：

| 服务                | workerId | 端口 |
| ------------------- | -------- | ---- |
| goods-store-auth    | 1        | 8082 |
| goods-store-brand   | 2        | 8081 |
| goods-store-web     | 3        | 8083 |
| goods-store-product | 4        | 8084 |
| goods-store-member  | 5        | 8085 |
| goods-store-trade   | 6        | 8086 |
| goods-store-seckill | 7        | 8087 |

单机开发环境由各服务 `application.yaml` 里 `snowflake.worker-id` 覆盖；将来集群部署同服务多实例时，改为环境变量 `SNOWFLAKE_WORKER_ID` 注入。

### 2.5 MQ 消息体设计原则

消息体放在 common 模块、保持**纯 POJO（Lombok @Data + Serializable）**，不引 amqp 依赖——这样不消费 MQ 的服务（如 member-api）引入 common 时不会被强拉 RabbitMQ 相关类。序列化由各服务的 `RabbitTemplate` messageConverter（Jackson2JsonMessageConverter）负责，Day 9 配置。

### 2.6 Jackson Long→String 的必要性

JS 的 `Number` 安全整数上限为 2^53-1（约 9×10^15），而雪花 ID 是 19 位（10^18 量级），直接用数值返回前端必然精度丢失（末几位变 0）。因此全局序列化规则：`Long` / `long` 一律输出为 String。同时统一 `LocalDateTime` 为 `yyyy-MM-dd HH:mm:ss`，避免 ISO `T` 分隔符前端解析麻烦。

### 2.7 包扫描说明（重要）

`BrandApiApplication` 等启动类位于根包 `com.fengluan`，因此 common 模块的 `@Component` / `@Configuration` 会被各服务自动扫描到，**无需** `scanBasePackages` 或 `@Import`。新建 common 类时保持包前缀 `com.fengluan.common.**` 即可。

---

## 三、详细操作步骤

### 步骤 1：Nacos 创建共享配置（对应任务 1.1）

1. 浏览器打开 `http://localhost:8848/nacos`，登录 nacos/nacos
2. 左侧菜单：**配置管理 → 配置列表**，右上角点击 **+**（创建配置）
3. 按下表填写：

   | 表单项   | 值                                            |
   | -------- | --------------------------------------------- |
   | Data ID  | `goods-store-common.yaml`                     |
   | Group    | `DEFAULT_GROUP`                               |
   | 命名空间 | `public`                                      |
   | 配置格式 | `YAML`                                        |
   | 配置内容 | 见下方完整 YAML                               |
   | 更多配置 | **勾选"支持热更新"（refresh=true）** ← 容易漏 |

4. 点击**发布**，回到列表确认配置存在

完整配置内容（直接粘贴）：

```yaml
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
      max-active: 10
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
            multi-statement-allow: true
            none-base-statement-allow: true
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
        concurrency: 3
        max-concurrency: 10

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

snowflake:
  worker-id: ${SNOWFLAKE_WORKER_ID:1}
  datacenter-id: ${SNOWFLAKE_DATACENTER_ID:1}
```

注意事项：

- **Day 1 只建配置，不引依赖。** datasource/redis/rabbitmq 段此时没有对应 starter，Spring Boot 会忽略未使用的属性，不会报错（真正生效从 Day 2 引 Druid、Day 9 引 AMQP 开始）
- `spring.data.redis` 是 Spring Boot 3+ 的新路径（不再是 `spring.redis`），写错路径热刷新验证时会查不到值
- 每个服务还会自动加载 `spring.application.name.yaml`（如 `goods-store-brand-api.yaml`），用于存放服务私有配置（端口、workerId），与共享配置按"服务私有覆盖共享"的优先级合并

### 步骤 2：父 POM 添加版本管理（对应任务 1.2）

编辑根 `pom.xml`（父 POM），做两处修改：

**① `<properties>` 新增版本属性：**

```xml
<properties>
    <java.version>25</java.version>
    <mybatis-plus.version>3.5.17</mybatis-plus.version>
    <!-- ↓ Day 1 新增 ↓ -->
    <redisson.version>3.50.0</redisson.version>
    <jjwt.version>0.12.6</jjwt.version>
    <hutool.version>5.8.38</hutool.version>
    <druid.version>1.2.25</druid.version>
</properties>
```

**② `<dependencyManagement>` 新增条目（追加在 mybatis-plus 条目之后）：**

```xml
<!-- Redisson 分布式锁 -->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>${redisson.version}</version>
</dependency>
<!-- JWT 三件套：api 编译期、impl/jackson 运行期 -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>${jjwt.version}</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>${jjwt.version}</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>${jjwt.version}</version>
</dependency>
<!-- Hutool 工具集（雪花算法、加密等） -->
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>${hutool.version}</version>
</dependency>
<!-- Druid 连接池（Day 2 实际引入） -->
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid-spring-boot-3-starter</artifactId>
    <version>${druid.version}</version>
</dependency>
```

`amqp` / `data-redis` 两个 starter 交给 BOM 管理，**不写版本号**，后续哪个服务用到直接加：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

> 验证版本可解析：执行 `mvn -N validate` 只构建父 POM，能通过说明属性与 POM 结构正确；第三方版本是否真的存在可执行 `mvn dependency:get -Dartifact=org.redisson:redisson-spring-boot-starter:3.50.0` 快速探测（不存在会报错，换最近可用版本并同步更新本手册）。

### 步骤 3：common 模块补充依赖

编辑 `goods-store-common/pom.xml`，`<dependencies>` 中追加：

```xml
<!-- 雪花算法（SnowflakeUtil） -->
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
</dependency>
<!-- Jackson 配置类需要：customizer 来自 spring-boot-autoconfigure -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot</artifactId>
</dependency>
<!-- LocalDateTime 序列化模块 -->
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>
```

> 这三个依赖均由父 POM 的 BOM 管版本，无需写版本号。`spring-boot`（而非 starter）只为拿到 `Jackson2ObjectMapperBuilderCustomizer` 类，不会拉起内嵌容器。

---

### 步骤 4：SnowflakeUtil 雪花算法工具类（对应任务 1.3）

新建 `goods-store-common/src/main/java/com/fengluan/common/util/SnowflakeUtil.java`：

```java
package com.fengluan.common.util;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 雪花算法工具：生成全局唯一订单号等业务 ID
 * workerId 按服务静态分配（见手册 2.4 分配表），集群部署时用环境变量覆盖
 */
@Component
public class SnowflakeUtil {

    private final Snowflake snowflake;

    public SnowflakeUtil(@Value("${snowflake.worker-id:1}") long workerId,
                         @Value("${snowflake.datacenter-id:1}") long datacenterId) {
        this.snowflake = IdUtil.getSnowflake(workerId, datacenterId);
    }

    /** 生成数值型 ID（19 位） */
    public long nextId() {
        return snowflake.nextId();
    }

    /** 生成字符串型 ID（推荐：配合 Jackson Long→String 或直接存 VARCHAR 字段） */
    public String nextIdStr() {
        return snowflake.nextIdStr();
    }
}
```

要点：

- 构造器注入而非 `@Value` 字段注入，保证对象创建时雪花实例已就绪
- 不加 `@RefreshScope`：雪花实例重建会破坏趋势递增，workerId 变更应重启生效

### 步骤 5：PageQuery 分页基类（对应任务 1.5）

新建 `goods-store-common/src/main/java/com/fengluan/common/dto/PageQuery.java`：

```java
package com.fengluan.common.dto;

import lombok.Data;

/**
 * 分页查询基类：各服务查询 Request 继承此类
 */
@Data
public class PageQuery {

    /** 页码，从 1 开始 */
    private Integer pageNo = 1;

    /** 每页条数，默认 10，上限 100 防止深翻页拖垮 DB */
    private Integer pageSize = 10;

    /** 排序字段（必须是实体属性名，服务端需白名单校验） */
    private String sortField;

    /** 排序方向：asc / desc */
    private String sortOrder = "desc";
}
```

要点：

- 排序字段服务端必须做白名单映射（MapStruct 式 switch 或枚举），否则有 SQL 注入风险，Day 5 品牌 CRUD 时会写统一校验方法
- 这里不直接依赖 MyBatis-Plus 的 `Page`，避免 common 与 ORM 耦合；各服务自行 `new Page<>(query.getPageNo(), query.getPageSize())`

### 步骤 6：JacksonConfig 全局序列化（对应任务 1.6）

新建 `goods-store-common/src/main/java/com/fengluan/common/config/JacksonConfig.java`：

```java
package com.fengluan.common.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 全局 Jackson 规则：
 * 1. Long/long → String（雪花 ID 19 位超出 JS Number 2^53 安全范围）
 * 2. LocalDateTime → yyyy-MM-dd HH:mm:ss
 */
@Configuration
public class JacksonConfig {

    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
        return builder -> builder
                .serializerByType(Long.class, ToStringSerializer.instance)
                .serializerByType(Long.TYPE, ToStringSerializer.instance)
                .serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(formatter))
                .deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));
    }
}
```

要点：

- 用 `Jackson2ObjectMapperBuilderCustomizer` 而不是自己 new `ObjectMapper`：前者走 Spring Boot 自动配置链路，能保留 Spring 默认行为并作用于 MVC 与 Feign 双方
- `LocalDateTime` 反序列化器同时注册，保证入参（如"2026-08-29 10:00:00"）也能解析
- 该类在 `com.fengluan.common.config` 包，由根包 `com.fengluan` 下的启动类自动扫描生效（见 2.7）

### 步骤 7：MQ 消息体 DTO（对应任务 1.4）

在 `goods-store-common/src/main/java/com/fengluan/common/mq/` 下新建 3 个类：

**① OrderCreateMessage.java**（Day 9-10 使用）

```java
package com.fengluan.common.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 订单创建消息：下单成功后由 trade-api 发出，消费者完成库存扣减
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单号（雪花 ID 字符串） */
    private String orderNo;

    /** 会员 ID */
    private Long memberId;

    /** 订单商品明细 */
    private List<Item> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 商品 ID */
        private Long goodId;
        /** SKU ID */
        private Long skuId;
        /** 购买数量 */
        private Integer count;
    }
}
```

**② SeckillOrderMessage.java**（Day 12-13 使用）

```java
package com.fengluan.common.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 秒杀下单消息：Redis 预减库存成功后由 seckill-api 发出，消费者落库创建订单
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillOrderMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 秒杀活动 ID */
    private Long seckillId;

    /** 秒杀商品 ID */
    private Long seckillGoodId;

    /** 商品 ID */
    private Long goodId;

    /** 会员 ID */
    private Long memberId;

    /** 秒杀价格 */
    private Long seckillPrice;

    /** 生成的订单号（消费者据此创建订单） */
    private String orderNo;
}
```

**③ OrderCancelMessage.java**（Day 11 使用）

```java
package com.fengluan.common.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 订单取消消息：超时未支付/用户主动取消后发出，消费者恢复库存
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单号 */
    private String orderNo;

    /** 取消原因：TIMEOUT-超时取消 / USER-用户主动取消 */
    private String reason;

    /** 需恢复库存的商品明细（复用 OrderCreateMessage.Item） */
    private java.util.List<OrderCreateMessage.Item> items;
}
```

要点：

- 三者均 `implements Serializable` 并声明 `serialVersionUID`——纯 POJO 不引 amqp，字段类型只用 Long/Integer/String/List
- 金额统一用 **Long（分为单位）** 而非 Double，避免浮点误差，与 DB `DECIMAL` 换算规则在 Day 9 再细化
- `@Builder` 方便生产端链式构造；`@NoArgsConstructor + @AllArgsConstructor` 保证 Jackson 反序列化可用

---

### 步骤 8：补齐各服务配置中心接入（对应任务 1.7 前置）

当前 5 个服务（brand/product/member/trade/seckill）已含 `spring.config.import`，**auth-api、gateway、web 三个尚未接入配置中心**，本步骤逐一补齐。

**① auth-api**（当前 application.yaml 仅有应用名，整体缺失）

替换 `goods-store-service/goods-store-auth-api/src/main/resources/application.yaml` 为：

```yaml
server:
  port: 8082

spring:
  application:
    name: goods-store-auth-api
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
        username: nacos
        password: nacos
        group: DEFAULT_GROUP
      config:
        server-addr: localhost:8848
        username: nacos
        password: nacos
        group: DEFAULT_GROUP
        file-extension: yaml
        namespace: public
  config:
    import:
      - optional:nacos:goods-store-common.yaml

# 服务私有配置：覆盖共享配置中的雪花 workerId（auth 固定为 1，见 2.4 分配表）
snowflake:
  worker-id: 1
```

同时检查 `goods-store-auth-api/pom.xml` 是否已引入（若无则添加）：

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
```

**② gateway**（有 discovery 无 config）

在 `goods-store-gateway/src/main/resources/application.yaml` 的 `spring.cloud.nacos` 下补 `config` 段，并追加 `spring.config.import`：

```yaml
spring:
  application:
    name: goods-store-gateway
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
        username: nacos
        password: nacos
        group: DEFAULT_GROUP
      config: # ← 新增
        server-addr: localhost:8848
        username: nacos
        password: nacos
        group: DEFAULT_GROUP
        file-extension: yaml
        namespace: public
    gateway:
      routes:
        - id: brand-api
          uri: lb://goods-store-brand-api
          predicates:
            - Path=/brand/api/**
  config: # ← 新增
    import:
      - optional:nacos:goods-store-common.yaml
```

gateway 的 `pom.xml` 同样补 `spring-cloud-starter-alibaba-nacos-config` 依赖。

**③ web**（有 discovery 无 config，同 gateway 方式补齐）

**④ 已接入的 5 个服务**：在各自 `application.yaml` 末尾追加本服务的雪花 workerId 覆盖（否则全部用共享配置默认值 1 会冲突）：

| 服务    | 追加内容                 |
| ------- | ------------------------ |
| brand   | `snowflake.worker-id: 2` |
| web     | `snowflake.worker-id: 3` |
| product | `snowflake.worker-id: 4` |
| member  | `snowflake.worker-id: 5` |
| trade   | `snowflake.worker-id: 6` |
| seckill | `snowflake.worker-id: 7` |

示例（brand）：

```yaml
snowflake:
  worker-id: 2
```

> 注意：brand 的 discovery 配置里有 `ip: 192.168.110.98`（本机局域网 IP），保持不动；其余服务未写 ip 则自动探测。

### 步骤 9：热刷新验证端点（对应任务 1.7 验证手段）

在 brand-api 新建临时验证 Controller `goods-store-service/goods-store-brand-api/src/main/java/com/fengluan/brand/api/ConfigTestController.java`：

```java
package com.fengluan.brand.api;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Day 1 热刷新验证端点（Day 14 可保留作为配置巡检）
 */
@RestController
@RequestMapping("/brand/api/config")
@RequiredArgsConstructor
@RefreshScope   // 关键：配置变更时该 Bean 重建，@Value 重新注入
public class ConfigTestController {

    @Value("${brand.test-flag:default}")
    private String testFlag;

    @Value("${snowflake.worker-id:0}")
    private long workerId;

    private final com.fengluan.common.util.SnowflakeUtil snowflakeUtil;

    @GetMapping("/check")
    public Map<String, Object> check() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("testFlag", testFlag);
        map.put("workerId", workerId);
        map.put("snowflakeId", snowflakeUtil.nextIdStr());
        return map;
    }
}
```

随后在 Nacos 中**新建一个服务私有配置**（不要改动共享的 `goods-store-common.yaml`）：将 `goods-store-common.yaml` 的完整内容克隆一份，Data ID 命名为 `goods-store-brand-api.yaml`（即 `spring.application.name` + `.yaml`，Nacos 会按应用名自动加载），命名空间 `public`、分组 `DEFAULT_GROUP`，再在末尾添加测试配置（验证完可删）：

```yaml
# Data ID: goods-store-brand-api.yaml（克隆自 goods-store-common.yaml，末尾追加如下测试配置）
brand:
  test-flag: v1
```

> 说明：`goods-store-brand-api.yaml` 会以**更高优先级**覆盖 `goods-store-common.yaml`，因此 `brand.test-flag`、以及脚本中 `snowflake.worker-id` 的覆盖都写在这个服务私有配置里，共享配置保持纯净。

### 步骤 10：编译与启动验证

按顺序执行：

```powershell
# 1. 全量编译（父 POM 变更后必须先 install 父 POM，否则子模块解析不到新依赖管理）
mvn -N install
# 2. 全模块编译
mvn clean compile
# 3. 启动 brand 服务（先启动 Nacos/MySQL/Redis/RabbitMQ）
cd goods-store-service/goods-store-brand-api
mvn spring-boot:run
```

启动后验证：

```powershell
# 通过网关访问（网关需同时启动）
curl http://localhost:8080/brand/api/config/check
# 或直连
curl http://localhost:8081/brand/api/config/check
```

预期返回：

```json
{ "testFlag": "v1", "workerId": 2, "snowflakeId": "1940xxxxxxxxxxxxxxx" }
```

---

## 四、验收标准

按顺序逐项验证，全部通过才算 Day 1 完成。

### 4.1 编译验收

| #   | 验收项           | 验证方法                                     | 预期结果                         | 通过 |
| --- | ---------------- | -------------------------------------------- | -------------------------------- | ---- |
| 1   | 父 POM 安装成功  | `mvn -N install`                             | BUILD SUCCESS                    | [ ]  |
| 2   | 全模块编译无报错 | `mvn clean compile`                          | BUILD SUCCESS，无 WARNING 级错误 | [ ]  |
| 3   | 第三方依赖可解析 | `mvn dependency:tree -pl goods-store-common` | 树中可见 hutool/jackson-jsr310   | [ ]  |

### 4.2 Nacos 配置中心验收

| #   | 验收项                 | 验证方法                                                                             | 预期结果                                  | 通过 |
| --- | ---------------------- | ------------------------------------------------------------------------------------ | ----------------------------------------- | ---- |
| 4   | 共享配置存在           | Nacos 控制台 → 配置列表                                                              | `goods-store-common.yaml` 存在，格式 YAML | [ ]  |
| 5   | 热更新开关已启用       | 配置详情页"更多配置"                                                                 | 已勾选支持热更新                          | [ ]  |
| 6   | brand 启动加载共享配置 | 启动日志搜索 `Located property source`                                               | 日志含 `nacos:goods-store-common.yaml`    | [ ]  |
| 7   | `@RefreshScope` 生效   | Nacos 修改 `brand.test-flag: v1` → `v2`，约 5 秒后再次请求 `/brand/api/config/check` | `testFlag` 从 v1 变为 v2，**服务未重启**  | [ ]  |

### 4.3 公共模块功能验收

| #   | 验收项              | 验证方法                                                                                  | 预期结果                                | 通过 |
| --- | ------------------- | ----------------------------------------------------------------------------------------- | --------------------------------------- | ---- |
| 8   | 雪花 ID 生成        | 请求 `/brand/api/config/check` 两次比对 `snowflakeId`                                     | 均为 19 位字符串且不相等，趋势递增      | [ ]  |
| 9   | workerId 覆盖生效   | 返回体 `workerId` 字段                                                                    | brand 为 2（而非共享配置默认 1）        | [ ]  |
| 10  | Jackson Long→String | 观察 `/brand/api/config/check` 响应体                                                     | `workerId` 输出为字符串 `"2"`（带引号） | [ ]  |
| 11  | MQ 消息体可序列化   | 在 brand 临时单测中 Jackson `writeValueAsString(new OrderCreateMessage())` 再 `readValue` | 字段往返一致（详细用例见 4.4）          | [ ]  |

### 4.4 补充单测（建议当天完成）

在 `goods-store-common/src/test/java/com/fengluan/common/` 下新建 `CommonDay1Test.java`：

```java
package com.fengluan.common;

import com.fengluan.common.config.JacksonConfig;
import com.fengluan.common.mq.OrderCreateMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommonDay1Test {

    @Test
    void jacksonShouldSerializeLongAsString() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        new JacksonConfig().jacksonCustomizer().customize(
                org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.json()
        );
        // 直接构造带 Long 的对象验证
        OrderCreateMessage msg = OrderCreateMessage.builder()
                .orderNo("1940000000000000001")
                .memberId(1234567890123456789L)
                .items(List.of(OrderCreateMessage.Item.builder()
                        .goodId(1L).skuId(2L).count(3).build()))
                .build();
        String json = mapper.writeValueAsString(msg);
        assertTrue(json.contains("\"memberId\":\"1234567890123456789\""),
                "Long 必须序列化为字符串，实际输出：" + json);
        OrderCreateMessage back = mapper.readValue(json, OrderCreateMessage.class);
        assertEquals(msg.getMemberId(), back.getMemberId());
    }

    @Test
    void localDateTimeShouldUseUnifiedPattern() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(
                java.util.Map.of("t", LocalDateTime.of(2026, 8, 29, 10, 30, 0)));
        assertTrue(json.contains("2026-08-29 10:30:00"), "实际输出：" + json);
    }
}
```

> 运行：`mvn test -pl goods-store-common`。第二个用例若失败，说明 customizer 未作用于该手动 new 的 mapper——属预期外行为，按 5.3 排查。

---

## 五、常见问题排查

### 5.1 启动报 `No spring.config.import property has been defined`

原因：引入了 nacos-config 依赖但没有 `spring.config.import`。
处理：确认该服务 `application.yaml` 含 `spring.config.import: optional:nacos:goods-store-common.yaml`，且缩进正确（`config` 与 `cloud` 同级，都在 `spring` 下）。

### 5.2 配置修改后不热刷新

排查顺序：

1. Nacos 配置详情页"更多配置"是否勾选热更新（对应元数据 `config-refresh=true`）
2. Bean 是否加了 `@RefreshScope`（`@Value` 才会重新注入；`@ConfigurationProperties` Bean 天然支持热刷新可不加）
3. 修改的是不是**本服务实际加载的** Data ID/Group/Namespace（Group 或 namespace 不匹配时会静默加载不到，日志无报错）
4. 观察启动日志是否有 `Ignore the empty nacos:configuration` 字样，有则说明 import 路径写错

### 5.3 Jackson 规则不生效（Long 仍返回数字）

排查顺序：

1. 该服务启动类是否在 `com.fengluan` 根包（保证扫描到 `common.config.JacksonConfig`）
2. 服务是否引了 `goods-store-common` 依赖且为最新 install 版本（`mvn -N install` + 子模块重新编译）
3. 服务内部是否自己 new 过 `ObjectMapper` / 自定义过 `MappingJackson2HttpMessageConverter`——自定义实例不走 Boot 自动配置链路，需改为注入容器中的 mapper

### 5.4 workerId 相关

- 症状：两个服务生成的雪花 ID 偶发相同 → 检查是否都用了默认 worker-id 1，确认各服务 `application.yaml` 的覆盖值按 2.4 分配表设置
- 症状：ID 长度不足 19 位 → 正常现象，雪花 ID 长度取决于时间戳，只要数值单调递增且不重复即正常

### 5.5 `mvn clean compile` 报找不到 hutool / redisson 版本

原因：父 POM 改完没有 `mvn -N install`，本地仓库里还是旧版父 POM。
处理：先 `mvn -N install` 再编译子模块。

### 5.6 Nacos 控制台看不到新配置的 namespace/group

`goods-store-common.yaml` 固定使用 namespace=`public`、group=`DEFAULT_GROUP`；控制台默认展示 public，若曾在服务配置里写了其他 namespace（如 dev），会加载不到共享配置——保持与服务 `spring.cloud.nacos.config.namespace` 一致。

---

## 六、本日产出核对表（提交前自查）

- [ ] Nacos `goods-store-common.yaml` 已创建且勾选热更新
- [ ] 父 POM 新增 4 个版本属性 + 6 条 dependencyManagement（redisson×1、jjwt×3、hutool×1、druid×1）
- [ ] common 模块新增 3 个依赖（hutool-all、spring-boot、jackson-datatype-jsr310）
- [ ] common 新增 6 个类：SnowflakeUtil、PageQuery、JacksonConfig、OrderCreateMessage、SeckillOrderMessage、OrderCancelMessage
- [ ] auth-api / gateway / web 配置中心接入补齐（yaml + pom）
- [ ] 7 个服务 workerId 按分配表覆盖
- [ ] brand-api ConfigTestController 验证端点就位
- [ ] 4.1–4.3 验收表 11 项全部通过
- [ ] common 模块单测通过（`mvn test -pl goods-store-common`）
- [ ] Git 提交：`feat(day1): Nacos共享配置+父POM版本管理+common基础能力`

---

## 七、明日预告（Day 2 衔接）

Day 2 将在今日共享配置基础上**实际引入** Druid 依赖（今日只锁版本），并搭建网关路由全景 + JWT 全局过滤器。今日遗留的注意事项：

- Druid starter 与 Spring Boot 4 兼容性需当天验证，冲突则按 2.3 的降级方案处理
- `wall` filter 的 `multi-statement-allow` 已预置，Day 2 集成后若仍拦截 JOIN 查询，参考 Day 6 商品列表查询的排查记录
- ConfigTestController 保留，Day 2 验证网关路由时继续复用
