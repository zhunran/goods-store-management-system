<template>
  <el-container class="admin-layout">
    <el-aside width="220px" class="sidebar">
      <div class="logo">商城管理后台</div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#1f2937"
        text-color="#cbd5e1"
        active-text-color="#ffffff"
        class="menu"
      >
        <el-menu-item
          v-for="item in visibleMenuItems"
          :key="item.path"
          :index="item.path"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item>首页</el-breadcrumb-item>
          <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
        </el-breadcrumb>
        <div class="header-right">
          <el-tag type="success" effect="light">{{ roleLabel }}</el-tag>
          <el-dropdown @command="onCommand">
            <span class="user-name">
              <el-icon><UserFilled /></el-icon>
              {{ userStore.userInfo?.account }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessageBox } from "element-plus";
import { useUserStore } from "@/stores/user";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const menuItems = [
  {
    path: "/dashboard",
    title: "数据看板",
    icon: "Odometer",
    permission: "dashboard:view",
  },
  {
    path: "/product",
    title: "商品管理",
    icon: "Goods",
    permission: "good:list",
  },
  { path: "/brand", title: "品牌管理", icon: "Flag", permission: "brand:list" },
  {
    path: "/seckill",
    title: "秒杀活动",
    icon: "Timer",
    permission: "seckill:list",
  },
  { path: "/order", title: "订单管理", icon: "List", permission: "order:list" },
  {
    path: "/member",
    title: "会员管理",
    icon: "User",
    permission: "member:list",
  },
  {
    path: "/role",
    title: "角色管理",
    icon: "Setting",
    permission: "role:manage",
  },
];

const visibleMenuItems = computed(() =>
  menuItems.filter((item) => userStore.hasPermission(item.permission)),
);

const activeMenu = computed(() => route.path);
const currentTitle = computed(() => (route.meta.title as string) || "");
const roleLabel = computed(() =>
  userStore.roles.includes("ROLE_ADMIN") ? "超级管理员" : "管理员",
);

async function onCommand(cmd: string) {
  if (cmd === "logout") {
    await ElMessageBox.confirm("确定退出登录吗？", "提示", { type: "warning" });
    userStore.logout();
    router.push("/login");
  }
}
</script>

<style scoped>
.admin-layout {
  height: 100%;
}
.sidebar {
  background: #1f2937;
  display: flex;
  flex-direction: column;
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 1px;
}
.menu {
  border-right: none;
  flex: 1;
}
.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e5e7eb;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.user-name {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #374151;
}
.main {
  background: #f5f6fa;
}
</style>
