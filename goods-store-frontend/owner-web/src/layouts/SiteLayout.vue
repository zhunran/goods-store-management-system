<template>
  <div class="site">
    <!-- 顶部导航 -->
    <header class="site-header">
      <div class="header-inner content-wrap">
        <router-link to="/" class="logo">
          <el-icon :size="28" color="var(--primary)"><ShoppingBag /></el-icon>
          <span class="logo-text">丰峦优选</span>
        </router-link>

        <nav class="nav">
          <router-link to="/" class="nav-link" :class="{ active: route.path === '/' }">首页</router-link>
          <router-link to="/product" class="nav-link" :class="{ active: route.path.startsWith('/product') }">全部商品</router-link>
          <router-link to="/seckill" class="nav-link seckill-link" :class="{ active: route.path === '/seckill' }">
            <el-icon><AlarmClock /></el-icon> 限时秒杀
          </router-link>
        </nav>

        <div class="header-right">
          <router-link to="/cart" class="cart-entry">
            <el-badge :value="cartStore.count" :hidden="!cartStore.count" :max="99">
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
          <el-icon :size="20" color="var(--primary)"><ShoppingBag /></el-icon>
          丰峦优选
        </div>
        <p class="footer-slogan">好物不贵，优选生活每一件</p>
        <p class="footer-copy">© 2026 丰峦优选 · Spring Cloud 微服务商城演示项目</p>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()

onMounted(() => {
  // 已登录时拉取购物车数量，导航栏展示徽标
  if (userStore.isLogin) {
    cartStore.refresh()
  }
})

async function onUserCommand(command: string) {
  if (command === 'logout') {
    await userStore.logout()
    cartStore.clear()
    ElMessage.success('已退出登录，欢迎再来逛逛～')
    router.push('/')
  } else if (command === 'user') {
    router.push('/user')
  } else if (command === 'order') {
    router.push('/order')
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
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(61, 58, 56, 0.06);
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
  gap: 8px;
  flex-shrink: 0;
}

.logo-text {
  font-size: 20px;
  font-weight: 800;
  background: var(--primary-gradient);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  letter-spacing: 1px;
}

.nav {
  display: flex;
  gap: 6px;
  flex: 1;
}

.nav-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 8px 16px;
  border-radius: 999px;
  font-size: 15px;
  color: var(--text-sub);
  transition: color 0.2s, background 0.2s;
}

.nav-link:hover {
  color: var(--primary);
  background: var(--primary-light);
}

.nav-link.active {
  color: var(--primary);
  background: var(--primary-light);
  font-weight: 600;
}

.seckill-link {
  color: var(--accent-green);
}

.seckill-link:hover,
.seckill-link.active {
  color: #0da271;
  background: var(--accent-green-light);
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
  transition: background 0.2s;
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
  border-radius: 999px;
  transition: all 0.2s;
}

.login-btn {
  color: var(--primary);
  border: 1px solid var(--primary);
}

.login-btn:hover {
  background: var(--primary-light);
}

.register-btn {
  color: #fff;
  background: var(--primary-gradient);
  box-shadow: 0 4px 12px rgba(249, 115, 22, 0.3);
}

.register-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(249, 115, 22, 0.4);
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

/* ---------- 底部 ---------- */
.site-footer {
  margin-top: 48px;
  padding: 32px 0;
  background: #fff;
  border-top: 1px solid rgba(61, 58, 56, 0.06);
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
}

.footer-slogan {
  margin-top: 8px;
  font-size: 13px;
  color: var(--text-sub);
}

.footer-copy {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-light);
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
