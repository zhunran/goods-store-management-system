<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="filter-bar">
        <el-input
          v-model="query.name"
          placeholder="活动名称"
          clearable
          style="width: 220px"
          @keyup.enter="load"
        />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <div class="toolbar">
        <el-button
          type="primary"
          @click="openCreate"
          v-permission="'seckill:create'"
        >
          <el-icon><Plus /></el-icon>&nbsp;新增活动
        </el-button>
      </div>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="活动名称" min-width="160" />
        <el-table-column label="开始时间" width="180">
          <template #default="{ row }">{{
            formatTime(row.startTime)
          }}</template>
        </el-table-column>
        <el-table-column label="结束时间" width="180">
          <template #default="{ row }">{{ formatTime(row.endTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">{{
              row.enabled ? "启用" : "停用"
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openGoods(row)">秒杀商品</el-button>
            <el-button
              size="small"
              @click="openEdit(row)"
              v-permission="'seckill:update'"
              >编辑</el-button
            >
            <el-button
              size="small"
              type="danger"
              @click="remove(row)"
              v-permission="'seckill:delete'"
              >删除</el-button
            >
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

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑活动' : '新增活动'"
      width="520px"
      @closed="resetForm"
    >
      <el-form
        :model="form"
        :rules="formRules"
        ref="formRef"
        label-width="90px"
      >
        <el-form-item label="活动名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit"
          >保存</el-button
        >
      </template>
    </el-dialog>

    <el-dialog
      v-model="goodsVisible"
      :title="`秒杀商品 - ${currentActivity?.name || ''}`"
      width="720px"
    >
      <div class="toolbar">
        <el-button type="primary" @click="goodsDialogVisible = true">
          <el-icon><Plus /></el-icon>&nbsp;添加商品
        </el-button>
      </div>
      <el-table :data="goodsList" v-loading="goodsLoading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="商品图" width="80">
          <template #default="{ row }">
            <el-image
              :src="row.goodPic"
              fit="cover"
              style="width: 44px; height: 44px; border-radius: 4px"
            >
              <template #error>
                <div class="img-fallback">暂无图片</div>
              </template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column
          prop="goodName"
          label="商品名称"
          min-width="160"
          show-overflow-tooltip
        />
        <el-table-column prop="originalPrice" label="原价" width="90" />
        <el-table-column prop="seckillPrice" label="秒杀价" width="90" />
        <el-table-column prop="stockCount" label="限量" width="70" />
        <el-table-column prop="stockSold" label="已售" width="70" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button size="small" type="danger" @click="removeGood(row)"
              >移除</el-button
            >
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="goodsDialogVisible" title="添加秒杀商品" width="480px">
      <el-form label-width="90px">
        <el-form-item label="商品">
          <el-select
            v-model="selectedGoodId"
            filterable
            placeholder="搜索商品"
            style="width: 100%"
          >
            <el-option
              v-for="g in goodOptions"
              :key="g.value"
              :label="g.label"
              :value="g.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="秒杀价" required>
          <el-input-number
            v-model="selectedGoodPrice"
            :min="0.01"
            :precision="2"
            :step="1"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="限量库存" required>
          <el-input-number
            v-model="selectedGoodCount"
            :min="1"
            :precision="0"
            :step="1"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="selectedGoodDesc" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="goodsDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="addingGood" @click="addGood"
          >确定</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules,
} from "element-plus";
import { get, post, put, del } from "@/utils/request";
import type { SeckillVO, SeckillGoodVO } from "@/api/seckill";

const list = ref<SeckillVO[]>([]);
const total = ref(0);
const loading = ref(false);
const dialogVisible = ref(false);
const saving = ref(false);
const editingId = ref<number | null>(null);
const formRef = ref<FormInstance>();

const goodsVisible = ref(false);
const goodsList = ref<SeckillGoodVO[]>([]);
const goodsLoading = ref(false);
const currentActivity = ref<SeckillVO | null>(null);

const goodsDialogVisible = ref(false);
const selectedGoodId = ref<number | null>(null);
const selectedGoodDesc = ref("");
const selectedGoodPrice = ref<number>(0.01);
const selectedGoodCount = ref<number>(1);
const addingGood = ref(false);
const goodOptions = ref<{ label: string; value: number }[]>([]);

const query = reactive({ pageNum: 1, pageSize: 10, name: "" });
const form = reactive({
  name: "",
  enabled: true,
  startTime: "",
  endTime: "",
  description: "",
});

const formRules: FormRules = {
  name: [{ required: true, message: "请输入活动名称", trigger: "blur" }],
  startTime: [{ required: true, message: "请选择开始时间", trigger: "change" }],
  endTime: [{ required: true, message: "请选择结束时间", trigger: "change" }],
};

function formatTime(t: string) {
  return t ? t.replace("T", " ").slice(0, 19) : "-";
}

async function load() {
  loading.value = true;
  try {
    const data = await get<{ total: number; records: SeckillVO[] }>(
      "/seckill/admin/activity/page",
      { ...query },
    );
    list.value = data.records;
    total.value = Number(data.total) ?? 0;
  } finally {
    loading.value = false;
  }
}

function reset() {
  query.name = "";
  query.pageNum = 1;
  load();
}

function resetForm() {
  editingId.value = null;
  Object.assign(form, {
    name: "",
    enabled: true,
    startTime: "",
    endTime: "",
    description: "",
  });
}

function openCreate() {
  resetForm();
  dialogVisible.value = true;
}

function openEdit(row: SeckillVO) {
  editingId.value = row.id;
  Object.assign(form, {
    name: row.name,
    enabled: row.enabled,
    startTime: formatTime(row.startTime),
    endTime: formatTime(row.endTime),
    description: row.description,
  });
  dialogVisible.value = true;
}

async function submit() {
  if (!formRef.value) return;
  await formRef.value.validate();
  saving.value = true;
  try {
    if (editingId.value) {
      await put(`/seckill/admin/activity/${editingId.value}`, form);
      ElMessage.success("更新成功");
    } else {
      await post("/seckill/admin/activity", form);
      ElMessage.success("新增成功");
    }
    dialogVisible.value = false;
    load();
  } finally {
    saving.value = false;
  }
}

async function remove(row: SeckillVO) {
  await ElMessageBox.confirm("确定删除该活动吗？", "警告", { type: "warning" });
  await del(`/seckill/admin/activity/${row.id}`);
  ElMessage.success("删除成功");
  load();
}

async function openGoods(row: SeckillVO) {
  currentActivity.value = row;
  goodsVisible.value = true;
  goodsLoading.value = true;
  try {
    goodsList.value = await get<SeckillGoodVO[]>(
      `/seckill/admin/activity/${row.id}/goods`,
    );
  } finally {
    goodsLoading.value = false;
  }
}

async function addGood() {
  if (!currentActivity.value || !selectedGoodId.value) {
    ElMessage.warning("请选择商品");
    return;
  }
  if (!selectedGoodPrice.value || selectedGoodPrice.value <= 0) {
    ElMessage.warning("请输入秒杀价");
    return;
  }
  if (!selectedGoodCount.value || selectedGoodCount.value < 1) {
    ElMessage.warning("请输入限量库存");
    return;
  }
  addingGood.value = true;
  try {
    await post(`/seckill/admin/activity/${currentActivity.value.id}/goods`, {
      goodId: selectedGoodId.value,
      seckillPrice: selectedGoodPrice.value,
      stockCount: selectedGoodCount.value,
      description: selectedGoodDesc.value,
    });
    ElMessage.success("添加成功");
    goodsDialogVisible.value = false;
    selectedGoodId.value = null;
    selectedGoodDesc.value = "";
    selectedGoodPrice.value = 0.01;
    selectedGoodCount.value = 1;
    openGoods(currentActivity.value);
  } finally {
    addingGood.value = false;
  }
}

async function removeGood(row: SeckillGoodVO) {
  await ElMessageBox.confirm("确定移除该秒杀商品吗？", "提示", {
    type: "warning",
  });
  await del(`/seckill/admin/activity/goods/${row.id}`);
  ElMessage.success("移除成功");
  openGoods(currentActivity.value!);
}

async function loadGoodOptions() {
  const goods = await get<any[]>("/product/list", {
    pageNum: 1,
    pageSize: 100,
  });
  goodOptions.value = (goods || []).map((g: any) => ({
    label: g.name,
    value: g.id,
  }));
}

onMounted(() => {
  load();
  loadGoodOptions();
});
</script>
