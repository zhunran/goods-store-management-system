<template>
  <div class="cashier-page content-wrap">
    <h1 class="page-title">收银台</h1>

    <!-- 骨架 -->
    <template v-if="loading">
      <div class="section card">
        <div class="skeleton-line" style="width: 45%"></div>
        <div
          class="skeleton-line"
          style="width: 90%; height: 56px; margin-top: 16px"
        ></div>
        <div
          class="skeleton-line"
          style="width: 30%; margin-top: 16px; margin-left: auto"
        ></div>
      </div>
    </template>

    <template v-else-if="order">
      <!-- 订单摘要 -->
      <div class="section card">
        <div class="order-brief">
          <div class="brief-left">
            <p class="brief-label">订单号</p>
            <p class="brief-no">{{ order.orderNo }}</p>
          </div>
          <div class="brief-amount">
            <span class="amount-label">应付金额</span>
            <span class="price amount">¥{{ order.totalPay }}</span>
          </div>
        </div>
        <div class="brief-items">
          共 <b>{{ itemCount }}</b> 件商品
          <span class="brief-link" @click="goDetail">查看订单详情</span>
        </div>
      </div>

      <!-- 非 待付款 状态 -->
      <div v-if="order.status !== '10'" class="section card">
        <el-empty
          :description="`当前订单状态为「${ORDER_STATUS_TEXT[order.status] || order.status}」，无需支付`"
        >
          <el-button type="primary" class="btn-primary" @click="goDetail">
            查看订单详情
          </el-button>
        </el-empty>
      </div>

      <template v-else>
        <!-- 倒计时 -->
        <div class="countdown-bar card" :class="{ 'countdown-over': expired }">
          <template v-if="!expired">
            <el-icon><AlarmClock /></el-icon>
            <span>请在 <b class="countdown-text">{{ countdownText }}</b> 内完成支付，超时订单将自动取消</span>
          </template>
          <template v-else>
            <el-icon><CircleClose /></el-icon>
            <span>订单已超时未支付，系统将自动取消，请重新下单</span>
          </template>
        </div>

        <!-- 支付方式 -->
        <div class="section card">
          <div class="section-head">
            <span class="section-title">支付方式</span>
            <span class="section-sub">模拟支付，点击确认后即时到账</span>
          </div>
          <div class="pay-list">
            <div
              v-for="p in payMethods"
              :key="p.code"
              class="pay-item"
              :class="{ active: payType === p.code }"
              @click="payType = p.code"
            >
              <span class="pay-icon" :class="p.iconClass">{{ p.iconText }}</span>
              <div class="pay-info">
                <p class="pay-name">
                  {{ p.name }}
                  <el-tag size="small" effect="light" class="mock-tag">模拟</el-tag>
                </p>
                <p class="pay-desc">{{ p.desc }}</p>
              </div>
              <span class="pay-check" v-if="payType === p.code">✓</span>
            </div>
          </div>
        </div>

        <!-- 确认支付 -->
        <div class="pay-bar card">
          <div class="pay-bar-info">
            <span v-if="payType" class="pay-bar-method">
              {{ payMethodName }}
            </span>
            <span class="price amount">¥{{ order.totalPay }}</span>
          </div>
          <el-button
            type="primary"
            size="large"
            class="btn-primary pay-btn"
            :loading="paying"
            :disabled="expired || !payType"
            @click="doPay"
          >
            {{ expired ? "订单已超时" : "确认支付" }}
          </el-button>
        </div>
      </template>
    </template>

    <el-empty v-else description="订单不存在或已被删除">
      <el-button type="primary" class="btn-primary" @click="$router.push('/order')">
        返回订单列表
      </el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { orderDetail, payOrder } from "@/api/order";
import { ORDER_STATUS_TEXT, type OrderDetailVO } from "@/api/types";

const route = useRoute();
const router = useRouter();

const loading = ref(true);
const paying = ref(false);
const order = ref<OrderDetailVO | null>(null);
const payType = ref("ALIPAY");

/** 支付倒计时（秒），0 表示已超时 */
const remainSec = ref(0);
let timer: number | undefined;

const payMethods = [
  { code: "ALIPAY", name: "支付宝", desc: "推荐，支付宝用户优先", iconClass: "icon-alipay", iconText: "支" },
  { code: "WECHAT", name: "微信支付", desc: "微信快捷支付", iconClass: "icon-wechat", iconText: "微" },
];

const orderId = computed(() => route.params.id as string);
const itemCount = computed(
  () => order.value?.items.reduce((s, i) => s + i.count, 0) ?? 0,
);
const payMethodName = computed(
  () => payMethods.find((p) => p.code === payType.value)?.name ?? "",
);
const expired = computed(() => remainSec.value <= 0);

const countdownText = computed(() => {
  const m = Math.floor(remainSec.value / 60);
  const s = remainSec.value % 60;
  return `${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
});

/** 后端 LocalDateTime 序列化为 "yyyy-MM-dd HH:mm:ss"，替换为兼容格式解析 */
function parseTime(str?: string) {
  if (!str) return 0;
  return new Date(str.replace(/-/g, "/")).getTime();
}

function startCountdown() {
  // 超时取消延迟消息在下单时发出，锚点即下单时间（checkoutTime 于创建时写入）
  const anchor = parseTime(order.value?.checkoutTime || order.value?.createdTime);
  if (!anchor) {
    remainSec.value = 0;
    return;
  }
  const deadline = anchor + 30 * 60 * 1000;
  const tick = () => {
    remainSec.value = Math.max(0, Math.floor((deadline - Date.now()) / 1000));
    if (remainSec.value <= 0) {
      stopTimer();
    }
  };
  tick();
  if (remainSec.value > 0) {
    timer = window.setInterval(tick, 1000);
  }
}

function stopTimer() {
  if (timer) {
    window.clearInterval(timer);
    timer = undefined;
  }
}

function goDetail() {
  router.push(`/order/${orderId.value}`);
}

async function loadOrder() {
  loading.value = true;
  try {
    order.value = await orderDetail(orderId.value);
    if (order.value?.status === "10") {
      startCountdown();
    }
  } catch {
    order.value = null;
  } finally {
    loading.value = false;
  }
}

async function doPay() {
  if (paying.value || expired.value) return;
  paying.value = true;
  try {
    await payOrder(orderId.value, payType.value);
    stopTimer();
    ElMessage.success("支付成功");
    router.replace(`/order/${orderId.value}`);
  } finally {
    paying.value = false;
  }
}

onMounted(loadOrder);
onBeforeUnmount(stopTimer);
</script>

<style scoped>
.cashier-page {
  padding-top: 24px;
  padding-bottom: 130px;
  max-width: 860px;
  margin: 0 auto;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 20px;
  position: relative;
  padding-left: 14px;
}

.page-title::before {
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

.section {
  padding: 20px 24px;
  margin-bottom: 16px;
}

.section-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
}

.section-sub {
  font-size: 12px;
  color: var(--text-light);
}

/* ---------- 订单摘要 ---------- */
.order-brief {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.brief-label {
  font-size: 12px;
  color: var(--text-light);
}

.brief-no {
  margin-top: 6px;
  font-size: 18px;
  font-weight: 700;
}

.brief-amount {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.amount-label {
  font-size: 13px;
  color: var(--text-light);
}

.amount {
  font-size: 26px;
}

.brief-items {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px dashed #e5eaf2;
  font-size: 13px;
  color: var(--text-sub);
}

.brief-items b {
  color: var(--primary);
}

.brief-link {
  margin-left: 12px;
  color: var(--primary);
  cursor: pointer;
}

.brief-link:hover {
  text-decoration: underline;
}

/* ---------- 倒计时 ---------- */
.countdown-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 24px;
  margin-bottom: 16px;
  font-size: 14px;
  color: var(--text-sub);
}

.countdown-bar .el-icon {
  color: var(--accent-hot);
}

.countdown-text {
  font-size: 18px;
  color: var(--accent-hot);
  font-variant-numeric: tabular-nums;
}

.countdown-over {
  color: #ef4444;
}

/* ---------- 支付方式 ---------- */
.pay-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 12px;
}

.pay-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border: 1.5px solid #eceae6;
  border-radius: var(--radius);
  cursor: pointer;
  transition: all 0.2s;
}

.pay-item:hover {
  border-color: #fdba8c;
}

.pay-item.active {
  border-color: var(--primary);
  background: var(--primary-light);
}

.pay-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  flex-shrink: 0;
}

.icon-alipay {
  background: #1677ff;
}

.icon-wechat {
  background: #07c160;
}

.pay-info {
  flex: 1;
  min-width: 0;
}

.pay-name {
  font-size: 15px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
}

.mock-tag {
  background: #fdf1f0;
  color: var(--accent-hot);
  border: none;
}

.pay-desc {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-light);
}

.pay-check {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--primary);
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

/* ---------- 确认支付栏 ---------- */
.pay-bar {
  position: fixed;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  width: min(828px, calc(100% - 32px));
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 24px;
  border-radius: var(--radius-lg);
  box-shadow: 0 8px 30px rgba(15, 23, 42, 0.12);
  z-index: 10;
}

.pay-bar-info {
  display: flex;
  align-items: baseline;
  gap: 16px;
}

.pay-bar-method {
  font-size: 13px;
  color: var(--text-sub);
}

.pay-bar .amount {
  font-size: 24px;
}

.pay-btn {
  padding: 0 42px;
  border-radius: var(--radius-sm);
}

@media (max-width: 768px) {
  .pay-bar {
    width: calc(100% - 32px);
  }
}
</style>
