<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="filter-bar">
        <el-input v-model="query.name" placeholder="商品名称/关键词" clearable style="width: 220px" @keyup.enter="load" />
        <el-select v-model="query.categoryId" placeholder="分类" clearable style="width: 180px">
          <el-option v-for="c in categoryOptions" :key="c.value" :label="c.label" :value="c.value" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <div class="toolbar">
        <el-button type="primary" @click="openCreate" v-permission="'good:create'">
          <el-icon><Plus /></el-icon>&nbsp;新增商品
        </el-button>
      </div>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="主图" width="90">
          <template #default="{ row }">
            <el-image :src="row.pic" fit="cover" style="width: 50px; height: 50px; border-radius: 4px">
              <template #error>
                <div class="img-fallback">暂无图片</div>
              </template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="商品名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="brandName" label="品牌" width="100" />
        <el-table-column prop="categoryName" label="分类" width="110" />
        <el-table-column prop="price" label="售价" width="100">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column prop="qty" label="库存" width="80" />
        <el-table-column label="热销" width="70">
          <template #default="{ row }">
            <el-tag :type="row.isHot ? 'danger' : 'info'" size="small">{{ row.isHot ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)" v-permission="'good:update'">编辑</el-button>
            <el-button size="small" :type="row.isDel ? 'success' : 'warning'" @click="toggleStatus(row)" v-permission="'good:update'">
              {{ row.isDel ? '上架' : '下架' }}
            </el-button>
            <el-button size="small" type="danger" @click="remove(row)" v-permission="'good:delete'">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="load"
          @size-change="load"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑商品' : '新增商品'" width="640px" @closed="resetForm">
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="90px">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" placeholder="选择分类" style="width: 100%">
            <el-option v-for="c in categoryOptions" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="品牌">
          <el-select v-model="form.brandId" placeholder="选择品牌" style="width: 100%">
            <el-option v-for="b in brandOptions" :key="b.value" :label="b.label" :value="b.value" />
          </el-select>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="售价" prop="price">
              <el-input-number v-model="form.price" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="划线价">
              <el-input-number v-model="form.markPrice" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="库存">
          <el-input-number v-model="form.qty" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="主图 URL">
          <el-input v-model="form.pic" placeholder="http://..." />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="form.summary" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="热销">
          <el-switch v-model="form.isHot" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { get, post, put, del } from '@/utils/request'
import type { GoodVO, CategoryTreeVO } from '@/api/product'

const list = ref<GoodVO[]>([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const categoryOptions = ref<{ label: string; value: number }[]>([])
const brandOptions = ref<{ label: string; value: number }[]>([])

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  name: '',
  categoryId: undefined as number | undefined
})

const form = reactive({
  name: '',
  categoryId: undefined as number | undefined,
  brandId: undefined as number | undefined,
  price: 0,
  markPrice: 0,
  qty: 0,
  pic: '',
  summary: '',
  isHot: false
})

const formRules: FormRules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  price: [{ required: true, message: '请输入售价', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const params: any = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.name) params.name = query.name
    if (query.categoryId) params.categoryId = query.categoryId
    const data = await get<GoodVO[]>('/product/list', params)
    list.value = data
    total.value = data.length
  } finally {
    loading.value = false
  }
}

function flattenTree(nodes: CategoryTreeVO[], depth = 0): { label: string; value: number }[] {
  const res: { label: string; value: number }[] = []
  for (const n of nodes) {
    res.push({ label: `${'　'.repeat(depth)}${n.name}`, value: n.id })
    if (n.children?.length) res.push(...flattenTree(n.children, depth + 1))
  }
  return res
}

async function loadOptions() {
  const tree = await get<CategoryTreeVO[]>('/product/tree')
  categoryOptions.value = flattenTree(tree)
  const brands = await get<{ records: any[] }>('/brand/admin/page', { pageNum: 1, pageSize: 100 })
  brandOptions.value = (brands.records || []).map((b: any) => ({ label: b.name, value: b.id }))
}

function reset() {
  query.name = ''
  query.categoryId = undefined
  query.pageNum = 1
  load()
}

function resetForm() {
  editingId.value = null
  Object.assign(form, {
    name: '', categoryId: undefined, brandId: undefined, price: 0,
    markPrice: 0, qty: 0, pic: '', summary: '', isHot: false
  })
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: GoodVO) {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name, categoryId: row.categoryId, brandId: row.brandId,
    price: row.price, markPrice: row.markPrice, qty: row.qty,
    pic: row.pic, summary: row.summary, isHot: row.isHot
  })
  dialogVisible.value = true
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate()
  saving.value = true
  try {
    if (editingId.value) {
      await put(`/product/admin/${editingId.value}`, { ...form, id: editingId.value })
      ElMessage.success('更新成功')
    } else {
      await post('/product/admin', form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: GoodVO) {
  await ElMessageBox.confirm(`确定${row.isDel ? '上架' : '下架'}该商品吗？`, '提示', { type: 'warning' })
  await put(`/product/admin/${row.id}/status`, null, { takeDown: !row.isDel })
  ElMessage.success('操作成功')
  load()
}

async function remove(row: GoodVO) {
  await ElMessageBox.confirm('删除后不可恢复，确定删除该商品吗？', '警告', { type: 'warning' })
  await del(`/product/admin/${row.id}`)
  ElMessage.success('删除成功')
  load()
}

onMounted(() => {
  load()
  loadOptions()
})
</script>
