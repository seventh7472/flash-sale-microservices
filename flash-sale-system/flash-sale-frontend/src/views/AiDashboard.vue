<template>
  <div>
    <h2 class="page-title">AI 流量预测</h2>
    <div style="display:flex;gap:12px;margin-bottom:20px;flex-wrap:wrap">
      <el-select v-model="sel" placeholder="选择活动" style="width:320px" clearable filterable>
        <el-option v-for="a in acts" :key="a.id" :value="a.id" :label="`${a.activityName}（${statusText(a.status)}）`">
          <div style="display:flex;justify-content:space-between;align-items:center;gap:8px">
            <span>{{ a.activityName }}</span>
            <el-tag size="small" :type="statusTagType(a.status)">{{ statusText(a.status) }}</el-tag>
          </div>
        </el-option>
      </el-select>
      <el-button type="primary" @click="load" :loading="loading">预测</el-button>
      <el-button @click="refresh" :loading="loading">刷新</el-button>
    </div>

    <el-alert v-if="r && r.warningLevel === 'NODATA'" type="info" :closable="false" :title="r.analysis || '该活动暂无流量数据'" style="margin-bottom:16px" />

    <template v-if="r && r.warningLevel !== 'NODATA'">
      <div class="cards">
        <el-card class="stat-card">
          <div class="stat-label">当前实例数</div>
          <div class="stat-value" style="color:#2563eb">{{ r.currentInstances }}</div>
        </el-card>
        <el-card class="stat-card">
          <div class="stat-label">预测峰值 QPS</div>
          <div class="stat-value" style="color:#f56c6c">{{ r.predictedPeakQps }}</div>
        </el-card>
        <el-card class="stat-card">
          <div class="stat-label">建议实例数</div>
          <div class="stat-value" style="color:#67c23a">{{ r.recommendedInstances }}</div>
        </el-card>
        <el-card class="stat-card">
          <div class="stat-label">置信度</div>
          <div class="stat-value" style="color:#e6a23c">{{ r.confidence }}%</div>
        </el-card>
      </div>
      <div v-if="r.textAnalysis" class="ai-analysis">
        <div class="ai-analysis-title">
          <svg class="ai-analysis-icon" viewBox="0 0 24 24" width="15" height="15" aria-hidden="true">
            <path d="M12 2l1.9 5.1L19 9l-5.1 1.9L12 16l-1.9-5.1L5 9l5.1-1.9L12 2z" fill="currentColor" />
            <path d="M18.5 14l.9 2.4 2.4.9-2.4.9-.9 2.4-.9-2.4-2.4-.9 2.4-.9.9-2.4z" fill="currentColor" opacity="0.6" />
          </svg>
          <span>AI 智能分析</span>
        </div>
        <p class="ai-analysis-text">{{ r.textAnalysis }}</p>
      </div>
      <el-card class="scale-card" shadow="never">
        <div class="scale-head">
          <span class="scale-label">扩容状态</span>
          <el-tag :type="scaleTagType" effect="dark">{{ scaleText }}</el-tag>
        </div>
        <p v-if="r.scaleStatus === 'SCALED'" class="scale-desc">本次预测已自动扩容：{{ r.currentInstances }} -> {{ r.recommendedInstances }} 实例<span v-if="r.scaleTime" class="scale-time">{{ r.scaleTime }}</span></p>
        <p v-else-if="r.scaleStatus === 'SUGGEST'" class="scale-desc">预测峰值接近当前容量，建议扩容至 {{ r.recommendedInstances }} 实例</p>
        <p v-else class="scale-desc">当前容量足以覆盖预测流量</p>
      </el-card>
    </template>

    <el-card v-if="r" style="margin-bottom:16px">
      <div class="analysis-line">
        <span class="trend-tag">趋势: {{ r.trend }}</span>
        <span class="trend-tag">预警: {{ r.warningLevel }}</span>
        <span class="trend-tag">预测 QPS: {{ r.predictedQps }}</span>
      </div>
      <p style="color:#666;margin:8px 0 0;line-height:1.8;font-size:13px">{{ r.analysis }}</p>
      <p v-if="r.dataSource" style="color:#999;font-size:12px;margin:6px 0 0">数据来源：{{ r.dataSource }}</p>
    </el-card>
    <el-card style="margin-bottom:16px">
      <div style="font-size:14px;font-weight:600;margin-bottom:12px">历史 QPS 与预测 QPS</div>
      <div style="font-size:12px;color:#999;margin:-4px 0 10px">说明：本页 QPS 为每分钟请求量口径（traffic_metrics 按分钟聚合）</div>
      <div v-if="r && r.timeSeries && r.timeSeries.length" ref="chartRef" class="chart"></div>
      <div v-else style="color:#999;font-size:13px;padding:40px 0;text-align:center">{{ r ? '该活动暂无可用流量数据，无法绘制曲线' : '选择活动并点击预测后展示真实数据曲线' }}</div>
    </el-card>
    <el-card>
      <div style="font-size:14px;font-weight:600;margin-bottom:12px">AI 反作弊</div>
      <div style="font-size:13px;color:#666;line-height:1.8">秒杀抢购时自动检测4个维度：高频请求、多账号同IP、请求间隔规律、集中抢购。超过60分判定高风险，自动拉黑24小时。</div>
    </el-card>
  </div>
</template>
<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { use, init } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { listAllActivities, predictTraffic } from '@/api'
import { ElMessage } from 'element-plus'

use([LineChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const STATUS_TEXT = ['待开始', '进行中', '已结束', '已取消']
const STATUS_TAG = ['info', 'success', 'warning', 'danger']
const statusText = s => STATUS_TEXT[s] || '未知'
const statusTagType = s => STATUS_TAG[s] || 'info'

const acts = ref([])
const sel = ref(null)
const r = ref(null)
const loading = ref(false)
const chartRef = ref(null)
let chart = null

const scaleStatus = computed(() => r.value?.scaleStatus || 'NORMAL')
const scaleTagType = computed(() => ({ NORMAL: 'success', SUGGEST: 'warning', SCALED: 'primary' })[scaleStatus.value] || 'info')
const scaleText = computed(() => ({ NORMAL: '容量正常', SUGGEST: '建议扩容', SCALED: '已扩容' })[scaleStatus.value] || '未知')

async function loadActs() {
  try {
    const x = await listAllActivities()
    if (x.data.code === 200) acts.value = x.data.data || []
  } catch {}
}

async function load() {
  if (!sel.value || loading.value) return
  loading.value = true
  try {
    const x = await predictTraffic(sel.value)
    if (x.data.code !== 200) {
      ElMessage.error(x.data.message || '预测失败')
      return
    }
    r.value = x.data.data
    await nextTick()
    renderChart()
  } catch {
    // HTTP 错误已由 axios 拦截器统一提示
  } finally { loading.value = false }
}

async function refresh() {
  await loadActs()
  if (!sel.value && acts.value.length) sel.value = acts.value[0].id
  if (sel.value) await load()
}

onMounted(async () => {
  window.addEventListener('resize', resize)
  await loadActs()
  if (acts.value.length) {
    sel.value = acts.value[0].id
    await load()
  }
})

function renderChart() {
  if (!chartRef.value) return
  if (!chart) chart = init(chartRef.value)
  const points = r.value && Array.isArray(r.value.timeSeries) ? r.value.timeSeries : []
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['历史 QPS', '预测 QPS'] },
    grid: { left: 52, right: 20, top: 40, bottom: 56 },
    xAxis: {
      type: 'category',
      data: points.map(p => p.time),
      axisLabel: { rotate: 30, fontSize: 11 }
    },
    yAxis: { type: 'value', name: 'QPS', minInterval: 1 },
    series: [
      {
        name: '历史 QPS',
        type: 'line',
        smooth: true,
        showSymbol: false,
        connectNulls: true,
        itemStyle: { color: '#2563eb' },
        lineStyle: { color: '#2563eb', width: 2 },
        data: points.map(p => p.historyQps)
      },
      {
        name: '预测 QPS',
        type: 'line',
        smooth: true,
        showSymbol: false,
        connectNulls: true,
        itemStyle: { color: '#e6a23c' },
        lineStyle: { color: '#e6a23c', width: 2, type: 'dashed' },
        data: points.map(p => p.predictedQps)
      }
    ]
  }, true)
}

function resize() {
  if (chart) chart.resize()
}

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  if (chart) {
    chart.dispose()
    chart = null
  }
})
</script>
<style scoped>
.page-title {
  font-size: 20px;
  margin: 0 0 16px;
  color: var(--ink);
  font-weight: 700;
}

.cards {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card :deep(.el-card__body) {
  padding: 14px;
}

.stat-label {
  font-size: 12px;
  color: #8a919f;
}

.stat-value {
  margin-top: 6px;
  font-size: 26px;
  font-weight: 700;
  line-height: 1.2;
}

.chart {
  width: 100%;
  height: 360px;
}

.analysis-line {
  display: flex;
  gap: 12px;
}

.trend-tag {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.ai-analysis {
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #f5f3ff;
  border-left: 3px dashed #7c3aed;
  border-radius: 6px;
}

.ai-analysis-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #7c3aed;
}

.ai-analysis-icon {
  flex: none;
}

.ai-analysis-text {
  margin: 6px 0 0;
  font-size: 13px;
  line-height: 1.8;
  color: #4b5563;
}

.scale-card {
  margin-bottom: 16px;
  border-radius: 8px;
}

.scale-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.scale-label {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.scale-desc {
  margin: 8px 0 0;
  font-size: 13px;
  color: #4b5563;
}

.scale-time {
  margin-left: 8px;
  color: #9ca3af;
  font-size: 12px;
}

@media (max-width: 768px) {
  .cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>