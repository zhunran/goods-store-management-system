<template>
  <div class="home-page">
    <!-- Hero 横幅（深蓝科技） -->
    <section class="hero">
      <div class="content-wrap hero-inner">
        <div class="hero-text">
          <h1 class="hero-title">
            智选数码好物<br /><span class="grad">科技触手可及</span>
          </h1>
          <p class="hero-sub">严选正品数码 · 原厂质保 · 极速送达</p>
          <div class="hero-chips">
            <span class="chip"
              ><el-icon :size="14"><CircleCheck /></el-icon>正品行货</span
            >
            <span class="chip"
              ><el-icon :size="14"><Medal /></el-icon>全国联保</span
            >
            <span class="chip"
              ><el-icon :size="14"><Van /></el-icon>顺丰包邮</span
            >
          </div>
          <div class="hero-actions">
            <el-button
              class="btn-hero"
              size="large"
              @click="$router.push('/product')"
            >
              探索全部好物 <el-icon class="btn-icon"><ArrowRight /></el-icon>
            </el-button>
            <el-button
              size="large"
              class="btn-hero-ghost"
              @click="$router.push('/seckill')"
            >
              限时秒杀
            </el-button>
          </div>
        </div>
        <div class="hero-deco">
          <div class="deco-ring r1"></div>
          <div class="deco-ring r2"></div>
          <div class="deco-core">
            <el-icon :size="36" color="#7dd3fc"><Cpu /></el-icon>
          </div>
          <div class="float-card fc1">
            <el-icon :size="20" color="#60a5fa"><Iphone /></el-icon>
            <span>大牌数码</span>
          </div>
          <div class="float-card fc2">
            <el-icon :size="20" color="#38bdf8"><Timer /></el-icon>
            <span>限时秒杀</span>
          </div>
          <div class="float-card fc3">
            <el-icon :size="20" color="#a5b4fc"><Service /></el-icon>
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
          <p class="section-sub">信赖品牌官方合作</p>
        </div>
        <div v-if="loading" class="brand-skeleton">
          <div v-for="i in 6" :key="i" class="skeleton-card card">
            <div
              class="skeleton-line"
              style="width: 48px; height: 48px; border-radius: 6px"
            ></div>
            <div
              class="skeleton-line"
              style="width: 60%; margin-top: 12px"
            ></div>
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
          <div v-if="!brands.length && !loading" class="empty-tip">
            暂无品牌入驻
          </div>
        </div>
      </section>

      <!-- 限时秒杀 -->
      <section class="section">
        <div class="section-head">
          <h2 class="section-title hot">限时秒杀</h2>
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
              <div
                class="skeleton-line"
                style="aspect-ratio: 1; border-radius: 6px"
              ></div>
              <div class="skeleton-line" style="margin-top: 12px"></div>
              <div
                class="skeleton-line"
                style="width: 45%; margin-top: 8px"
              ></div>
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
                    <div class="img-fallback">
                      <el-icon :size="24"><Picture /></el-icon>
                    </div>
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
          <div v-else class="empty-tip card">
            当前没有进行中的秒杀活动，去看看别的吧～
          </div>
        </template>

        <!-- 未登录：引导登录 -->
        <div v-else class="seckill-login-guide card" @click="goLogin">
          <el-icon :size="28" color="#2563eb"><Lock /></el-icon>
          <p>登录后即可查看专属秒杀会场</p>
          <el-button type="primary" size="small" class="btn-primary"
            >立即登录</el-button
          >
        </div>
      </section>

      <!-- 热销好物 -->
      <section class="section">
        <div class="section-head">
          <h2 class="section-title">热销好物</h2>
          <p class="section-sub">人气数码榜</p>
          <el-button
            link
            type="primary"
            class="section-more"
            @click="$router.push('/product')"
          >
            查看全部 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
        <div v-if="loading" class="good-skeleton">
          <div v-for="i in 8" :key="i" class="skeleton-card card">
            <div
              class="skeleton-line"
              style="aspect-ratio: 1; border-radius: 6px"
            ></div>
            <div class="skeleton-line" style="margin-top: 12px"></div>
            <div
              class="skeleton-line"
              style="width: 60%; margin-top: 8px"
            ></div>
            <div
              class="skeleton-line"
              style="width: 40%; margin-top: 10px"
            ></div>
          </div>
        </div>
        <div v-else class="good-grid">
          <GoodCard v-for="g in hotGoods" :key="g.id" :good="g" />
        </div>
        <div v-if="!hotGoods.length && !loading" class="empty-tip">
          暂无热销商品
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import GoodCard from "@/components/GoodCard.vue";
import CountDown from "@/components/CountDown.vue";
import { brandList, goodList } from "@/api/product";
import { seckillList } from "@/api/seckill";
import type { BrandVO, GoodVO, SeckillGoodVO } from "@/api/types";
import { useUserStore } from "@/stores/user";

const router = useRouter();
const userStore = useUserStore();

const loading = ref(true);
const brands = ref<BrandVO[]>([]);
const hotGoods = ref<GoodVO[]>([]);
const seckillGoods = ref<SeckillGoodVO[]>([]);

async function loadSeckill() {
  if (!userStore.isLogin) return;
  try {
    const list = await seckillList();
    // 进行中优先，其次即将开始
    const order: Record<string, number> = {
      IN_PROGRESS: 0,
      NOT_STARTED: 1,
      ENDED: 2,
    };
    seckillGoods.value = [...list]
      .sort((a, b) => (order[a.status] ?? 9) - (order[b.status] ?? 9))
      .slice(0, 4);
  } catch {
    seckillGoods.value = [];
  }
}

function goBrand(b: BrandVO) {
  router.push({ path: "/product", query: { brandId: b.id } });
}

function goLogin() {
  router.push({ path: "/login", query: { redirect: "/" } });
}

function statusText(status: string) {
  if (status === "IN_PROGRESS") return "进行中";
  if (status === "NOT_STARTED") return "即将开始";
  return "已结束";
}

function statusClass(status: string) {
  return (
    {
      IN_PROGRESS: "in-progress",
      NOT_STARTED: "not-started",
      ENDED: "ended",
    }[status] ?? "ended"
  );
}

onMounted(async () => {
  try {
    const [brandRes, goodRes] = await Promise.all([
      brandList(),
      goodList({ isHot: true, pageNum: 1, pageSize: 8 }),
    ]);
    brands.value = brandRes;
    hotGoods.value = goodRes;
    loadSeckill();
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
/* ---------- Hero（深蓝科技） ---------- */
.hero {
  position: relative;
  background:
    radial-gradient(
      circle at 82% 18%,
      rgba(56, 189, 248, 0.28),
      transparent 52%
    ),
    radial-gradient(
      circle at 12% 88%,
      rgba(37, 99, 235, 0.38),
      transparent 55%
    ),
    linear-gradient(135deg, #0a1633 0%, #10296b 45%, #1e4fbe 100%);
  overflow: hidden;
}

/* 细网格纹理 */
.hero::before {
  content: "";
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.045) 1px, transparent 1px);
  background-size: 44px 44px;
  -webkit-mask-image: radial-gradient(
    ellipse at 72% 40%,
    #000 30%,
    transparent 78%
  );
  mask-image: radial-gradient(ellipse at 72% 40%, #000 30%, transparent 78%);
  pointer-events: none;
}

.hero-inner {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  min-height: 340px;
  padding-top: 52px;
  padding-bottom: 52px;
}

.hero-title {
  font-size: 44px;
  line-height: 1.28;
  font-weight: 700;
  letter-spacing: 1px;
  color: #ffffff;
}

.hero-title .grad {
  background: linear-gradient(120deg, #7dd3fc 0%, #38bdf8 45%, #818cf8 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.hero-sub {
  margin-top: 14px;
  font-size: 15px;
  letter-spacing: 0.5px;
  color: rgba(226, 240, 255, 0.78);
}

.hero-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 20px;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 14px;
  border-radius: 2px;
  font-size: 13px;
  letter-spacing: 0.5px;
  color: #dbeafe;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.16);
  backdrop-filter: blur(8px);
}

.hero-actions {
  margin-top: 30px;
  display: flex;
  gap: 12px;
}

.btn-hero {
  border: none;
  border-radius: var(--radius-sm);
  font-weight: 600;
  letter-spacing: 1px;
  color: #1d4ed8;
  background: #ffffff;
  transition:
    transform 0.25s var(--ease),
    box-shadow 0.25s var(--ease),
    background-color 0.25s var(--ease);
}

.btn-hero:hover {
  color: #1e40af;
  background: #f0f6ff;
  transform: translateY(-1px);
  box-shadow: 0 12px 28px rgba(2, 10, 40, 0.4);
}

.btn-hero-ghost {
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.35);
  color: #eaf2ff;
  backdrop-filter: blur(8px);
  letter-spacing: 1px;
  transition:
    transform 0.25s var(--ease),
    background-color 0.25s var(--ease),
    border-color 0.25s var(--ease);
}

.btn-hero-ghost:hover {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.14);
  border-color: rgba(255, 255, 255, 0.55);
  transform: translateY(-1px);
}

.btn-icon {
  margin-left: 4px;
  transition: transform 0.25s var(--ease);
}

.btn-hero:hover .btn-icon {
  transform: translateX(3px);
}

/* Hero 装饰（发光圆环 + 玻璃卡片） */
.hero-deco {
  position: relative;
  flex: 1;
  height: 260px;
  margin-left: 40px;
}

.deco-ring {
  position: absolute;
  top: 50%;
  border-radius: 50%;
}

.r1 {
  width: 240px;
  height: 240px;
  right: 16px;
  border: 1.5px dashed rgba(125, 211, 252, 0.4);
  transform: translateY(-50%);
  animation: spin 30s linear infinite;
}

.r2 {
  width: 176px;
  height: 176px;
  right: 48px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  transform: translateY(-50%);
  animation: spin 22s linear infinite reverse;
}

@keyframes spin {
  to {
    transform: translateY(-50%) rotate(360deg);
  }
}

.deco-core {
  position: absolute;
  top: 50%;
  right: 84px;
  transform: translateY(-50%);
  width: 104px;
  height: 104px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(59, 130, 246, 0.18);
  border: 1px solid rgba(125, 211, 252, 0.35);
  backdrop-filter: blur(10px);
  animation: pulse-glow 4s ease-in-out infinite;
}

@keyframes pulse-glow {
  0%,
  100% {
    box-shadow:
      0 0 48px rgba(56, 189, 248, 0.28),
      inset 0 0 20px rgba(56, 189, 248, 0.12);
  }
  50% {
    box-shadow:
      0 0 72px rgba(56, 189, 248, 0.45),
      inset 0 0 28px rgba(56, 189, 248, 0.22);
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
  color: #eaf1ff;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  backdrop-filter: blur(12px);
  box-shadow: 0 10px 26px rgba(2, 8, 30, 0.35);
  animation: floaty 5s ease-in-out infinite;
}

.fc1 {
  left: 0;
  top: 8px;
}

.fc2 {
  left: 10%;
  bottom: 18px;
  animation-delay: 1.2s;
}

.fc3 {
  left: -6%;
  bottom: 92px;
  animation-delay: 2.4s;
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
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 18px;
  border-radius: 0;
  background: var(--primary-gradient);
}

.section-title.hot::before {
  background: var(--accent-hot-gradient);
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
  border-radius: var(--radius);
  background: #eef2f7;
}

.brand-logo-fallback {
  border-radius: var(--radius);
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
  border-radius: var(--radius);
  overflow: hidden;
  background: #eef2f7;
}

.seckill-pic {
  width: 100%;
  height: 100%;
  transition: transform 0.4s var(--ease);
}

.seckill-item:hover .seckill-pic {
  transform: scale(1.05);
}

.seckill-status {
  position: absolute;
  top: 8px;
  left: 8px;
  padding: 2px 10px;
  border-radius: 2px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 1px;
}

.seckill-status.in-progress {
  background: var(--accent-hot-gradient);
  box-shadow: 0 2px 10px rgba(239, 68, 68, 0.4);
}

.seckill-status.not-started {
  background: linear-gradient(135deg, #60a5fa, #2563eb);
}

.seckill-status.ended {
  background: #94a3b8;
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
  transition: box-shadow 0.3s var(--ease);
}

.seckill-login-guide:hover {
  box-shadow: var(--shadow-hover);
}

.seckill-login-guide p {
  flex: 1;
  font-size: 15px;
}

.btn-primary {
  background: var(--primary-gradient);
  border: none;
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
    min-height: 260px;
  }

  .hero-title {
    font-size: 30px;
  }
}
</style>
