<template>
  <div class="home-page">
    <!-- Hero 横幅 -->
    <section class="hero">
      <div class="content-wrap hero-inner">
        <div class="hero-text">
          <h1 class="hero-title">好物不贵<br />优选生活每一件</h1>
          <p class="hero-sub">严选品质好货，温和的价格，舒适的日子</p>
          <div class="hero-actions">
            <el-button class="btn-primary" size="large" @click="$router.push('/product')">
              逛逛全部好物 <el-icon class="btn-icon"><ArrowRight /></el-icon>
            </el-button>
            <el-button size="large" class="btn-ghost" @click="$router.push('/seckill')">
              限时秒杀
            </el-button>
          </div>
        </div>
        <div class="hero-deco">
          <div class="circle c1"></div>
          <div class="circle c2"></div>
          <div class="float-card fc1 card">
            <el-icon :size="22" color="#f97316"><Goods /></el-icon>
            <span>品质严选</span>
          </div>
          <div class="float-card fc2 card">
            <el-icon :size="22" color="#10b981"><Timer /></el-icon>
            <span>限时秒杀</span>
          </div>
          <div class="float-card fc3 card">
            <el-icon :size="22" color="#f59e0b"><Present /></el-icon>
            <span>售后无忧</span>
          </div>
        </div>
      </div>
    </section>

    <div class="content-wrap body-wrap">
      <!-- 品牌街 -->
      <section class="section">
        <div class="section-head">
          <h2 class="section-title">品牌街</h2>
          <p class="section-sub">值得信赖的好品牌</p>
        </div>
        <div v-if="loading" class="brand-skeleton">
          <div v-for="i in 6" :key="i" class="skeleton-card card">
            <div class="skeleton-line" style="width: 48px; height: 48px; border-radius: 12px"></div>
            <div class="skeleton-line" style="width: 60%; margin-top: 12px"></div>
          </div>
        </div>
        <div v-else class="brand-row">
          <div
            v-for="b in brands"
            :key="b.id"
            class="brand-item card card-hover"
            @click="goBrand(b)"
          >
            <el-image :src="b.logo" fit="cover" class="brand-logo">
              <template #error>
                <div class="img-fallback brand-logo-fallback">
                  <el-icon :size="20"><Shop /></el-icon>
                </div>
              </template>
            </el-image>
            <p class="brand-name" :title="b.name">{{ b.name }}</p>
          </div>
          <div v-if="!brands.length && !loading" class="empty-tip">暂无品牌入驻</div>
        </div>
      </section>

      <!-- 限时秒杀 -->
      <section class="section">
        <div class="section-head">
          <h2 class="section-title green">限时秒杀</h2>
          <p class="section-sub">手快有，手慢无</p>
          <el-button
            link
            type="primary"
            class="section-more"
            @click="$router.push('/seckill')"
          >
            进入会场 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>

        <!-- 已登录：秒杀商品预览 -->
        <template v-if="userStore.isLogin">
          <div v-if="loading" class="seckill-skeleton">
            <div v-for="i in 4" :key="i" class="skeleton-card card">
              <div class="skeleton-line" style="aspect-ratio: 1; border-radius: 10px"></div>
              <div class="skeleton-line" style="margin-top: 12px"></div>
              <div class="skeleton-line" style="width: 45%; margin-top: 8px"></div>
            </div>
          </div>
          <div v-else-if="seckillGoods.length" class="seckill-row">
            <div
              v-for="s in seckillGoods"
              :key="s.id"
              class="seckill-item card card-hover"
              @click="$router.push('/seckill')"
            >
              <div class="seckill-pic-wrap">
                <el-image :src="s.goodPic" fit="cover" class="seckill-pic" lazy>
                  <template #error>
                    <div class="img-fallback"><el-icon :size="24"><Picture /></el-icon></div>
                  </template>
                </el-image>
                <span class="seckill-status" :class="statusClass(s.status)">{{
                  statusText(s.status)
                }}</span>
              </div>
              <p class="seckill-name" :title="s.goodName">{{ s.goodName }}</p>
              <div class="seckill-bottom">
                <span class="price">¥{{ s.originalPrice }}</span>
                <CountDown
                  v-if="s.status !== 'ENDED'"
                  :seconds="s.countdownSec"
                  small
                  @finish="loadSeckill"
                />
                <span v-else class="ended-text">已结束</span>
              </div>
            </div>
          </div>
          <div v-else class="empty-tip card">当前没有进行中的秒杀活动，去看看别的吧～</div>
        </template>

        <!-- 未登录：引导登录 -->
        <div v-else class="seckill-login-guide card" @click="goLogin">
          <el-icon :size="28" color="#10b981"><Lock /></el-icon>
          <p>登录后即可查看专属秒杀会场</p>
          <el-button type="primary" round size="small" class="btn-primary">立即登录</el-button>
        </div>
      </section>

      <!-- 热销好物 -->
      <section class="section">
        <div class="section-head">
          <h2 class="section-title">热销好物</h2>
          <p class="section-sub">大家都在买</p>
          <el-button link type="primary" class="section-more" @click="$router.push('/product')">
            查看全部 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
        <div v-if="loading" class="good-skeleton">
          <div v-for="i in 8" :key="i" class="skeleton-card card">
            <div class="skeleton-line" style="aspect-ratio: 1; border-radius: 10px"></div>
            <div class="skeleton-line" style="margin-top: 12px"></div>
            <div class="skeleton-line" style="width: 60%; margin-top: 8px"></div>
            <div class="skeleton-line" style="width: 40%; margin-top: 10px"></div>
          </div>
        </div>
        <div v-else class="good-grid">
          <GoodCard v-for="g in hotGoods" :key="g.id" :good="g" />
        </div>
        <div v-if="!hotGoods.length && !loading" class="empty-tip">暂无热销商品</div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import GoodCard from '@/components/GoodCard.vue'
import CountDown from '@/components/CountDown.vue'
import { brandList, goodList } from '@/api/product'
import { seckillList } from '@/api/seckill'
import type { BrandVO, GoodVO, SeckillGoodVO } from '@/api/types'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const brands = ref<BrandVO[]>([])
const hotGoods = ref<GoodVO[]>([])
const seckillGoods = ref<SeckillGoodVO[]>([])

async function loadSeckill() {
  if (!userStore.isLogin) return
  try {
    const list = await seckillList()
    // 进行中优先，其次即将开始
    const order: Record<string, number> = { IN_PROGRESS: 0, NOT_STARTED: 1, ENDED: 2 }
    seckillGoods.value = [...list]
      .sort((a, b) => (order[a.status] ?? 9) - (order[b.status] ?? 9))
      .slice(0, 4)
  } catch {
    seckillGoods.value = []
  }
}

function goBrand(b: BrandVO) {
  router.push({ path: '/product', query: { brandId: b.id } })
}

function goLogin() {
  router.push({ path: '/login', query: { redirect: '/' } })
}

function statusText(status: string) {
  if (status === 'IN_PROGRESS') return '进行中'
  if (status === 'NOT_STARTED') return '即将开始'
  return '已结束'
}

function statusClass(status: string) {
  return {
    IN_PROGRESS: 'in-progress',
    NOT_STARTED: 'not-started',
    ENDED: 'ended',
  }[status] ?? 'ended'
}

onMounted(async () => {
  try {
    const [brandRes, goodRes] = await Promise.all([
      brandList(),
      goodList({ isHot: true, pageNum: 1, pageSize: 8 }),
    ])
    brands.value = brandRes
    hotGoods.value = goodRes
    loadSeckill()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
/* ---------- Hero ---------- */
.hero {
  background: linear-gradient(160deg, #fff4e8 0%, #fdf1e0 45%, #ecfdf5 100%);
  overflow: hidden;
}

.hero-inner {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 320px;
  padding-top: 48px;
  padding-bottom: 48px;
}

.hero-title {
  font-size: 40px;
  line-height: 1.3;
  font-weight: 700;
  letter-spacing: 1px;
}

.hero-sub {
  margin-top: 14px;
  font-size: 15px;
  color: var(--text-sub);
}

.hero-actions {
  margin-top: 28px;
  display: flex;
  gap: 12px;
}

.btn-primary {
  background: var(--primary-gradient);
  border: none;
}

.btn-ghost {
  border-color: var(--accent-green);
  color: var(--accent-green);
  background: rgba(255, 255, 255, 0.6);
}

.btn-ghost:hover {
  background: var(--accent-green-light);
}

.btn-icon {
  margin-left: 4px;
  transition: transform 0.2s;
}

.btn-primary:hover .btn-icon {
  transform: translateX(3px);
}

/* Hero 浮动装饰 */
.hero-deco {
  position: relative;
  flex: 1;
  height: 240px;
  margin-left: 40px;
}

.circle {
  position: absolute;
  border-radius: 50%;
  filter: blur(0.5px);
  opacity: 0.75;
}

.c1 {
  width: 220px;
  height: 220px;
  right: 40px;
  top: 10px;
  background: radial-gradient(circle at 30% 30%, #ffd9b3, #f9731644);
  animation: floaty 6s ease-in-out infinite;
}

.c2 {
  width: 140px;
  height: 140px;
  right: 200px;
  bottom: 0;
  background: radial-gradient(circle at 30% 30%, #b7f0d8, #10b98144);
  animation: floaty 7s ease-in-out infinite reverse;
}

@keyframes floaty {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-14px);
  }
}

.float-card {
  position: absolute;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  font-size: 14px;
  font-weight: 600;
  animation: floaty 5s ease-in-out infinite;
}

.fc1 {
  left: 8%;
  top: 24px;
}

.fc2 {
  left: 30%;
  bottom: 30px;
  animation-delay: 1.2s;
}

.fc3 {
  left: 2%;
  bottom: 80px;
  animation-delay: 2.4s;
}

/* ---------- 区块 ---------- */
.body-wrap {
  padding-top: 36px;
  padding-bottom: 48px;
}

.section {
  margin-bottom: 48px;
}

.section-head {
  position: relative;
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 20px;
}

.section-title {
  font-size: 22px;
  font-weight: 700;
  position: relative;
  padding-left: 14px;
}

.section-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 5px;
  height: 18px;
  border-radius: 3px;
  background: var(--primary-gradient);
}

.section-title.green::before {
  background: linear-gradient(135deg, #34d399, #10b981);
}

.section-sub {
  font-size: 13px;
  color: var(--text-sub);
}

.section-more {
  margin-left: auto;
}

/* ---------- 品牌街 ---------- */
.brand-row {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 14px;
}

.brand-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 18px 12px 14px;
  cursor: pointer;
}

.brand-logo {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: #f5f2ee;
}

.brand-logo-fallback {
  border-radius: 12px;
}

.brand-name {
  margin-top: 10px;
  font-size: 13px;
  font-weight: 600;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ---------- 秒杀 ---------- */
.seckill-row {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 14px;
}

.seckill-item {
  padding: 10px;
  cursor: pointer;
}

.seckill-pic-wrap {
  position: relative;
  aspect-ratio: 1;
  border-radius: 10px;
  overflow: hidden;
  background: #f5f2ee;
}

.seckill-pic {
  width: 100%;
  height: 100%;
  transition: transform 0.4s ease;
}

.seckill-item:hover .seckill-pic {
  transform: scale(1.05);
}

.seckill-status {
  position: absolute;
  top: 8px;
  left: 8px;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
}

.seckill-status.in-progress {
  background: linear-gradient(135deg, #34d399, #10b981);
}

.seckill-status.not-started {
  background: linear-gradient(135deg, #fbbf24, #f59e0b);
}

.seckill-status.ended {
  background: #b8b2ac;
}

.seckill-name {
  margin-top: 10px;
  font-size: 14px;
  line-height: 1.4;
  height: 20px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.seckill-bottom {
  margin-top: 6px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.seckill-bottom .price {
  font-size: 16px;
}

.ended-text {
  font-size: 12px;
  color: var(--text-light);
}

.seckill-login-guide {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px 28px;
  cursor: pointer;
  color: var(--text-sub);
  transition: box-shadow 0.3s;
}

.seckill-login-guide:hover {
  box-shadow: var(--shadow-hover);
}

.seckill-login-guide p {
  flex: 1;
  font-size: 15px;
}

/* ---------- 商品网格 ---------- */
.good-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

/* ---------- 骨架屏 ---------- */
.brand-skeleton,
.seckill-skeleton,
.good-skeleton {
  display: grid;
  gap: 14px;
}

.brand-skeleton {
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
}

.seckill-skeleton {
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
}

.good-skeleton {
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
}

.skeleton-card {
  padding: 12px;
}

.empty-tip {
  padding: 40px;
  text-align: center;
  color: var(--text-light);
  font-size: 14px;
}

/* ---------- 响应式 ---------- */
@media (max-width: 768px) {
  .hero-deco {
    display: none;
  }

  .hero-inner {
    min-height: 240px;
  }

  .hero-title {
    font-size: 28px;
  }
}
</style>
