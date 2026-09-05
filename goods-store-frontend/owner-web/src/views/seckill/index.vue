<template>
  <div class="seckill-page">
    <!-- 会场头图 -->
    <section class="seckill-hero">
      <div class="content-wrap hero-inner">
        <div class="hero-text">
          <h1>限时秒杀</h1>
          <p>好价稍纵即逝，拼的就是手速</p>
        </div>
        <div class="hero-deco">
          <el-icon :size="34" class="deco-icon d1"><AlarmClock /></el-icon>
          <el-icon :size="26" class="deco-icon d2"><Lightning /></el-icon>
        </div>
      </div>
    </section>

    <div class="content-wrap body-wrap">
      <!-- 骨架 -->
      <template v-if="loading">
        <div class="seckill-grid">
          <div v-for="i in 4" :key="i" class="skeleton-card card">
            <div
              class="skeleton-line"
              style="aspect-ratio: 1; border-radius: 10px"
            ></div>
            <div class="skeleton-line" style="margin-top: 12px"></div>
            <div
              class="skeleton-line"
              style="width: 45%; margin-top: 8px"
            ></div>
            <div
              class="skeleton-line"
              style="
                width: 70%;
                height: 34px;
                margin-top: 14px;
                border-radius: 999px;
              "
            ></div>
          </div>
        </div>
      </template>

      <template v-else>
        <!-- 进行中 -->
        <section v-if="inProgressList.length" class="group">
          <div class="group-head">
            <h2 class="group-title green">正在秒杀</h2>
            <span class="group-sub">手快有手慢无</span>
          </div>
          <div class="seckill-grid">
            <div
              v-for="s in inProgressList"
              :key="s.id"
              class="seckill-card card card-hover"
            >
              <div class="pic-wrap" @click="goGood(s)">
                <el-image :src="s.goodPic" fit="cover" class="pic" lazy>
                  <template #error>
                    <div class="img-fallback">
                      <el-icon :size="26"><Picture /></el-icon>
                    </div>
                  </template>
                </el-image>
                <span class="live-tag">
                  <span class="live-dot"></span>进行中
                </span>
              </div>
              <div class="info">
                <p class="name" :title="s.goodName">{{ s.goodName }}</p>
                <div class="price-row">
                  <span class="price">¥{{ s.originalPrice }}</span>
                  <span
                    v-if="s.stockLeft != null && Number(s.stockLeft) > 0"
                    class="stock-left"
                    :class="{ low: Number(s.stockLeft) <= 5 }"
                  >
                    仅剩 {{ s.stockLeft }} 件
                  </span>
                </div>
                <CountDown :seconds="s.countdownSec" @finish="loadList" />
                <el-button
                  v-if="s.robbed"
                  type="success"
                  plain
                  class="btn-wait"
                  disabled
                >
                  <el-icon><CircleCheckFilled /></el-icon>
                  已抢到
                </el-button>
                <el-button v-else-if="isSoldOut(s)" class="btn-wait" disabled>
                  已售空
                </el-button>
                <el-button
                  v-else
                  type="primary"
                  class="btn-seckill"
                  :loading="robbingId === s.id"
                  :disabled="robbing && robbingId !== s.id"
                  @click="rob(s)"
                >
                  <el-icon v-if="robbingId !== s.id"><Lightning /></el-icon>
                  {{ robbingId === s.id ? "抢购中…" : "立即抢购" }}
                </el-button>
              </div>
            </div>
          </div>
        </section>

        <!-- 即将开始 -->
        <section v-if="upcomingList.length" class="group">
          <div class="group-head">
            <h2 class="group-title amber">即将开始</h2>
            <span class="group-sub">定好闹钟，别错过</span>
          </div>
          <div class="seckill-grid">
            <div
              v-for="s in upcomingList"
              :key="s.id"
              class="seckill-card card card-hover"
            >
              <div class="pic-wrap" @click="goGood(s)">
                <el-image :src="s.goodPic" fit="cover" class="pic" lazy>
                  <template #error>
                    <div class="img-fallback">
                      <el-icon :size="26"><Picture /></el-icon>
                    </div>
                  </template>
                </el-image>
                <span class="upcoming-tag">即将开始</span>
              </div>
              <div class="info">
                <p class="name" :title="s.goodName">{{ s.goodName }}</p>
                <div class="price-row">
                  <span class="price">¥{{ s.originalPrice }}</span>
                </div>
                <CountDown :seconds="s.countdownSec" @finish="loadList" />
                <el-button class="btn-wait" disabled>未开始</el-button>
              </div>
            </div>
          </div>
        </section>

        <!-- 已结束 -->
        <section v-if="endedList.length" class="group">
          <div class="group-head">
            <h2 class="group-title gray">已结束</h2>
            <span class="group-sub">下期更精彩</span>
          </div>
          <div class="seckill-grid ended">
            <div v-for="s in endedList" :key="s.id" class="seckill-card card">
              <div class="pic-wrap">
                <el-image :src="s.goodPic" fit="cover" class="pic" lazy>
                  <template #error>
                    <div class="img-fallback">
                      <el-icon :size="26"><Picture /></el-icon>
                    </div>
                  </template>
                </el-image>
                <span class="ended-tag">已结束</span>
              </div>
              <div class="info">
                <p class="name" :title="s.goodName">{{ s.goodName }}</p>
                <div class="price-row">
                  <span class="price">¥{{ s.originalPrice }}</span>
                </div>
                <el-button
                  v-if="s.robbed"
                  type="success"
                  plain
                  class="btn-wait"
                  disabled
                >
                  <el-icon><CircleCheckFilled /></el-icon>
                  已抢到
                </el-button>
                <el-button v-else class="btn-wait" disabled plain
                  >已抢完</el-button
                >
              </div>
            </div>
          </div>
        </section>

        <!-- 空 -->
        <div v-if="!list.length" class="empty-wrap card">
          <el-empty description="当前暂无秒杀活动，敬请期待～">
            <el-button
              type="primary"
              class="btn-primary"
              @click="$router.push('/product')"
            >
              先去逛逛
            </el-button>
          </el-empty>
        </div>
      </template>
    </div>

    <!-- 抢购结果对话框 -->
    <el-dialog
      v-model="resultVisible"
      :show-close="polling !== 'polling'"
      width="380px"
      align-center
      class="result-dialog"
    >
      <div class="result-body">
        <template v-if="polling === 'polling'">
          <div class="result-icon spinning">
            <el-icon :size="40"><Loading /></el-icon>
          </div>
          <h3>排队抢购中…</h3>
          <p>前方还有小伙伴在排队，正在为您确认结果</p>
          <div class="poll-dots">
            <span class="dot"></span>
            <span class="dot"></span>
            <span class="dot"></span>
          </div>
        </template>
        <template v-else-if="polling === 'success'">
          <div class="result-icon success">
            <el-icon :size="44"><SuccessFilled /></el-icon>
          </div>
          <h3>恭喜，抢到啦！</h3>
          <p class="order-no">订单号：{{ resultOrderNo }}</p>
          <el-button
            type="primary"
            class="btn-primary"
            round
            @click="resultVisible = false"
          >
            开心收下
          </el-button>
        </template>
        <template v-else-if="polling === 'timeout'">
          <div class="result-icon warn">
            <el-icon :size="40"><WarningFilled /></el-icon>
          </div>
          <h3>排队的人有点多</h3>
          <p>抢购资格已保留，请稍后刷新本页查看最终结果</p>
          <el-button round @click="resultVisible = false">知道了</el-button>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { seckillList, seckillOrder, seckillResult } from "@/api/seckill";
import type { SeckillGoodVO } from "@/api/types";

const router = useRouter();

const loading = ref(true);
const list = ref<SeckillGoodVO[]>([]);

const inProgressList = computed(() =>
  list.value.filter((s) => s.status === "IN_PROGRESS"),
);
const upcomingList = computed(() =>
  list.value.filter((s) => s.status === "NOT_STARTED"),
);
const endedList = computed(() =>
  list.value.filter((s) => s.status === "ENDED"),
);

// 抢购状态
const robbing = ref(false);
const robbingId = ref<string>("");
const resultVisible = ref(false);
const polling = ref<"polling" | "success" | "timeout">("polling");
const resultOrderNo = ref("");
let pollTimer: ReturnType<typeof setTimeout> | null = null;
let pollCount = 0;
const POLL_MAX = 20;

async function loadList() {
  try {
    list.value = await seckillList();
  } finally {
    loading.value = false;
  }
}

function goGood(s: SeckillGoodVO) {
  router.push(`/product/${s.goodId}`);
}

/** 已售空：Redis 库存已预热且为 0（未预热 null 不算售空） */
function isSoldOut(s: SeckillGoodVO) {
  return s.stockLeft != null && Number(s.stockLeft) <= 0;
}

async function rob(s: SeckillGoodVO) {
  if (robbing.value) return;
  if (s.robbed || isSoldOut(s)) return;
  robbing.value = true;
  robbingId.value = s.id;
  try {
    const res = await seckillOrder(s.id);
    // 下单受理成功，进入结果轮询
    resultOrderNo.value = res.orderNo;
    polling.value = "polling";
    resultVisible.value = true;
    pollCount = 0;
    poll();
  } catch {
    // 失败文案已由 request 拦截器统一弹出；刷新列表同步 已抢到/已售空 状态
    loadList();
  } finally {
    robbing.value = false;
    robbingId.value = "";
  }
}

function poll() {
  if (pollTimer) clearTimeout(pollTimer);
  pollTimer = setTimeout(async () => {
    try {
      const res = await seckillResult(resultOrderNo.value);
      if (res.status === "SUCCESS") {
        polling.value = "success";
        loadList();
        return;
      }
    } catch {
      // 查询异常视为仍在处理，继续轮询
    }
    pollCount += 1;
    if (pollCount >= POLL_MAX) {
      polling.value = "timeout";
      return;
    }
    poll();
  }, 1500);
}

onBeforeUnmount(() => {
  if (pollTimer) clearTimeout(pollTimer);
});

onMounted(loadList);
</script>

<style scoped>
/* ---------- 头图 ---------- */
.seckill-hero {
  background: linear-gradient(140deg, #0f766e 0%, #10b981 55%, #34d399 100%);
  color: #fff;
}

.hero-inner {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 44px;
  padding-bottom: 44px;
}

.hero-text h1 {
  font-size: 34px;
  letter-spacing: 2px;
}

.hero-text p {
  margin-top: 10px;
  opacity: 0.9;
  font-size: 14px;
}

.hero-deco {
  display: flex;
  gap: 18px;
}

.deco-icon {
  animation: floaty 4s ease-in-out infinite;
}

.d2 {
  animation-delay: 1.5s;
}

@keyframes floaty {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

.body-wrap {
  padding-top: 28px;
  padding-bottom: 48px;
}

/* ---------- 分组 ---------- */
.group {
  margin-bottom: 40px;
}

.group-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 18px;
}

.group-title {
  font-size: 20px;
  font-weight: 700;
  position: relative;
  padding-left: 14px;
}

.group-title::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 5px;
  height: 16px;
  border-radius: 3px;
}

.group-title.green::before {
  background: linear-gradient(135deg, #34d399, #10b981);
}

.group-title.amber::before {
  background: linear-gradient(135deg, #fbbf24, #f59e0b);
}

.group-title.gray::before {
  background: #cfc9c2;
}

.group-sub {
  font-size: 13px;
  color: var(--text-sub);
}

/* ---------- 卡片 ---------- */
.seckill-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
  gap: 16px;
}

.seckill-card {
  padding: 10px;
}

.pic-wrap {
  position: relative;
  aspect-ratio: 1;
  border-radius: 10px;
  overflow: hidden;
  background: #f5f2ee;
  cursor: pointer;
}

.pic {
  width: 100%;
  height: 100%;
  transition: transform 0.4s ease;
}

.seckill-card:hover .pic {
  transform: scale(1.05);
}

.live-tag,
.upcoming-tag,
.ended-tag {
  position: absolute;
  top: 10px;
  left: 10px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
}

.live-tag {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  box-shadow: 0 2px 8px rgba(220, 38, 38, 0.4);
}

.live-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #fff;
  animation: blink 1.2s ease infinite;
}

@keyframes blink {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.2;
  }
}

.upcoming-tag {
  background: linear-gradient(135deg, #fbbf24, #f59e0b);
}

.ended-tag {
  background: #b8b2ac;
}

.info {
  padding: 10px 4px 4px;
}

.name {
  font-size: 14px;
  line-height: 1.4;
  height: 20px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.price-row {
  margin: 8px 0;
}

.price-row .price {
  font-size: 20px;
}

.stock-left {
  margin-left: 8px;
  font-size: 12px;
  color: var(--text-sub);
}

.stock-left.low {
  color: #ef4444;
  font-weight: 600;
}

.btn-seckill {
  margin-top: 10px;
  width: 100%;
  border-radius: 999px;
  border: none;
  background: linear-gradient(135deg, #34d399 0%, #10b981 100%);
  font-weight: 600;
  letter-spacing: 1px;
}

.btn-seckill:hover:not(.is-disabled) {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
}

.btn-wait {
  margin-top: 10px;
  width: 100%;
  border-radius: 999px;
}

.seckill-grid.ended .pic {
  filter: grayscale(0.7);
  opacity: 0.75;
}

.seckill-grid.ended .price-row .price {
  color: var(--text-light);
}

/* ---------- 结果对话框 ---------- */
.result-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 12px 8px 20px;
  text-align: center;
}

.result-body h3 {
  font-size: 19px;
  margin-top: 8px;
}

.result-body p {
  font-size: 13px;
  color: var(--text-sub);
  line-height: 1.6;
}

.order-no {
  margin-top: 4px;
  word-break: break-all;
}

.result-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  border-radius: 50%;
}

.result-icon.spinning {
  color: var(--accent-green);
  background: var(--accent-green-light);
}

.result-icon.spinning .el-icon {
  animation: spin 1.2s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.result-icon.success {
  color: #10b981;
  background: var(--accent-green-light);
}

.result-icon.warn {
  color: #f59e0b;
  background: #fef3e2;
}

.result-body .el-button {
  margin-top: 16px;
}

.poll-dots {
  display: flex;
  gap: 6px;
  margin-top: 10px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #34d399;
  animation: dot-jump 1.2s ease infinite;
}

.dot:nth-child(2) {
  animation-delay: 0.15s;
}

.dot:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes dot-jump {
  0%,
  100% {
    transform: translateY(0);
    opacity: 0.5;
  }
  50% {
    transform: translateY(-6px);
    opacity: 1;
  }
}

.empty-wrap {
  padding: 60px 0;
}

@media (max-width: 768px) {
  .hero-text h1 {
    font-size: 26px;
  }

  .hero-deco {
    display: none;
  }
}
</style>
