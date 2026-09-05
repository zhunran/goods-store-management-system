<template>
  <div class="product-page content-wrap">
    <!-- 筛选条 -->
    <div class="filter-bar card">
      <div class="filter-row">
        <span class="filter-label">关键词</span>
        <el-input
          v-model="keywordInput"
          placeholder="搜一搜心仪好物"
          clearable
          class="keyword-input"
          @keyup.enter="applySearch"
          @clear="applySearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <span class="filter-label">价格区间</span>
        <div class="price-range">
          <el-input-number
            v-model="minPriceInput"
            :min="0"
            :controls="false"
            placeholder="最低"
            class="price-input"
          />
          <span class="range-sep">—</span>
          <el-input-number
            v-model="maxPriceInput"
            :min="0"
            :controls="false"
            placeholder="最高"
            class="price-input"
          />
        </div>
        <el-button class="btn-primary" type="primary" @click="applySearch">
          <el-icon><Search /></el-icon>&nbsp;筛选
        </el-button>
        <el-button
          v-if="hasFilter"
          text
          @click="resetFilter"
        >
          重置
        </el-button>
      </div>
      <!-- 品牌筛选 -->
      <div class="filter-row" v-if="brands.length">
        <span class="filter-label">品牌</span>
        <div class="brand-chips">
          <span
            class="brand-chip"
            :class="{ active: !query.brandId }"
            @click="selectBrand(undefined)"
          >
            全部
          </span>
          <span
            v-for="b in brands"
            :key="b.id"
            class="brand-chip"
            :class="{ active: query.brandId === Number(b.id) }"
            :title="b.name"
            @click="selectBrand(Number(b.id))"
          >
            {{ b.name }}
          </span>
        </div>
      </div>
    </div>

    <div class="main-area">
      <!-- 分类侧栏 -->
      <aside class="category-panel card">
        <div class="category-head">全部分类</div>
        <div
          class="category-item"
          :class="{ active: !query.categoryId }"
          @click="selectCategory(undefined)"
        >
          全部商品
        </div>
        <template v-for="c in categories" :key="c.id">
          <div
            class="category-item"
            :class="{ active: query.categoryId === Number(c.id) }"
            @click="selectCategory(Number(c.id))"
          >
            <span class="category-name" :title="c.name">{{ c.name }}</span>
            <el-icon
              v-if="c.children && c.children.length"
              class="expand-icon"
              :class="{ expanded: expandedMap[c.id] }"
              @click.stop="toggleExpand(c.id)"
            >
              <ArrowRight />
            </el-icon>
          </div>
          <div v-if="expandedMap[c.id] && c.children?.length" class="sub-category-list">
            <div
              v-for="sub in c.children"
              :key="sub.id"
              class="category-item sub"
              :class="{ active: query.categoryId === Number(sub.id) }"
              @click="selectCategory(Number(sub.id))"
            >
              {{ sub.name }}
            </div>
          </div>
        </template>
      </aside>

      <!-- 商品网格 -->
      <div class="result-area">
        <div v-if="loading" class="good-grid">
          <div v-for="i in 8" :key="i" class="skeleton-card card">
            <div class="skeleton-line" style="aspect-ratio: 1; border-radius: 10px"></div>
            <div class="skeleton-line" style="margin-top: 12px"></div>
            <div class="skeleton-line" style="width: 60%; margin-top: 8px"></div>
            <div class="skeleton-line" style="width: 40%; margin-top: 10px"></div>
          </div>
        </div>
        <template v-else>
          <div v-if="list.length" class="good-grid">
            <GoodCard v-for="g in list" :key="g.id" :good="g" />
          </div>
          <el-empty v-else description="没有找到相关商品，换个条件试试吧～" />
        </template>

        <div v-if="!loading && list.length" class="pager-wrap">
          <el-pagination
            v-model:current-page="query.pageNum"
            :page-size="query.pageSize"
            :total="inferredTotal"
            layout="prev, pager, next, jumper"
            background
            hide-on-single-page
            @current-change="loadList"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import GoodCard from '@/components/GoodCard.vue'
import { brandList, categoryTree, goodList } from '@/api/product'
import type { BrandVO, CategoryTreeVO, GoodQuery, GoodVO } from '@/api/types'

const route = useRoute()

const loading = ref(false)
const list = ref<GoodVO[]>([])
const brands = ref<BrandVO[]>([])
const categories = ref<CategoryTreeVO[]>([])
const expandedMap = reactive<Record<string, boolean>>({})

// 查询条件（生效值）
const query = reactive<Required<Pick<GoodQuery, 'pageNum' | 'pageSize'>> & GoodQuery>({
  pageNum: 1,
  pageSize: 12,
  keyword: '',
  categoryId: undefined,
  brandId: undefined,
  minPrice: undefined,
  maxPrice: undefined,
})

// 输入框缓冲值（点击筛选才生效）
const keywordInput = ref('')
const minPriceInput = ref<number | undefined>(undefined)
const maxPriceInput = ref<number | undefined>(undefined)

const hasFilter = computed(
  () =>
    !!query.keyword ||
    query.categoryId !== undefined ||
    query.brandId !== undefined ||
    query.minPrice !== undefined ||
    query.maxPrice !== undefined,
)

/**
 * 后端返回 List<GoodVO> 而非分页对象（无 total）：
 * 当本页满页时 +1 表示可能还有下一页可点；不足一页则为真实总数。
 */
const inferredTotal = computed(() => {
  const loaded = (query.pageNum - 1) * query.pageSize + list.value.length
  return list.value.length === query.pageSize ? loaded + 1 : loaded
})

function toggleExpand(id: string) {
  expandedMap[id] = !expandedMap[id]
}

function applySearch() {
  query.keyword = keywordInput.value.trim() || undefined
  query.minPrice = minPriceInput.value ?? undefined
  query.maxPrice = maxPriceInput.value ?? undefined
  if (
    query.minPrice !== undefined &&
    query.maxPrice !== undefined &&
    query.minPrice > query.maxPrice
  ) {
    ;[query.minPrice, query.maxPrice] = [query.maxPrice, query.minPrice]
    ;[minPriceInput.value, maxPriceInput.value] = [maxPriceInput.value, minPriceInput.value]
  }
  reload()
}

function selectBrand(brandId: number | undefined) {
  query.brandId = brandId
  reload()
}

function selectCategory(categoryId: number | undefined) {
  query.categoryId = categoryId
  reload()
}

function resetFilter() {
  keywordInput.value = ''
  minPriceInput.value = undefined
  maxPriceInput.value = undefined
  query.keyword = undefined
  query.brandId = undefined
  query.categoryId = undefined
  query.minPrice = undefined
  query.maxPrice = undefined
  reload()
}

function reload() {
  query.pageNum = 1
  loadList()
}

async function loadList() {
  loading.value = true
  try {
    list.value = await goodList({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword,
      categoryId: query.categoryId,
      brandId: query.brandId,
      minPrice: query.minPrice,
      maxPrice: query.maxPrice,
    })
  } finally {
    loading.value = false
  }
}

/** 从路由 query 同步筛选（首页品牌街/搜索跳转入口） */
function syncFromRoute() {
  const { keyword, categoryId, brandId } = route.query
  keywordInput.value = (keyword as string) || ''
  query.keyword = (keyword as string) || undefined
  query.categoryId = categoryId ? Number(categoryId) : undefined
  query.brandId = brandId ? Number(brandId) : undefined
}

watch(
  () => route.query,
  () => {
    if (route.name === 'product') {
      syncFromRoute()
      reload()
    }
  },
)

onMounted(async () => {
  syncFromRoute()
  const [brandRes, treeRes] = await Promise.all([brandList(), categoryTree()])
  brands.value = brandRes
  categories.value = treeRes
  loadList()
})
</script>

<style scoped>
.product-page {
  padding-top: 20px;
  padding-bottom: 48px;
}

/* ---------- 筛选条 ---------- */
.filter-bar {
  padding: 18px 22px;
  margin-bottom: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-sub);
  flex-shrink: 0;
}

.keyword-input {
  width: 240px;
}

.price-range {
  display: flex;
  align-items: center;
  gap: 8px;
}

.price-input {
  width: 110px;
}

.range-sep {
  color: var(--text-light);
}

.brand-chips {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  flex: 1;
}

.brand-chip {
  padding: 5px 14px;
  border-radius: 999px;
  font-size: 13px;
  background: #f5f2ee;
  color: var(--text-sub);
  cursor: pointer;
  transition: all 0.2s;
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.brand-chip:hover {
  background: var(--primary-light);
  color: var(--primary);
}

.brand-chip.active {
  background: var(--primary-gradient);
  color: #fff;
  font-weight: 600;
}

/* ---------- 主体 ---------- */
.main-area {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.category-panel {
  width: 200px;
  flex-shrink: 0;
  padding: 14px 10px;
  position: sticky;
  top: 80px;
}

.category-head {
  padding: 4px 10px 12px;
  font-size: 15px;
  font-weight: 700;
  border-bottom: 1px solid #f0ede9;
  margin-bottom: 8px;
}

.category-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px 10px;
  border-radius: var(--radius-sm);
  font-size: 14px;
  color: var(--text-main);
  cursor: pointer;
  transition: all 0.2s;
}

.category-item:hover {
  background: var(--primary-light);
  color: var(--primary);
}

.category-item.active {
  background: var(--primary-light);
  color: var(--primary);
  font-weight: 600;
}

.category-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.expand-icon {
  flex-shrink: 0;
  color: var(--text-light);
  transition: transform 0.2s;
  padding: 2px;
}

.expand-icon.expanded {
  transform: rotate(90deg);
}

.sub-category-list {
  padding-left: 12px;
}

.category-item.sub {
  font-size: 13px;
  color: var(--text-sub);
}

.result-area {
  flex: 1;
  min-width: 0;
}

.good-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.skeleton-card {
  padding: 12px;
}

.pager-wrap {
  margin-top: 32px;
  display: flex;
  justify-content: center;
}

/* ---------- 响应式 ---------- */
@media (max-width: 768px) {
  .main-area {
    flex-direction: column;
  }

  .category-panel {
    width: 100%;
    position: static;
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }

  .category-head {
    width: 100%;
    border-bottom: none;
    margin-bottom: 0;
  }

  .category-item {
    padding: 6px 12px;
    background: #f5f2ee;
    border-radius: 999px;
  }

  .sub-category-list {
    display: contents;
  }

  .keyword-input {
    width: 100%;
  }

  .filter-row {
    gap: 8px;
  }
}
</style>
