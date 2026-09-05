<template>
  <div class="order-page content-wrap">
    <h1 class="page-title">我的订单</h1>

    <el-tabs
      v-model="activeStatus"
      class="status-tabs"
      @tab-change="onTabChange"
    >
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane label="待付款" name="10" />
      <el-tab-pane label="已支付" name="20" />
      <el-tab-pane label="已发货" name="30" />
      <el-tab-pane label="已完成" name="40" />
      <el-tab-pane label="已取消" name="50" />
    </el-tabs>

    <!-- 骨架 -->
    <template v-if="loading">
      <div v-for="i in 3" :key="i" class="order-card card">
        <div class="skeleton-line" style="width: 35%"></div>
        <div
          class="skeleton-line"
          style="width: 90%; height: 56px; margin-top: 16px"
        ></div>
        <div
          class="skeleton-line"
          style="width: 25%; margin-top: 16px; margin-left: auto"
        ></div>
      </div>
    </template>

    <template v-else>
      <div v-if="records.length" class="order-list">
        <div v-for="o in records" :key="o.id" class="order-card card">
          <div class="order-head">
            <div class="head-left">
              <span class="order-no">订单号：{{ o.orderNo }}</span>
              <span class="order-time">{{ o.createdTime }}</span>
            </div>
            <el-tag :type="ORDER_STATUS_TAG[o.status]" effect="light" round>
              {{ ORDER_STATUS_TEXT[o.status] || o.status }}
            </el-tag>
          </div>

          <div class="order-body" @click="goDetail(o)">
            <span class="status-hint">{{ statusHint(o.status) }}</span>
            <div class="amount-wrap">
              <span class="amount-label">实付</span>
              <span class="price amount">¥{{ o.totalPay }}</span>
            </div>
          </div>

          <div class="order-foot">
            <span class="foot-info">{{ statusFootInfo(o) }}</span>
            <div class="foot-btns">
              <el-button
                v-if="o.status === '10'"
                size="small"
                @click.stop="onCancel(o)"
              >
                取消订单
              </el-button>
              <el-button
                v-if="o.status === '30'"
                size="small"
                class="btn-confirm"
                @click.stop="onConfirm(o)"
              >
                确认收货
              </el-button>
              <el-button
                size="small"
                type="primary"
                plain
                @click.stop="goDetail(o)"
              >
                查看详情
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="empty-wrap card">
        <el-empty :description="emptyText">
          <el-button
            type="primary"
            class="btn-primary"
            @click="$router.push('/product')"
          >
            去逛逛
          </el-button>
        </el-empty>
      </div>

      <div v-if="records.length && Number(total) > pageSize" class="pager-wrap">
        <el-pagination
          v-model:current-page="pageNum"
          :page-size="pageSize"
          :total="Number(total)"
          layout="prev, pager, next"
          background
          @current-change="loadList"
        />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { cancelOrder, confirmOrder, orderPage } from "@/api/order";
import { ORDER_STATUS_TAG, ORDER_STATUS_TEXT, type OrderVO } from "@/api/types";

const router = useRouter();

const loading = ref(true);
const activeStatus = ref("all");
const pageNum = ref(1);
const pageSize = 5;
const total = ref("0");
const records = ref<OrderVO[]>([]);

const emptyText = computed(() =>
  activeStatus.value === "all"
    ? "还没有订单，来下一单试试～"
    : "该状态下暂无订单",
);

function statusHint(status: string) {
  switch (status) {
    case "10":
      return "订单已提交，正在等待付款";
    case "20":
      return "商家正在加紧备货中";
    case "30":
      return "包裹正在飞奔向您而来";
    case "40":
      return "交易完成，感谢您的信任";
    case "50":
      return "订单已取消";
    default:
      return "";
  }
}

function statusFootInfo(o: OrderVO) {
  if (o.status === "20" && o.payTime) return `支付时间：${o.payTime}`;
  if (o.status === "30" && o.shipTime) return `发货时间：${o.shipTime}`;
  if (o.status === "40" && o.checkoutTime) return `完成时间：${o.checkoutTime}`;
  return "";
}

async function loadList() {
  loading.value = true;
  try {
    const res = await orderPage(
      pageNum.value,
      pageSize,
      activeStatus.value === "all" ? undefined : activeStatus.value,
    );
    records.value = res.records;
    total.value = res.total;
  } finally {
    loading.value = false;
  }
}

function onTabChange() {
  pageNum.value = 1;
  loadList();
}

function goDetail(o: OrderVO) {
  router.push(`/order/${o.id}`);
}

async function onCancel(o: OrderVO) {
  await ElMessageBox.confirm(`确定取消订单 ${o.orderNo} 吗？`, "取消订单", {
    confirmButtonText: "确定取消",
    cancelButtonText: "再想想",
    type: "warning",
  });
  await cancelOrder(o.id);
  ElMessage.success("订单已取消");
  loadList();
}

async function onConfirm(o: OrderVO) {
  await ElMessageBox.confirm("确认已收到包裹了吗？", "确认收货", {
    confirmButtonText: "确认收货",
    cancelButtonText: "再等等",
    type: "info",
  });
  await confirmOrder(o.id);
  ElMessage.success("确认收货成功，订单已完成");
  loadList();
}

onMounted(loadList);
</script>

<style scoped>
.order-page {
  padding-top: 24px;
  padding-bottom: 48px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 16px;
  position: relative;
  padding-left: 14px;
}

.page-title::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 5px;
  height: 18px;
  border-radius: 3px;
  background: var(--primary-gradient);
}

.status-tabs {
  margin-bottom: 20px;
}

:deep(.el-tabs__item) {
  font-size: 15px;
}

:deep(.el-tabs__item.is-active) {
  font-weight: 600;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-card {
  padding: 18px 22px;
  transition: box-shadow 0.3s;
}

.order-card:hover {
  box-shadow: var(--shadow-hover);
}

.order-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 14px;
  border-bottom: 1px solid #f0ede9;
}

.head-left {
  display: flex;
  align-items: baseline;
  gap: 14px;
  flex-wrap: wrap;
}

.order-no {
  font-size: 14px;
  font-weight: 600;
}

.order-time {
  font-size: 12px;
  color: var(--text-light);
}

.order-body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 0 16px;
  cursor: pointer;
}

.status-hint {
  font-size: 14px;
  color: var(--text-sub);
}

.amount-wrap {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.amount-label {
  font-size: 12px;
  color: var(--text-light);
}

.amount {
  font-size: 22px;
}

.order-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 14px;
  border-top: 1px solid #f0ede9;
}

.foot-info {
  font-size: 12px;
  color: var(--text-light);
}

.foot-btns {
  display: flex;
  gap: 8px;
}

.btn-confirm {
  color: var(--accent-green);
  border-color: var(--accent-green);
}

.btn-confirm:hover {
  background: var(--accent-green-light);
}

.empty-wrap {
  padding: 60px 0;
}

.pager-wrap {
  margin-top: 28px;
  display: flex;
  justify-content: center;
}
</style>
