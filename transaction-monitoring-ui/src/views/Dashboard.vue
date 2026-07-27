<script setup>
import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Bell,
  CreditCard,
  Histogram,
  Money,
  PieChart,
  TrendCharts,
  Warning,
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import http from '../api/axios'

const loading = ref(false)
const errorMessage = ref('')
const transactionTrendRef = ref()
const alertTypeRef = ref()
const severityRef = ref()

const statistics = reactive({
  totalTransactions: 0,
  totalAmount: 0,
  totalAlerts: 0,
  openAlerts: 0,
})

let transactionTrendChart
let alertTypeChart
let severityChart

const formatNumber = (value) =>
  Number(value || 0).toLocaleString(undefined, {
    maximumFractionDigits: 2,
  })

const countBy = (items, field) =>
  items.reduce((counts, item) => {
    const key = item[field] || 'UNKNOWN'
    counts[key] = (counts[key] || 0) + 1
    return counts
  }, {})

const buildHourlyTrend = (transactions) => {
  const hourlyAmounts = new Map()

  transactions.forEach((transaction) => {
    const hour = new Date(transaction.transactionTime)
    hour.setMinutes(0, 0, 0)
    const timestamp = hour.getTime()
    hourlyAmounts.set(
      timestamp,
      (hourlyAmounts.get(timestamp) || 0) + Number(transaction.amount),
    )
  })

  return [...hourlyAmounts.entries()].sort(
    ([leftTimestamp], [rightTimestamp]) => leftTimestamp - rightTimestamp,
  )
}

const formatTrendTime = (timestamp, includeDate = true) => {
  const date = new Date(timestamp)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')

  return includeDate ? `${month}-${day} ${hour}:00` : `${hour}:00`
}

const renderTransactionTrend = (transactions) => {
  transactionTrendChart = echarts.init(transactionTrendRef.value)

  const trendData = buildHourlyTrend(transactions)
  const includedDates = new Set(
    trendData.map(([timestamp]) => new Date(timestamp).toDateString()),
  )
  const includeDate = includedDates.size > 1

  transactionTrendChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(15, 23, 42, 0.94)',
      borderWidth: 0,
      textStyle: {
        color: '#ffffff',
      },
      formatter: (parameters) => {
        const point = parameters[0]
        return [
          `<strong>${formatTrendTime(point.value[0], true)}</strong>`,
          `Hourly amount: ${formatNumber(point.value[1])}`,
        ].join('<br>')
      },
    },
    grid: {
      top: 28,
      right: 28,
      bottom: 42,
      left: 70,
    },
    xAxis: {
      type: 'time',
      boundaryGap: false,
      splitNumber: 6,
      axisLabel: {
        color: '#7b8798',
        hideOverlap: true,
        formatter: (value) => formatTrendTime(value, includeDate),
      },
      axisLine: {
        lineStyle: {
          color: '#d9e1ec',
        },
      },
      axisTick: {
        show: false,
      },
      splitLine: {
        show: false,
      },
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        color: '#7b8798',
        formatter: (value) => formatNumber(value),
      },
      splitLine: {
        lineStyle: {
          color: '#edf1f6',
        },
      },
    },
    series: [
      {
        name: 'Hourly Amount',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        showSymbol: trendData.length <= 16,
        lineStyle: {
          width: 3,
          color: '#4f7df3',
        },
        itemStyle: {
          color: '#ffffff',
          borderColor: '#4f7df3',
          borderWidth: 3,
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            {
              offset: 0,
              color: 'rgba(79, 125, 243, 0.30)',
            },
            {
              offset: 1,
              color: 'rgba(79, 125, 243, 0.02)',
            },
          ]),
        },
        data: trendData,
      },
    ],
  })
}

const renderAlertTypeChart = (alerts) => {
  alertTypeChart = echarts.init(alertTypeRef.value)
  const typeCounts = countBy(alerts, 'ruleType')
  const ruleTypes = [
    {
      key: 'AMOUNT_THRESHOLD',
      label: 'Amount',
      color: '#ef4444',
    },
    {
      key: 'NEW_PAYEE',
      label: 'New Payee',
      color: '#8b5cf6',
    },
    {
      key: 'VELOCITY',
      label: 'Velocity',
      color: '#f59e0b',
    },
    {
      key: 'DAILY_LIMIT',
      label: 'Daily Limit',
      color: '#10b981',
    },
  ]

  alertTypeChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(15, 23, 42, 0.94)',
      borderWidth: 0,
      textStyle: {
        color: '#ffffff',
      },
      axisPointer: {
        type: 'shadow',
        shadowStyle: {
          color: 'rgba(148, 163, 184, 0.10)',
        },
      },
    },
    grid: {
      top: 24,
      right: 24,
      bottom: 44,
      left: 56,
    },
    xAxis: {
      type: 'category',
      data: ruleTypes.map((ruleType) => ruleType.label),
      axisLabel: {
        interval: 0,
        color: '#64748b',
        fontSize: 11,
      },
      axisLine: {
        lineStyle: {
          color: '#d9e1ec',
        },
      },
      axisTick: {
        show: false,
      },
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: {
        color: '#7b8798',
      },
      splitLine: {
        lineStyle: {
          color: '#edf1f6',
        },
      },
    },
    series: [
      {
        name: 'Alerts',
        type: 'bar',
        barMaxWidth: 48,
        data: ruleTypes.map((ruleType) => ({
          value: typeCounts[ruleType.key] || 0,
          itemStyle: {
            color: ruleType.color,
            borderRadius: [7, 7, 0, 0],
          },
        })),
        label: {
          show: true,
          position: 'top',
          color: '#64748b',
          fontWeight: 600,
        },
      },
    ],
  })
}

const renderSeverityChart = (alerts) => {
  severityChart = echarts.init(severityRef.value)
  const severityCounts = countBy(alerts, 'severity')
  const severities = [
    {
      name: 'HIGH',
      color: '#ef4444',
    },
    {
      name: 'MEDIUM',
      color: '#f59e0b',
    },
    {
      name: 'LOW',
      color: '#22c55e',
    },
  ]

  severityChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)',
      backgroundColor: 'rgba(15, 23, 42, 0.94)',
      borderWidth: 0,
      textStyle: {
        color: '#ffffff',
      },
    },
    legend: {
      bottom: 8,
      itemWidth: 14,
      itemHeight: 8,
      textStyle: {
        color: '#64748b',
      },
    },
    series: [
      {
        name: 'Severity',
        type: 'pie',
        radius: ['48%', '70%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: true,
        label: {
          color: '#475569',
          formatter: '{b}\n{c}',
        },
        labelLine: {
          length: 12,
          length2: 8,
        },
        emphasis: {
          scaleSize: 8,
        },
        data: severities
          .filter((severity) => severityCounts[severity.name])
          .map((severity) => ({
            name: severity.name,
            value: severityCounts[severity.name],
            itemStyle: {
              color: severity.color,
            },
          })),
      },
    ],
  })
}

const renderCharts = async (transactions, alerts) => {
  await nextTick()
  transactionTrendChart?.dispose()
  alertTypeChart?.dispose()
  severityChart?.dispose()
  renderTransactionTrend(transactions)
  renderAlertTypeChart(alerts)
  renderSeverityChart(alerts)
}

const loadDashboard = async () => {
  loading.value = true
  errorMessage.value = ''

  try {
    const statisticsResponse = await http.get('/dashboard/statistics')
    Object.assign(statistics, statisticsResponse.data)

    const [transactionsResponse, alertsResponse] = await Promise.all([
      http.get('/transactions', {
        params: {
          page: 0,
          size: Math.max(statistics.totalTransactions, 1),
        },
      }),
      http.get('/alerts', {
        params: {
          page: 0,
          size: Math.max(statistics.totalAlerts, 1),
        },
      }),
    ])

    await renderCharts(
      transactionsResponse.data.content,
      alertsResponse.data.content,
    )
  } catch (error) {
    errorMessage.value = 'Failed to load dashboard data. Please try again later.'
    ElMessage.error(errorMessage.value)
  } finally {
    loading.value = false
  }
}

const resizeCharts = () => {
  transactionTrendChart?.resize()
  alertTypeChart?.resize()
  severityChart?.resize()
}

onMounted(() => {
  window.addEventListener('resize', resizeCharts)
  loadDashboard()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  transactionTrendChart?.dispose()
  alertTypeChart?.dispose()
  severityChart?.dispose()
})
</script>

<template>
  <section v-loading="loading" class="dashboard-page">
    <div class="page-heading">
      <div>
        <span class="page-eyebrow">Overview</span>
        <h1>Dashboard</h1>
        <p>Live transaction activity and risk monitoring summary.</p>
      </div>
      <span class="live-indicator">
        <span />
        Live data
      </span>
    </div>

    <el-alert
      v-if="errorMessage"
      class="dashboard-error"
      :title="errorMessage"
      type="error"
      show-icon
      :closable="false"
    />

    <div class="statistics-grid">
      <el-card class="statistic-card statistic-card--blue" shadow="hover">
        <div class="statistic-content">
          <span class="statistic-icon">
            <el-icon><CreditCard /></el-icon>
          </span>
          <div>
            <div class="statistic-label">Total Transactions</div>
            <div class="statistic-value">
              {{ formatNumber(statistics.totalTransactions) }}
            </div>
          </div>
        </div>
      </el-card>

      <el-card class="statistic-card statistic-card--green" shadow="hover">
        <div class="statistic-content">
          <span class="statistic-icon">
            <el-icon><Money /></el-icon>
          </span>
          <div>
            <div class="statistic-label">Total Amount</div>
            <div class="statistic-value">
              {{ formatNumber(statistics.totalAmount) }}
            </div>
          </div>
        </div>
      </el-card>

      <el-card class="statistic-card statistic-card--amber" shadow="hover">
        <div class="statistic-content">
          <span class="statistic-icon">
            <el-icon><Bell /></el-icon>
          </span>
          <div>
            <div class="statistic-label">Total Alerts</div>
            <div class="statistic-value">
              {{ formatNumber(statistics.totalAlerts) }}
            </div>
          </div>
        </div>
      </el-card>

      <el-card class="statistic-card statistic-card--red" shadow="hover">
        <div class="statistic-content">
          <span class="statistic-icon">
            <el-icon><Warning /></el-icon>
          </span>
          <div>
            <div class="statistic-label">Open Alerts</div>
            <div class="statistic-value">
              {{ formatNumber(statistics.openAlerts) }}
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <div class="charts-grid">
      <el-card class="chart-card" shadow="hover">
        <template #header>
          <div class="chart-title">
            <span class="chart-title-icon">
              <el-icon><TrendCharts /></el-icon>
            </span>
            <div>
              <strong>Transaction Amount Trend</strong>
              <small>Hourly transaction volume</small>
            </div>
          </div>
        </template>
        <div ref="transactionTrendRef" class="chart" />
      </el-card>

      <el-card class="chart-card" shadow="hover">
        <template #header>
          <div class="chart-title">
            <span class="chart-title-icon">
              <el-icon><Histogram /></el-icon>
            </span>
            <div>
              <strong>Alerts by Rule Type</strong>
              <small>Color-coded monitoring rule triggers</small>
            </div>
          </div>
        </template>
        <div ref="alertTypeRef" class="chart" />
      </el-card>

      <el-card class="chart-card" shadow="hover">
        <template #header>
          <div class="chart-title">
            <span class="chart-title-icon">
              <el-icon><PieChart /></el-icon>
            </span>
            <div>
              <strong>Alert Severity Distribution</strong>
              <small>Risk concentration by severity</small>
            </div>
          </div>
        </template>
        <div ref="severityRef" class="chart" />
      </el-card>
    </div>
  </section>
</template>

<style scoped>
.dashboard-page {
  min-height: calc(100vh - 120px);
}

.page-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 28px;
}

.page-heading h1 {
  margin: 2px 0 7px;
}

.page-heading p {
  margin: 0;
  color: #7b8798;
  font-size: 14px;
}

.page-eyebrow {
  color: #3b82f6;
  font-size: 10px;
  font-weight: 750;
  letter-spacing: 1.4px;
  text-transform: uppercase;
}

.live-indicator {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 13px;
  border: 1px solid #dcfce7;
  border-radius: 999px;
  color: #15803d;
  font-size: 12px;
  font-weight: 650;
  background: #f0fdf4;
}

.live-indicator span {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 0 4px rgb(34 197 94 / 12%);
}

.dashboard-error {
  margin-bottom: 20px;
}

.statistics-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
  margin-bottom: 22px;
}

.statistic-card {
  position: relative;
  overflow: hidden;
  border: 1px solid rgb(226 232 240 / 80%);
  border-radius: 15px;
  box-shadow: 0 8px 28px rgb(15 23 42 / 6%);
}

.statistic-card::after {
  content: "";
  position: absolute;
  top: -42px;
  right: -34px;
  width: 110px;
  height: 110px;
  border-radius: 50%;
  background: currentColor;
  opacity: 0.05;
}

.statistic-card--blue {
  color: #2563eb;
}

.statistic-card--green {
  color: #16a34a;
}

.statistic-card--amber {
  color: #d97706;
}

.statistic-card--red {
  color: #dc2626;
}

.statistic-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.statistic-icon {
  display: grid;
  flex: 0 0 48px;
  width: 48px;
  height: 48px;
  place-items: center;
  border-radius: 13px;
  font-size: 23px;
  background: currentColor;
}

.statistic-icon :deep(.el-icon) {
  color: #ffffff;
}

.statistic-label {
  margin-bottom: 7px;
  color: #7b8798;
  font-size: 13px;
  font-weight: 550;
}

.statistic-value {
  color: #172033;
  font-size: clamp(28px, 2.2vw, 36px);
  font-weight: 750;
  letter-spacing: -1px;
  line-height: 1;
}

.charts-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.chart-card:first-child {
  grid-column: 1 / -1;
}

.chart-card {
  border: 1px solid rgb(226 232 240 / 85%);
  border-radius: 15px;
  box-shadow: 0 8px 28px rgb(15 23 42 / 5%);
}

.chart-card :deep(.el-card__header) {
  padding: 18px 20px;
  border-bottom-color: #edf1f6;
}

.chart-title {
  display: flex;
  align-items: center;
  gap: 11px;
}

.chart-title-icon {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 10px;
  color: #2563eb;
  background: #eff6ff;
}

.chart-title strong,
.chart-title small {
  display: block;
}

.chart-title strong {
  color: #263247;
  font-size: 14px;
}

.chart-title small {
  margin-top: 3px;
  color: #94a3b8;
  font-size: 11px;
}

.chart {
  width: 100%;
  height: 360px;
}

@media (max-width: 1200px) {
  .statistics-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .statistics-grid,
  .charts-grid {
    grid-template-columns: 1fr;
  }

  .chart-card:first-child {
    grid-column: auto;
  }
}
</style>
