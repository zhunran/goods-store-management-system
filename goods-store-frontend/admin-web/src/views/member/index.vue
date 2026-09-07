<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="filter-bar">
        <el-input
          v-model="query.keyword"
          placeholder="账号/姓名"
          clearable
          style="width: 220px"
          @keyup.enter="load"
        />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="头像" width="80">
          <template #default="{ row }">
            <el-avatar :src="row.portrait" :size="40" />
          </template>
        </el-table-column>
        <el-table-column prop="account" label="账号" min-width="130" />
        <el-table-column prop="name" label="姓名" min-width="100" />
        <el-table-column prop="sex" label="性别" width="70" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column
          prop="email"
          label="邮箱"
          min-width="160"
          show-overflow-tooltip
        />
        <el-table-column prop="birthday" label="生日" width="110" />
        <el-table-column label="注册时间" width="180">
          <template #default="{ row }">{{
            formatTime(row.createdTime)
          }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-switch
              :model-value="!!row.enabled"
              :loading="!!toggling[row.id]"
              @change="
                (val: string | number | boolean) => onToggleEnabled(row, val)
              "
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)"
              >编辑</el-button
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
      v-model="editVisible"
      title="编辑会员"
      width="520px"
      destroy-on-close
    >
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="姓名">
          <el-input v-model="editForm.name" maxlength="20" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="editForm.sex">
            <el-radio value="男">男</el-radio>
            <el-radio value="女">女</el-radio>
            <el-radio value="保密">保密</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="生日">
          <el-date-picker
            v-model="editForm.birthday"
            type="date"
            placeholder="选择生日"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="editForm.phone" maxlength="11" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit"
          >保存</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { get, put } from "@/utils/request";
import type { MemberVO } from "@/api/member";

const list = ref<MemberVO[]>([]);
const total = ref(0);
const loading = ref(false);

const query = reactive({ pageNum: 1, pageSize: 10, keyword: "" });
const toggling = reactive<Record<number, boolean>>({});

function formatTime(t: string) {
  return t ? t.replace("T", " ").slice(0, 19) : "-";
}

async function load() {
  loading.value = true;
  try {
    const data = await get<{ total: number; records: MemberVO[] }>(
      "/member/admin/page",
      { ...query },
    );
    list.value = data.records;
    total.value = Number(data.total) ?? 0;
  } finally {
    loading.value = false;
  }
}

function reset() {
  query.keyword = "";
  query.pageNum = 1;
  load();
}

async function onToggleEnabled(row: MemberVO, val: string | number | boolean) {
  const enabled = Boolean(val);
  const action = enabled ? "启用" : "禁用";
  try {
    await ElMessageBox.confirm(
      `确定${action}会员「${row.account}」吗？`,
      "提示",
      { type: "warning" },
    );
  } catch {
    return;
  }
  toggling[row.id] = true;
  try {
    await put(`/member/admin/${row.id}/enabled`, null, { enabled });
    row.enabled = enabled;
    ElMessage.success(`${action}成功`);
  } finally {
    toggling[row.id] = false;
  }
}

const editVisible = ref(false);
const saving = ref(false);
const editForm = reactive({
  id: 0,
  name: "",
  sex: "保密",
  birthday: "",
  phone: "",
  email: "",
});

function openEdit(row: MemberVO) {
  editForm.id = row.id;
  editForm.name = row.name ?? "";
  editForm.sex = row.sex ?? "保密";
  editForm.birthday = row.birthday ? row.birthday.slice(0, 10) : "";
  editForm.phone = row.phone ?? "";
  editForm.email = row.email ?? "";
  editVisible.value = true;
}

async function saveEdit() {
  saving.value = true;
  try {
    await put(`/member/admin/${editForm.id}`, {
      name: editForm.name?.trim() || undefined,
      sex: editForm.sex,
      birthday: editForm.birthday || undefined,
      phone: editForm.phone?.trim() || undefined,
      email: editForm.email?.trim() || undefined,
    });
    ElMessage.success("已保存");
    editVisible.value = false;
    load();
  } finally {
    saving.value = false;
  }
}

onMounted(load);
</script>
