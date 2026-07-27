<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CreditCard, Refresh, Search } from '@element-plus/icons-vue'
import http from '../api/axios'

const transactions = ref([])
const loading = ref(false)
const errorMessage = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const totalElements = ref(0)
const filters = reactive({
  accountId: '',
  payeeId: '',
  type: '',
  minAmount: null,
  maxAmount: null,
})

const transactionTypeTag = {
  DEBIT: 'danger',
  CREDIT: 'success',
  TRANSFER: 'primary',
  PAYMENT: 'warning',
}

const formatAmount = (value) =>
  Number(value || 0).toLocaleString(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 4,
  })

const loadTransactions = async () => {
  loading.value = true
  errorMessage.value = ''

  try {
    const response = await http.get('/transactions', {
      params: {
        page: currentPage.value - 1,
        size: pageSize.value,
      },
    })
    applyPage(response.data)
  } catch (error) {
    errorMessage.value = 'Failed to load transactions. Please try again later.'
    ElMessage.error(errorMessage.value)
  } finally {
    loading.value = false
  }
}

const searchTransactions = async () => {
  loading.value = true
  errorMessage.value = ''

  const params = Object.fromEntries(
    Object.entries(filters).filter(([, value]) => value !== '' && value !== null),
  )
  params.page = currentPage.value - 1
  params.size = pageSize.value

  try {
    const response = await http.get('/transactions/search', { params })
    applyPage(response.data)
  } catch (error) {
    errorMessage.value = 'Failed to search transactions. Please try again later.'
    ElMessage.error(errorMessage.value)
  } finally {
    loading.value = false
  }
}

const hasActiveFilters = () =>
  Object.values(filters).some((value) => value !== '' && value !== null)

const applyPage = (page) => {
  transactions.value = page.content
  totalElements.value = page.totalElements
}

const executeSearch = () => {
  currentPage.value = 1
  searchTransactions()
}

const loadCurrentPage = () => {
  if (hasActiveFilters()) {
    searchTransactions()
  } else {
    loadTransactions()
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
  Object.assign(filters, {
    accountId: '',
    payeeId: '',
    type: '',
    minAmount: null,
    maxAmount: null,
  })
  currentPage.value = 1
  loadTransactions()
}

onMounted(loadTransactions)
</script>

<template>
  <section class="transactions-page">
    <div class="page-heading">
      <span class="page-icon">
        <el-icon><CreditCard /></el-icon>
      </span>
      <div>
        <h1>Transactions</h1>
        <p>Review every transaction evaluated by monitoring rules.</p>
      </div>
    </div>

    <el-alert
      v-if="errorMessage"
      class="transactions-error"
      :title="errorMessage"
      type="error"
      show-icon
      :closable="false"
    />

    <el-card class="filter-card" shadow="never">
      <div class="filter-grid">
        <el-input
          v-model="filters.accountId"
          clearable
          placeholder="Account ID"
          @keyup.enter="executeSearch"
        />
        <el-input
          v-model="filters.payeeId"
          clearable
          placeholder="Payee ID"
          @keyup.enter="executeSearch"
        />
        <el-select v-model="filters.type" clearable placeholder="Transaction Type">
          <el-option label="DEBIT" value="DEBIT" />
          <el-option label="CREDIT" value="CREDIT" />
        </el-select>
        <el-input-number
          v-model="filters.minAmount"
          :min="0"
          :precision="2"
          controls-position="right"
          placeholder="Min Amount"
        />
        <el-input-number
          v-model="filters.maxAmount"
          :min="0"
          :precision="2"
          controls-position="right"
          placeholder="Max Amount"
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
        :data="transactions"
        stripe
        empty-text="No transactions found"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="accountId" label="Account ID" min-width="140" />
        <el-table-column prop="payeeId" label="Payee ID" min-width="140" />
        <el-table-column label="Amount" min-width="130">
          <template #default="{ row }">
            <span class="amount-value">{{ formatAmount(row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="currency" label="Currency" width="100" />
        <el-table-column label="Type" min-width="130">
          <template #default="{ row }">
            <el-tag
              :type="transactionTypeTag[row.type] || 'info'"
              effect="light"
              round
            >
              {{ row.type }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Risk Status" min-width="145">
          <template #default="{ row }">
            <el-tag
              :type="row.hasAlert ? 'danger' : 'success'"
              :effect="row.hasAlert ? 'dark' : 'light'"
              round
            >
              {{ row.hasAlert ? `Risk Alert (${row.alertCount})` : 'Normal' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="transactionTime"
          label="Transaction Time"
          min-width="190"
        />
        <el-table-column
          prop="description"
          label="Description"
          min-width="220"
          show-overflow-tooltip
        />

        <template #empty>
          <el-empty
            description="No transactions have been recorded yet"
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
  color: #2563eb;
  font-size: 23px;
  background: #eaf2ff;
}

.transactions-error {
  margin-bottom: 20px;
}

.filter-card {
  margin-bottom: 18px;
  border: 1px solid #e6ebf2;
  border-radius: 15px;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(145px, 1fr)) auto;
  gap: 12px;
  align-items: center;
}

.filter-grid :deep(.el-input-number) {
  width: 100%;
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

.amount-value {
  color: #172033;
  font-variant-numeric: tabular-nums;
  font-weight: 700;
}

:deep(.el-table__body tr:hover > td.el-table__cell) {
  background: #f3f7ff !important;
}

@media (max-width: 1200px) {
  .filter-grid {
    grid-template-columns: repeat(3, minmax(180px, 1fr));
  }
}

@media (max-width: 720px) {
  .filter-grid {
    grid-template-columns: 1fr;
  }
}
</style>
