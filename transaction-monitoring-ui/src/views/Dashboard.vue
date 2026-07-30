<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Bell,
  CreditCard,
  Histogram,
  Money,
  PieChart,
  Refresh,
  TrendCharts,
  Warning,
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import http from '../api/axios'
import { formatDateTime, parseUtcAwareDate, toTimestamp } from '../utils/dateTime'
import { subscribeToTransactionUpdates } from '../utils/notificationStream'

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
  baseCurrency: 'USD',
  exchangeRates: {},
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

const activeAlertRate = computed(() => {
  if (!statistics.totalAlerts) {
    return 0
  }

  return Math.round((statistics.openAlerts / statistics.totalAlerts) * 100)
})

const baseCurrencyLabel = computed(() => statistics.baseCurrency || 'USD')

const formatNumber = (value) =>
  Number(value || 0).toLocaleString(undefined, {
    maximumFractionDigits: 2,
  })

const normalizeCurrency = (currency) =>
  typeof currency === 'string' ? currency.trim().toUpperCase() : ''

const convertToBaseCurrency = (amount, currency) => {
  const numericAmount = Number(amount || 0)
  const normalizedCurrency = normalizeCurrency(currency)
  const normalizedBaseCurrency = normalizeCurrency(baseCurrencyLabel.value)

  if (!normalizedCurrency || normalizedCurrency === normalizedBaseCurrency) {
    return numericAmount
  }

  const rate = Number(statistics.exchangeRates?.[normalizedCurrency])
  return Number.isFinite(rate) ? numericAmount * rate : numericAmount
}

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
  const hourlyActivity = new Map()

  transactions.forEach((transaction) => {
    const hour = parseUtcAwareDate(transaction.transactionTime)
    if (!hour) {
      return
    }
    hour.setMinutes(0, 0, 0)
    const timestamp = hour.getTime()
    const current = hourlyActivity.get(timestamp) || { count: 0, amount: 0 }
    current.count += 1
    current.amount += convertToBaseCurrency(
      transaction.amount,
      transaction.currency,
    )
    hourlyActivity.set(timestamp, current)
  })

  return [...hourlyActivity.entries()].sort(
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
        const amountPoint = parameters.find(
          (parameter) => parameter.seriesName === 'Amount',
        )
        const countPoint = parameters.find(
          (parameter) => parameter.seriesName === 'Transactions',
        )
        const timestamp = parameters[0]?.value?.[0]
        return [
          `<strong>${formatTrendTime(timestamp, true)}</strong>`,
          `Transactions: ${formatNumber(countPoint?.value?.[1])}`,
          `Amount: ${formatNumber(amountPoint?.value?.[1])} ${baseCurrencyLabel.value}`,
        ].join('<br>')
      },
    },
    legend: {
      top: 0,
      right: 12,
      itemWidth: 16,
      itemHeight: 8,
      textStyle: {
        color: '#8fa2b7',
      },
    },
    grid: {
      top: 42,
      right: 68,
      bottom: 42,
      left: 52,
    },
    xAxis: {
      type: 'time',
      boundaryGap: false,
      splitNumber: 6,
      axisLabel: {
        color: '#70849a',
        hideOverlap: true,
        formatter: (value) => formatTrendTime(value, includeDate),
      },
      axisLine: {
        lineStyle: {
          color: '#1b3047',
        },
      },
      axisTick: {
        show: false,
      },
      splitLine: {
        show: false,
      },
    },
    yAxis: [
      {
        type: 'value',
        minInterval: 1,
        name: 'Transactions',
        nameTextStyle: {
          color: '#60758c',
        },
        axisLabel: {
          color: '#70849a',
        },
        splitLine: {
          lineStyle: {
            color: '#172b40',
          },
        },
      },
      {
        type: 'value',
        name: baseCurrencyLabel.value,
        nameTextStyle: {
          color: '#60758c',
        },
        axisLabel: {
          color: '#70849a',
          formatter: (value) => formatNumber(value),
        },
        splitLine: {
          show: false,
        },
      },
    ],
    series: [
      {
        name: 'Transactions',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        showSymbol: trendData.length <= 16,
        lineStyle: {
          width: 2,
          color: '#1687ff',
        },
        itemStyle: {
          color: '#1687ff',
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            {
              offset: 0,
              color: 'rgba(22, 135, 255, 0.20)',
            },
            {
              offset: 1,
              color: 'rgba(22, 135, 255, 0.01)',
            },
          ]),
        },
        data: trendData.map(([timestamp, activity]) => [
          timestamp,
          activity.count,
        ]),
      },
      {
        name: 'Amount',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        showSymbol: trendData.length <= 16,
        lineStyle: {
          width: 2,
          color: '#14b8a6',
        },
        itemStyle: {
          color: '#14b8a6',
        },
        data: trendData.map(([timestamp, activity]) => [
          timestamp,
          activity.amount,
        ]),
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
      top: 8,
      right: 28,
      bottom: 8,
      left: 76,
    },
    xAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: {
        color: '#70849a',
        fontSize: 9,
      },
      splitLine: {
        lineStyle: {
          color: '#172b40',
        },
      },
    },
    yAxis: {
      type: 'category',
      inverse: true,
      data: ruleTypes.map((ruleType) => ruleType.label),
      axisLabel: {
        color: '#8fa2b7',
        fontSize: 9,
      },
      axisLine: {
        lineStyle: {
          color: '#1b3047',
        },
      },
      axisTick: {
        show: false,
      },
    },
    series: [
      {
        name: 'Alerts',
        type: 'bar',
        barMaxWidth: 14,
        data: ruleTypes.map((ruleType) => ({
          value: typeCounts[ruleType.key] || 0,
          itemStyle: {
            color: ruleType.color,
            borderRadius: [0, 7, 7, 0],
          },
        })),
        label: {
          show: true,
          position: 'right',
          color: '#8fa2b7',
          fontSize: 9,
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
        color: '#8fa2b7',
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
          color: '#a9b8c8',
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

let unsubscribeUpdates = null

onMounted(() => {
  window.addEventListener('resize', resizeCharts)
  loadDashboard()
  unsubscribeUpdates = subscribeToTransactionUpdates(() => {
    loadDashboard()
  })
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  unsubscribeUpdates?.()
  transactionTrendChart?.dispose()
  alertTypeChart?.dispose()
  severityChart?.dispose()
})
</script>

<template>
  <section v-loading="loading" class="dashboard-page">
    <div class="page-heading">
      <div>
        <h1>Dashboard</h1>
        <p>Overview of transaction monitoring and alerts</p>
      </div>
      <div class="live-status">
        <span class="live-indicator">
          <span />
          Live data
        </span>
        <small>Last updated: {{ lastUpdated || '—' }}</small>
        <el-button
          class="refresh-button"
          :loading="loading"
          :icon="Refresh"
          @click="loadDashboard"
        >
          Refresh
        </el-button>
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
              {{ baseCurrencyLabel }}
          </div>
            <div class="statistic-description">
              Total transaction volume in {{ baseCurrencyLabel }} equivalent
            </div>
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

    <div class="overview-grid">
      <el-card class="risk-overview-card" shadow="never">
        <div class="risk-overview">
          <div class="risk-summary">
            <span class="section-eyebrow">Risk Overview</span>
            <div class="risk-score" :class="riskLevelClass">
              {{ riskOverview.score }}
            </div>
            <span class="risk-level-badge" :class="riskLevelClass">
              {{ riskOverview.level }}
            </span>
            <p>Current risk level based on active alerts</p>
            <span class="active-rate">
              {{ activeAlertRate }}% of alerts currently open
            </span>
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

      <el-card class="rule-chart-card chart-card" shadow="hover">
        <template #header>
          <div class="chart-title">
            <span class="chart-title-icon">
              <el-icon><Histogram /></el-icon>
            </span>
            <div>
              <strong>Triggered Rules</strong>
              <small>Alerts by monitoring rule</small>
            </div>
          </div>
        </template>
        <div ref="alertTypeRef" class="chart chart--rule" />
      </el-card>
    </div>

    <div class="analytics-grid">
      <el-card class="chart-card transaction-chart-card" shadow="hover">
        <template #header>
          <div class="chart-title">
            <span class="chart-title-icon">
              <el-icon><TrendCharts /></el-icon>
            </span>
            <div>
              <strong>Transaction Trend</strong>
              <small>Hourly transaction count and monitored amount</small>
            </div>
          </div>
        </template>
        <div ref="transactionTrendRef" class="chart" />
      </el-card>

      <el-card class="chart-card severity-chart-card" shadow="hover">
        <template #header>
          <div class="chart-title">
            <span class="chart-title-icon">
              <el-icon><PieChart /></el-icon>
            </span>
            <div>
              <strong>Risk Level Distribution</strong>
              <small>Alerts grouped by severity</small>
            </div>
          </div>
        </template>
        <div ref="severityRef" class="chart chart--compact" />
      </el-card>

      <el-card class="top-risk-card" shadow="hover">
        <template #header>
          <div class="chart-title">
            <span class="chart-title-icon">
              <el-icon><Warning /></el-icon>
            </span>
            <div>
              <strong>Top Risky Transactions</strong>
              <small>Highest-priority monitored activity</small>
            </div>
          </div>
        </template>
        <div v-if="recentAlerts.length" class="top-risk-list">
          <div
            v-for="(alert, index) in recentAlerts"
            :key="`risk-${alert.id}`"
            class="top-risk-item"
          >
            <span
              class="risk-rank"
              :class="`risk-rank--${alert.severity?.toLowerCase()}`"
            >
              {{ index + 1 }}
            </span>
            <div class="top-risk-details">
              <strong>Transaction #{{ alert.transactionId }}</strong>
              <span v-if="alert.amount != null">
                {{ formatNumber(alert.amount) }} {{ alert.currency || '' }}
              </span>
              <span v-else>{{ alert.ruleName || alert.ruleType }}</span>
            </div>
            <el-tag :type="severityTagType(alert.severity)" effect="dark">
              {{ alert.severity }}
            </el-tag>
          </div>
        </div>
        <el-empty
          v-else
          :image-size="58"
          description="No risk events"
        />
      </el-card>
    </div>

    <el-card class="recent-alerts-card" shadow="never">
      <template #header>
        <div class="recent-alerts-heading">
          <div>
            <span class="section-eyebrow">Risk Feed</span>
            <h2>Recent Alerts</h2>
            <p>Latest events prioritized by severity.</p>
          </div>
          <span class="recent-count">{{ recentAlerts.length }} events</span>
        </div>
      </template>

      <div v-if="recentAlerts.length" class="recent-alerts-table">
        <div class="recent-alerts-row recent-alerts-row--header">
          <span>Alert ID</span>
          <span>Time</span>
          <span>Transaction</span>
          <span>Rule Triggered</span>
          <span>Amount</span>
          <span>Risk Level</span>
          <span>Status</span>
        </div>
        <div
          v-for="alert in recentAlerts"
          :key="alert.id"
          class="recent-alerts-row"
        >
          <strong>#{{ alert.id }}</strong>
          <time>{{ formatAlertTime(alert.createdTime) }}</time>
          <span>#{{ alert.transactionId }}</span>
          <span class="rule-name">{{ alert.ruleName || alert.ruleType }}</span>
          <span class="alert-amount">
            <template v-if="alert.amount != null">
              {{ formatNumber(alert.amount) }} {{ alert.currency || '' }}
            </template>
            <template v-else>—</template>
          </span>
          <span>
            <el-tag :type="severityTagType(alert.severity)" effect="dark">
              {{ alert.severity }}
            </el-tag>
          </span>
          <span>
            <el-tag :type="statusTagType(alert.status)" effect="plain">
              {{ alert.status }}
            </el-tag>
          </span>
        </div>
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

.risk-level-badge {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 4px 10px;
  border: 1px solid rgb(34 197 94 / 24%);
  border-radius: 5px;
  color: #66d79c;
  font-size: 11px;
  font-weight: 750;
  letter-spacing: 0.45px;
  line-height: 1;
  background: rgb(34 197 94 / 10%);
}

.risk-level-badge.medium-risk {
  border-color: rgb(245 158 11 / 26%);
  color: #f5b84f;
  background: rgb(245 158 11 / 10%);
}

.risk-level-badge.high-risk {
  border-color: rgb(239 68 68 / 28%);
  color: #ff7b87;
  background: rgb(239 68 68 / 11%);
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

/* Dark financial monitoring presentation */
.dashboard-page {
  color: #dbe7f5;
}

.page-heading {
  align-items: center;
  margin-bottom: 24px;
}

.page-heading h1 {
  margin: 0 0 6px;
  color: #f3f7fc;
  font-size: 30px;
}

.page-heading p {
  color: #8a9db2;
}

.live-status {
  display: grid;
  grid-template-columns: auto auto;
  align-items: center;
  justify-items: end;
  gap: 7px 12px;
}

.live-indicator {
  border-color: rgb(52 211 153 / 20%);
  color: #72e1ad;
  background: rgb(16 185 129 / 8%);
}

.live-status small {
  color: #657b92;
}

.refresh-button {
  grid-column: 1 / -1;
  border-color: rgb(22 135 255 / 38%);
  color: #66b1ff;
  background: rgb(22 135 255 / 8%);
}

.refresh-button:hover,
.refresh-button:focus {
  border-color: #1687ff;
  color: #ffffff;
  background: rgb(22 135 255 / 22%);
}

.statistics-grid {
  gap: 16px;
}

.statistic-card {
  --statistic-accent: #1687ff;
  --statistic-soft: rgb(22 135 255 / 16%);
  --statistic-border: rgb(22 135 255 / 22%);
  min-height: 148px;
  border-color: var(--statistic-border);
  border-radius: 11px;
  background:
    linear-gradient(145deg, rgb(17 53 93 / 96%), rgb(8 31 56 / 98%));
  box-shadow: 0 12px 30px rgb(0 0 0 / 20%);
}

.statistic-card:hover {
  box-shadow: 0 16px 36px rgb(0 0 0 / 28%);
}

.statistic-card::before {
  width: 2px;
}

.statistic-card::after {
  top: auto;
  right: -18px;
  bottom: -72px;
  width: 190px;
  height: 130px;
  border-radius: 50%;
  opacity: 0.09;
  filter: blur(1px);
}

.statistic-card :deep(.el-card__body) {
  padding: 19px 20px;
}

.statistic-card--blue {
  --statistic-accent: #1687ff;
  --statistic-soft: rgb(22 135 255 / 16%);
  --statistic-border: rgb(22 135 255 / 23%);
}

.statistic-card--green {
  --statistic-accent: #12b886;
  --statistic-soft: rgb(18 184 134 / 15%);
  --statistic-border: rgb(18 184 134 / 22%);
  background: linear-gradient(145deg, #0b4a3f, #0a2c2c);
}

.statistic-card--amber {
  --statistic-accent: #f59e0b;
  --statistic-soft: rgb(245 158 11 / 16%);
  --statistic-border: rgb(245 158 11 / 24%);
  background: linear-gradient(145deg, #553a15, #292615);
}

.statistic-card--red {
  --statistic-accent: #f04455;
  --statistic-soft: rgb(240 68 85 / 16%);
  --statistic-border: rgb(240 68 85 / 25%);
  background: linear-gradient(145deg, #54212d, #2e1824);
}

.statistic-content {
  min-height: 104px;
}

.statistic-header {
  width: 100%;
  flex-direction: row-reverse;
  justify-content: space-between;
}

.statistic-icon {
  border-color: rgb(255 255 255 / 8%);
  color: #ffffff;
  background: var(--statistic-accent);
  box-shadow: 0 8px 20px color-mix(in srgb, var(--statistic-accent) 28%, transparent);
}

.statistic-label {
  color: #d8e5f1;
  font-size: 12px;
}

.statistic-value {
  margin-top: 12px;
  color: #ffffff;
  font-size: clamp(27px, 1.85vw, 34px);
}

.statistic-value small {
  color: #a9bbcc;
  font-size: 12px;
  letter-spacing: 0.4px;
}

.statistic-description {
  color: #9aafc4;
}

.risk-overview-card,
.chart-card,
.top-risk-card,
.recent-alerts-card {
  border-color: #1b3047;
  border-radius: 11px;
  background:
    linear-gradient(145deg, rgb(14 31 50 / 98%), rgb(10 24 40 / 98%));
  box-shadow: 0 12px 32px rgb(0 0 0 / 18%);
}

.risk-overview-card {
  height: 250px;
  margin-bottom: 0;
  background:
    radial-gradient(circle at 8% 18%, rgb(22 135 255 / 10%), transparent 30%),
    linear-gradient(145deg, #0d1c2e, #0a1828);
}

.risk-overview-card :deep(.el-card__body) {
  height: 100%;
  padding: 22px 26px;
}

.risk-score {
  margin: 10px 0 8px;
  font-size: 46px;
}

.risk-summary {
  border-right-color: #1b3047;
}

.section-eyebrow {
  color: #6f87a0;
}

.risk-summary p {
  color: #8296aa;
}

.active-rate {
  display: block;
  margin-top: 7px;
  color: #5e748b;
  font-size: 10px;
}

.risk-row-heading {
  color: #8095aa;
}

.risk-row-heading strong {
  color: #dbe7f5;
}

.risk-track {
  background: #14283d;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.overview-grid .risk-overview-card {
  grid-column: span 3;
}

.overview-grid .rule-chart-card {
  grid-column: span 1;
  height: 250px;
  margin-top: 0;
  overflow: hidden;
}

.overview-grid .rule-chart-card :deep(.el-card__header) {
  padding: 14px 15px;
}

.overview-grid .rule-chart-card :deep(.el-card__body) {
  padding: 6px 10px 8px;
  overflow: hidden;
}

.analytics-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.analytics-grid .transaction-chart-card {
  grid-column: span 2;
}

.analytics-grid .severity-chart-card,
.analytics-grid .top-risk-card {
  grid-column: span 1;
}

.analytics-grid > .el-card {
  height: 375px;
  overflow: hidden;
}

.chart-card :deep(.el-card__header),
.top-risk-card :deep(.el-card__header),
.recent-alerts-card :deep(.el-card__header) {
  padding: 16px 18px;
  border-bottom-color: #1b3047;
}

.chart-card :deep(.el-card__body) {
  padding: 12px 14px 14px;
  overflow: hidden;
}

.chart-title-icon {
  color: #50a7ff;
  background: rgb(22 135 255 / 11%);
}

.chart-title strong {
  color: #e3edf7;
}

.chart-title small {
  color: #71869c;
}

.analytics-grid .chart-title {
  gap: 9px;
}

.analytics-grid .chart-title-icon {
  width: 32px;
  height: 32px;
}

.analytics-grid .chart-title strong {
  font-size: 12.5px;
}

.analytics-grid .chart-title small {
  font-size: 10px;
}

.chart {
  height: 275px;
}

.chart--compact {
  height: 275px;
}

.rule-chart-card {
  margin-top: 0;
}

.chart--rule {
  height: 168px;
}

.top-risk-card :deep(.el-card__body) {
  padding: 7px 16px 10px;
}

.top-risk-list {
  display: grid;
}

.top-risk-item {
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  min-height: 55px;
  border-bottom: 1px solid #172b40;
}

.top-risk-item:last-child {
  border-bottom: 0;
}

.risk-rank {
  display: grid;
  width: 25px;
  height: 25px;
  place-items: center;
  border-radius: 50%;
  color: #9cb0c4;
  font-size: 10px;
  background: #14283d;
}

.risk-rank--high {
  color: #ff8b98;
  background: rgb(239 68 68 / 18%);
}

.risk-rank--medium {
  color: #ffc45c;
  background: rgb(245 158 11 / 18%);
}

.risk-rank--low {
  color: #62dab0;
  background: rgb(16 185 129 / 16%);
}

.top-risk-details {
  display: grid;
  min-width: 0;
  gap: 4px;
}

.top-risk-details strong,
.top-risk-details span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.top-risk-details strong {
  color: #dbe7f5;
  font-size: 11px;
}

.top-risk-details span {
  color: #71869c;
  font-size: 10px;
}

.recent-alerts-card {
  overflow: hidden;
}

.recent-alerts-card :deep(.el-card__body) {
  padding: 0;
}

.recent-alerts-heading h2 {
  color: #e3edf7;
}

.recent-alerts-heading p {
  color: #71869c;
}

.recent-count {
  color: #8fa2b7;
  background: #14283d;
}

.recent-alerts-table {
  min-width: 940px;
  overflow: hidden;
}

.recent-alerts-row {
  display: grid;
  grid-template-columns:
    minmax(76px, 0.7fr)
    minmax(120px, 1.15fr)
    minmax(92px, 0.8fr)
    minmax(150px, 1.5fr)
    minmax(112px, 0.95fr)
    minmax(92px, 0.75fr)
    minmax(112px, 0.9fr);
  align-items: center;
  min-height: 50px;
  padding: 0 18px;
  border-bottom: 1px solid #172b40;
  color: #a9bacb;
  font-size: 11px;
}

.recent-alerts-row:not(.recent-alerts-row--header):hover {
  background: rgb(22 135 255 / 5%);
}

.recent-alerts-row:last-child {
  border-bottom: 0;
}

.recent-alerts-row--header {
  min-height: 43px;
  color: #71869c;
  font-size: 9px;
  font-weight: 750;
  letter-spacing: 0.7px;
  text-transform: uppercase;
  background: #101f32;
}

.recent-alerts-row strong {
  color: #64b1ff;
}

.recent-alerts-row time {
  color: #8296aa;
  font-variant-numeric: tabular-nums;
}

.recent-alerts-row .rule-name {
  overflow: hidden;
  color: #d0dce8;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent-alerts-row .alert-amount {
  color: #dbe7f5;
  font-weight: 650;
}

@media (max-width: 1200px) {
  .analytics-grid,
  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .analytics-grid .transaction-chart-card {
    grid-column: span 2;
  }

  .overview-grid .risk-overview-card,
  .overview-grid .rule-chart-card {
    grid-column: span 2;
  }

  .overview-grid .rule-chart-card {
    height: 300px;
  }

  .overview-grid .chart--rule {
    height: 215px;
  }

  .analytics-grid > .el-card {
    height: auto;
    min-height: 360px;
  }
}

@media (max-width: 900px) {
  .analytics-grid,
  .overview-grid {
    grid-template-columns: 1fr;
  }

  .analytics-grid .transaction-chart-card,
  .analytics-grid .severity-chart-card,
  .analytics-grid .top-risk-card,
  .overview-grid .risk-overview-card,
  .overview-grid .rule-chart-card {
    grid-column: span 1;
  }

  .risk-summary {
    border-right: 0;
    border-bottom-color: #1b3047;
  }

  .recent-alerts-card :deep(.el-card__body) {
    overflow-x: auto;
  }
}

@media (max-width: 560px) {
  .live-status {
    justify-items: start;
  }
}
</style>
