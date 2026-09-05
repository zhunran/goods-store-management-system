<template>
  <div class="order-detail-page content-wrap">
    <el-breadcrumb class="crumb" separator="/">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item :to="{ path: '/order' }">我的订单</el-breadcrumb-item>
      <el-breadcrumb-item>订单详情</el-breadcrumb-item>
    </el-breadcrumb>

    <!-- 骨架 -->
    <template v-if="loading">
      <div class="section card">
        <div class="skeleton-line" style="width: 40%; height: 28px"></div>
        <div class="skeleton-line" style="width: 60%; margin-top: 14px"></div>
      </div>
      <div class="section card">
        <div v-for="i in 2" :key="i" style="display: flex; gap: 14px; align-items: center; padding: 10px 0">
          <div class="skeleton-line" style="width: 64px; height: 64px; border-radius: 8px"></div>
          <div class="skeleton-line" style="flex: 1"></div>
        </div>
      </div>
    </template>

    <template v-else-if="order">
      <!-- 状态卡 -->
      <div class="section card status-card">
        <div class="status-main">
          <div class="status-text">
            <h2 class="status-name">{{ ORDER_STATUS_TEXT[order.status] || order.status }}</h2>
            <p class="status-desc">{{ statusDesc }}</p>
          </div>
          <div class="status-amount">
            <span class="amount-label">实付金额</span>
            <span class="price amount">¥{{ order.totalPay }}</span>
          </div>
          <div class="status-btns">
            <el-button v-if="order.status === '10'" @click="onCancel">取消订单</el-button>
            <el-button
              v-if="order.status === '30'"
              class="btn-confirm"
              @click="onConfirm"
            >
              确认收货
            </el-button>
          </div>
        </div>

        <!-- 进度 -->
        <el-steps
          v-if="order.status !== '50'"
          :active="stepActive"
          align-center
          class="status-steps"
        >
          <el-step title="提交订单" :description="order.createdTime" />
          <el-step title="支付订单" :description="order.payTime || '—'" />
          <el-step title="商品发货" :description="order.shipTime || '—'" />
          <el-step title="确认收货" :description="order.checkoutTime || '—'" />
        </el-steps>
        <div v-else class="cancelled-tip">
          <el-icon><CircleClose /></el-icon>
          订单已于 {{ order.updatedTime || order.createdTime }} 取消
        </div>
      </div>

      <!-- 收货信息 -->
      <div class="section card">
        <div class="section-head">
          <span class="section-title">收货信息</span>
        </div>
        <div class="addr-grid">
          <div class="addr-item">
            <span class="addr-label">收货人</span>
            <span>{{ order.receiverName || '—' }}</span>
          </div>
          <div class="addr-item">
            <span class="addr-label">联系电话</span>
            <span>{{ order.receiverPhone || '—' }}</span>
          </div>
          <div class="addr-item">
            <span class="addr-label">收货地址</span>
            <span>{{ order.receiverAddrDetail || '—' }}</span>
          </div>
          <div class="addr-item">
            <span class="addr-label">买家账号</span>
            <span>{{ order.memberAccount || '—' }}</span>
          </div>
        </div>
      </div>

      <!-- 商品清单 -->
      <div class="section card">
        <div class="section-head">
          <span class="section-title">商品清单</span>
          <span class="section-sub">共 {{ itemCount }} 件</span>
        </div>
        <div v-for="(item, i) in order.items" :key="i" class="good-row">
          <el-image
            :src="item.goodPic"
            fit="cover"
            class="good-pic"
            @click="$router.push(`/product/${item.goodId}`)"
          >
            <template #error>
              <div class="img-fallback"><el-icon :size="20"><Picture /></el-icon></div>
            </template>
          </el-image>
          <p class="good-name" @click="$router.push(`/product/${item.goodId}`)">
            {{ item.goodName }}
          </p>
          <span class="good-price">¥{{ item.dealPrice }}</span>
          <span class="good-qty">× {{ item.count }}</span>
          <span class="good-subtotal price">¥{{ (item.dealPrice * item.count).toFixed(2) }}</span>
        </div>
      </div>

      <!-- 订单信息 -->
      <div class="section card">
        <div class="section-head">
          <span class="section-title">订单信息</span>
        </div>
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">订单编号</span>
            <span>{{ order.orderNo }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">创建时间</span>
            <span>{{ order.createdTime || '—' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">支付时间</span>
            <span>{{ order.payTime || '—' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">发货时间</span>
            <span>{{ order.shipTime || '—' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">完成时间</span>
            <span>{{ order.checkoutTime || '—' }}</span>
          </div>
          <div class="info-item" v-if="order.orderComment">
            <span class="info-label">订单备注</span>
            <span>{{ order.orderComment }}</span>
          </div>
        </div>
      </div>
    </template>

    <el-empty v-else description="订单不存在或已被删除">
      <el-button type="primary" class="btn-primary" @click="$router.push('/order')">
        返回订单列表
      </el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cancelOrder, confirmOrder, orderDetail } from '@/api/order'
import { ORDER_STATUS_TEXT, type OrderDetailVO } from '@/api/types'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const order = ref<OrderDetailVO | null>(null)

const itemCount = computed(() =>
  order.value?.items.reduce((s, i) => s + i.count, 0) ?? 0,
)

const statusDesc = computed(() => {
  switch (order.value?.status) {
    case '10':
      return '订单已提交，正在等待付款'
    case '20':
      return '商家正在加紧备货中'
    case '30':
      return '包裹正在飞奔向您而来，记得确认收货哦'
    case '40':
      return '交易完成，感谢您的信任与支持'
    case '50':
      return '订单已取消，期待下次相遇'
    default:
      return ''
  }
})

const stepActive = computed(() => {
  switch (order.value?.status) {
    case '10':
      return 1
    case '20':
      return 2
    case '30':
      return 3
    case '40':
      return 4
    default:
      return 0
  }
})

async function loadDetail(id: string) {
  loading.value = true
  try {
    order.value = await orderDetail(id)
  } catch {
    order.value = null
  } finally {
    loading.value = false
  }
}

async function onCancel() {
  if (!order.value) return
  await ElMessageBox.confirm(`确定取消订单 ${order.value.orderNo} 吗？`, '取消订单', {
    confirmButtonText: '确定取消',
    cancelButtonText: '再想想',
    type: 'warning',
  })
  await cancelOrder(order.value.id)
  ElMessage.success('订单已取消')
  loadDetail(order.value.id)
}

async function onConfirm() {
  if (!order.value) return
  await ElMessageBox.confirm('确认已收到包裹了吗？', '确认收货', {
    confirmButtonText: '确认收货',
    cancelButtonText: '再等等',
    type: 'info',
  })
  await confirmOrder(order.value.id)
  ElMessage.success('确认收货成功，订单已完成')
  loadDetail(order.value.id)
}

onMounted(() => loadDetail(route.params.id as string))

watch(
  () => route.params.id,
  (id) => {
    if (id && route.name === 'orderDetail') loadDetail(id as string)
  },
)
</script>

<style scoped>
.order-detail-page {
  padding-top: 20px;
  padding-bottom: 48px;
}

.crumb {
  margin-bottom: 16px;
}

.section {
  padding: 22px 26px;
  margin-bottom: 16px;
}

.section-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 18px;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  position: relative;
  padding-left: 12px;
}

.section-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 15px;
  border-radius: 2px;
  background: var(--primary-gradient);
}

.section-sub {
  font-size: 12px;
  color: var(--text-light);
}

/* ---------- 状态卡 ---------- */
.status-main {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
}

.status-text {
  flex: 1;
  min-width: 200px;
}

.status-name {
  font-size: 24px;
  font-weight: 700;
}

.status-desc {
  margin-top: 8px;
  font-size: 13px;
  color: var(--text-sub);
}

.status-amount {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.amount-label {
  font-size: 13px;
  color: var(--text-light);
}

.amount {
  font-size: 28px;
}

.status-btns {
  display: flex;
  gap: 10px;
}

.btn-confirm {
  color: var(--accent-green);
  border-color: var(--accent-green);
}

.btn-confirm:hover {
  background: var(--accent-green-light);
}

.status-steps {
  margin-top: 28px;
}

.cancelled-tip {
  margin-top: 22px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-radius: var(--radius-sm);
  background: #fdf1f0;
  color: #ef4444;
  font-size: 13px;
}

/* ---------- 收货信息 / 订单信息 ---------- */
.addr-grid,
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px 32px;
  font-size: 14px;
}

.addr-item,
.info-item {
  display: flex;
  gap: 14px;
  line-height: 1.6;
}

.addr-label,
.info-label {
  color: var(--text-light);
  flex-shrink: 0;
  font-size: 13px;
  min-width: 60px;
  padding-top: 1px;
}

/* ---------- 商品清单 ---------- */
.good-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px dashed #f0ede9;
}

.good-row:last-child {
  border-bottom: none;
}

.good-pic {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  flex-shrink: 0;
  background: #f5f2ee;
  cursor: pointer;
}

.good-name {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.good-name:hover {
  color: var(--primary);
}

.good-price {
  font-size: 13px;
  color: var(--text-sub);
  flex-shrink: 0;
}

.good-qty {
  font-size: 13px;
  color: var(--text-sub);
  width: 50px;
  text-align: center;
  flex-shrink: 0;
}

.good-subtotal {
  width: 100px;
  text-align: right;
  font-size: 15px;
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .addr-grid,
  .info-grid {
    grid-template-columns: 1fr;
  }

  .good-name {
    white-space: normal;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }

  .good-price,
  .good-qty {
    display: none;
  }

  .status-amount {
    order: 3;
  }
}
</style>
