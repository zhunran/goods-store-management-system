<template>
  <div class="detail-page content-wrap">
    <!-- 加载骨架 -->
    <template v-if="loading">
      <div class="detail-card card">
        <div
          class="skeleton-line"
          style="width: 400px; height: 400px; border-radius: 6px"
        ></div>
        <div class="skeleton-info">
          <div class="skeleton-line" style="width: 70%; height: 26px"></div>
          <div class="skeleton-line" style="width: 40%; margin-top: 16px"></div>
          <div
            class="skeleton-line"
            style="width: 90%; height: 60px; margin-top: 24px"
          ></div>
          <div class="skeleton-line" style="width: 30%; margin-top: 24px"></div>
        </div>
      </div>
    </template>

    <template v-else-if="good">
      <!-- 面包屑 -->
      <el-breadcrumb class="crumb" separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/product' }"
          >全部商品</el-breadcrumb-item
        >
        <el-breadcrumb-item v-if="good.categoryName">{{
          good.categoryName
        }}</el-breadcrumb-item>
        <el-breadcrumb-item>{{ good.name }}</el-breadcrumb-item>
      </el-breadcrumb>

      <!-- 主信息卡 -->
      <div class="detail-card card">
        <div class="main-pic-wrap">
          <el-image :src="good.pic" fit="cover" class="main-pic">
            <template #error>
              <div class="img-fallback" style="width: 100%; height: 100%">
                <el-icon :size="40"><Picture /></el-icon>
              </div>
            </template>
          </el-image>
          <span v-if="good.isHot" class="hot-tag">热销</span>
          <span v-if="good.isSeckill" class="seckill-tag">秒杀进行中</span>
        </div>

        <div class="info-area">
          <h1 class="good-title">{{ good.name }}</h1>
          <p v-if="good.alias || good.summary" class="good-sub">
            {{ good.alias || good.summary }}
          </p>

          <div class="price-panel">
            <span class="price big">
              <span class="price-symbol">¥</span>{{ good.price }}
            </span>
            <span
              v-if="good.markPrice && good.markPrice > good.price"
              class="price-origin"
            >
              ¥{{ good.markPrice }}
            </span>
          </div>

          <div class="meta-panel">
            <div class="meta-item">
              <span class="meta-label">品牌</span>
              <span>{{ good.brandName || "—" }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">分类</span>
              <span>{{ good.categoryName || "—" }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">库存</span>
              <span :class="{ 'qty-warn': good.qty <= 5 }">
                {{ good.qty > 0 ? `仅剩 ${good.qty} 件` : "暂时缺货" }}
              </span>
            </div>
            <div class="meta-item">
              <span class="meta-label">编号</span>
              <span>{{ good.spuNo || good.id }}</span>
            </div>
          </div>

          <div class="buy-panel">
            <div class="qty-picker">
              <span class="qty-label">数量</span>
              <el-input-number
                v-model="qty"
                :min="1"
                :max="Math.max(1, good.qty)"
              />
            </div>
            <div class="btn-group">
              <el-button
                size="large"
                class="btn-primary"
                type="primary"
                :disabled="good.qty <= 0"
                @click="addCart"
              >
                <el-icon><ShoppingCart /></el-icon>&nbsp;加入购物车
              </el-button>
              <el-button
                size="large"
                class="btn-buy"
                :disabled="good.qty <= 0"
                @click="buyNow"
              >
                立即购买
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 详情图 -->
      <div class="detail-pics card" v-if="good.detailPicList?.length">
        <div class="pics-head">商品详情</div>
        <el-image
          v-for="(pic, i) in good.detailPicList"
          :key="i"
          :src="pic"
          fit="contain"
          class="detail-pic"
          lazy
        >
          <template #error>
            <div class="img-fallback" style="width: 100%; height: 200px">
              <el-icon :size="32"><Picture /></el-icon>
            </div>
          </template>
        </el-image>
      </div>
    </template>

    <el-empty v-else description="商品不存在或已下架">
      <el-button
        type="primary"
        class="btn-primary"
        @click="$router.push('/product')"
      >
        去逛逛别的
      </el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { goodDetail } from "@/api/product";
import { cartAdd } from "@/api/cart";
import type { GoodVO } from "@/api/types";
import { useUserStore } from "@/stores/user";
import { useCartStore } from "@/stores/cart";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const cartStore = useCartStore();

const loading = ref(true);
const good = ref<GoodVO | null>(null);
const qty = ref(1);

async function loadDetail(id: string) {
  loading.value = true;
  qty.value = 1;
  try {
    good.value = await goodDetail(id);
  } catch {
    good.value = null;
  } finally {
    loading.value = false;
  }
}

function ensureLogin(): boolean {
  if (!userStore.isLogin) {
    ElMessage.warning("先登录，再来加购心仪好物吧～");
    router.push({ path: "/login", query: { redirect: route.fullPath } });
    return false;
  }
  return true;
}

async function addCart() {
  if (!ensureLogin() || !good.value) return;
  await cartAdd(good.value.id, qty.value);
  cartStore.refresh();
  ElMessage.success(`已将 ${qty.value} 件加入购物车`);
}

/** 立即购买：加购后直接进入结算页 */
async function buyNow() {
  if (!ensureLogin() || !good.value) return;
  await cartAdd(good.value.id, qty.value);
  cartStore.refresh();
  router.push("/checkout");
}

onMounted(() => loadDetail(route.params.id as string));

watch(
  () => route.params.id,
  (id) => {
    if (id && route.name === "productDetail") loadDetail(id as string);
  },
);
</script>

<style scoped>
.detail-page {
  padding-top: 20px;
  padding-bottom: 48px;
}

.crumb {
  margin-bottom: 16px;
}

.detail-card {
  display: flex;
  gap: 36px;
  padding: 28px;
}

.main-pic-wrap {
  position: relative;
  flex-shrink: 0;
}

.main-pic {
  width: 400px;
  height: 400px;
  border-radius: var(--radius);
  background: #eef2f7;
}

.hot-tag,
.seckill-tag {
  position: absolute;
  top: 12px;
  padding: 3px 12px;
  border-radius: 2px;
  font-size: 12px;
  color: #fff;
  font-weight: 600;
  letter-spacing: 1px;
}

.hot-tag {
  left: 12px;
  background: var(--primary-gradient);
}

.seckill-tag {
  right: 12px;
  background: var(--accent-hot-gradient);
}

.skeleton-info {
  flex: 1;
  padding-top: 8px;
}

.info-area {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.good-title {
  font-size: 22px;
  line-height: 1.4;
  font-weight: 700;
}

.good-sub {
  margin-top: 8px;
  font-size: 13px;
  color: var(--text-sub);
  line-height: 1.6;
}

.price-panel {
  margin-top: 20px;
  padding: 18px 20px;
  border-radius: var(--radius);
  background: linear-gradient(135deg, #eef4ff 0%, #e2ecff 100%);
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.price.big {
  font-size: 32px;
}

.meta-panel {
  margin-top: 20px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 24px;
  font-size: 13px;
  color: var(--text-sub);
}

.meta-item {
  display: flex;
  gap: 12px;
}

.meta-label {
  color: var(--text-light);
  flex-shrink: 0;
}

.qty-warn {
  color: #ef4444;
  font-weight: 600;
}

.buy-panel {
  margin-top: auto;
  padding-top: 24px;
}

.qty-picker {
  display: flex;
  align-items: center;
  gap: 16px;
}

.qty-label {
  font-size: 13px;
  color: var(--text-sub);
}

.btn-group {
  margin-top: 20px;
  display: flex;
  gap: 14px;
}

.btn-primary {
  background: var(--primary-gradient);
  border: none;
  padding: 0 28px;
}

.btn-primary:hover:not(.is-disabled) {
  background: var(--primary-hover);
}

.btn-buy {
  padding: 0 28px;
  border: 1.5px solid var(--primary);
  color: var(--primary);
  background: transparent;
  transition: all 0.25s;
}

.btn-buy:hover:not(.is-disabled) {
  background: var(--primary-light);
}

/* ---------- 详情图 ---------- */
.detail-pics {
  margin-top: 24px;
  padding: 28px;
}

.pics-head {
  font-size: 17px;
  font-weight: 700;
  padding-bottom: 16px;
  margin-bottom: 20px;
  border-bottom: 1px solid #e5eaf2;
  position: relative;
  padding-left: 14px;
}

.pics-head::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 16px;
  border-radius: 0;
  background: var(--primary-gradient);
}

.detail-pic {
  width: 100%;
  display: block;
}

/* ---------- 响应式 ---------- */
@media (max-width: 768px) {
  .detail-card {
    flex-direction: column;
    padding: 16px;
    gap: 20px;
  }

  .main-pic {
    width: 100%;
    height: auto;
    aspect-ratio: 1;
  }

  .meta-panel {
    grid-template-columns: 1fr;
  }

  .btn-group {
    flex-direction: column;
  }
}
</style>
