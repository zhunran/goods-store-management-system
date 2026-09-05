import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/register/index.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/',
    component: () => import('@/layouts/SiteLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/home/index.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'product',
        name: 'ProductList',
        component: () => import('@/views/product/list.vue'),
        meta: { title: '全部商品' }
      },
      {
        path: 'product/:id',
        name: 'ProductDetail',
        component: () => import('@/views/product/detail.vue'),
        meta: { title: '商品详情' }
      },
      {
        path: 'seckill',
        name: 'Seckill',
        component: () => import('@/views/seckill/index.vue'),
        meta: { title: '限时秒杀', requiresAuth: true }
      },
      {
        path: 'cart',
        name: 'Cart',
        component: () => import('@/views/cart/index.vue'),
        meta: { title: '购物车', requiresAuth: true }
      },
      {
        path: 'checkout',
        name: 'Checkout',
        component: () => import('@/views/checkout/index.vue'),
        meta: { title: '确认下单', requiresAuth: true }
      },
      {
        path: 'order',
        name: 'OrderList',
        component: () => import('@/views/order/list.vue'),
        meta: { title: '我的订单', requiresAuth: true }
      },
      {
        path: 'order/:id',
        name: 'OrderDetail',
        component: () => import('@/views/order/detail.vue'),
        meta: { title: '订单详情', requiresAuth: true }
      },
      {
        path: 'user',
        name: 'UserProfile',
        component: () => import('@/views/user/index.vue'),
        meta: { title: '个人中心', requiresAuth: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  const token = localStorage.getItem('user_access_token')
  if (to.meta.requiresAuth && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if ((to.path === '/login' || to.path === '/register') && token) {
    return '/'
  }
  document.title = to.meta.title ? `${to.meta.title} · 丰峦优选` : '丰峦优选'
  return true
})

export default router
