<template>
  <div class="checkout-page content-wrap">
    <h1 class="page-title">确认订单</h1>

    <template v-if="!loading">
      <!-- 购物车为空 -->
      <div v-if="!items.length" class="empty-wrap card">
        <el-empty description="购物车里还没有商品，先去逛逛吧～">
          <el-button
            type="primary"
            class="btn-primary"
            @click="$router.push('/product')"
          >
            去逛逛
          </el-button>
        </el-empty>
      </div>

      <template v-else>
        <!-- 收货地址 -->
        <div class="section card">
          <div class="section-head">
            <span class="section-title">收货地址</span>
            <el-button
              link
              type="primary"
              size="small"
              @click="$router.push('/user')"
            >
              管理地址 <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>

          <div v-if="addresses.length" class="addr-list">
            <div
              v-for="a in addresses"
              :key="a.id"
              class="addr-item"
              :class="{ active: selectedAddrId === a.id }"
              @click="selectAddr(a)"
            >
              <div class="addr-top">
                <span class="addr-name">{{ a.receiver }}</span>
                <span class="addr-phone">{{ a.phone }}</span>
                <el-tag
                  v-if="a.isDefault"
                  size="small"
                  class="default-tag"
                  effect="light"
                >
                  默认
                </el-tag>
              </div>
              <p class="addr-detail">{{ a.addrDetail }}</p>
              <span
                v-if="selectedAddrId === a.id && !a.isDefault"
                class="switching-tip"
              >
                已切换，下单将使用此地址
              </span>
            </div>
          </div>
          <div v-else class="no-addr">
            <p>还没有收货地址，先去个人中心添加一个吧～</p>
            <el-button
              type="primary"
              class="btn-primary"
              @click="$router.push('/user')"
            >
              去添加地址
            </el-button>
          </div>
        </div>

        <!-- 商品清单 -->
        <div class="section card">
          <div class="section-head">
            <span class="section-title">商品清单</span>
            <span class="section-sub">以下商品将一次性结算</span>
          </div>
          <div v-for="item in items" :key="item.cartId" class="good-row">
            <el-image :src="item.goodPic" fit="cover" class="good-pic">
              <template #error>
                <div class="img-fallback">
                  <el-icon :size="20"><Picture /></el-icon>
                </div>
              </template>
            </el-image>
            <p class="good-name">{{ item.goodName }}</p>
            <span class="good-price">¥{{ item.price }}</span>
            <span class="good-qty">× {{ item.qty }}</span>
            <span class="good-subtotal price"
              >¥{{ (item.price * item.qty).toFixed(2) }}</span
            >
          </div>
        </div>

        <!-- 备注 -->
        <div class="section card">
          <div class="section-head">
            <span class="section-title">订单备注</span>
            <span class="section-sub">选填，最多 100 字</span>
          </div>
          <el-input
            v-model="comment"
            type="textarea"
            :rows="2"
            maxlength="100"
            show-word-limit
            placeholder="给商家捎句话，如：放前台代收～"
          />
        </div>

        <!-- 提交栏 -->
        <div class="submit-bar card">
          <div class="submit-info">
            共 <b>{{ totalCount }}</b> 件，应付总额
            <span class="price amount">¥{{ totalAmount.toFixed(2) }}</span>
          </div>
          <el-button
            type="primary"
            size="large"
            class="btn-primary submit-btn"
            :loading="submitting"
            :disabled="!addresses.length"
            @click="doSubmit"
          >
            提交订单
          </el-button>
        </div>
      </template>
    </template>

    <!-- 骨架 -->
    <template v-else>
      <div class="section card">
        <div class="skeleton-line" style="width: 30%"></div>
        <div class="skeleton-line" style="width: 80%; margin-top: 16px"></div>
        <div class="skeleton-line" style="width: 60%; margin-top: 10px"></div>
      </div>
      <div class="section card">
        <div
          v-for="i in 2"
          :key="i"
          style="display: flex; gap: 14px; align-items: center; padding: 10px 0"
        >
          <div
            class="skeleton-line"
            style="width: 64px; height: 64px; border-radius: 8px"
          ></div>
          <div class="skeleton-line" style="flex: 1"></div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { cartList } from "@/api/cart";
import { submitOrder } from "@/api/order";
import { getProfile, setDefaultAddress } from "@/api/member";
import type { MemberAddressVO } from "@/api/types";
import { useCartStore } from "@/stores/cart";

const router = useRouter();
const cartStore = useCartStore();

const loading = ref(true);
const submitting = ref(false);
const items = ref<Awaited<ReturnType<typeof cartList>>>([]);
const addresses = ref<MemberAddressVO[]>([]);
const selectedAddrId = ref<string>("");
const comment = ref("");

const totalCount = computed(() => items.value.reduce((s, i) => s + i.qty, 0));
const totalAmount = computed(() =>
  items.value.reduce((s, i) => s + i.price * i.qty, 0),
);

/**
 * 后端下单固定使用默认地址（忽略请求中的 addressId），
 * 因此选择地址时若非默认则同步切换默认地址，保证所见即所得。
 */
async function selectAddr(addr: MemberAddressVO) {
  if (selectedAddrId.value === addr.id) return;
  if (addr.isDefault) {
    selectedAddrId.value = addr.id;
    return;
  }
  try {
    await setDefaultAddress(addr.id);
    addresses.value = addresses.value.map((a) => ({
      ...a,
      isDefault: a.id === addr.id,
    }));
    selectedAddrId.value = addr.id;
    ElMessage.success(`已切换默认地址为「${addr.receiver}」`);
  } catch {
    // 切换失败保持原默认
  }
}

async function doSubmit() {
  if (submitting.value) return;
  submitting.value = true;
  try {
    const defaultAddr = addresses.value.find((a) => a.isDefault);
    const res = await submitOrder(
      defaultAddr?.id,
      comment.value.trim() || undefined,
    );
    cartStore.refresh();
    ElMessage.success(`下单成功，订单号 ${res.orderNo}`);
    router.push(`/cashier/${res.id}`);
  } finally {
    submitting.value = false;
  }
}

onMounted(async () => {
  try {
    const [cartRes, profileRes] = await Promise.all([cartList(), getProfile()]);
    // 结算页只读购物车中勾选的商品，与后端按 selected 过滤下单保持一致
    items.value = cartRes.filter((i) => i.selected);
    addresses.value = profileRes.addresses;
    const def = addresses.value.find((a) => a.isDefault);
    selectedAddrId.value = def?.id ?? "";
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.checkout-page {
  padding-top: 24px;
  padding-bottom: 110px;
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

/* ---------- 地址 ---------- */
.addr-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}

.addr-item {
  position: relative;
  padding: 14px 16px;
  border: 1.5px solid #eceae6;
  border-radius: var(--radius);
  cursor: pointer;
  transition: all 0.2s;
}

.addr-item:hover {
  border-color: #fdba8c;
}

.addr-item.active {
  border-color: var(--primary);
  background: var(--primary-light);
}

.addr-top {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.addr-name {
  font-weight: 600;
  font-size: 15px;
}

.addr-phone {
  font-size: 13px;
  color: var(--text-sub);
}

.default-tag {
  background: var(--primary-light);
  color: var(--primary);
  border: none;
}

.addr-detail {
  margin-top: 8px;
  font-size: 13px;
  color: var(--text-sub);
  line-height: 1.5;
}

.switching-tip {
  display: inline-block;
  margin-top: 6px;
  font-size: 12px;
  color: var(--primary);
}

.no-addr {
  text-align: center;
  padding: 24px 0;
  color: var(--text-sub);
  font-size: 14px;
}

.no-addr .el-button {
  margin-top: 14px;
}

/* ---------- 商品清单 ---------- */
.good-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px dashed #e5eaf2;
}

.good-row:last-child {
  border-bottom: none;
}

.good-pic {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  flex-shrink: 0;
  background: #eef2f7;
}

.good-name {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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

/* ---------- 提交栏 ---------- */
.submit-bar {
  position: fixed;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  width: min(1168px, calc(100% - 32px));
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 24px;
  border-radius: var(--radius-lg);
  box-shadow: 0 8px 30px rgba(15, 23, 42, 0.12);
  z-index: 10;
}

.submit-info {
  font-size: 14px;
  color: var(--text-sub);
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.submit-info b {
  color: var(--primary);
}

.amount {
  font-size: 24px;
}

.submit-btn {
  padding: 0 42px;
  border-radius: var(--radius-sm);
}

.empty-wrap {
  padding: 60px 0;
}

@media (max-width: 768px) {
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
}
</style>
