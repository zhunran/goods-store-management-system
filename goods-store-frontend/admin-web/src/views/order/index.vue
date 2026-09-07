<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="filter-bar">
        <el-input
          v-model="query.orderNo"
          placeholder="订单编号"
          clearable
          style="width: 220px"
          @keyup.enter="load"
        />
        <el-input
          v-model="query.memberAccount"
          placeholder="会员账号"
          clearable
          style="width: 180px"
          @keyup.enter="load"
        />
        <el-select
          v-model="query.status"
          placeholder="订单状态"
          clearable
          style="width: 140px"
        >
          <el-option
            v-for="s in statusOptions"
            :key="s.value"
            :label="s.label"
            :value="s.value"
          />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column
          prop="orderNo"
          label="订单编号"
          min-width="190"
          show-overflow-tooltip
        />
        <el-table-column prop="memberAccount" label="会员账号" width="120" />
        <el-table-column prop="totalPay" label="金额" width="100">
          <template #default="{ row }">¥{{ row.totalPay }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{
              statusText(row.status)
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" width="180">
          <template #default="{ row }">{{
            formatTime(row.checkoutTime)
          }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === '20'"
              size="small"
              type="primary"
              @click="ship(row)"
              v-permission="'order:ship'"
            >
              发货
            </el-button>
            <el-button
              v-if="row.status === '20'"
              size="small"
              type="danger"
              plain
              @click="refund(row)"
              v-permission="'order:refund'"
            >
              退款
            </el-button>
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

    <el-dialog v-model="detailVisible" title="订单详情" width="640px">
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单编号" :span="2">{{
            detail.orderNo
          }}</el-descriptions-item>
          <el-descriptions-item label="会员">{{
            detail.memberAccount
          }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{
            statusText(detail.status)
          }}</el-descriptions-item>
          <el-descriptions-item label="金额"
            >¥{{ detail.totalPay }}</el-descriptions-item
          >
          <el-descriptions-item label="支付方式">{{
            detail.payType || "-"
          }}</el-descriptions-item>
          <el-descriptions-item label="收货人">{{
            detail.receiverName
          }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{
            detail.receiverPhone
          }}</el-descriptions-item>
          <el-descriptions-item label="收货地址" :span="2">{{
            detail.receiverAddrDetail
          }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{
            detail.orderComment || "-"
          }}</el-descriptions-item>
        </el-descriptions>
        <el-table
          :data="detail.items || []"
          style="margin-top: 16px"
          size="small"
          border
        >
          <el-table-column prop="goodName" label="商品" min-width="160" />
          <el-table-column prop="dealPrice" label="单价" width="90" />
          <el-table-column prop="count" label="数量" width="70" />
        </el-table>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { get, put } from "@/utils/request";
import type { OrderVO, OrderDetailVO } from "@/api/order";

const list = ref<OrderVO[]>([]);
const total = ref(0);
const loading = ref(false);
const detailVisible = ref(false);
const detail = ref<OrderDetailVO | null>(null);

const statusOptions = [
  { value: "10", label: "待付款" },
  { value: "20", label: "已支付" },
  { value: "30", label: "已发货" },
  { value: "40", label: "已完成" },
  { value: "50", label: "已取消" },
];

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  orderNo: "",
  memberAccount: "",
  status: "",
});

function statusText(s: string) {
  return statusOptions.find((o) => o.value === s)?.label || s;
}

function statusType(s: string) {
  const map: Record<string, string> = {
    "10": "warning",
    "20": "primary",
    "30": "success",
    "40": "info",
    "50": "danger",
    "60": "danger",
  };
  return map[s] || "info";
}

function formatTime(t: string) {
  return t ? t.replace("T", " ").slice(0, 19) : "-";
}

async function load() {
  loading.value = true;
  try {
    const params: any = { pageNum: query.pageNum, pageSize: query.pageSize };
    if (query.orderNo) params.orderNo = query.orderNo;
    if (query.memberAccount) params.memberAccount = query.memberAccount;
    if (query.status) params.status = query.status;
    const data = await get<{ total: number; records: OrderVO[] }>(
      "/order/admin/page",
      params,
    );
    list.value = data.records;
    total.value = Number(data.total) ?? 0;
  } finally {
    loading.value = false;
  }
}

function reset() {
  query.orderNo = "";
  query.memberAccount = "";
  query.status = "";
  query.pageNum = 1;
  load();
}

async function openDetail(row: OrderVO) {
  detail.value = await get<OrderDetailVO>(`/order/admin/${row.id}`);
  detailVisible.value = true;
}

async function ship(row: OrderVO) {
  await ElMessageBox.confirm(`确定对订单 ${row.orderNo} 发货吗？`, "提示", {
    type: "warning",
  });
  await put(`/order/admin/${row.id}/ship`);
  ElMessage.success("发货成功");
  load();
}

async function refund(row: OrderVO) {
  await ElMessageBox.confirm(
    `确定对订单 ${row.orderNo} 退款吗？模拟退款将即时到账，且不可恢复。`,
    "退款确认",
    { type: "warning" },
  );
  await put(`/order/admin/${row.id}/refund`);
  ElMessage.success("退款成功");
  load();
}

onMounted(load);
</script>
