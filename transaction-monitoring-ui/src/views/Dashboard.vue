<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
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
import { formatDateTime, parseUtcAwareDate, toTimestamp } from '../utils/dateTime'

const loading = ref(false)
const errorMessage = ref('')
const transactionTrendRef = ref()
const alertTypeRef = ref()
const severityRef = ref()
const recentAlerts = ref([])
const lastUpdated = ref('')

const statistics = reactive({
  totalTransactions: 0,
  totalAmount: 0,
  totalAlerts: 0,
  openAlerts: 0,
})

const riskOverview = reactive({
  score: 0,
  level: 'LOW RISK',
  high: 0,
  medium: 0,
  low: 0,
})

let transactionTrendChart
let alertTypeChart
let severityChart

const riskLevelClass = computed(() =>
  riskOverview.level.toLowerCase().replace(' ', '-'),
)

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

const severityWeight = {
  HIGH: 3,
  MEDIUM: 2,
  LOW: 1,
}

const updateRiskOverview = (alerts) => {
  const activeAlerts = alerts.filter((alert) => alert.status === 'OPEN')
  const counts = countBy(activeAlerts, 'severity')

  riskOverview.high = counts.HIGH || 0
  riskOverview.medium = counts.MEDIUM || 0
  riskOverview.low = counts.LOW || 0
  riskOverview.score =
    riskOverview.high * 3 + riskOverview.medium * 2 + riskOverview.low
  riskOverview.level =
    riskOverview.score >= 70
      ? 'HIGH RISK'
      : riskOverview.score >= 40
        ? 'MEDIUM RISK'
        : 'LOW RISK'
}

const updateRecentAlerts = (alerts, transactions) => {
  const transactionsById = new Map(
    transactions.map((transaction) => [transaction.id, transaction]),
  )

  recentAlerts.value = [...alerts]
    .sort((left, right) => {
      const severityDifference =
        (severityWeight[right.severity] || 0) -
        (severityWeight[left.severity] || 0)

      if (severityDifference !== 0) {
        return severityDifference
      }

      return toTimestamp(right.createdTime) - toTimestamp(left.createdTime)
    })
    .slice(0, 5)
    .map((alert) => {
      const transaction = transactionsById.get(alert.transactionId)

      return {
        ...alert,
        amount: transaction?.amount,
        currency: transaction?.currency,
      }
    })
}

const formatUpdatedTime = (date) =>
  date.toLocaleTimeString([], {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })

const formatAlertTime = (value) => {
  return formatDateTime(value)
}

const severityTagType = (severity) =>
  ({
    HIGH: 'danger',
    MEDIUM: 'warning',
    LOW: 'success',
  })[severity] || 'info'

const statusTagType = (status) =>
  ({
    OPEN: 'danger',
    ACKNOWLEDGED: 'warning',
    INVESTIGATING: 'primary',
    CLOSED: 'success',
    DISMISSED: 'info',
  })[status] || 'info'

const riskBarWidth = (count) => {
  const maximum = Math.max(
    riskOverview.high,
    riskOverview.medium,
    riskOverview.low,
    1,
  )

  return `${Math.max((count / maximum) * 100, count ? 8 : 0)}%`
}

const buildHourlyTrend = (transactions) => {
  const hourlyAmounts = new Map()

  transactions.forEach((transaction) => {
    const hour = parseUtcAwareDate(transaction.transactionTime)
    if (!hour) {
      return
    }
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
          `Transaction Amount: ¥${formatNumber(point.value[1])}`,
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
      formatter: (parameters) => {
        const point = parameters[0]
        return [
          `<strong>Rule Name</strong>: ${point.axisValue}`,
          `Alert Count: ${formatNumber(point.value)}`,
        ].join('<br>')
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
      formatter: (parameter) =>
        [
          `<strong>Severity Level</strong>: ${parameter.name}`,
          `Count: ${formatNumber(parameter.value)}`,
        ].join('<br>'),
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

    const transactions = transactionsResponse.data.content
    const alerts = alertsResponse.data.content

    updateRiskOverview(alerts)
    updateRecentAlerts(alerts, transactions)
    lastUpdated.value = formatUpdatedTime(new Date())
    await renderCharts(transactions, alerts)
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
      <div class="live-status">
        <span class="live-indicator">
          <span />
          Live data
        </span>
        <small>Last updated: {{ lastUpdated || '—' }}</small>
      </div>
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
          <div class="statistic-header">
            <span class="statistic-icon">
              <el-icon><CreditCard /></el-icon>
            </span>
            <div class="statistic-label">Total Transactions</div>
          </div>
          <div class="statistic-value">
            {{ formatNumber(statistics.totalTransactions) }}
          </div>
          <div class="statistic-description">Monitored transactions</div>
        </div>
      </el-card>

      <el-card class="statistic-card statistic-card--green" shadow="hover">
        <div class="statistic-content">
          <div class="statistic-header">
            <span class="statistic-icon">
              <el-icon><Money /></el-icon>
            </span>
            <div class="statistic-label">Transaction Amount</div>
          </div>
          <div class="statistic-value">
            {{ formatNumber(statistics.totalAmount) }}
          </div>
          <div class="statistic-description">Total transaction volume</div>
        </div>
      </el-card>

      <el-card class="statistic-card statistic-card--amber" shadow="hover">
        <div class="statistic-content">
          <div class="statistic-header">
            <span class="statistic-icon">
              <el-icon><Warning /></el-icon>
            </span>
            <div class="statistic-label">Risk Alerts</div>
          </div>
          <div class="statistic-value">
            {{ formatNumber(statistics.totalAlerts) }}
          </div>
          <div class="statistic-description">
            Detected suspicious activities
          </div>
        </div>
      </el-card>

      <el-card class="statistic-card statistic-card--red" shadow="hover">
        <div class="statistic-content">
          <div class="statistic-header">
            <span class="statistic-icon">
              <el-icon><Bell /></el-icon>
            </span>
            <div class="statistic-label">Open Cases</div>
          </div>
          <div class="statistic-value">
            {{ formatNumber(statistics.openAlerts) }}
          </div>
          <div class="statistic-description">Requires investigation</div>
        </div>
      </el-card>
    </div>

    <el-card class="risk-overview-card" shadow="never">
      <div class="risk-overview">
        <div class="risk-summary">
          <span class="section-eyebrow">Risk Overview</span>
          <div class="risk-score" :class="riskLevelClass">
            {{ riskOverview.score }}
          </div>
          <el-tag
            class="risk-level-tag"
            :type="severityTagType(riskOverview.level.split(' ')[0])"
            effect="light"
          >
            {{ riskOverview.level }}
          </el-tag>
          <p>Current risk level based on active alerts</p>
        </div>

        <div class="risk-breakdown">
          <div
            v-for="item in [
              { label: 'HIGH', value: riskOverview.high },
              { label: 'MEDIUM', value: riskOverview.medium },
              { label: 'LOW', value: riskOverview.low },
            ]"
            :key="item.label"
            class="risk-row"
          >
            <div class="risk-row-heading">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
            <div class="risk-track">
              <span
                :class="`risk-fill risk-fill--${item.label.toLowerCase()}`"
                :style="{ width: riskBarWidth(item.value) }"
              />
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <div class="charts-grid">
      <el-card class="chart-card" shadow="hover">
        <template #header>
          <div class="chart-title">
            <span class="chart-title-icon">
              <el-icon><TrendCharts /></el-icon>
            </span>
            <div>
              <strong>Transaction Activity</strong>
              <small>Hourly monitored transactions</small>
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
              <strong>Triggered Rules</strong>
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
              <strong>Risk Severity Distribution</strong>
              <small>Risk concentration by severity</small>
            </div>
          </div>
        </template>
        <div ref="severityRef" class="chart" />
      </el-card>
    </div>

    <el-card class="recent-alerts-card" shadow="never">
      <template #header>
        <div class="recent-alerts-heading">
          <div>
            <span class="section-eyebrow">Risk Feed</span>
            <h2>Recent Critical Alerts</h2>
            <p>Latest events prioritized by severity.</p>
          </div>
          <span class="recent-count">{{ recentAlerts.length }} events</span>
        </div>
      </template>

      <div v-if="recentAlerts.length" class="recent-alerts-list">
        <article
          v-for="alert in recentAlerts"
          :key="alert.id"
          class="recent-alert-item"
        >
          <div class="alert-severity">
            <el-tag :type="severityTagType(alert.severity)" effect="light">
              {{ alert.severity }}
            </el-tag>
          </div>
          <div class="alert-description">
            <strong>{{ alert.ruleName || alert.ruleType }}</strong>
            <span>Transaction #{{ alert.transactionId }}</span>
            <span v-if="alert.amount != null" class="alert-amount">
              Amount: {{ formatNumber(alert.amount) }}
              {{ alert.currency || '' }}
            </span>
          </div>
          <div class="alert-meta">
            <el-tag :type="statusTagType(alert.status)" effect="plain">
              {{ alert.status }}
            </el-tag>
            <time>{{ formatAlertTime(alert.createdTime) }}</time>
          </div>
        </article>
      </div>

      <el-empty
        v-else
        :image-size="72"
        description="No recent risk events"
      />
    </el-card>
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

.live-status {
  display: flex;
  align-items: flex-end;
  flex-direction: column;
  gap: 7px;
}

.live-status small {
  color: #94a3b8;
  font-size: 11px;
  font-variant-numeric: tabular-nums;
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
  --statistic-accent: #3568a8;
  --statistic-soft: #edf4fc;
  --statistic-border: #d8e5f4;
  position: relative;
  min-height: 174px;
  overflow: hidden;
  border: 1px solid var(--statistic-border);
  border-radius: 16px;
  background:
    linear-gradient(145deg, #ffffff 55%, var(--statistic-soft) 150%);
  box-shadow: 0 9px 26px rgb(15 23 42 / 6%);
  transition:
    transform 180ms ease,
    box-shadow 180ms ease;
}

.statistic-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 32px rgb(15 23 42 / 9%);
}

.statistic-card::before {
  content: "";
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  background: var(--statistic-accent);
}

.statistic-card::after {
  content: "";
  position: absolute;
  top: -42px;
  right: -34px;
  width: 110px;
  height: 110px;
  border-radius: 50%;
  background: var(--statistic-accent);
  opacity: 0.045;
}

.statistic-card :deep(.el-card__body) {
  height: 100%;
  padding: 22px 22px 20px 24px;
}

.statistic-card--blue {
  --statistic-accent: #3568a8;
  --statistic-soft: #edf4fc;
  --statistic-border: #d9e6f5;
}

.statistic-card--green {
  --statistic-accent: #397b5e;
  --statistic-soft: #edf7f1;
  --statistic-border: #d8eadf;
}

.statistic-card--amber {
  --statistic-accent: #b46b2d;
  --statistic-soft: #fbf3e9;
  --statistic-border: #eedfcb;
}

.statistic-card--red {
  --statistic-accent: #b64d57;
  --statistic-soft: #fbf0f1;
  --statistic-border: #efdadd;
}

.statistic-content {
  display: flex;
  min-height: 130px;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
}

.statistic-header {
  display: flex;
  align-items: center;
  gap: 11px;
}

.statistic-icon {
  display: grid;
  flex: 0 0 38px;
  width: 38px;
  height: 38px;
  place-items: center;
  border: 1px solid color-mix(
    in srgb,
    var(--statistic-accent) 18%,
    transparent
  );
  border-radius: 11px;
  color: var(--statistic-accent);
  font-size: 19px;
  background: var(--statistic-soft);
}

.statistic-icon :deep(.el-icon) {
  color: inherit;
}

.statistic-label {
  color: #475569;
  font-size: 13px;
  font-weight: 680;
  letter-spacing: 0.1px;
}

.statistic-value {
  margin-top: 16px;
  color: #182235;
  font-size: clamp(30px, 2.35vw, 38px);
  font-variant-numeric: tabular-nums;
  font-weight: 760;
  letter-spacing: -1.2px;
  line-height: 1;
}

.statistic-description {
  margin-top: 9px;
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.35;
}

.risk-overview-card {
  margin-bottom: 22px;
  border: 1px solid #dfe7f1;
  border-radius: 16px;
  background:
    radial-gradient(circle at 8% 12%, rgb(59 130 246 / 8%), transparent 28%),
    #ffffff;
  box-shadow: 0 8px 28px rgb(15 23 42 / 5%);
}

.risk-overview-card :deep(.el-card__body) {
  padding: 26px 28px;
}

.risk-overview {
  display: grid;
  grid-template-columns: minmax(220px, 0.8fr) minmax(320px, 1.6fr);
  align-items: center;
  gap: 44px;
}

.risk-summary {
  padding-right: 36px;
  border-right: 1px solid #e7edf5;
}

.section-eyebrow {
  color: #64748b;
  font-size: 10px;
  font-weight: 750;
  letter-spacing: 1.3px;
  text-transform: uppercase;
}

.risk-score {
  margin: 12px 0 9px;
  color: #15803d;
  font-size: 54px;
  font-variant-numeric: tabular-nums;
  font-weight: 780;
  letter-spacing: -2px;
  line-height: 1;
}

.risk-score.medium-risk {
  color: #b45309;
}

.risk-score.high-risk {
  color: #b91c1c;
}

.risk-level-tag {
  font-weight: 700;
  letter-spacing: 0.45px;
}

.risk-summary p {
  margin: 13px 0 0;
  color: #94a3b8;
  font-size: 12px;
}

.risk-breakdown {
  display: grid;
  gap: 20px;
}

.risk-row-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  color: #64748b;
  font-size: 11px;
  font-weight: 700;
}

.risk-row-heading strong {
  color: #334155;
  font-size: 13px;
  font-variant-numeric: tabular-nums;
}

.risk-track {
  height: 9px;
  overflow: hidden;
  border-radius: 999px;
  background: #eef2f7;
}

.risk-fill {
  display: block;
  height: 100%;
  border-radius: inherit;
}

.risk-fill--high {
  background: #dc5962;
}

.risk-fill--medium {
  background: #e79a38;
}

.risk-fill--low {
  background: #4ca777;
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

.recent-alerts-card {
  margin-top: 22px;
  border: 1px solid #dfe7f1;
  border-radius: 16px;
  box-shadow: 0 8px 28px rgb(15 23 42 / 5%);
}

.recent-alerts-card :deep(.el-card__header) {
  padding: 19px 22px;
  border-bottom-color: #edf1f6;
}

.recent-alerts-card :deep(.el-card__body) {
  padding: 0 22px;
}

.recent-alerts-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.recent-alerts-heading h2 {
  margin: 4px 0 3px;
  color: #263247;
  font-size: 16px;
}

.recent-alerts-heading p {
  margin: 0;
  color: #94a3b8;
  font-size: 11px;
}

.recent-count {
  padding: 6px 10px;
  border-radius: 999px;
  color: #64748b;
  font-size: 11px;
  font-weight: 650;
  background: #f1f5f9;
}

.recent-alert-item {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr) auto;
  align-items: center;
  gap: 18px;
  padding: 17px 2px;
  border-bottom: 1px solid #edf1f6;
}

.recent-alert-item:last-child {
  border-bottom: 0;
}

.alert-description {
  display: grid;
  min-width: 0;
  gap: 5px;
}

.alert-description strong {
  overflow: hidden;
  color: #334155;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.alert-description span {
  color: #8491a3;
  font-size: 11px;
}

.alert-description .alert-amount {
  color: #475569;
  font-weight: 650;
}

.alert-meta {
  display: flex;
  align-items: flex-end;
  flex-direction: column;
  gap: 7px;
}

.alert-meta time {
  color: #94a3b8;
  font-size: 10px;
  font-variant-numeric: tabular-nums;
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

  .page-heading {
    align-items: flex-start;
  }

  .risk-overview {
    grid-template-columns: 1fr;
    gap: 28px;
  }

  .risk-summary {
    padding-right: 0;
    padding-bottom: 24px;
    border-right: 0;
    border-bottom: 1px solid #e7edf5;
  }

  .recent-alert-item {
    grid-template-columns: 78px minmax(0, 1fr);
  }

  .alert-meta {
    grid-column: 2;
    align-items: flex-start;
  }
}

@media (max-width: 560px) {
  .page-heading {
    flex-direction: column;
    gap: 16px;
  }

  .live-status {
    align-items: flex-start;
  }

  .risk-overview-card :deep(.el-card__body) {
    padding: 22px 20px;
  }

  .recent-alerts-heading {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }
}
</style>
