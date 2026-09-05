<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="filter-bar">
        <el-input
          v-model="query.name"
          placeholder="品牌名称"
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
          v-permission="'brand:create'"
        >
          <el-icon><Plus /></el-icon>&nbsp;新增品牌
        </el-button>
      </div>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="Logo" width="90">
          <template #default="{ row }">
            <el-image
              :src="row.logo"
              fit="cover"
              style="width: 50px; height: 50px; border-radius: 4px"
            >
              <template #error>
                <div class="img-fallback">暂无图片</div>
              </template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="品牌名称" min-width="120" />
        <el-table-column
          prop="company"
          label="公司"
          min-width="140"
          show-overflow-tooltip
        />
        <el-table-column
          prop="site"
          label="官网"
          min-width="160"
          show-overflow-tooltip
        />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              @click="openEdit(row)"
              v-permission="'brand:update'"
              >编辑</el-button
            >
            <el-button
              size="small"
              type="danger"
              @click="remove(row)"
              v-permission="'brand:delete'"
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
      :title="editingId ? '编辑品牌' : '新增品牌'"
      width="520px"
      @closed="resetForm"
    >
      <el-form
        :model="form"
        :rules="formRules"
        ref="formRef"
        label-width="80px"
      >
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="公司">
          <el-input v-model="form.company" />
        </el-form-item>
        <el-form-item label="Logo URL">
          <el-input v-model="form.logo" placeholder="http://..." />
        </el-form-item>
        <el-form-item label="官网">
          <el-input v-model="form.site" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit"
          >保存</el-button
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
import type { BrandVO } from "@/api/brand";

const list = ref<BrandVO[]>([]);
const total = ref(0);
const loading = ref(false);
const dialogVisible = ref(false);
const saving = ref(false);
const editingId = ref<number | null>(null);
const formRef = ref<FormInstance>();

const query = reactive({ pageNum: 1, pageSize: 10, name: "" });
const form = reactive({
  name: "",
  company: "",
  logo: "",
  site: "",
  description: "",
});

const formRules: FormRules = {
  name: [{ required: true, message: "请输入品牌名称", trigger: "blur" }],
};

async function load() {
  loading.value = true;
  try {
    const data = await get<{ total: number; records: BrandVO[] }>(
      "/brand/admin/page",
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
    company: "",
    logo: "",
    site: "",
    description: "",
  });
}

function openCreate() {
  resetForm();
  dialogVisible.value = true;
}

function openEdit(row: BrandVO) {
  editingId.value = row.id;
  Object.assign(form, {
    name: row.name,
    company: row.company,
    logo: row.logo,
    site: row.site,
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
      await put(`/brand/admin/${editingId.value}`, form);
      ElMessage.success("更新成功");
    } else {
      await post("/brand/admin", form);
      ElMessage.success("新增成功");
    }
    dialogVisible.value = false;
    load();
  } finally {
    saving.value = false;
  }
}

async function remove(row: BrandVO) {
  await ElMessageBox.confirm("确定删除该品牌吗？", "警告", { type: "warning" });
  await del(`/brand/admin/${row.id}`);
  ElMessage.success("删除成功");
  load();
}

onMounted(load);
</script>
