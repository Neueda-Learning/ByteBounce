<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Bell, Edit, Refresh, Search, View } from '@element-plus/icons-vue'
import http from '../api/axios'

const alerts = ref([])
const loading = ref(false)
const errorMessage = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const totalElements = ref(0)
const dialogVisible = ref(false)
const selectedAlert = ref(null)
const selectedStatus = ref('')
const submitting = ref(false)
const detailsDialogVisible = ref(false)
const detailsLoading = ref(false)
const alertDetails = ref(null)
const alertTimeline = ref([])
const filters = reactive({
  severity: '',
  status: '',
  ruleName: '',
})

const statusOptions = [
  'ACKNOWLEDGED',
  'INVESTIGATING',
  'DISMISSED',
  'CLOSED',
]

const statusTagType = {
  OPEN: 'danger',
  ACKNOWLEDGED: 'warning',
  INVESTIGATING: 'primary',
  CLOSED: 'success',
  DISMISSED: 'info',
}

const severityTagType = {
  LOW: 'info',
  MEDIUM: 'warning',
  HIGH: 'danger',
}

const loadAlerts = async () => {
  loading.value = true
  errorMessage.value = ''

  try {
    const response = await http.get('/alerts', {
      params: {
        page: currentPage.value - 1,
        size: pageSize.value,
      },
    })
    applyPage(response.data)
  } catch (error) {
    errorMessage.value = 'Failed to load alerts. Please try again later.'
    ElMessage.error(errorMessage.value)
  } finally {
    loading.value = false
  }
}

const searchAlerts = async () => {
  loading.value = true
  errorMessage.value = ''
  const params = Object.fromEntries(
    Object.entries(filters).filter(([, value]) => value !== ''),
  )
  params.page = currentPage.value - 1
  params.size = pageSize.value

  try {
    const response = await http.get('/alerts/search', { params })
    applyPage(response.data)
  } catch (error) {
    errorMessage.value = 'Failed to search alerts. Please try again later.'
    ElMessage.error(errorMessage.value)
  } finally {
    loading.value = false
  }
}

const hasActiveFilters = () =>
  Object.values(filters).some((value) => value !== '')

const applyPage = (page) => {
  alerts.value = page.content
  totalElements.value = page.totalElements
}

const executeSearch = () => {
  currentPage.value = 1
  searchAlerts()
}

const loadCurrentPage = () => {
  if (hasActiveFilters()) {
    searchAlerts()
  } else {
    loadAlerts()
  }
}

const handlePageChange = (page) => {
  currentPage.value = page
  loadCurrentPage()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadCurrentPage()
}

const resetFilters = () => {
  Object.assign(filters, { severity: '', status: '', ruleName: '' })
  currentPage.value = 1
  loadAlerts()
}

const formatDateTime = (value) =>
  value ? value.replace('T', ' ').slice(0, 19) : ''

const openStatusDialog = (alert) => {
  selectedAlert.value = alert
  selectedStatus.value = ''
  dialogVisible.value = true
}

const viewDetails = async (alert) => {
  detailsDialogVisible.value = true
  detailsLoading.value = true
  alertDetails.value = null
  alertTimeline.value = []

  try {
    const [detailsResponse, timelineResponse] = await Promise.all([
      http.get(`/alerts/${alert.id}/details`),
      http.get(`/alerts/${alert.id}/timeline`),
    ])
    alertDetails.value = detailsResponse.data
    alertTimeline.value = timelineResponse.data
  } catch (error) {
    detailsDialogVisible.value = false
    const message =
      error.response?.data?.message || 'Failed to load alert details.'
    ElMessage.error(message)
  } finally {
    detailsLoading.value = false
  }
}

const updateAlertStatus = async () => {
  if (!selectedAlert.value || !selectedStatus.value) {
    return
  }

  submitting.value = true

  try {
    await http.put(`/alerts/${selectedAlert.value.id}/status`, {
      status: selectedStatus.value,
    })
    ElMessage.success('Alert status updated successfully.')
    dialogVisible.value = false
    await loadCurrentPage()
  } catch (error) {
    const message =
      error.response?.data?.message ||
      'Failed to update alert status. Please check the selected transition.'
    ElMessage.error(message)
  } finally {
    submitting.value = false
  }
}

onMounted(loadAlerts)
</script>

<template>
  <section class="alerts-page">
    <div class="page-heading">
      <span class="page-icon page-icon--alert">
        <el-icon><Bell /></el-icon>
      </span>
      <div>
        <h1>Alerts</h1>
        <p>Investigate generated risk signals and manage their lifecycle.</p>
      </div>
    </div>

    <el-alert
      v-if="errorMessage"
      class="alerts-error"
      :title="errorMessage"
      type="error"
      show-icon
      :closable="false"
    />

    <el-card class="filter-card" shadow="never">
      <div class="filter-grid">
        <el-select v-model="filters.severity" clearable placeholder="Severity">
          <el-option label="HIGH" value="HIGH" />
          <el-option label="MEDIUM" value="MEDIUM" />
          <el-option label="LOW" value="LOW" />
        </el-select>
        <el-select v-model="filters.status" clearable placeholder="Status">
          <el-option label="OPEN" value="OPEN" />
          <el-option label="ACKNOWLEDGED" value="ACKNOWLEDGED" />
          <el-option label="INVESTIGATING" value="INVESTIGATING" />
          <el-option label="CLOSED" value="CLOSED" />
          <el-option label="DISMISSED" value="DISMISSED" />
        </el-select>
        <el-input
          v-model="filters.ruleName"
          clearable
          placeholder="Rule Name"
          @keyup.enter="executeSearch"
        />
        <div class="filter-actions">
          <el-button type="primary" :icon="Search" @click="executeSearch">
            Search
          </el-button>
          <el-button :icon="Refresh" @click="resetFilters">Reset</el-button>
        </div>
      </div>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table
        v-loading="loading"
        :data="alerts"
        stripe
        empty-text="No alerts found"
      >
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column
        prop="transactionId"
        label="Transaction ID"
        width="130"
      />
      <el-table-column
        prop="ruleName"
        label="Rule Name"
        min-width="210"
        show-overflow-tooltip
      />
      <el-table-column prop="ruleType" label="Type" min-width="180" />
      <el-table-column label="Severity" width="120">
        <template #default="{ row }">
          <el-tag
            :type="severityTagType[row.severity] || 'info'"
            effect="light"
            round
          >
            {{ row.severity }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Status" min-width="150">
        <template #default="{ row }">
          <el-tag
            :type="statusTagType[row.status] || 'info'"
            effect="dark"
            round
          >
            {{ row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        prop="message"
        label="Message"
        min-width="260"
        show-overflow-tooltip
      />
      <el-table-column
        prop="createdTime"
        label="Created Time"
        min-width="190"
      />
      <el-table-column label="Actions" width="240" fixed="right">
        <template #default="{ row }">
          <div class="action-buttons">
            <el-button
              type="primary"
              size="small"
              plain
              :icon="View"
              @click="viewDetails(row)"
            >
              View Details
            </el-button>
            <el-button
              type="warning"
              size="small"
              plain
              :icon="Edit"
              @click="openStatusDialog(row)"
            >
              Change Status
            </el-button>
          </div>
        </template>
      </el-table-column>
        <template #empty>
          <el-empty
            description="No risk alerts have been generated"
            :image-size="92"
          />
        </template>
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination
          :current-page="currentPage"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="totalElements"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      title="Change Alert Status"
      width="420px"
      class="management-dialog"
    >
      <el-form label-position="top">
        <el-form-item label="Alert">
          <el-input
            :model-value="selectedAlert ? `#${selectedAlert.id}` : ''"
            disabled
          />
        </el-form-item>

        <el-form-item label="New Status" required>
          <el-select
            v-model="selectedStatus"
            placeholder="Select a status"
            class="status-select"
          >
            <el-option
              v-for="status in statusOptions"
              :key="status"
              :label="status"
              :value="status"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">
          Cancel
        </el-button>
        <el-button
          type="primary"
          :loading="submitting"
          :disabled="!selectedStatus"
          @click="updateAlertStatus"
        >
          Update
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="detailsDialogVisible"
      title="Alert Details"
      width="760px"
      class="management-dialog"
    >
      <div v-loading="detailsLoading" class="details-content">
        <template v-if="alertDetails">
          <h3>Alert Information</h3>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="Alert ID">
              {{ alertDetails.alertId }}
            </el-descriptions-item>
            <el-descriptions-item label="Rule ID">
              {{ alertDetails.ruleId }}
            </el-descriptions-item>
            <el-descriptions-item label="Rule Name">
              {{ alertDetails.ruleName }}
            </el-descriptions-item>
            <el-descriptions-item label="Type">
              {{ alertDetails.ruleType }}
            </el-descriptions-item>
            <el-descriptions-item label="Severity">
              <el-tag
                :type="severityTagType[alertDetails.severity] || 'info'"
                round
              >
                {{ alertDetails.severity }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="Status">
              <el-tag
                :type="statusTagType[alertDetails.status] || 'info'"
                effect="dark"
                round
              >
                {{ alertDetails.status }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="Message" :span="2">
              {{ alertDetails.message }}
            </el-descriptions-item>
          </el-descriptions>

          <h3>Triggered Transaction</h3>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="Transaction ID">
              {{ alertDetails.transactionId }}
            </el-descriptions-item>
            <el-descriptions-item label="Type">
              {{ alertDetails.type }}
            </el-descriptions-item>
            <el-descriptions-item label="Account ID">
              {{ alertDetails.accountId }}
            </el-descriptions-item>
            <el-descriptions-item label="Payee ID">
              {{ alertDetails.payeeId }}
            </el-descriptions-item>
            <el-descriptions-item label="Amount">
              {{ alertDetails.amount }}
            </el-descriptions-item>
            <el-descriptions-item label="Currency">
              {{ alertDetails.currency }}
            </el-descriptions-item>
            <el-descriptions-item label="Transaction Time" :span="2">
              {{ alertDetails.transactionTime }}
            </el-descriptions-item>
          </el-descriptions>

          <h3>Alert Timeline</h3>
          <el-timeline v-if="alertTimeline.length" class="alert-timeline">
            <el-timeline-item
              v-for="(event, index) in alertTimeline"
              :key="`${event.status}-${event.changedTime}-${index}`"
              :timestamp="formatDateTime(event.changedTime)"
              placement="top"
              :type="statusTagType[event.status] || 'primary'"
            >
              <el-tag
                :type="statusTagType[event.status] || 'info'"
                :effect="index === alertTimeline.length - 1 ? 'dark' : 'light'"
                round
              >
                {{ event.status }}
              </el-tag>
            </el-timeline-item>
          </el-timeline>
          <el-empty
            v-else
            description="No status history available"
            :image-size="72"
          />
        </template>
      </div>

      <template #footer>
        <el-button @click="detailsDialogVisible = false">
          Close
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.page-heading {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 24px;
}

.page-heading h1 {
  margin: 0 0 5px;
}

.page-heading p {
  margin: 0;
  color: #7b8798;
  font-size: 14px;
}

.page-icon {
  display: grid;
  width: 46px;
  height: 46px;
  place-items: center;
  border-radius: 13px;
  font-size: 23px;
}

.page-icon--alert {
  color: #dc2626;
  background: #fff0f0;
}

.alerts-error {
  margin-bottom: 20px;
}

.filter-card {
  margin-bottom: 18px;
  border: 1px solid #e6ebf2;
  border-radius: 15px;
}

.filter-grid {
  display: grid;
  grid-template-columns: minmax(160px, 0.8fr) minmax(190px, 1fr) minmax(240px, 1.5fr) auto;
  gap: 12px;
  align-items: center;
}

.filter-actions {
  display: flex;
  white-space: nowrap;
}

.table-card {
  overflow: hidden;
  border: 1px solid #e6ebf2;
  border-radius: 15px;
  box-shadow: 0 8px 28px rgb(15 23 42 / 5%);
}

.table-card :deep(.el-card__body) {
  padding: 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 16px 20px;
  border-top: 1px solid #eef1f5;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 7px;
}

.action-buttons :deep(.el-button + .el-button) {
  margin-left: 0;
}

.status-select {
  width: 100%;
}

.details-content {
  min-height: 180px;
}

.details-content h3 {
  margin: 22px 0 12px;
  color: #263247;
  font-size: 15px;
  font-weight: 700;
}

.details-content h3:first-child {
  margin-top: 0;
}

.alert-timeline {
  padding: 8px 12px 0;
}

:deep(.el-descriptions__label) {
  color: #64748b !important;
  font-weight: 650 !important;
}

:deep(.el-table__body tr:hover > td.el-table__cell) {
  background: #fff8f8 !important;
}

@media (max-width: 900px) {
  .filter-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 620px) {
  .filter-grid {
    grid-template-columns: 1fr;
  }
}
</style>
