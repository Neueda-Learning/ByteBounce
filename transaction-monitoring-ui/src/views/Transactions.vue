<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CreditCard, Plus, Refresh, Search } from '@element-plus/icons-vue'
import http from '../api/axios'

const transactions = ref([])
const loading = ref(false)
const errorMessage = ref('')
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref()
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

const transactionTypeOptions = ['DEBIT', 'CREDIT', 'TRANSFER', 'PAYMENT']

const emptyForm = () => ({
  accountId: '',
  payeeId: '',
  amount: null,
  currency: 'CNY',
  type: 'TRANSFER',
  description: '',
})

const transactionForm = reactive(emptyForm())

const formRules = {
  accountId: [
    { required: true, message: 'Account ID is required', trigger: 'blur' },
  ],
  payeeId: [
    { required: true, message: 'Payee ID is required', trigger: 'blur' },
  ],
  amount: [
    { required: true, message: 'Amount is required', trigger: 'blur' },
  ],
  currency: [
    { required: true, message: 'Currency is required', trigger: 'blur' },
    {
      pattern: /^[A-Za-z]{3}$/,
      message: 'Currency must be a 3-letter code',
      trigger: 'blur',
    },
  ],
  type: [
    { required: true, message: 'Transaction type is required', trigger: 'change' },
  ],
}

const formatAmount = (value) =>
  Number(value || 0).toLocaleString(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 4,
  })

const resetTransactionForm = () => {
  Object.assign(transactionForm, emptyForm())
  nextTick(() => formRef.value?.clearValidate())
}

const openCreateDialog = () => {
  resetTransactionForm()
  dialogVisible.value = true
}

const normalizeCreatePayload = () => ({
  accountId: transactionForm.accountId.trim(),
  payeeId: transactionForm.payeeId.trim(),
  amount: transactionForm.amount,
  currency: transactionForm.currency.trim().toUpperCase(),
  type: transactionForm.type,
  description: transactionForm.description.trim() || null,
})

const extractErrorMessage = (error, fallback) => {
  const responseData = error.response?.data
  const fieldErrors = responseData?.errors

  if (fieldErrors && typeof fieldErrors === 'object') {
    return Object.values(fieldErrors)[0]
  }

  return responseData?.detail || responseData?.message || fallback
}

const createTransaction = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || !(transactionForm.amount > 0)) {
    if (!(transactionForm.amount > 0)) {
      ElMessage.warning('Amount must be greater than zero.')
    }
    return
  }

  submitting.value = true

  try {
    await http.post('/transactions', normalizeCreatePayload())
    ElMessage.success('Transaction created successfully.')
    dialogVisible.value = false
    currentPage.value = 1
    resetFilters()
  } catch (error) {
    ElMessage.error(
      extractErrorMessage(error, 'Failed to create the transaction.'),
    )
  } finally {
    submitting.value = false
  }
}

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
    <div class="page-header">
      <div class="page-heading">
        <span class="page-icon">
          <el-icon><CreditCard /></el-icon>
        </span>
        <div>
          <h1>Transactions</h1>
          <p>Review every transaction evaluated by monitoring rules.</p>
        </div>
      </div>
      <el-button
        type="primary"
        :icon="Plus"
        class="create-button"
        @click="openCreateDialog"
      >
        Create Transaction
      </el-button>
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

    <el-dialog
      v-model="dialogVisible"
      title="Create Transaction"
      width="520px"
      class="management-dialog"
    >
      <el-form
        ref="formRef"
        :model="transactionForm"
        :rules="formRules"
        label-position="top"
      >
        <el-form-item label="Account ID" prop="accountId">
          <el-input
            v-model="transactionForm.accountId"
            maxlength="64"
            placeholder="Enter account ID"
          />
        </el-form-item>

        <el-form-item label="Payee ID" prop="payeeId">
          <el-input
            v-model="transactionForm.payeeId"
            maxlength="64"
            placeholder="Enter payee ID"
          />
        </el-form-item>

        <el-form-item label="Amount" prop="amount">
          <el-input-number
            v-model="transactionForm.amount"
            class="form-control"
            :min="0.0001"
            :precision="4"
            :controls="false"
            placeholder="Enter amount"
          />
        </el-form-item>

        <el-form-item label="Currency" prop="currency">
          <el-input
            v-model="transactionForm.currency"
            maxlength="3"
            placeholder="Enter 3-letter currency code"
          />
        </el-form-item>

        <el-form-item label="Type" prop="type">
          <el-select
            v-model="transactionForm.type"
            class="form-control"
            placeholder="Select transaction type"
          >
            <el-option
              v-for="type in transactionTypeOptions"
              :key="type"
              :label="type"
              :value="type"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="Description">
          <el-input
            v-model="transactionForm.description"
            type="textarea"
            :rows="3"
            maxlength="500"
            placeholder="Enter description"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button
          type="primary"
          :loading="submitting"
          @click="createTransaction"
        >
          Save
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.page-heading {
  display: flex;
  align-items: center;
  gap: 14px;
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

.create-button {
  height: 40px;
  padding: 0 18px;
  border-radius: 10px;
  box-shadow: 0 7px 16px rgb(37 99 235 / 20%);
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

.form-control {
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
  .page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-grid {
    grid-template-columns: 1fr;
  }
}
</style>
