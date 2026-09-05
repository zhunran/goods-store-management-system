<template>
  <div class="page-container">
    <el-row :gutter="16" class="stat-cards">
      <el-col :span="6" v-for="c in statCards" :key="c.label">
        <div class="stat-card">
          <div class="stat-icon" :style="{ background: c.color }">
            <el-icon :size="24"><component :is="c.icon" /></el-icon>
          </div>
          <div>
            <div class="stat-value">{{ c.value }}</div>
            <div class="stat-label">{{ c.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-card shadow="never" class="chart-card">
      <template #header>订单趋势（近 7 日）</template>
      <div ref="chartRef" class="chart"></div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref } from 'vue'
import * as echarts from 'echarts'

const statCards = [
  { label: '今日订单', value: '128', icon: 'List', color: '#2563eb' },
  { label: '商品总数', value: '1,024', icon: 'Goods', color: '#10b981' },
  { label: '会员总数', value: '3,562', icon: 'User', color: '#f59e0b' },
  { label: '秒杀活动', value: '5', icon: 'Timer', color: '#ef4444' }
]

const chartRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null

function renderChart() {
  if (!chartRef.value) return
  chart = echarts.init(chartRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: {
      type: 'category',
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: '订单量',
        type: 'line',
        smooth: true,
        areaStyle: { opacity: 0.15 },
        data: [82, 93, 90, 120, 110, 135, 128],
        itemStyle: { color: '#2563eb' }
      }
    ]
  })
}

function onResize() {
  chart?.resize()
}

onMounted(() => {
  renderChart()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  chart?.dispose()
})
</script>

<style scoped>
.stat-cards {
  margin-bottom: 16px;
}
.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
}
.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}
.stat-value {
  font-size: 24px;
  font-weight: 700;
}
.stat-label {
  color: #6b7280;
  font-size: 13px;
  margin-top: 2px;
}
.chart-card {
  border-radius: 8px;
}
.chart {
  height: 360px;
}
</style>
