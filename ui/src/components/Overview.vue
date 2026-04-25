<template>
  <VCard>
    <div class="space-y-6">
      <!-- 概览卡片 -->
      <div class="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-4">
        <el-card class="custom-card">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-slate-500">句子总数</p>
              <p class="mt-1 text-3xl font-bold text-slate-900">2,000</p>
            </div>
            <div class="flex items-center justify-center w-12 h-12 bg-blue-100 rounded-full">
              <el-icon :size="24" color="#3b82f6">
                <Document/>
              </el-icon>
            </div>
          </div>
        </el-card>

        <el-card class="custom-card">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-slate-500">分类数量</p>
              <p class="mt-1 text-3xl font-bold text-slate-900">12</p>
            </div>
            <div class="flex items-center justify-center w-12 h-12 bg-purple-100 rounded-full">
              <el-icon :size="24" color="#8b5cf6">
                <PriceTag/>
              </el-icon>
            </div>
          </div>
        </el-card>

        <el-card class="custom-card">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-slate-500">正常句子</p>
              <div class="flex items-baseline gap-2 mt-1">
                <p class="text-3xl font-bold text-green-600">1,800</p>
                <p class="text-sm text-slate-500">(90%)</p>
              </div>
            </div>
            <div class="flex items-center justify-center w-12 h-12 bg-green-100 rounded-full">
              <el-icon :size="24" color="#10b981">
                <CircleCheck/>
              </el-icon>
            </div>
          </div>
        </el-card>

        <el-card class="custom-card">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-slate-500">禁用句子</p>
              <div class="flex items-baseline gap-2 mt-1">
                <p class="text-3xl font-bold text-red-600">200</p>
                <p class="text-sm text-slate-500">(10%)</p>
              </div>
            </div>
            <div class="flex items-center justify-center w-12 h-12 bg-red-100 rounded-full">
              <el-icon :size="24" color="#ef4444">
                <CircleClose/>
              </el-icon>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 图表区域 -->
      <div class="grid grid-cols-2 gap-6 lg:grid-cols-2">
        <el-card class="custom-card">
          <template #header>
            <div class="flex items-center justify-between">
              <span class="font-semibold text-slate-900">分类句子分布</span>
              <el-tag type="info" size="small">4 个分类</el-tag>
            </div>
          </template>
          <v-chart :option="categoryChartOption" style="height: 320px; width: 100%;" autoresize/>
        </el-card>

        <el-card class="custom-card">
          <template #header>
            <div class="flex items-center justify-between">
              <span class="font-semibold text-slate-900">句子状态统计</span>
              <el-tag type="info" size="small">共 2,000 条</el-tag>
            </div>
          </template>
          <v-chart :option="statusChartOption" style="height: 320px; width: 100%;" autoresize/>
        </el-card>
      </div>

      <!-- 分类详情表格 -->
      <el-card class="custom-card">
        <template #header>
          <div class="flex items-center justify-between">
            <span class="font-semibold text-slate-900">分类详情</span>
            <el-tag type="info" size="small">4 个分类</el-tag>
          </div>
        </template>
        <el-table
                :data="categoryTableData"
                style="width: 100%"
                :stripe="true"
        >
          <el-table-column prop="name" label="分类名称" width="200"/>
          <el-table-column label="句子总数" width="120">
            <template #default="{ row }">
              <el-tag type="info" effect="plain">{{ row.total }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="启用" width="100">
            <template #default="{ row }">
              <span class="font-medium text-green-600">{{ row.enabled }}</span>
            </template>
          </el-table-column>
          <el-table-column label="禁用" width="100">
            <template #default="{ row }">
              <span class="font-medium text-red-600">{{ row.disabled }}</span>
            </template>
          </el-table-column>
          <el-table-column label="启用比例" min-width="200">
            <template #default="{ row }">
              <div class="flex items-center gap-3">
                <el-progress
                        :percentage="row.total > 0 ? Math.round((row.enabled / row.total) * 100) : 0"
                        :stroke-width="10"
                        :color="getProgressColor(row.enabled, row.total)"
                        :show-text="false"
                />
                <span class="w-12 text-sm text-right text-slate-600">
                  {{ row.total > 0 ? Math.round((row.enabled / row.total) * 100) : 0 }}%
                </span>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </VCard>
</template>

<script setup lang="ts">
import {Document, PriceTag, CircleCheck, CircleClose} from '@element-plus/icons-vue'
import VChart from 'vue-echarts'
import {use} from 'echarts/core'
import {PieChart, BarChart} from 'echarts/charts'
import {TitleComponent, TooltipComponent, LegendComponent, GridComponent} from 'echarts/components'
import {CanvasRenderer} from 'echarts/renderers'
import {VCard} from "@halo-dev/components"

use([PieChart, BarChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent, CanvasRenderer])

// 静态数据
const categoryTableData = [
  {name: '古诗', total: 800, enabled: 750, disabled: 50},
  {name: '名言', total: 600, enabled: 540, disabled: 60},
  {name: '段子', total: 400, enabled: 340, disabled: 60},
  {name: '鸡汤', total: 200, enabled: 170, disabled: 30},
]

const getProgressColor = (enabled: number, total: number) => {
  if (total === 0) return '#ef4444'
  const ratio = enabled / total
  if (ratio >= 0.8) return '#10b981'
  if (ratio >= 0.5) return '#f59e0b'
  return '#ef4444'
}

const categoryChartOption = {
  tooltip: {
    trigger: 'axis',
    axisPointer: {type: 'shadow'}
  },
  legend: {
    data: ['启用', '禁用'],
    bottom: 0
  },
  grid: {
    left: '3%',
    right: '4%',
    bottom: '15%',
    top: '3%',
    containLabel: true
  },
  xAxis: {
    type: 'category',
    data: ['古诗', '名言', '段子', '鸡汤']
  },
  yAxis: {
    type: 'value'
  },
  series: [
    {
      name: '启用',
      type: 'bar',
      stack: 'total',
      data: [750, 540, 340, 170],
      itemStyle: {color: '#10b981'}
    },
    {
      name: '禁用',
      type: 'bar',
      stack: 'total',
      data: [50, 60, 60, 30],
      itemStyle: {color: '#ef4444', borderRadius: [4, 4, 0, 0]}
    }
  ]
}

const statusChartOption = {
  tooltip: {
    trigger: 'item',
    formatter: '{b}: {c} ({d}%)'
  },
  legend: {
    orient: 'vertical',
    left: 'left',
    top: 'center'
  },
  series: [
    {
      name: '句子状态',
      type: 'pie',
      radius: ['45%', '75%'],
      center: ['60%', '50%'],
      itemStyle: {
        borderRadius: 10,
        borderColor: '#fff',
        borderWidth: 3
      },
      label: {
        show: true,
        position: 'center',
        formatter: () => '{total|2000}\n{label|总计}',
        rich: {
          total: {
            fontSize: 28,
            fontWeight: 'bold',
            color: '#1e293b'
          },
          label: {
            fontSize: 14,
            color: '#64748b',
            padding: [5, 0, 0, 0]
          }
        }
      },
      labelLine: {
        show: false
      },
      data: [
        {value: 1800, name: '正常', itemStyle: {color: '#10b981'}},
        {value: 200, name: '禁用', itemStyle: {color: '#ef4444'}}
      ]
    }
  ]
}
</script>

<style scoped>
.custom-card {
  border-radius: 1rem;
  border: 1px solid #e5e7eb !important;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  transition: box-shadow 0.3s, transform 0.3s;
  margin-bottom: 0;
}

.custom-card:hover {
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.07);
  transform: translateY(-2px);
}

:deep(.el-card__body) {
  padding: 1.5rem;
}

:deep(.el-card__header) {
  padding: 1rem 1.5rem;
  border-bottom: 1px solid #e5e7eb;
}

:deep(.el-table) {
  border: none !important;
  border-radius: 1rem;
  overflow: hidden;
}

:deep(.el-table th.el-table__cell) {
  background-color: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
  padding: 0.75rem 1rem;
}

:deep(.el-table td.el-table__cell) {
  border-bottom: 1px solid #f3f4f6;
  padding: 0.75rem 1rem;
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) {
  background-color: #f9fafb;
}

:deep(.el-progress-bar__outer) {
  background-color: #f1f5f9;
  border-radius: 9999px;
}

:deep(.el-progress-bar__inner) {
  border-radius: 9999px;
}

:deep(.el-tag) {
  border: none;
  font-weight: 500;
}

.grid {
  gap: 1.5rem;
}

.space-y-6 > * + * {
  margin-top: 1.5rem;
}
</style>
