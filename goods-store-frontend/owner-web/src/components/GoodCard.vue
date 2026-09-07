<template>
  <div class="good-card card card-hover" @click="goDetail">
    <div class="pic-wrap">
      <el-image :src="good.pic" fit="cover" class="pic" lazy>
        <template #error>
          <div class="img-fallback">
            <el-icon :size="28"><Picture /></el-icon>
          </div>
        </template>
      </el-image>
      <span v-if="good.isHot" class="hot-tag">热销</span>
      <span v-if="good.isSeckill" class="seckill-tag">秒杀</span>
    </div>

    <div class="info">
      <p class="name" :title="good.name">{{ good.name }}</p>
      <p class="brand" v-if="good.brandName">{{ good.brandName }}</p>
      <div class="bottom">
        <div class="price-wrap">
          <span class="price">
            <span class="price-symbol">¥</span>{{ good.price }}
          </span>
          <span
            v-if="good.markPrice && good.markPrice > good.price"
            class="price-origin"
          >
            ¥{{ good.markPrice }}
          </span>
        </div>
        <button class="add-cart-btn" title="加入购物车" @click.stop="addCart">
          <el-icon :size="16"><Plus /></el-icon>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import { useUserStore } from "@/stores/user";
import { useCartStore } from "@/stores/cart";
import { cartAdd } from "@/api/cart";
import type { GoodVO } from "@/api/types";

const props = defineProps<{ good: GoodVO }>();

const router = useRouter();
const userStore = useUserStore();
const cartStore = useCartStore();

function goDetail() {
  router.push(`/product/${props.good.id}`);
}

async function addCart() {
  if (!userStore.isLogin) {
    ElMessage.warning("先登录，再来加购心仪好物吧～");
    router.push({
      path: "/login",
      query: { redirect: router.currentRoute.value.fullPath },
    });
    return;
  }
  await cartAdd(props.good.id, 1);
  cartStore.refresh();
  ElMessage.success("已加入购物车");
}
</script>

<style scoped>
.good-card {
  cursor: pointer;
  overflow: hidden;
  border-radius: var(--radius);
}

.pic-wrap {
  position: relative;
  aspect-ratio: 1;
  overflow: hidden;
  background: #eef2f7;
}

.pic {
  width: 100%;
  height: 100%;
  transition: transform 0.4s var(--ease);
}

.good-card:hover .pic {
  transform: scale(1.06);
}

.hot-tag,
.seckill-tag {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 2px 10px;
  border-radius: 2px;
  font-size: 12px;
  color: #fff;
  font-weight: 600;
  letter-spacing: 1px;
}

.hot-tag {
  background: var(--primary-gradient);
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.4);
}

.seckill-tag {
  left: auto;
  right: 10px;
  background: var(--accent-hot-gradient);
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.4);
}

.info {
  padding: 12px 14px 14px;
}

.name {
  font-size: 14px;
  line-height: 1.5;
  height: 42px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.brand {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-light);
}

.bottom {
  margin-top: 10px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
}

.price-wrap {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
}

.price {
  font-size: 20px;
}

.add-cart-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  color: var(--primary);
  background: #fff;
  cursor: pointer;
  transition:
    background-color 0.25s var(--ease),
    color 0.25s var(--ease),
    border-color 0.25s var(--ease),
    box-shadow 0.25s var(--ease);
}

.add-cart-btn:hover {
  color: #fff;
  background: var(--primary);
  border-color: var(--primary);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.35);
}

.add-cart-btn:active {
  transform: scale(0.92);
}
</style>
