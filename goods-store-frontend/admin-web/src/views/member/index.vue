<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="filter-bar">
        <el-input v-model="query.keyword" placeholder="账号/姓名" clearable style="width: 220px" @keyup.enter="load" />
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
        <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
        <el-table-column prop="birthday" label="生日" width="110" />
        <el-table-column label="注册时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createdTime) }}</template>
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
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { get } from '@/utils/request'
import type { MemberVO } from '@/api/member'

const list = ref<MemberVO[]>([])
const total = ref(0)
const loading = ref(false)

const query = reactive({ pageNum: 1, pageSize: 10, keyword: '' })

function formatTime(t: string) {
  return t ? t.replace('T', ' ').slice(0, 19) : '-'
}

async function load() {
  loading.value = true
  try {
    const data = await get<{ total: number; records: MemberVO[] }>('/member/admin/page', { ...query })
    list.value = data.records
    total.value = Number(data.total) ?? 0
  } finally {
    loading.value = false
  }
}

function reset() {
  query.keyword = ''
  query.pageNum = 1
  load()
}

onMounted(load)
</script>
