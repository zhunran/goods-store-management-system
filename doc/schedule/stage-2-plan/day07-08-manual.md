# Day 7-8 操作执行手册 — 商品服务（下）+ 会员服务 + spi 契约兑现 + web 聚合

> 对应计划：[14-day-implementation-plan.md](file:///d:/.workspace/javaproject/goods-store-management-system-parent/doc/schedule/14-day-implementation-plan.md) Day 7（商品·下）/ Day 8（会员服务）
> 版本：v1.0　编写日期：2026-09-02　执行日期：待填
> 主题：商品新增/编辑/上下架/逻辑删除/库存扣减恢复 + ProductApi 契约扩展；会员资料/收货地址管理 + MemberApi 契约兑现；web 商品管理与会员聚合收口
> 依赖：Day 1-6 已完成（基础设施 + 认证 + 品牌 + 商品·上），均编译通过并启动成功
> **架构前提**：遵循 14 天计划「⭐ 分层架构规范」——spi 是**纯 HTTP 契约层**（禁 `@FeignClient`）、controller `implements` 契约、消费方 Feign `extends` 契约、web 是 BFF 聚合层（前端唯一入口 `/app/api/**`）。

---

## 一、Day 7-8 目标与产出物

### 目标

1. **商品服务补齐管理与库存能力**：新增/编辑/上下架/逻辑删除 + 乐观锁扣减/恢复库存；`ProductApi` 扩展为完整商品契约（含库存方法），供后续 `trade-api`/`seckill-api` `extends` 复用
2. **member-spi 从「带 `@FeignClient` 的测试接口」重构为纯契约** `MemberApi`（资料风化 + 收货地址 CRUD + 默认地址），并补充 DTO/VO
3. **member-api 落地实现**：会员资料查询/编辑（脱敏）+ 收货地址管理（默认地址互斥），`MemberController`/`AddressController implements MemberApi`，越权校验
4. **契约消费**：member 的默认地址接口进契约，供 Day 9-10 `trade-api` 下单取收货地址复用
5. **web 聚合收口**：商品管理 `/app/api/product/admin/*` + 会员 `/app/api/member/*`（BFF）

### 产出物清单

| 模块            | 产出物                                                                                     |
| --------------- | ------------------------------------------------------------------------------------------ |
| product-spi     | `ProductApi` 扩展：新增 create/update/delete/status/deductStock/restoreStock（纯契约，去 `@FeignClient`）+ `GoodCreateRequest/GoodUpdateRequest` |
| product-api     | `GoodMapper` 库存自定义 SQL + `GoodService` 扩展 CRUD/上下架/库存 + `GoodController` 完整 `implements` |
| member-spi      | `MemberApi`（纯契约，去 `@FeignClient`）+ `MemberProfileUpdateRequest` + `MemberAddressRequest` + `MemberAddressVO` + `MemberVO` 补脱敏字段 |
| member-api      | `MemberRepository/MemberMapper` + `MemberAddressMapper` + `MemberService/Impl` + `AddressService/Impl` + `MemberController/AddressController` + `DesensitizeUtil` |
| web             | `product/` 补 `WebProductAdminController/Service` + `member/` 三件套（Feign 客户端 `extends` 契约 + 聚合 Service + Controller `/app/api/**`） |
| gateway         | 白名单按需追加浏览类端点；管理/会员类端点保持登录鉴权                                          |

---

## 二、技术要点

### Day 7 技术要点

| 要点              | 说明                                                                                                   |
| ----------------- | ------------------------------------------------------------------------------------------------------ |
| 商品逻辑删除      | `good` 表已有 `is_del` 列；Nacos 公共配置已声明 `logic-delete-field: isDel`，`GoodEntity.isDel` 全局生效；删除后查询自动过滤 |
| 上下架            | 更新 `is_take_down` 字段；已下架商品下单校验返回 2003（`GOOD_NOT_FOUND`/`GOOD_STOCK_INSUFFICIENT` 亦可复用） |
| 库存乐观锁        | 自定义 SQL `UPDATE good SET qty=qty-#{count} WHERE id=#{id} AND qty>=#{count}`，影响行数 0 即代表库存不足，防超卖（**不依赖 version 列，保持轻量**） |
| 契约即实现        | `ProductApi` 已是纯契约（无 `@FeignClient`）；Day7 在其上**追加方法**，`GoodController` 完整 `implements`，`trade-api`/`seckill-api` 后续 `extends` 即可扣库存 |
| spi 纯净返回类型  | 库存扣减返回 `Boolean`（true=成功）、恢复/删除/上下架返回 `Void`（纯 JDK 类型，不依赖 common 的 `ApiResult`） |
| 契约路径（商品）  | 控制器类级前缀 `/good/api`；库存相对 `/stock/deduct`、`/stock/restore`，上下架 `/status`                  |
| web 商品管理收口  | 管理端（上下架/删除/编辑）走 `/app/api/product/admin/*`；库存扣减/恢复属**内部契约**，由 trade 消费，web 不暴露 |

### Day 8 技术要点

| 要点            | 说明                                                                                                      |
| --------------- | --------------------------------------------------------------------------------------------------------- |
| 数据脱敏        | `DesensitizeUtil`：手机号 3-4-4 中 4 位 `****`；身份证中间脱敏。会员查询返回脱敏后的 `MemberVO`            |
| 当前用户与越权  | 网关 [JwtAuthFilter.java](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-gateway/src/main/java/com/fengluan/gateway/filter/JwtAuthFilter.java#L70) 已把 `X-User-Id` 写入请求头；member-api 从 `@RequestHeader X-User-Id` 取当前用户，**只允许操作自己的资料/地址** |
| 默认地址互斥    | 设置某地址为默认时，先批量把该会员其余地址 `is_default=0`，再置目标为 1（`@Transactional`）                |
| 关联键          | `member_address` 表用 **`member_account`(varchar)** 关联会员（非 member.id）；需先按当前用户 id 查 `member.account` 再操作地址 |
| 契约即实现      | `MemberApi` 去 `@FeignClient`、只写路径注解 + DTO/VO；`MemberController`/`AddressController implements`；`trade-api`/web `extends` 继承 |
| 契约路径（会员）| 控制器类级前缀 `/member/api`（与网关路由一致）；会员资料 `/{id}`；地址 `/{id}/address...`                  |
| 脱敏字段         | `MemberVO` 补 `maskedCardId`（或 cardId 不返回），`phone` 直接放脱敏值，避免敏感信息外泄                    |

> 计划书 Day 8 附录 A 将会员错误码写在 6xxx，但当前 `ErrorCode` 的 6xxx 已被认证段占用；**以现状为准，会员沿用已存在的 1xxx 段**（`MEMBER_NOT_FOUND(1001)`），任务步内新增地址错误码放 1xxx。

---

## 三、Day 7-8 实施状态盘点

> 基于当前工程实测（Day 5-6 已编译通过并启动成功）。

### Day 7 现状

| 计划任务 | 内容                                   | 当前状态                                                                                | 手册步骤 |
| -------- | -------------------------------------- | --------------------------------------------------------------------------------------- | -------- |
| 7.1      | 商品新增/编辑（含详情图）              | ❌ `GoodServiceImpl` 仅 `page/detail`；无 create/update                                     | 步骤 D7-2 |
| 7.2      | 商品上下架                             | ❌ 无 status 操作                                       | 步骤 D7-2 |
| 7.3      | 商品删除（逻辑删除）                   | ❌ `GoodEntity.isDel` 存在但无删除方法；全局 `logic-delete-field:isDel` 已配置可生效     | 步骤 D7-2 |
| 7.4/7.5  | 库存扣减/恢复                          | ❌ `GoodMapper` 仅 `extends BaseMapper`，无自定义 SQL                                  | 步骤 D7-1/D7-2 |
| 7.6      | ProductApi 契约化                      | ⚠️ `ProductApi` **已是纯契约**（无 `@FeignClient`），但仅 categoryTree/page/getById 三方法 | 步骤 D7-1 |
| 7.7      | 商品详情图接口（批量）                 | ⚠️ 详情图已进 `GoodVO.detailPicList`；本轮可交由商品编辑时同步维护，独立接口可选         | 步骤 D7-2 |
| 7.8      | web 商品管理收口                       | ❌ web 无商品管理端点                                                                    | 步骤 D7-4 |

`good` 表（[table_structure_export.sql](file:///d:/.workspace/javaproject/goods-store-management-system-parent/sql/table_structure_export.sql#L71-L96)）已含 `is_take_down`、`is_del`、`qty`、`price` 列；**无 version 列**（本手册库存用 `WHERE qty>=` 乐观，不加列）。

### Day 8 现状

| 计划任务 | 内容                               | 当前状态                                                                                    | 手册步骤 |
| -------- | ---------------------------------- | ------------------------------------------------------------------------------------------- | -------- |
| 8.1/8.2  | 会员信息查询/编辑（脱敏）          | ❌ member-api 仅有 `MemberEntity`/`MemberAddressEntity` + 启动类（端口 8085），无 controller/service/mapper | 步骤 D8-2/D8-3 |
| 8.3      | 收货地址 CRUD                       | ❌ 无任何地址代码                                                                            | 步骤 D8-3 |
| 8.4      | 默认地址互斥                        | ❌ 无默认地址逻辑                                                                            | 步骤 D8-3 |
| 8.5      | MemberApi 契约化                    | ⚠️ [MemberApi.java](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-spi/goods-store-member-spi/src/main/java/com/fengluan/spi/member/MemberApi.java#L13-L17) 仍带 `@FeignClient`、只有 `list()` → 必须推倒重构；`member-spi/pom` 依赖 openfeign（需改） | 步骤 D8-1 |
| 8.6      | 脱敏工具类                          | ❌ 无                                                                                        | 步骤 D8-2 |
| 8.7      | web 会员聚合                        | ❌ web 无 member-spi / member 三件套                                                          | 步骤 D8-4 |

`member` 表字段极丰富（含 `phone`、`card_id`、`portrait` 等，可脱敏）；`member_address` 表以 `member_account` 关联（[SQL](file:///d:/.workspace/javaproject/goods-store-management-system-parent/sql/table_structure_export.sql#L160-L170)）。member-api application.yaml 端口 **8085**、已 import 共享配置。网关路由已含 `/member/api/**`。

---

## 四、详细操作步骤

### 前置约定

- **spi 纯净**：`member-spi` 参照 `product-spi` 只依赖 `spring-web`，**不要**再依赖 `spring-cloud-starter-openfeign`；分页/返回用纯 JDK 类型或 spi 内 VO。
- **契约路径**：商品控制器前缀 `/good/api`；会员控制器前缀 `/member/api`。Feign 客户端 `path` 与之保持一致。
- **当前用户**：受保护接口从 `@RequestHeader("X-User-Id")` 取值，做越权校验（web/Front 经由网关注入）。
- **spi 不依赖 common**：契约方法返回 `Boolean`/`Void`/VO，不用 `ApiResult`。

---

### Day 7 操作步骤

#### 步骤 D7-1：product-spi 契约扩展（任务 7.6）

**1a. 新增 DTO** `com.fengluan.spi.product.dto`：

```java
// GoodCreateRequest.java
public class GoodCreateRequest {
    @NotBlank private String name;      // 商品名
    private String alias; private String summary;
    private Integer categoryId; private Integer brandId;
    private BigDecimal markPrice; private BigDecimal price;
    private Integer qty; private String pic; private String pic2;
    private String detail;
    private Boolean isTakeDown; private Boolean isHot;
    private List<String> detailPicList;   // 详情图（一次性提交）
}

// GoodUpdateRequest.java —— 字段同 Create，仅多 @NotNull id
public class GoodUpdateRequest {
    @NotNull private Long id;
    // ... 其余同 GoodCreateRequest
}
```

**1b. 扩展 `ProductApi`（追加契约方法，`GoodController` 需同步实现）**：

```java
public interface ProductApi {
    // —— 已有（Day6）——
    @GetMapping("/category/tree") List<CategoryTreeVO> categoryTree();
    @GetMapping("/page") List<GoodVO> page(GoodQueryRequest request);
    @GetMapping("/{id}") GoodVO getById(@PathVariable Long id);

    // —— 新增（Day7）——
    @PostMapping GoodVO create(@Valid @RequestBody GoodCreateRequest request);
    @PutMapping("/{id}") GoodVO update(@PathVariable Long id, @Valid @RequestBody GoodUpdateRequest request);
    @DeleteMapping("/{id}") Void delete(@PathVariable Long id);
    @PutMapping("/{id}/status") Void updateStatus(@PathVariable Long id, @RequestParam Boolean takeDown);
    /** 扣减库存：true=成功，false=库存不足 */
    @PutMapping("/{id}/stock/deduct") Boolean deductStock(@PathVariable Long id, @RequestParam Integer count);
    /** 恢复库存 */
    @PutMapping("/{id}/stock/restore") Void restoreStock(@PathVariable Long id, @RequestParam Integer count);
}
```

> 相对路径叠加 `/good/api`，Feign/网关路径不变；`trade-api`/`seckill-api` 后续 `@FeignClient(name="goods-store-product-api", path="/good/api") interface TradeProductClient extends ProductApi` 即可复用扣库存。

#### 步骤 D7-2：GoodMapper 库存 SQL + GoodService 扩展（任务 7.1/7.2/7.3/7.4/7.5/7.7）

**2a. `GoodMapper` 加乐观锁库存 SQL**：

```java
@Mapper
public interface GoodMapper extends BaseMapper<GoodEntity> {
    /** 乐观锁扣减：无库存/库存不足返回 0 */
    @Update("UPDATE good SET qty = qty - #{count} WHERE id = #{goodId} AND qty >= #{count}")
    int deductStock(@Param("goodId") Long goodId, @Param("count") Integer count);

    @Update("UPDATE good SET qty = qty + #{count} WHERE id = #{goodId}")
    int restoreStock(@Param("goodId") Long goodId, @Param("count") Integer count);
}
```

> 库存扣除用 `WHERE qty>=` 保证原子不超卖，无需新增 `version` 列。

**2b. `GoodService` 接口扩展**：

```java
public interface GoodService extends IService<GoodEntity> {
    List<GoodVO> page(GoodQueryRequest query);
    GoodVO detail(Long id);
    GoodVO create(GoodCreateRequest request);
    GoodVO update(Long id, GoodUpdateRequest request);
    void delete(Long id);
    void updateStatus(Long id, Boolean takeDown);
    boolean deductStock(Long id, Integer count);
    void restoreStock(Long id, Integer count);
}
```

> 注意与 IService 同名冲突：`getById(Serializable)` 已存在 → 业务详情沿用 `detail(Long)`（Day6 已约定），新增方法避开 `getById`/`removeById` 等基类签名权衡，必要时重命名。

**2c. `GoodServiceImpl` 实现要点**（注入 `GoodMapper`/`GoodDetailPicsMapper`）：

- `create`：插入 `good`，并把 `request.detailPicList` 逐条插入 `good_detail_pics`（`goodId`、`url`、`sort` 用 index）。
- `update`：先查存在（`GOOD_NOT_FOUND`），`updateById`；详情图可"先删该商品图片再按新列表重插"（保持简单），或仅当传入非空才替换。
- `delete`：`super.removeById(id)`（触发 `is_del=1` 逻辑删除）。
- `updateStatus`：查出后 `setIsTakeDown(takeDown)` + `updateById`；不存在抛 `GOOD_NOT_FOUND`。
- `deductStock`：`return goodMapper.deductStock(id, count) > 0;`。
- `restoreStock`：`goodMapper.restoreStock(id, count);`。

**2d. `GoodController` 完整 `implements ProductApi`**：在 [GoodController.java](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-service/goods-store-product-api/src/main/java/com/fengluan/product/api/GoodController.java) 追加 `create/update/delete/updateStatus/deductStock/restoreStock` 六个 `@Override`，逐一委托 `GoodService`。

> 商品编辑涉及图片上传：图片上传仍走 web/前端直连上传（不进契约，同品牌 upload 先例）；契约只接收图片 URL 数组。

#### 步骤 D7-3：商品逻辑删除自检（无独立步骤）

`good` 表已含 `is_del` 列；Nacos `goods-store-common.yaml` 已有 `global-config.db-config.logic-delete-field: isDel`，`GoodEntity.isDel` 字段名匹配即全局生效。若核实 `delete` 未过滤，可在 `GoodEntity.isDel` 上加 `@TableLogic`（显式声明，兜底）。验收以"删除后 page/getById 不再返回该商品"为准。

#### 步骤 D7-4：web 商品管理收口 + 白名单（任务 7.8）

**4a. `com.fengluan.web.product.WebProductAdminService/Controller`**：管理端（`WebProductAdminController /app/api/product/admin/*`）代理 `product-api` 的上下架/删除/编辑，复用 `WebProductService` 内注入的 `ProductFeignClient`（`extends ProductApi`，已含新契约方法）。

```java
@RestController
@RequestMapping("/app/api/product/admin")
@RequiredArgsConstructor
public class WebProductAdminController {
    private final WebProductService webProductService; // 或独立 WebProductAdminService

    @PutMapping("/{id}/status") public ApiResult<Void> updateStatus(@PathVariable Long id, @RequestParam Boolean takeDown) { ... }
    @PutMapping("/{id}")        public ApiResult<GoodVO> update(@PathVariable Long id, @RequestBody GoodUpdateRequest r) { ... }
    @DeleteMapping("/{id}")     public ApiResult<Void> delete(@PathVariable Long id) { ... }
}
```

**4b. 网关白名单**：管理端点**不加白名单**（需登录鉴权）；浏览类 `/app/api/product/list|tree` 已放行。若需新增浏览端点再在 [WhiteListConfig.java](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-gateway/src/main/java/com/fengluan/gateway/config/WhiteListConfig.java) 追加。

---

### Day 8 操作步骤

#### 步骤 D8-1：member-spi 重构为纯契约（任务 8.5）

**1a. 改 pom**：编辑 [member-spi/pom.xml](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-spi/goods-store-member-spi/pom.xml)，把 `spring-cloud-starter-openfeign` 换成 `spring-web`（同 product-spi）。

**1b. 新增 DTO/VO** `com.fengluan.spi.member.dto/vo`：

```java
// MemberProfileUpdateRequest.java
public class MemberProfileUpdateRequest {
    private String name; private String sex;
    private LocalDate birthday; private String portrait;
    private String email; private String phone;
}

// MemberAddressRequest.java
public class MemberAddressRequest {
    @NotBlank private String receiver;   // 收货人
    @NotBlank private String phone;
    private Integer addrId;              // 街道编号
    private String addrDetail;           // 地址详情
    private Boolean isDefault;           // 是否设为默认
}

// MemberAddressVO.java
public class MemberAddressVO {
    private Long id;
    private String receiver; private String phone;
    private Integer addrId; private String addrDetail;
    private Boolean isDefault;
}
```

**1c. `MemberVO` 补充脱敏字段**：`phone` 返回脱敏值，新增 `maskedCardId`（身份证脱敏），避免裸身份信息。

**1d. 重写 `MemberApi`（去 `@FeignClient`，纯契约）**：

```java
public interface MemberApi {
    // —— 会员资料 ——
    @GetMapping("/{id}") MemberVO getProfile(@PathVariable Long id);
    @PutMapping("/{id}") MemberVO updateProfile(@PathVariable Long id, @Valid @RequestBody MemberProfileUpdateRequest request);

    // —— 收货地址 ——
    @GetMapping("/{id}/address") List<MemberAddressVO> listAddress(@PathVariable Long id);
    @PostMapping("/{id}/address") MemberAddressVO createAddress(@PathVariable Long id, @Valid @RequestBody MemberAddressRequest request);
    @PutMapping("/{id}/address/{addrId}") MemberAddressVO updateAddress(@PathVariable Long id, @PathVariable Long addrId, @Valid @RequestBody MemberAddressRequest request);
    @DeleteMapping("/{id}/address/{addrId}") Void deleteAddress(@PathVariable Long id, @PathVariable Long addrId);
    @PutMapping("/{id}/address/{addrId}/default") Void setDefaultAddress(@PathVariable Long id, @PathVariable Long addrId);
    /** 供 trade-api 下单取默认地址 */
    @GetMapping("/{id}/address/default") MemberAddressVO getDefaultAddress(@PathVariable Long id);
}
```

> **越权语义**：`id` 为被操作会员；controller 从 `X-User-Id` 取当前用户校验 `id`==当前用户，不等抛 `FORBIDDEN`（`ErrorCode.FORBIDDEN(403)`）。

#### 步骤 D8-2：member-api 骨架 + 脱敏（任务 8.6 + 资料）

**2a. pom 补 `goods-store-member-spi`**（参照 brand-api，common 已由父管理）：

```xml
<dependency>
    <groupId>com.fengluan</groupId>
    <artifactId>goods-store-member-spi</artifactId>
    <version>1.0.0</version>
</dependency>
```

**2b. Mapper** `com.fengluan.member.repository`（`MemberMapper`/`MemberAddressMapper` `extends BaseMapper`）。账户→id：`MemberAddressEntity` 用 `memberAccount`(String) 关联，需 `MemberMapper.selectOne(eq(account))` 或 `MemberAddressMapper` 按 `member_account` 查询。

**2c. 脱敏工具类** `util/DesensitizeUtil.java`：

```java
public class DesensitizeUtil {
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) return idCard;
        return idCard.substring(0, 4) + "********" + idCard.substring(idCard.length() - 4);
    }
}
```

**2d. `MemberService/Impl`**：`getProfile(id)` 查 `member` → 转 `MemberVO`（`phone` 脱敏、`maskedCardId` 置脱敏值、不暴露 `password`）；`updateProfile` 校验存在并越权后 `updateById`。

#### 步骤 D8-3：地址 Service/Controller implements（任务 8.3/8.4/8.5）

**3a. `AddressService/Impl`**（默认地址互斥需事务）：

```java
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {
    private final MemberAddressMapper addressMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(String memberAccount, Long addrId) {
        // 1. 取消该会员所有默认地址
        addressMapper.update(null, new LambdaQueryWrapper<MemberAddressEntity>()
            .eq(MemberAddressEntity::getMemberAccount, memberAccount)
            .set(MemberAddressEntity::getIsDefault, false));
        // 2. 置目标地址为默认
        addressMapper.update(null, new LambdaQueryWrapper<MemberAddressEntity>()
            .eq(MemberAddressEntity::getId, addrId)
            .set(MemberAddressEntity::getIsDefault, true));
    }
    // listAddress / create / update / delete / getDefaultAddress
}
```

> 注：MyBatis-Plus 的 `update(entitySetNull, wrapper)` 中 `.set(...)` 可直接传入 `UpdateWrapper`；若用 `LambdaUpdateWrapper` 则 `new LambdaUpdateWrapper<...>().set(...).eq(...)`，`addressMapper.update(null, wrapper)`。以实际 MP 版本 API 为准。
>
> 关联键：操作前用当前用户 id 查 `member.account`（`member_account`），再按 account 操作地址；越权校验在校验 account==该用户对应 account。

**3b. `MemberController`/`AddressController implements MemberApi`**：控制器类级前缀 `/member/api`。`MemberController` 实现 `getProfile/updateProfile`；`AddressController` 实现 6 个地址方法。每个方法从 `@RequestHeader("X-User-Id") Long currentUserId` 取值（缺失视为未登录），校验与被操作 `id` 一致。

> 可让 `MemberController` 一个类实现全部积约（含地址），也可拆 `AddressController`。为清晰建议拆两个类，均 `implements` 契约并各实现对应方法。

**3c. 会员错误码**：`MemberAddress` 相关存在用 `ErrorCode` 1xxx 段；需新增 `MEMBER_ADDRESS_NOT_FOUND(1005,"地址不存在")` 等（在 [ErrorCode.java](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-common/src/main/java/com/fengluan/common/exception/ErrorCode.java#L18-L22) 会员段补充）。

#### 步骤 D8-4：web 会员聚合三件套（任务 8.7）

**4a. web pom 加 `goods-store-member-spi`**（同 brand/product 依赖）。

**4b. 三件套**（`com.fengluan.web.member`）：

```java
@FeignClient(name = "goods-store-member-api", path = "/member/api")
public interface MemberFeignClient extends MemberApi { }

@Service
@RequiredArgsConstructor
public class WebMemberService {
    private final MemberFeignClient memberFeignClient;
    // BFF 聚合：个人中心 = 资料 + 地址列表（聚合规则 1：补字段/一次编排）
    public Map<String, Object> profile(Long memberId) {
        return Map.of("profile", memberFeignClient.getProfile(memberId),
                      "addresses", memberFeignClient.listAddress(memberId));
    }
}

@RestController
@RequestMapping("/app/api/member")
@RequiredArgsConstructor
public class WebMemberController {
    private final WebMemberService webMemberService;
    // 当前用户从网关 X-User-Id 取（前端经 /app/api 进）
    @GetMapping("/profile")
    public ApiResult<?> profile(@RequestHeader("X-User-Id") Long memberId) {
        return ApiResult.success(webMemberService.profile(memberId));
    }
    // 地址增删改/默认 依次委托 ...
}
```

**4c. web 启动类**已 `@EnableFeignClients(basePackages="com.fengluan")`，`MemberFeignClient` 在 `com.fengluan.web.member` 内会被注册；`com.fengluan.spi.member.MemberApi` 纯契约无 `@FeignClient` 自动忽略。

**4d. 网关白名单**：会员资料/地址**不加白名单**（需登录，靠 `X-User-Id`）。若首页需匿名展示资料可单独追加。

---

## 五、验收标准

### Day 7 验收

- [ ] `ProductApi` 已**无** `@FeignClient`；新增 create/update/delete/status/deductStock/restoreStock 契约方法，`GoodController implements` 编译通过
- [ ] `POST /good/api` 新增商品（含详情图数组），`good_detail_pics` 写入对应 `good_id`
- [ ] `PUT /good/api/{id}` 编辑成功
- [ ] `PUT /good/api/{id}/status?takeDown=true` 下架成功；详情页/下单校验提示下架
- [ ] `DELETE /good/api/{id}` 逻辑删除，`good.is_del=1`，`page/getById` 不再返回
- [ ] `PUT /good/api/{id}/stock/deduct?count=5` 库存充足返回 `true`，不足返回 `false` 且库存不变（不超卖）
- [ ] `PUT /good/api/{id}/stock/restore?count=5` 库存恢复
- [ ] **契约闭环**：product 契约单一来源，`trade-api`/`seckill-api` `extends ProductApi` 可直接扣库存
- [ ] **web 收口**：`/app/api/product/admin/*`（经网关，需登录）完成上下架/删除/编辑

### Day 8 验收

- [ ] `member-spi` 的 `MemberApi` 已**无** `@FeignClient`；pom 只依赖 spring-web
- [ ] `GET /member/api/{id}` 返回脱敏会员（`phone` 已 `****`、`maskedCardId` 脱敏、无 `password`）
- [ ] `PUT /member/api/{id}` 修改昵称/邮箱成功
- [ ] 跨用户访问（header `X-User-Id` 与路径 id 不一致）返回 403
- [ ] `GET /member/api/{id}/address` 地址列表，默认地址排第一
- [ ] `POST /member/api/{id}/address` 新增地址
- [ ] `PUT /member/api/{id}/address/{addrId}/default` 设默认，旧默认自动取消（互斥）
- [ ] `GET /member/api/{id}/address/default` 返回默认地址（供 trade 下单复用）
- [ ] **契约闭环**：`MemberController`/`AddressController implements MemberApi`；trade/web 消费只 `extends` 不手写路径
- [ ] **web 收口**：`GET /app/api/member/profile`（经网关 + `X-User-Id`）返回资料 + 地址

---

## 六、端口分配表

| 服务                    | 端口 | 说明                                     |
| ----------------------- | ---- | ---------------------------------------- |
| goods-store-gateway     | 8888 | 网关，前端唯一入口                       |
| goods-store-web         | 8090 | BFF 聚合层                               |
| goods-store-brand-api   | 8081 | 品牌服务                                 |
| goods-store-product-api | 8084 | 商品服务                                 |
| goods-store-member-api  | 8085 | 会员服务（现状）                         |
| goods-store-auth-api    | 8083 | 认证服务                                 |
| Nacos                   | 8848 | 配置中心 / 服务注册                      |
| Sentinel Dashboard      | 8080 | 限流控制台                               |

---

## 七、常见问题 / 回滚

| 现象                                                                    | 处理                                                                                                         |
| ----------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------ |
| `MemberApi` 删除 `@FeignClient` 后旧调用方报错                          | 全站统一改注入 `XxxFeignClient extends MemberApi`；`@EnableFeignClients(basePackages="com.fengluan")`          |
| spi 出现 `ApiResult`/`BaseMapper`/mybatis 类型                           | spi 不依赖 common/mybatis；返回 `Boolean`/`Void`/VO；分页/C 谷用 spi 内 POJO                                        |
| 商品逻辑删除不生效                                                      | 确认 `good.is_del` 列存在 + 全局 `logic-delete-field:isDel`；必要时在 `GoodEntity.isDel` 加 `@TableLogic`        |
| 库存扣减不阻止超卖                                                      | 使用 `UPDATE ... WHERE qty>=#{count}` 原子语句；勿用先查后更（非原子）                                           |
| 项目组织新增 `member_address` 找不到数据                                | 该表用 `member_account`(varchar) 关联，需先按 id 查 `member.account` 再做地址操作                                 |
| 跨用户越权                                                              | 所有受保护方法 `@RequestHeader("X-User-Id")` 校验 == 路径 id，不等抛 `FORBIDDEN(403)`                           |
| 默认地址互斥未生效（同时多个默认）                                      | 用 `@Transactional` 包裹「先清 0 再置 1」两步；用 `LambdaUpdateWrapper`.set/.eq 写法，勿用 select-then-update     |
| 会员查询返回了 `password`                                               | `MemberVO` 不含 password；`toVO` 用 BeanUtils 拷贝后手动置空 + 脱敏敏感字段                                      |
| 商品管理端点 401                                                       | 管理/会员端点默认需登录，勿加白名单；仅在确属匿名浏览时追加 `WhiteListConfig`                                    |
| 回滚                                                                    | 还原 spi pom/MemberApi/ProductApi 契约/Controller/Service/web POM 及三件套/`WhiteListConfig`/`ErrorCode` 即可   |
```