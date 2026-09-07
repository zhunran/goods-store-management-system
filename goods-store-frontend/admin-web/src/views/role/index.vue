<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>角色列表</template>
      <el-table :data="roles" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="code" label="角色编码" min-width="160" />
        <el-table-column prop="name" label="角色名称" min-width="140" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openAssign(row)"
              >分配权限</el-button
            >
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="assignVisible"
      :title="`分配权限 - ${currentRole?.name ?? ''}`"
      width="620px"
      destroy-on-close
    >
      <el-transfer
        v-model="assignedIds"
        :data="permOptions"
        :titles="['未授权', '已授权']"
        filterable
        filter-placeholder="搜索权限"
      />
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveAssign"
          >保存</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import {
  listRoles,
  listPermissions,
  listRolePermissions,
  assignRolePermissions,
  type RoleVO,
  type PermissionVO,
} from "@/api/role";

const roles = ref<RoleVO[]>([]);
const allPermissions = ref<PermissionVO[]>([]);
const loading = ref(false);

const assignVisible = ref(false);
const saving = ref(false);
const currentRole = ref<RoleVO | null>(null);
const assignedIds = ref<number[]>([]);

const permOptions = computed(() =>
  allPermissions.value.map((p) => ({
    key: p.id,
    label: `${p.name}（${p.code}）`,
  })),
);

async function load() {
  loading.value = true;
  try {
    const [roleList, perms] = await Promise.all([
      listRoles(),
      listPermissions(),
    ]);
    roles.value = roleList;
    allPermissions.value = perms;
  } finally {
    loading.value = false;
  }
}

async function openAssign(role: RoleVO) {
  currentRole.value = role;
  assignedIds.value = await listRolePermissions(role.id);
  assignVisible.value = true;
}

async function saveAssign() {
  if (!currentRole.value) return;
  saving.value = true;
  try {
    await assignRolePermissions(currentRole.value.id, assignedIds.value);
    ElMessage.success("权限已保存（重新登录后生效）");
    assignVisible.value = false;
  } finally {
    saving.value = false;
  }
}

onMounted(load);
</script>
