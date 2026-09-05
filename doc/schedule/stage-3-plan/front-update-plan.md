# 商城前端实施计划（用户端 + 管理端）

> 阶段：Stage 3 —— 在「后端业务全部编译通过、中间件连接正常、Nacos 可发现所有服务」的基础上，补齐 Web 前端。
> 范围：**用户端（C 端）** 与 **管理端（后台）** 两个独立前端工程；后端仅在 web(BFF) 层做少量聚合接口补齐，不重写业务。

---

## 一、目标

1. **用户端**：面向消费者，完成「浏览 → 购物车 → 下单 → 订单 → 秒杀抢购」完整购物链路；设计追求**舒适、灵动、自然**。
2. **管理端**：面向运营，完成「商品/品牌/秒杀活动/订单/会员」管理；设计追求**标准、设计、便捷、合理**。
3. 双端共用统一请求封装、鉴权与错误处理，形成可面试演示的完整闭环。

---

## 二、现状盘点

### 2.1 后端已就绪的 BFF 接口（前端唯一入口 `/app/api/**`，经网关 8888 转发到 web）

| 域                                | 已就绪接口                                                                    | 说明                                       |
| --------------------------------- | ----------------------------------------------------------------------------- | ------------------------------------------ |
| 认证 `/app/api/auth`              | `POST /login` `POST /register` `POST /refresh` `POST /logout` `PUT /password` | `login` 的 `loginType` 支持 `member/admin` |
| 品牌 `/app/api/brand`             | `GET /list`                                                                   | 仅首页品牌                                 |
| 商品浏览 `/app/api/product`       | `GET /list` `GET /tree`                                                       | 分类树 + 多条件列表                        |
| 商品管理 `/app/api/product/admin` | `GET /{id}` `PUT /{id}` `PUT /{id}/status` `DELETE /{id}`                     | 缺 `create`、缺详情/列表暴露               |
| 购物车 `/app/api/cart`            | `GET /list` `POST /add` `PUT /{cartId}` `DELETE /{cartId}`                    | 依赖网关注入 `X-User-Id`                   |
| 下单 `/app/api/order`             | `POST /submit`                                                                | —                                          |
| 我的订单 `/app/api/order`         | `GET /page` `GET /{id}` `PUT /{id}/cancel` `PUT /{id}/confirm`                | 仅当前会员                                 |
| 会员 `/app/api/member`            | `GET /profile` `PUT /profile` 地址 CRUD                                       | 仅当前会员                                 |
| 秒杀 `/app/api/seckill`           | `GET /list` `POST /order` `GET /order/{orderNo}/result`                       | 抢购 + 结果轮询                            |

### 2.2 网关与鉴权（已就绪）

- 网关端口 **8888**，`/app/api/**` → `lb://goods-store-web`；其余 `/auth|brand|good|member|trade|seckill/api/**` 直连业务服务。
- `JwtAuthFilter`：白名单放行，其余校验 `Authorization: Bearer <token>`，并向下游透传 `X-User-Id`、`X-User-Roles`。
- 白名单（[WhiteListConfig.java](file:///d:.workspace/javaproject/goods-store-management-system-parent/goods-store-gateway/src/main/java/com/fengluan/gateway/config/WhiteListConfig.java)）：登录/注册/refresh、品牌列表、商品 list/tree 等。
- 统一返回：`ApiResult{code, message, data}`（成功时 `data` 为业务对象，失败走 GlobalExceptionHandler 的 `code+message`）。

### 2.3 缺口分析（需后端配套补齐，详见第七节）

| #   | 缺口                                                                                                                   | 影响           | 优先级 |
| --- | ---------------------------------------------------------------------------------------------------------------------- | -------------- | ------ |
| B1  | C 端商品**详情**未暴露（`WebProductService.getById` 已存在，Controller 未加路由）                                      | 商品详情页     | **P0** |
| B2  | 商品管理缺 `create`、管理端分页列表                                                                                    | 管理端商品新增 | **P0** |
| B3  | 秒杀**活动管理**（活动 CRUD + 添加/移除秒杀商品）web 未暴露（业务侧 `SeckillController/SeckillGoodController` 已齐全） | 管理端秒杀配置 | **P0** |
| B4  | 管理端登录端点未单独暴露（可复用 `login?loginType=admin`）                                                             | 管理端登录     | **P0** |
| B5  | BFF 的 `/admin/**` 接口未做角色校验                                                                                    | 管理端越权     | **P0** |
| B6  | 品牌管理 CRUD 未暴露（业务侧 `BrandApi` 已齐全）                                                                       | 管理端品牌管理 | P1     |
| B7  | 订单管理端（全部订单分页 + 发货）业务侧无接口                                                                          | 管理端订单处理 | P1     |
| B8  | 会员列表（分页）业务侧无接口                                                                                           | 管理端会员管理 | P1     |
| B9  | 数据看板（GMV/订单量）无聚合接口                                                                                       | 管理端看板     | P2     |

---

## 三、技术选型与工程结构

### 3.1 技术栈（两端统一）

- **Vue 3 + TypeScript + Vite**
- **Pinia**（状态） + **Vue Router**（路由/守卫） + **Axios**（请求封装）
- **Element Plus**（两端共用 UI，中文文档全、组件齐全）
- **ECharts**（仅管理端数据看板）
- 包管理：npm/pnpm（二选一，推荐 pnpm）

### 3.2 工程结构（独立目录，与后端平级或单仓 `frontend/`）

```
frontend/
├── user-web/                  # 用户端
│   ├── src/
│   │   ├── api/               # 接口层（对应 BFF 各域）
│   │   ├── assets/            # 静态图、样式变量
│   │   ├── components/        # 通用组件（骨架屏、倒计时、空态）
│   │   ├── layouts/           # 布局（顶部导航 + 内容）
│   │   ├── router/            # 路由 + 守卫
│   │   ├── stores/            # Pinia（user、cart、seckill）
│   │   ├── styles/            # 主题变量、动效
│   │   ├── utils/request.ts   # Axios 封装
│   │   └── views/             # 页面
│   ├── .env.development       # VITE_API_BASE=/app/api（Vite proxy → 8888）
│   └── vite.config.ts
└── admin-web/                 # 管理端
    └── src/
        ├── api/  layouts/  router/  stores/  utils/  components/  views/
        ├── directives/        # 按钮级权限 v-permission
        └── ...
```

### 3.3 本地联调

- 开发环境：Vite `server.proxy` 将 `/app/api` 代理到 `http://localhost:8888`，规避跨域（网关已有 CORS，也可直连）。
- 生产：Nginx 托管静态资源，`/app/api` 反代到网关 8888。

---

## 四、全局约定

1. **请求封装**（`utils/request.ts`）
   - 自动携带 `Authorization: Bearer <accessToken>`（localStorage）。
   - 自动生成并透传 `X-Trace-Id`（复用后端 TraceId 链路）。
   - 统一解包 `ApiResult`：`code` 为空/`0`/`200` 视为成功返回 `data`；否则 `ElMessage.error(message)` 并 reject。
   - 401：清除 token 跳登录页；4004/4005 秒杀专属码给出对应文案。
2. **登录态**
   - C 端：accessToken + refreshToken 双 token；拦截器对 401 自动 refresh 一次后重放。
   - 管理端：独立 token（`loginType=admin`），与 C 端隔离。
3. **图片**：商品图/轮播图优先用后端 `pic`/`logo` 字段；缺省时用 `text_to_image` 生成或本地占位图（`image_size` 按场景取 `square`/`landscape_16_9`）。
4. **秒杀交互**：`SeckillGoodVO` 携带 `status`（NOT_STARTED/IN_PROGRESS/ENDED）与 `countdownSec`，前端据此渲染倒计时/立即抢购/已结束；抢购返回 `PENDING` 后进入轮询 `result` 直至 `SUCCESS`。

---

## 五、用户端实施计划（舒适 · 灵动 · 自然）

### 5.1 设计规范

- **视觉**：柔和主色（暖橙/清新绿为主，低饱和），卡片式布局，圆角 + 轻投影，充足留白。
- **动效**：页面切换淡入淡出、列表骨架屏、商品卡 hover 浮起、价格/倒计时数字动画；克制不喧宾。
- **交互**：响应式（桌面 + 移动端自适应）、滚动加载、友好空态与错误态、秒杀进度条与倒计时实时更新。
- **自然**：文案口语化、操作反馈即时（Toast/局部 loading）、表单即时校验。

### 5.2 页面清单与路由

| 路由                 | 页面      | 核心内容                           |
| -------------------- | --------- | ---------------------------------- |
| `/`                  | 首页      | 轮播、品牌、热销商品、秒杀入口     |
| `/product`           | 商品列表  | 分类树筛选、关键词、价格区间、分页 |
| `/product/:id`       | 商品详情  | 大图、价格、库存、加购、详情图     |
| `/seckill`           | 秒杀会场  | 进行中/即将开始倒计时、抢购按钮    |
| `/cart`              | 购物车    | 列表、数量加减、结算               |
| `/checkout`          | 确认下单  | 收货地址、备注、提交               |
| `/order`             | 我的订单  | 状态 Tab、取消/确认收货            |
| `/order/:id`         | 订单详情  | 明细、状态、收货信息               |
| `/login` `/register` | 登录/注册 | 表单 + 校验                        |
| `/user`              | 个人中心  | 资料、地址管理、改密               |

### 5.3 接口映射（BFF → 页面）

| 功能      | 接口                                                             |
| --------- | ---------------------------------------------------------------- |
| 登录/注册 | `POST /app/api/auth/login` `/register`                           |
| 首页品牌  | `GET /app/api/brand/list`                                        |
| 商品列表  | `GET /app/api/product/list`                                      |
| 分类树    | `GET /app/api/product/tree`                                      |
| 商品详情  | `GET /app/api/product/{id}`（**B1 补**）                         |
| 秒杀列表  | `GET /app/api/seckill/list`                                      |
| 抢购      | `POST /app/api/seckill/order?seckillGoodId=`                     |
| 结果轮询  | `GET /app/api/seckill/order/{orderNo}/result`                    |
| 购物车    | `GET/POST/PUT/DELETE /app/api/cart/**`                           |
| 下单      | `POST /app/api/order/submit`                                     |
| 我的订单  | `GET /app/api/order/page` `/{id}` `/{id}/cancel` `/{id}/confirm` |
| 个人资料  | `GET/PUT /app/api/member/profile`                                |
| 收货地址  | `/app/api/member/address/**`                                     |

### 5.4 分阶段任务

- **U1**：工程初始化（Vite+TS+Element Plus+Pinia+Router+Axios 封装+主题变量+代理）。
- **U2**：布局与通用组件（导航、骨架屏、空态、商品卡、倒计时）。
- **U3**：认证（登录/注册/刷新/登出 + 路由守卫）。
- **U4**：首页 + 商品列表/详情（依赖 B1）。
- **U5**：购物车 + 下单 + 我的订单。
- **U6**：秒杀会场 + 抢购 + 结果轮询。
- **U7**：个人中心（资料/地址/改密） + 响应式与动效打磨。

---

## 六、管理端实施计划（标准 · 设计 · 便捷 · 合理）

### 6.1 设计规范

- **标准**：经典后台布局（侧边菜单 + 顶栏 + 面包屑 + 内容区），遵循 Element Plus 官方规范与中后台最佳实践。
- **设计**：统一间距/字号/颜色 Token，统一表格（斑马纹、列宽、溢出省略）、表单（栅格对齐、必填标识）、按钮（主次/危险语义）。
- **便捷**：列表页标准「筛选区 + 操作区 + 分页表格」，详情抽屉/弹窗复用，批量操作，刷新/重置，快捷键（回车搜索）。
- **合理**：角色权限（菜单级 + 按钮级 `v-permission`），危险操作二次确认，删除软提示，表单严格校验。

### 6.2 页面清单与路由

| 路由                          | 页面     | 核心内容                       |
| ----------------------------- | -------- | ------------------------------ |
| `/login`                      | 登录     | `loginType=admin`              |
| `/dashboard`                  | 数据看板 | GMV/订单量/商品销量（P2）      |
| `/product`                    | 商品管理 | 列表、新增、编辑、上下架、删除 |
| `/brand`                      | 品牌管理 | CRUD 列表                      |
| `/seckill/activity`           | 秒杀活动 | 活动 CRUD、启停、时间窗        |
| `/seckill/activity/:id/goods` | 秒杀商品 | 添加/移除秒杀商品              |
| `/order`                      | 订单管理 | 全部订单、发货                 |
| `/member`                     | 会员管理 | 会员列表/详情                  |

### 6.3 接口映射（BFF → 页面）

| 功能                 | 接口                                                            | 后端      |
| -------------------- | --------------------------------------------------------------- | --------- |
| 登录                 | `POST /app/api/auth/login`（`loginType=admin`）                 | 就绪      |
| 商品列表/详情        | `GET /app/api/product/list` `GET /app/api/product/admin/{id}`   | 就绪      |
| 商品新增             | `POST /app/api/product/admin`                                   | **B2 补** |
| 商品编辑/上下架/删除 | `PUT /app/api/product/admin/{id}` `/{id}/status` `DELETE /{id}` | 就绪      |
| 品牌 CRUD            | `/app/api/brand/admin/**`                                       | **B6 补** |
| 秒杀活动 CRUD/分页   | `/app/api/seckill/admin/activity/**`                            | **B3 补** |
| 添加/移除秒杀商品    | `/app/api/seckill/admin/activity/{id}/goods`                    | **B3 补** |
| 订单列表/发货        | `/app/api/order/admin/**`                                       | **B7 补** |
| 会员列表             | `/app/api/member/admin/list`                                    | **B8 补** |

### 6.4 分阶段任务

- **A1**：工程初始化（同 U1 + ECharts + 权限指令）。
- **A2**：布局与权限框架（侧边菜单/面包屑/路由守卫/`v-permission`）。
- **A3**：登录 + 管理员信息。
- **A4**：商品管理（列表/新增/编辑/上下架/删除，依赖 B2）。
- **A5**：秒杀活动管理（活动 CRUD + 秒杀商品配置，依赖 B3）。
- **A6**：品牌管理 + 订单管理 + 会员管理（依赖 B6/B7/B8）。
- **A7**：数据看板 + 交互打磨。

---

## 七、后端配套改动清单（web 层 + 少量业务侧）

> 原则：业务服务契约（spi）与实现基本不动，主要补 **web(BFF) 聚合暴露**；个别缺口（B7/B8）需业务侧新增接口。

### P0（演示核心，必做）

- **B1**：`WebProductController` 增加 `GET /app/api/product/{id}`（转发 `WebProductService.getById`）；网关白名单增加 `/app/api/product/*`。
- **B2**：`WebProductAdminController` 增加 `POST /app/api/product/admin`（新建商品）；`WebProductService` 补 `create`。
- **B3**：新增 `WebSeckillAdminController`（`/app/api/seckill/admin`）暴露活动 CRUD/分页 + 添加/移除秒杀商品；web 增加 `SeckillActivityFeignClient extends SeckillActivityApi`（Feign 单继承，复用 `SeckillGoodFeignClient`）。
- **B4**：管理端登录——复用 `POST /app/api/auth/login`（body `loginType=admin`），如需语义化可加 `/app/api/auth/admin/login` 转发。
- **B5**：BFF 层对 `/app/api/**/admin/**` 增加角色校验（读取网关透传的 `X-User-Roles`，含 `admin` 才放行，否则 403）；或在网关 `JwtAuthFilter` 对 admin 路径做角色断言。

### P1（完整管理端，建议做）

- **B6**：新增 `WebBrandAdminController` + `BrandFeignClient` 扩展，暴露品牌 CRUD（业务侧 `BrandApi` 已齐全）。
- **B7**：`trade-api` 增加管理端订单接口（全部订单分页、发货），同步扩展 `OrderApi` 或新增 `OrderAdminApi` 契约，web 暴露。
- **B8**：`member-api` 增加会员分页列表接口（`MemberQueryRequest` 已预留），同步扩展契约，web 暴露。

### P2（可选）

- **B9**：数据看板聚合接口（可先前端 mock 或基于现有订单/商品接口聚合）。

---

## 八、实施顺序（里程碑）

1. **里程碑 M1（地基）**：后端 P0 补齐（B1~B5）→ 两端工程初始化（U1/U2 + A1/A2）→ 联调打通。
2. **里程碑 M2（C 端闭环）**：U3~U6，完成「登录→浏览→加购→下单→订单→秒杀抢购」。
3. **里程碑 M3（管理端闭环）**：A3~A5 + 后端 P1（B6~B8），完成「商品/品牌/秒杀活动/订单/会员」管理。
4. **里程碑 M4（打磨）**：U7 + A6/A7，响应式、动效、看板、权限细节。

> 依赖关系：M2 依赖 M1；M3 依赖 M1 的 B1~B5 与 M3 内后端 P1；M4 依赖前三者。

---

## 九、演示脚本（面试）

1. **管理端准备**：登录后台 → 新建/上架商品 → 配置秒杀活动并添加秒杀商品（预热由 `StockPreheatJob` 完成）。
2. **用户端体验**：注册/登录 → 首页浏览 → 商品详情 → 加购 → 下单 → 查看订单。
3. **秒杀亮点**：进入秒杀会场 → 倒计时归零 → 点击抢购 → 轮询显示「已抢到」→ 再次抢购被 4005 拦截；演示 Lua 防超卖与 MQ 异步建单。
4. **管理端收尾**：订单列表查看刚产生的订单 → 发货 → 数据看板展示。
5. **技术点口述**：网关鉴权与 TraceId 全链路、BFF 聚合、Feign 契约复用、Redis Lua + MQ 异步、RabbitMQ 手动 ack 与补偿。

---

## 十、验收标准

- **用户端**：主链路可走通；秒杀倒计时/抢购/轮询正常；移动端与桌面端均可正常浏览；无阻塞性报错。
- **管理端**：登录后可完成商品/品牌/秒杀活动/订单全量管理操作；越权访问 admin 接口返回 403；交互流畅、校验到位。
- **工程**：两端 `npm run build` 通过；接口层类型与后端 VO 对齐；统一请求/鉴权/错误处理无遗漏。
