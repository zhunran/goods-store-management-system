import {
  createRouter,
  createWebHistory,
  type RouteRecordRaw,
} from "vue-router";
import { useUserStore } from "@/stores/user";

const routes: RouteRecordRaw[] = [
  {
    path: "/login",
    name: "Login",
    component: () => import("@/views/login/index.vue"),
    meta: { title: "登录" },
  },
  {
    path: "/",
    component: () => import("@/layouts/AdminLayout.vue"),
    redirect: "/dashboard",
    children: [
      {
        path: "dashboard",
        name: "Dashboard",
        component: () => import("@/views/dashboard/index.vue"),
        meta: {
          title: "数据看板",
          icon: "Odometer",
          permission: "dashboard:view",
        },
      },
      {
        path: "product",
        name: "Product",
        component: () => import("@/views/product/index.vue"),
        meta: { title: "商品管理", icon: "Goods", permission: "good:list" },
      },
      {
        path: "brand",
        name: "Brand",
        component: () => import("@/views/brand/index.vue"),
        meta: { title: "品牌管理", icon: "Flag", permission: "brand:list" },
      },
      {
        path: "seckill",
        name: "Seckill",
        component: () => import("@/views/seckill/index.vue"),
        meta: { title: "秒杀活动", icon: "Timer", permission: "seckill:list" },
      },
      {
        path: "order",
        name: "Order",
        component: () => import("@/views/order/index.vue"),
        meta: { title: "订单管理", icon: "List", permission: "order:list" },
      },
      {
        path: "member",
        name: "Member",
        component: () => import("@/views/member/index.vue"),
        meta: { title: "会员管理", icon: "User", permission: "member:list" },
      },
      {
        path: "role",
        name: "Role",
        component: () => import("@/views/role/index.vue"),
        meta: { title: "角色管理", icon: "Setting", permission: "role:manage" },
      },
    ],
  },
  {
    path: "/:pathMatch(.*)*",
    redirect: "/",
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to) => {
  const token = localStorage.getItem("admin_access_token");
  if (to.path !== "/login" && !token) {
    return { path: "/login", query: { redirect: to.fullPath } };
  }
  if (to.path === "/login" && token) {
    return "/dashboard";
  }
  const userStore = useUserStore();
  const permission = to.meta.permission as string | undefined;
  if (permission && !userStore.hasPermission(permission)) {
    return "/dashboard";
  }
  document.title = to.meta.title
    ? `${to.meta.title} · 商城管理后台`
    : "商城管理后台";
  return true;
});

export default router;
