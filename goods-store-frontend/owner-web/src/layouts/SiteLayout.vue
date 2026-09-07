<template>
  <div class="site">
    <!-- 顶部导航 -->
    <header class="site-header">
      <div class="header-inner content-wrap">
        <router-link to="/" class="logo">
          <span class="logo-mark">
            <el-icon :size="18" color="#fff"><Cpu /></el-icon>
          </span>
          <span class="logo-text">丰峦智选</span>
        </router-link>

        <nav class="nav">
          <router-link
            to="/"
            class="nav-link"
            :class="{ active: route.path === '/' }"
            >首页</router-link
          >
          <router-link
            to="/product"
            class="nav-link"
            :class="{ active: route.path.startsWith('/product') }"
            >全部商品</router-link
          >
          <router-link
            to="/seckill"
            class="nav-link seckill-link"
            :class="{ active: route.path === '/seckill' }"
          >
            <el-icon><AlarmClock /></el-icon> 限时秒杀
          </router-link>
        </nav>

        <div class="header-right">
          <router-link to="/cart" class="cart-entry">
            <el-badge
              :value="cartStore.count"
              :hidden="!cartStore.count"
              :max="99"
            >
              <el-icon :size="22"><ShoppingCart /></el-icon>
            </el-badge>
            <span class="cart-text">购物车</span>
          </router-link>

          <template v-if="userStore.isLogin">
            <el-dropdown trigger="click" @command="onUserCommand">
              <span class="user-entry">
                <el-avatar :size="32" class="user-avatar">
                  {{ userStore.displayName.slice(0, 1).toUpperCase() }}
                </el-avatar>
                <span class="user-name">{{ userStore.displayName }}</span>
                <el-icon class="arrow"><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="user">
                    <el-icon><User /></el-icon>个人中心
                  </el-dropdown-item>
                  <el-dropdown-item command="order">
                    <el-icon><List /></el-icon>我的订单
                  </el-dropdown-item>
                  <el-dropdown-item divided command="logout">
                    <el-icon><SwitchButton /></el-icon>退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <router-link to="/login" class="login-btn">登录</router-link>
            <router-link to="/register" class="register-btn">注册</router-link>
          </template>
        </div>
      </div>
    </header>

    <!-- 内容区（页面切换淡入淡出） -->
    <main class="site-main">
      <router-view v-slot="{ Component }">
        <transition name="page-fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <!-- 底部 -->
    <footer class="site-footer">
      <div class="content-wrap footer-inner">
        <div class="footer-brand">
          <el-icon :size="20" color="#60a5fa"><Cpu /></el-icon>
          丰峦智选
        </div>
        <p class="footer-slogan">正品数码 · 原厂质保 · 极速送达</p>
        <p class="footer-copy">
          © 2026 丰峦智选 · Spring Cloud 微服务商城演示项目
        </p>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { useUserStore } from "@/stores/user";
import { useCartStore } from "@/stores/cart";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const cartStore = useCartStore();

onMounted(() => {
  // 已登录时拉取购物车数量，导航栏展示徽标
  if (userStore.isLogin) {
    cartStore.refresh();
  }
});

async function onUserCommand(command: string) {
  if (command === "logout") {
    await userStore.logout();
    cartStore.clear();
    ElMessage.success("已退出登录，欢迎再来逛逛～");
    router.push("/");
  } else if (command === "user") {
    router.push("/user");
  } else if (command === "order") {
    router.push("/order");
  }
}
</script>

<style scoped>
.site {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

/* ---------- 顶部 ---------- */
.site-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(16px) saturate(1.4);
  -webkit-backdrop-filter: blur(16px) saturate(1.4);
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
}

.header-inner {
  display: flex;
  align-items: center;
  gap: 32px;
  height: 64px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

/* 芯片形 logo 徽标（方形锐角，精密仪器感） */
.logo-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  background: var(--primary-gradient);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.35);
  transition: transform 0.3s var(--ease);
}

.logo:hover .logo-mark {
  transform: translateY(-1px);
}

.logo-text {
  font-size: 20px;
  font-weight: 800;
  color: var(--text-main);
  letter-spacing: 1px;
}

.nav {
  display: flex;
  gap: 4px;
  flex: 1;
  align-self: stretch;
}

/* 底部指示线导航（精密直角风格） */
.nav-link {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 0 18px;
  font-size: 15px;
  color: var(--text-sub);
  transition: color 0.25s var(--ease);
}

.nav-link::after {
  content: "";
  position: absolute;
  left: 18px;
  right: 18px;
  bottom: 0;
  height: 2px;
  background: var(--primary);
  transform: scaleX(0);
  transition: transform 0.3s var(--ease);
}

.nav-link:hover {
  color: var(--primary);
}

.nav-link.active {
  color: var(--primary);
  font-weight: 600;
}

.nav-link.active::after {
  transform: scaleX(1);
}

.seckill-link {
  color: var(--accent-hot);
}

.seckill-link::after {
  background: var(--accent-hot);
}

.seckill-link:hover,
.seckill-link.active {
  color: var(--accent-hover);
}

/* ---------- 右侧 ---------- */
.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-shrink: 0;
}

.cart-entry {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-main);
  padding: 6px 10px;
  border-radius: var(--radius-sm);
  transition: background 0.25s var(--ease);
}

.cart-entry:hover {
  background: var(--primary-light);
}

.cart-text {
  font-size: 14px;
}

.login-btn,
.register-btn {
  font-size: 14px;
  padding: 7px 18px;
  border-radius: var(--radius-sm);
  transition: all 0.25s var(--ease);
}

.login-btn {
  color: var(--primary);
  border: 1px solid rgba(37, 99, 235, 0.4);
}

.login-btn:hover {
  background: var(--primary-light);
  border-color: var(--primary);
}

.register-btn {
  color: #fff;
  background: var(--primary-gradient);
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.35);
}

.register-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(37, 99, 235, 0.45);
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.user-avatar {
  background: var(--primary-gradient);
  color: #fff;
  font-weight: 700;
}

.user-name {
  font-size: 14px;
  max-width: 96px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.arrow {
  font-size: 12px;
  color: var(--text-light);
}

/* ---------- 内容 ---------- */
.site-main {
  flex: 1;
}

/* ---------- 底部（深蓝夜空） ---------- */
.site-footer {
  margin-top: 64px;
  padding: 40px 0;
  background: linear-gradient(180deg, #0b1e4b 0%, #0f172a 100%);
  color: #cbd5e1;
}

.footer-inner {
  text-align: center;
}

.footer-brand {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 16px;
  font-weight: 700;
  color: #f1f5f9;
}

.footer-slogan {
  margin-top: 10px;
  font-size: 13px;
  color: #94a3b8;
  letter-spacing: 2px;
}

.footer-copy {
  margin-top: 6px;
  font-size: 12px;
  color: #64748b;
}

@media (max-width: 768px) {
  .header-inner {
    gap: 12px;
  }

  .nav-link {
    padding: 6px 10px;
    font-size: 13px;
  }

  .cart-text,
  .user-name {
    display: none;
  }
}
</style>
