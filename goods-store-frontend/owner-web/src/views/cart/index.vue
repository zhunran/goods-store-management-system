<template>
  <div class="cart-page content-wrap">
    <h1 class="page-title">购物车</h1>

    <!-- 骨架 -->
    <template v-if="loading">
      <div v-for="i in 3" :key="i" class="cart-item card">
        <div
          class="skeleton-line"
          style="width: 88px; height: 88px; border-radius: 6px"
        ></div>
        <div style="flex: 1">
          <div class="skeleton-line" style="width: 50%"></div>
          <div class="skeleton-line" style="width: 25%; margin-top: 10px"></div>
        </div>
      </div>
    </template>

    <template v-else>
      <!-- 空购物车 -->
      <div v-if="!items.length" class="empty-wrap card">
        <el-empty description="购物车空空如也，去挑点好物吧～">
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
        <div class="cart-toolbar card">
          <el-checkbox
            :model-value="allSelected"
            :indeterminate="indeterminate"
            @change="(v: string | number | boolean) => toggleAll(v)"
          >
            全选
          </el-checkbox>
          <el-button
            text
            type="danger"
            :disabled="!selectedItems.length"
            @click="removeBatch"
          >
            批量删除
          </el-button>
        </div>

        <div class="cart-list">
          <div v-for="item in items" :key="item.cartId" class="cart-item card">
            <el-checkbox
              :model-value="item.selected"
              @change="(v: string | number | boolean) => toggleOne(item, v)"
            />

            <el-image
              :src="item.goodPic"
              fit="cover"
              class="item-pic"
              @click="$router.push(`/product/${item.goodId}`)"
            >
              <template #error>
                <div class="img-fallback">
                  <el-icon :size="24"><Picture /></el-icon>
                </div>
              </template>
            </el-image>

            <div class="item-info">
              <p
                class="item-name"
                @click="$router.push(`/product/${item.goodId}`)"
              >
                {{ item.goodName }}
              </p>
              <p class="item-price price">¥{{ item.price }}</p>
            </div>

            <div class="item-qty">
              <el-input-number
                :model-value="item.qty"
                :min="1"
                :max="99"
                size="small"
                @change="(v: number | undefined) => changeQty(item, v ?? 1)"
              />
            </div>

            <div class="item-subtotal">
              <span class="price"
                >¥{{ (item.price * item.qty).toFixed(2) }}</span
              >
            </div>

            <el-button
              text
              type="danger"
              class="item-remove"
              @click="removeItem(item)"
            >
              <el-icon><Delete /></el-icon>&nbsp;删除
            </el-button>
          </div>
        </div>

        <!-- 结算条 -->
        <div class="settle-bar card">
          <div class="settle-info">
            <span
              >共 <b class="total-count">{{ totalCount }}</b> 件商品</span
            >
            <span class="settle-sep">|</span>
            <span>
              合计
              <span class="price total-amount"
                >¥{{ totalAmount.toFixed(2) }}</span
              >
            </span>
          </div>
          <el-button
            type="primary"
            size="large"
            class="btn-primary settle-btn"
            :disabled="!selectedItems.length"
            @click="goCheckout"
          >
            去结算
          </el-button>
        </div>
      </template>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  cartList,
  cartRemove,
  cartRemoveBatch,
  cartUpdateQty,
  cartUpdateSelected,
} from "@/api/cart";
import type { CartItemVO } from "@/api/types";
import { useCartStore } from "@/stores/cart";

const router = useRouter();
const cartStore = useCartStore();

const loading = ref(true);
const items = ref<CartItemVO[]>([]);

const selectedItems = computed(() => items.value.filter((i) => i.selected));
const allSelected = computed(
  () => items.value.length > 0 && items.value.every((i) => i.selected),
);
const indeterminate = computed(
  () =>
    items.value.some((i) => i.selected) &&
    !items.value.every((i) => i.selected),
);

const totalCount = computed(() =>
  selectedItems.value.reduce((s, i) => s + i.qty, 0),
);
const totalAmount = computed(() =>
  selectedItems.value.reduce((s, i) => s + i.price * i.qty, 0),
);

async function load() {
  loading.value = true;
  try {
    items.value = await cartList();
    cartStore.refresh();
  } finally {
    loading.value = false;
  }
}

async function changeQty(item: CartItemVO, qty: number) {
  try {
    await cartUpdateQty(item.cartId, qty);
    item.qty = qty;
    cartStore.refresh();
  } catch {
    // 失败时回滚展示由下一次加载修正
    load();
  }
}

async function removeItem(item: CartItemVO) {
  await cartRemove(item.cartId);
  ElMessage.success("已从购物车移除");
  load();
}

async function toggleOne(item: CartItemVO, v: string | number | boolean) {
  const selected = Boolean(v);
  try {
    await cartUpdateSelected([item.cartId], selected);
    item.selected = selected;
  } catch {
    load();
  }
}

async function toggleAll(v: string | number | boolean) {
  const selected = Boolean(v);
  try {
    await cartUpdateSelected(
      items.value.map((i) => i.cartId),
      selected,
    );
    items.value.forEach((i) => (i.selected = selected));
  } catch {
    load();
  }
}

async function removeBatch() {
  const ids = selectedItems.value.map((i) => i.cartId);
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${ids.length} 件商品吗？`, "提示", {
      type: "warning",
    });
  } catch {
    return;
  }
  await cartRemoveBatch(ids);
  ElMessage.success("已批量删除");
  load();
}

function goCheckout() {
  router.push("/checkout");
}

onMounted(load);
</script>

<style scoped>
.cart-page {
  padding-top: 24px;
  padding-bottom: 96px;
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

.empty-wrap {
  padding: 60px 0;
}

.cart-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  margin-bottom: 14px;
}

.cart-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.cart-item {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 16px 20px;
}

.item-pic {
  width: 88px;
  height: 88px;
  border-radius: var(--radius);
  flex-shrink: 0;
  background: #eef2f7;
  cursor: pointer;
}

.item-info {
  flex: 1;
  min-width: 0;
}

.item-name {
  font-size: 15px;
  line-height: 1.4;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-name:hover {
  color: var(--primary);
}

.item-price {
  margin-top: 8px;
  font-size: 14px;
}

.item-qty {
  flex-shrink: 0;
}

.item-subtotal {
  width: 110px;
  text-align: right;
  flex-shrink: 0;
}

.item-subtotal .price {
  font-size: 17px;
}

.item-remove {
  flex-shrink: 0;
}

/* ---------- 结算条 ---------- */
.settle-bar {
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

.settle-info {
  font-size: 14px;
  color: var(--text-sub);
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.settle-sep {
  color: #e3ded8;
}

.total-count {
  color: var(--primary);
  font-size: 17px;
}

.total-amount {
  font-size: 24px;
}

.settle-btn {
  padding: 0 42px;
  border-radius: var(--radius-sm);
}

/* ---------- 响应式 ---------- */
@media (max-width: 768px) {
  .cart-item {
    flex-wrap: wrap;
    gap: 10px;
    padding: 12px;
  }

  .item-info {
    flex-basis: calc(100% - 106px);
  }

  .item-qty {
    margin-left: auto;
  }

  .item-subtotal {
    width: auto;
    text-align: left;
  }
}
</style>