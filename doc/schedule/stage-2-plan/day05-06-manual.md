# Day 5-6 操作执行手册 — 品牌服务 + 商品服务（上）+ spi 契约兑现 + web 聚合

> 对应计划：[14-day-implementation-plan.md](file:///d:/.workspace/javaproject/goods-store-management-system-parent/doc/schedule/14-day-implementation-plan.md) Day 5（品牌）/ Day 6（商品·上）
> 版本：v1.0　编写日期：2026-09-02　执行日期：待填
> 主题：品牌完整 CRUD + Logo 上传 + BrandApi 契约兑现；商品分类树 + 列表/详情/搜索 + 品牌 Feign 降级 + web 商品浏览聚头
> 依赖：Day 1-4 已完成（Nacos 配置中心 + 公共模块 + 6 个 API 接入配置中心 + Druid + 网关路由/JWT 鉴权 + 认证服务闭环 + web 收口脚手架 `/app/api/auth/*`）
> **架构前提**：遵循 14 天计划「⭐ 分层架构规范」——spi 是**纯 HTTP 契约层**（禁 `@FeignClient`）、controller `implements` 契约、消费方 Feign `extends` 契约、web 是 BFF 聚合层（前端唯一入口 `/app/api/**`）。

---

## 一、Day 5-6 目标与产出物

### 目标

1. **brand-api** 交付完整品牌能力：分页/详情/新增/编辑/逻辑删除 + Logo 上传，`BrandController implements BrandApi`
2. **brand-spi** 从「带 `@FeignClient` 的测试接口」重构为**纯契约** `BrandApi`（CRUD + DTO/VO），全站保持一致
3. **product-spi / product-api** 交付分类树 + 商品分页/详情/搜索，`GoodController implements ProductApi`
4. **契约消费**：product-api 通过 Feign `extends` brand 契约拿品牌名，异常降级显示 "-"，**不另造品牌接口**
5. **web 聚合**：品牌位 `/app/api/brand/list` + 商品浏览 `/app/api/product/*` 收口（BFF）

### 产出物清单

| 模块          | 产出物                                                                                                     |
| ------------- | ---------------------------------------------------------------------------------------------------------- |
| brand-spi     | `BrandApi`（纯契约，去 `@FeignClient`，CRUD）+ `BrandCreateRequest/BrandUpdateRequest` + `PageVO<T>`       |
| brand-api     | `BrandController implements BrandApi` + `FileUploadUtil/LocalFileUploadUtil` + `WebMvcConfig` + 逻辑删除    |
| 品牌 DB        | `ALTER TABLE brand ADD is_del`（逻辑删除字段）                                                             |
| product-spi   | `ProductApi`（纯契约，去 `@FeignClient`）+ `CategoryTreeVO` + `GoodVO` 补 brandName/categoryName/detailPicList |
| product-api   | `CategoryMapper/CategoryService/Controller` + `GoodServiceImpl` 扩展 + `GoodDetailPicsMapper` + `BrandRemoteClient`(fallback)/`BrandRemoteService` |
| web           | `brand/` 三件套 + `product/` 三件套（Feign 客户端 `extends` 契约 + 聚合 Service + Controller `/app/api/**`） |

---

## 二、技术要点

### Day 5 技术要点

| 要点                  | 说明                                                                                                                      |
| --------------------- | ------------------------------------------------------------------------------------------------------------------------- |
| MyBatis-Plus 逻辑删除 | `@TableLogic` 注解 + DDL；删除后查询自动过滤 `is_del=1`                                                                   |
| spi 契约即实现        | `BrandApi` 去 `@FeignClient`、只写路径注解 + DTO/VO；`BrandController implements BrandApi`                                |
| PageVO 与 spi 纯净性  | spi 不得依赖 common/mybatis，分页返回不能是 `IPage`（mybatis 类型）→ 在 spi 内定义纯 POJO `PageVO<T>`，controller 做转换  |
| 文件上传              | `MultipartFile` 存本地 `/static/upload/brand/`，`@ConditionalOnProperty` 切换 OSS；**upload 不进契约**                   |
| 名称唯一性            | 新增/编辑时按 `name` 唯一校验，重复抛业务异常                                                                              |
| 静态资源映射          | `WebMvcConfig` 把 `/static/**` 映射到本地上传目录                                                                          |

> 注：当前 [BrandApi.java](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-spi/goods-store-brand-spi/src/main/java/com/fengluan/spi/brand/BrandApi.java#L13-L18) 带 `@FeignClient` 且只有 `list()`，是本轮必须推倒重构的对象。

### Day 6 技术要点

| 要点           | 说明                                                                                                  |
| -------------- | ----------------------------------------------------------------------------------------------------- |
| 分类树         | 一次查全表 + `Map<Integer,List<...>> parentMap` 内存构建，避免 N+1；顶层 parentId=null/0               |
| 品牌名称组装   | **契约消费**：`BrandRemoteClient extends BrandApi` + `fallbackFactory`，不可用降级返回空 → brandName "-" |
| 多条件筛选     | MyBatis-Plus `LambdaQueryWrapper` 动态拼接（categoryId / brandId / price 区间 / isHot / 关键词）        |
| 关键词搜索     | `LIKE` 匹配 `name`、`alias`、`summary`                                                                 |
| 详情图一对多   | 查 `good_detail_pics` 表，按 `sort` 升序收集 `List<String>` 存入 `GoodVO.detailPicList`                |
| 网关/白名单    | 商品控制器前缀实际是 **`/good/api`**（`GoodController`），路由与白名单均已放行该前缀                     |

> 注意：计划书 Day 6 写 `/product/api`，但当前工程实际前缀是 **`/good/api/**`**，手册以工程为准。方法仍由 `ProductApi` 契约声明。

---

## 三、Day 5-6 实施状态盘点

> 基于当前工程实测。

| 计划任务 | 内容                              | 当前状态                                                                                    | 手册步骤 |
| -------- | --------------------------------- | ------------------------------------------------------------------------------------------- | -------- |
| 5.7      | BrandApi 契约兑现（去 @FeignClient）| ⚠️ `BrandApi` 仍带 `@FeignClient`、只有 `list()`；`BrandController` 未 `implements`（返回 IPage）| 步骤 D5-1 |
| 5.1/5.5  | BrandService 增删改 + 名称唯一    | ⚠️ 只有 `page()`；实体无 `isDel`（无逻辑删除）                                              | 步骤 D5-2 |
| 5.6      | BrandVO 完善                      | ✅ 已含 id/name/company/logo/site/description；brand-spi pom 仍依赖 openfeign（需改）        | 步骤 D5-1 |
| 5.3/5.4  | 文件上传 + 静态资源映射           | ❌ 无 `FileUploadUtil`/`WebMvcConfig`                                                       | 步骤 D5-3 |
| 5.2      | BrandController 5 接口            | ❌ 仅有 `/page`，且返回 `ApiResult<IPage<BrandVO>>`                                          | 步骤 D5-2 |
| 5.8      | web 品牌聚合 `/app/api/brand/list` | ❌ web 无 brand-spi / web 无 brand 三件套                                                    | 步骤 D5-4 |
| 6.1      | 分类树                            | ⚠️ `CategoryEntity` 已建，但无 `CategoryMapper/Service/Controller`                           | 步骤 D6-1 |
| 6.3/6.4  | GoodService/Controller 完善       | ⚠️ 只有 `/good/api/page` 空壳；`GoodServiceImpl` 仅按 name LIKE 分页                          | 步骤 D6-2 |
| 6.5      | 品牌 Feign 调用（降级）           | ❌ product-api pom 缺 brand-spi；无 `BrandRemoteClient/Service`                              | 步骤 D6-3 |
| 6.7      | GoodDetailPicsMapper              | ⚠️ `GoodDetailPicsEntity` 已建，无 Mapper                                                  | 步骤 D6-2 |
| 6.6      | GoodVO 完善                       | ⚠️ `GoodVO` 缺 brandName/categoryName/detailPicList；`ProductApi` 仍带 @FeignClient          | 步骤 D6-1 |
| 6.8      | web 商品聚合 `/app/api/product/*` | ❌ web 无 product-spi / 无 product 三件套                                                    | 步骤 D6-4 |

网关路由（[application.yaml](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-gateway/src/main/resources/application.yaml)）已具备 `/app/api/**→web`、`/brand/api/**`、`/good/api/**`；白名单已放行 `/brand/api/**`、`/good/api/**`（较计划「仅浏览」更宽，见注意事项）。web 端口已为 **8090**。

---

## 四、详细操作步骤

### 前置约定

- **spi 纯净**：`brand-spi`/`product-spi` 的 pom 只依赖 `spring-web` + `jakarta.validation-api`（同 auth-spi），**不要**再依赖 `spring-cloud-starter-openfeign`。
- **契约路径**：brand 控制器类级前缀 `/brand/api`；product 控制器类级前缀 `/good/api`。Feign 客户端 `path` 与之保持一致。
- **分页对象**：契约方法返回 spi 内定义的 `PageVO<T>`（纯 POJO），api 层把 mybatis `IPage` 转换为 `PageVO`。
- **白名单提醒**：前端浏览类端点（`/app/api/brand/list`、`/app/api/product/list|tree|detail`）需在网关白名单放行（无需登录）。

---

### Day 5 操作步骤

#### 步骤 D5-1：brand-spi 重构为纯契约（任务 5.7 / 5.6）

**1a. 改 pom**：编辑 [brand-spi/pom.xml](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-spi/goods-store-brand-spi/pom.xml)，把 `spring-cloud-starter-openfeign` 换成：

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-web</artifactId>
</dependency>
<dependency>
    <groupId>jakarta.validation</groupId>
    <artifactId>jakarta.validation-api</artifactId>
</dependency>
```

**1b. 新增纯分页对象** `src/main/java/com/fengluan/spi/brand/vo/PageVO.java`：

```java
package com.fengluan.spi.brand.vo;

import lombok.Data;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageVO<T> {
    private Long total;
    private List<T> records;
    public static <T> PageVO<T> empty() {
        return new PageVO<>(0L, Collections.emptyList());
    }
}
```

**1c. 新增 DTO** `com.fengluan.spi.brand.dto`：

```java
// BrandCreateRequest.java
public class BrandCreateRequest {
    @NotBlank(message = "品牌名称不能为空") private String name;
    private String company;
    private String logo;
    private String site;
    private String description;
}

// BrandUpdateRequest.java —— 字段同 Create，仅多 @NotNull id（或并入 create 使用）
public class BrandUpdateRequest {
    @NotNull private Long id;
    @NotBlank private String name;
    private String company; private String logo; private String site; private String description;
}
```

**1d. 重写 `BrandApi`（去 `@FeignClient`，纯契约）**：

```java
package com.fengluan.spi.brand;

import com.fengluan.spi.brand.dto.BrandCreateRequest;
import com.fengluan.spi.brand.dto.BrandQueryRequest;
import com.fengluan.spi.brand.dto.BrandUpdateRequest;
import com.fengluan.spi.brand.vo.BrandVO;
import com.fengluan.spi.brand.vo.PageVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

public interface BrandApi {

    @GetMapping("/page")
    PageVO<BrandVO> page(BrandQueryRequest query);

    @GetMapping("/{id}")
    BrandVO getById(@PathVariable Long id);

    @PostMapping
    BrandVO create(@Valid @RequestBody BrandCreateRequest request);

    @PutMapping("/{id}")
    BrandVO update(@PathVariable Long id, @Valid @RequestBody BrandUpdateRequest request);

    @DeleteMapping("/{id}")
    Void delete(@PathVariable Long id);
}
```

> 上传 `upload` **不进契约**（MultipartFile 属人机交互）。

#### 步骤 D5-2：brand-api 逻辑删除 + CRUD（任务 5.1 / 5.5 / 5.2）

**2a. DDL + 实体**：先执行 SQL，再给 [BrandEntity.java](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-service/goods-store-brand-api/src/main/java/com/fengluan/brand/entity/BrandEntity.java) 加 `@TableLogic` 字段。

```sql
ALTER TABLE brand ADD COLUMN is_del BIT(1) DEFAULT b'0' COMMENT '是否逻辑删除(0否1是)';
```

```java
@TableLogic
private Boolean isDel;
```

带 `@TableLogic` 后 `selectById/deleteById/page` 自动过滤/置 1，无需手写 SQL。

**2b. `BrandService` 接口扩展**（[BrandService.java](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-service/goods-store-brand-api/src/main/java/com/fengluan/brand/service/BrandService.java)：接口签名返回类型改 `PageVO<BrandVO>`；新增 `getById/create/update/delete`）：

```java
public interface BrandService extends IService<BrandEntity> {
    PageVO<BrandVO> page(BrandQueryRequest query);
    BrandVO getById(Long id);
    BrandVO create(BrandCreateRequest request);
    BrandVO update(Long id, BrandUpdateRequest request);
    void delete(Long id);
}
```

**2c. `BrandServiceImpl` 实现**（[BrandServiceImpl.java](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-service/goods-store-brand-api/src/main/java/com/fengluan/brand/service/impl/BrandServiceImpl.java))。要点：

- `page`：分页查询后用 `IPage` → `PageVO` 转换（`getTotal()`、`getRecords()`）；保留 `name` LIKE + `orderByDesc(id)`。
- `create/update`：按 `name` 做唯一性校验（排除自身），命中抛 `BusinessException`（新增 `ErrorCode.BRAND_EXISTS(5002,"品牌名称已存在")`，或复用 `BAD_REQUEST`）。
- `getById/delete`：查不到抛 `BusinessException(ErrorCode.BRAND_NOT_FOUND)`。

**2d. `BrandController implements BrandApi`**：

```java
@RestController
@RequestMapping("/brand/api")
@RequiredArgsConstructor
public class BrandController implements BrandApi {
    private final BrandService brandService;

    @Override public PageVO<BrandVO> page(BrandQueryRequest q) { return brandService.page(q); }
    @Override public BrandVO getById(Long id) { return brandService.getById(id); }
    @Override public BrandVO create(BrandCreateRequest r) { return brandService.create(r); }
    @Override public BrandVO update(Long id, BrandUpdateRequest r) { return brandService.update(id, r); }
    @Override public Void delete(Long id) { brandService.delete(id); return null; }

    // 上传不进契约，单独暴露（MultipartFile）
    @PostMapping("/upload")
    public ApiResult<String> upload(@RequestParam("file") MultipartFile file) { /* 见 D5-3 */ }
}
```

#### 步骤 D5-3：文件上传 + 静态映射（任务 5.3 / 5.4）

**3a. 上传接口** `com.fengluan.brand.util.FileUploadUtil`（接口）+ `LocalFileUploadUtil`（实现）：

```java
public interface FileUploadUtil { String upload(MultipartFile file); }

@Component
@ConditionalOnProperty(name = "upload.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileUploadUtil implements FileUploadUtil {
    public String upload(MultipartFile file) {
        // 生成唯一文件名，存到 ./static/upload/brand/，返回可访问 URL 前缀 + 相对路径
    }
}
```

> 生产可再加 `OssFileUploadUtil`（`havingValue="oss"`），本轮不写。

**3b. 配置**：Nacos `goods-store-brand-api.yaml` 增加：

```yaml
upload:
  storage:
    type: ${UPLOAD_STORAGE_TYPE:local}
```

**3c. 静态资源映射** `config/WebMvcConfig.java`：

```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/upload/brand/**")
                .addResourceLocations("file:" + Paths.get("./static/upload/brand").toAbsolutePath() + "/");
    }
}
```

`LocalFileUploadUtil` 里 `upload` 返回 `/static/upload/brand/<新文件名>`，网关/浏览器拼主机访问。

#### 步骤 D5-4：web 品牌聚合三件套（任务 5.8）

**4a. web pom 加 brand-spi**（[goods-store-web/pom.xml](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-web/pom.xml)）。

```xml
<dependency>
    <groupId>com.fengluan</groupId>
    <artifactId>goods-store-brand-spi</artifactId>
    <version>1.0.0</version>
</dependency>
```

**4b. 三件套**（`com.fengluan.web.brand`）：

```java
@FeignClient(name = "goods-store-brand-api", path = "/brand/api")
public interface BrandFeignClient extends BrandApi { }

@Service
@RequiredArgsConstructor
public class WebBrandService {
    private final BrandFeignClient brandFeignClient;
    public List<BrandVO> listForHome() {
        BrandQueryRequest q = new BrandQueryRequest();
        q.setPageNum(1L); q.setPageSize(8L);
        return brandFeignClient.page(q).getRecords();
    }
}

@RestController
@RequestMapping("/app/api/brand")
@RequiredArgsConstructor
public class WebBrandController {
    private final WebBrandService webBrandService;
    @GetMapping("/list")
    public ApiResult<List<BrandVO>> listForHome() {
        return ApiResult.success(webBrandService.listForHome());
    }
}
```

> web 启动类已 `@EnableFeignClients(basePackages="com.fengluan")`，`BrandFeignClient` 在 `com.fengluan.web.brand` 内会被注册为 Feign；纯契约 `com.fengluan.spi.brand.BrandApi` 无 `@FeignClient` 自动忽略。

**4c. 网关白名单**：向 [WhiteListConfig.java](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-gateway/src/main/java/com/fengluan/gateway/config/WhiteListConfig.java) 追加 `/app/api/brand/list`（浏览无需登录）。

---

### Day 6 操作步骤

#### 步骤 D6-1：product-spi 重构 + 分类树（任务 6.1 / 6.2 / 6.6）

**1a. 改 product-spi pom**：[product-spi/pom.xml](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-spi/goods-store-product-spi/pom.xml) 去 openfeign，换 `spring-web`。

**1b. 新增 `CategoryTreeVO`** `com.fengluan.spi.product.vo.CategoryTreeVO`：

```java
@Data
public class CategoryTreeVO {
    private Long id;
    private Integer parentId;
    private String name;
    private String icon;
    private List<CategoryTreeVO> children = new ArrayList<>();
}
```

**1c. `GoodVO` 补充字段**（[GoodVO.java](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-spi/goods-store-product-spi/src/main/java/com/fengluan/spi/product/vo/GoodVO.java)），并补 price/pic（前端列表渲染需要）：

```java
private BigDecimal markPrice;
private BigDecimal price;
private String pic;
private String alias;      // 已有
private String brandName;      // 品牌名（由 brand Feign 填充）
private String categoryName;   // 分类名
private List<String> detailPicList;  // good_detail_pics 表按 sort 排序收集
```

> 注意：`GoodEntity` 本身有一列 `detailPics String`，与 `good_detail_pics` 表并存易混淆。契约 VO 用 `detailPicList` 表达一对多详情图，避免与单列混用。

**1d. 重写 `ProductApi`（纯契约）**：

```java
package com.fengluan.spi.product;

import com.fengluan.spi.product.dto.GoodQueryRequest;
import com.fengluan.spi.product.vo.CategoryTreeVO;
import com.fengluan.spi.product.vo.GoodVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;

public interface ProductApi {

    @GetMapping("/category/tree")
    List<CategoryTreeVO> categoryTree();

    @GetMapping("/good/page")
    List<GoodVO> page(GoodQueryRequest request);

    @GetMapping("/good/{id}")
    GoodVO getById(@PathVariable Long id);
}
```

> 商品分页建议也改为 `PageVO<GoodVO>`（与品牌一致），非必须；本轮可先返回 `List<GoodVO>`，保持与网关/白名单 `/good/api/**` 一致（实战按 `PageVO` 规整更佳）。

**1e. 分类树**：新增 `CategoryMapper`（`extends BaseMapper<CategoryEntity>`）、`CategoryService/Impl`、`CategoryController`。树构建：

```java
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryTreeVO> getTree() {
        List<CategoryEntity> all = categoryMapper.selectList(
            new LambdaQueryWrapper<CategoryEntity>().orderByAsc(CategoryEntity::getSort));
        Map<Integer, List<CategoryTreeVO>> parentMap = new HashMap<>();
        List<CategoryTreeVO> vos = all.stream().map(this::toVO).toList();
        for (CategoryTreeVO vo : vos) parentMap.computeIfAbsent(vo.getParentId(), k -> new ArrayList<>()).add(vo);
        for (CategoryTreeVO vo : vos) vo.setChildren(parentMap.getOrDefault(vo.getId(), Collections.emptyList()));
        return parentMap.getOrDefault(null, parentMap.getOrDefault(0, Collections.emptyList()));
    }
}

@RestController
@RequestMapping("/good/api")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;
    @GetMapping("/category/tree")
    public ApiResult<List<CategoryTreeVO>> tree() {
        return ApiResult.success(categoryService.getTree());
    }
}
```

#### 步骤 D6-2：GoodService / Controller 完善 + 详情图（任务 6.3 / 6.4 / 6.7）

**2a. 扩 `GoodQueryRequest`**（[GoodQueryRequest.java](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-spi/goods-store-product-spi/src/main/java/com/fengluan/spi/product/dto/GoodQueryRequest.java)）增加筛选/搜索字段：

```java
private Integer categoryId;
private Integer brandId;
private BigDecimal minPrice;
private BigDecimal maxPrice;
private Boolean isHot;
private String keyword;   // 关键词：匹配 name/alias/summary
```

**2b. `GoodDetailPicsMapper`**（`com.fengluan.product.repository`）：

```java
@Mapper
public interface GoodDetailPicsMapper extends BaseMapper<GoodDetailPicsEntity> { }
```

**2c. `GoodServiceImpl` 重写**（[GoodServiceImpl.java](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-service/goods-store-product-api/src/main/java/com/fengluan/product/service/impl/GoodServiceImpl.java)）：注入 `CategoryMapper` + `GoodDetailPicsMapper` + `BrandRemoteService`（D6-3）；多条件 + 关键词 + 组装 brandName/categoryName/detailPicList。要点：

```java
LambdaQueryWrapper<GoodEntity> w = new LambdaQueryWrapper<GoodEntity>()
    .eq(query.getCategoryId() != null, GoodEntity::getCategoryId, query.getCategoryId())
    .eq(query.getBrandId() != null, GoodEntity::getBrandId, query.getBrandId())
    .ge(query.getMinPrice() != null, GoodEntity::getPrice, query.getMinPrice())
    .le(query.getMaxPrice() != null, GoodEntity::getPrice, query.getMaxPrice())
    .eq(query.getIsHot() != null, GoodEntity::getIsHot, query.getIsHot())
    .and(StringUtils.isNotBlank(query.getKeyword()), subQuery -> subQuery
        .like(GoodEntity::getName, query.getKeyword())
        .or().like(GoodEntity::getAlias, query.getKeyword())
        .or().like(GoodEntity::getSummary, query.getKeyword()))
    .orderByAsc(GoodEntity::getId);
```

- 详情图：`goodDetailPicsMapper.selectList(new LambdaQueryWrapper<GoodDetailPicsEntity>()
      .eq(GoodDetailPicsEntity::getGoodId, id).orderByAsc(GoodDetailPicsEntity::getSort))`
      `.stream().map(GoodDetailPicsEntity::getUrl).toList()` → `vo.setDetailPicList(...)`。
- 品牌名：一批商品 `brandId` 收集后，交给 `BrandRemoteService.getBrandNameMap(...)`（返回 `Map<Integer,String>`），`vo.setBrandName(map.get(brandId))` 缺省 `"-"`。
- 分类名：查 `CategoryMapper.selectById(categoryId)` 得 name，缺省 `"-"`。

**2d. `GoodController implements ProductApi`**：

```java
@RestController
@RequestMapping("/good/api")
@RequiredArgsConstructor
public class GoodController implements ProductApi {
    private final GoodService goodService;
    @Override public List<CategoryTreeVO> categoryTree() { /* 或委托 CategoryController */ }
    @Override public List<GoodVO> page(GoodQueryRequest q) { return goodService.page(q); }
    @Override public GoodVO getById(Long id) { return goodService.getById(id); }
}
```

> 若分类树已有 `CategoryController`，`GoodController` 仅需 `implements` 中 `page/getById` 两个；`ProductApi` 声明的方法 controller 都需实现（契约即实现）。

#### 步骤 D6-3：品牌 Feign 契约消费 + 降级（任务 6.5）

**3a. product-api pom 加 brand-spi**（[product-api/pom.xml](file:///d:/.workspace/javaproject/goods-store-management-system-parent/goods-store-service/goods-store-product-api/pom.xml)）：已含 openfeign/loadbalancer，补：

```xml
<dependency>
    <groupId>com.fengluan</groupId>
    <artifactId>goods-store-brand-spi</artifactId>
    <version>1.0.0</version>
</dependency>
```

**3b. `BrandRemoteClient extends BrandApi`（fallback）** `com.fengluan.product.remote`：

```java
@FeignClient(name = "goods-store-brand-api", path = "/brand/api",
             fallbackFactory = BrandClientFallbackFactory.class)
public interface BrandRemoteClient extends BrandApi { }

@Component
public class BrandClientFallbackFactory implements FallbackFactory<BrandRemoteClient> {
    @Override
    public BrandRemoteClient create(Throwable cause) {
        return new BrandRemoteClient() {
            @Override public PageVO<BrandVO> page(BrandQueryRequest q) { return PageVO.empty(); }
            @Override public BrandVO getById(Long id) { return null; }
            @Override public BrandVO create(BrandCreateRequest r) { return null; }
            @Override public BrandVO update(Long id, BrandUpdateRequest r) { return null; }
            @Override public Void delete(Long id) { return null; }
        };
    }
}
```

**3c. `BrandRemoteService`（封装降级，保底）**：

```java
@Component
@RequiredArgsConstructor
public class BrandRemoteService {
    private final BrandRemoteClient brandRemoteClient;
    public Map<Integer, String> getBrandNameMap(Collection<Integer> brandIds) {
        try {
            BrandQueryRequest q = new BrandQueryRequest();
            q.setPageNum(1L); q.setPageSize(1000L);
            return brandRemoteClient.page(q).getRecords().stream()
                .filter(b -> brandIds.contains(b.getId().intValue()))
                .collect(Collectors.toMap(BrandVO::getId, Integer::intValue,
                        (a, b) -> b,
                        () -> { /* 不写，见下 */ }));
        } catch (Exception e) {
            log.error("查询品牌名称失败，降级返回空", e);
            return Collections.emptyMap();
        }
    }
}
```

> `BrandQueryRequest` 现在没有 `page` 之外按 id 批量查的契约方法；若仍拿全量不够优雅，可后续在 `BrandApi` 增加 `@GetMapping("/ids")` 批量方法。当前）用 `page(q)` 拉全量再过滤即可。降级时 `getBrandNameMap` 返空 → `goodServiceImpl` 补 `"-"`。

#### 步骤 D6-4：web 商品浏览聚合三件套（任务 6.8）

**4a. web pom 加 product-spi**：

```xml
<dependency>
    <groupId>com.fengluan</groupId>
    <artifactId>goods-store-product-spi</artifactId>
    <version>1.0.0</version>
</dependency>
```

**4b. 三件套**（`com.fengluan.web.product`）：

```java
@FeignClient(name = "goods-store-product-api", path = "/good/api")
public interface ProductFeignClient extends ProductApi { }

@Service
@RequiredArgsConstructor
public class WebProductService {
    private final ProductFeignClient productFeignClient;
    public Object list(GoodQueryRequest q) { return productFeignClient.page(q); }
    public Object tree() { return productFeignClient.categoryTree(); }
}

@RestController
@RequestMapping("/app/api/product")
@RequiredArgsConstructor
public class WebProductController {
    private final WebProductService webProductService;
    @GetMapping("/list")  public ApiResult<?> list(GoodQueryRequest q) { return ApiResult.success(webProductService.list(q)); }
    @GetMapping("/tree")  public ApiResult<?> tree() { return ApiResult.success(webProductService.tree()); }
}
```

**4c. 网关白名单**追加 `/app/api/product/list`、`/app/api/product/tree`（浏览无需登录）。

---

## 五、验收标准

### Day 5 验收

- [ ] `brand-spi` 的 `BrandApi` 已**无** `@FeignClient`；pom 只依赖 spring-web/validation（服务端依赖一致）
- [ ] `BrandController implements BrandApi` 编译通过，契约方法 5 个（page/getById/create/update/delete）
- [ ] `GET /brand/api/page?pageNum=1&pageSize=10&name=华为` 返回 `PageVO`（含 total/records）
- [ ] `GET /brand/api/1` 详情；不存在返回 5001
- [ ] `POST /brand/api` 新增成功；重复 `name` 抛业务异常（5002 / BAD_REQUEST）
- [ ] `PUT /brand/api/1` 修改成功
- [ ] `DELETE /brand/api/1` 后 `good`-表`is_del=1`，`page/getById` 不再返回
- [ ] `DB: brand` 表已有 `is_del` 列
- [ ] `POST /brand/api/upload` 上传 Logo 返回可访问 URL，`GET` 该 URL 可见图片
- [ ] **web 收口**：`GET /app/api/brand/list`（经网关）→ web → Feign → brand-api 返回前 8 条品牌（含 logo）

### Day 6 验收

- [ ] `product-spi` 的 `ProductApi` 已无 `@FeignClient`；pom 只依赖 spring-web/validation
- [ ] `GET /good/api/category/tree` 返回完整嵌套分类树
- [ ] `GET /good/api/good/page?categoryId=10&keyword=手机` 按分类+关键词筛选（匹配 name/alias/summary）
- [ ] `GET /good/api/good/1` 返回详情，含 brandName/categoryName/detailPicList（详情图按 sort 升序）
- [ ] 商品不存在返回 2001
- [ ] 停掉 brand-api，商品列表 `brandName` 显示 `"-"`（降级生效，product-api 不宕机）
- [ ] **契约消费**：product-api 品牌名只走 `brand-spi` 契约，无自造品牌接口/URL
- [ ] **web 收口**：`GET /app/api/product/list`、`/app/api/product/tree`（经网关）正常返回

---

## 六、端口分配表

| 服务                    | 端口 | 说明                                     |
| ----------------------- | ---- | ---------------------------------------- |
| goods-store-gateway     | 8888 | 网关，前端唯一入口                       |
| goods-store-web         | 8090 | BFF 聚合层（Day3-4 已改定）           |
| goods-store-brand-api   | 8081 | 品牌服务                                 |
| goods-store-product-api | 8084 | 商品服务                                 |
| goods-store-auth-api    | 8083 | 认证服务（Day3-4）                       |
| Nacos                   | 8848 | 配置中心 / 控制台 http://localhost:8848  |
| Sentinel Dashboard      | 8080 | 限流控制台                               |

> 品牌上传文件本地目录为运行目录 `./static/upload/brand/`。

---

## 七、常见问题 / 回滚

| 现象                                                             | 处理                                                                                                                   |
| ---------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------- |
| `BrandApi` 删除 `@FeignClient` 后旧调用方报错                    | 全站统一改注入 `XxxFeignClient extends BrandApi`；确保 `@EnableFeignClients(basePackages="com.fengluan")`             |
| spi 出现了 `IPage`/`BaseMapper`（mybatis/types）                  | spi 不许依赖 mybatis/common；改为 spi 内 `PageVO<T>`（纯 POJO），api 层转换                                          |
| 逻辑删除不生效 / 查不到                                           | 确认 DDL 已加 `is_del` 列 + 实体字段有 `@TableLogic`；逻辑删除字段默认参与所有 select                          |
| 商品 brandName 不显示 / 显示 null                                | 确认 product-api 已注入 `BrandRemoteService` 并补 `"-"`；brand-api 已起、`BrandRemoteClient` path 为 `/brand/api`        |
| 分类树出现无限引用/栈溢出                                         | 检查 category 表是否存在 parentId 成环数据；`parentMap.getOrDefault` 先判 null 再取 0                               |
| detailPicList 为空                                                | 确认 `good_detail_pics` 表有对应 `good_id` 数据；使用 `GoodDetailPicsEntity.getGoodId`（Integer）等于 `GoodEntity.id`    |
| `/app/api/*` 返回 401                                             | 浏览类端点（brand/list、product/list|tree|detail）未加白名单，在 `WhiteListConfig` 追加即可                             |
| product 用 `/product/api` 404                                     | 工程实际是 `/good/api/**`（`GoodController` 前缀），路由与 Feign path 都用 `/good/api`                                  |
| 回滚                                                             | 还原 spi pom、BrandApi/ProductApi 接口、Controller/Service、web POM 及三件套、`WhiteListConfig` 新增行、`DB` 回滚 DDL 即可 |