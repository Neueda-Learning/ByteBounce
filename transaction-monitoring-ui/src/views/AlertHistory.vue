<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api/axios'

const historyRecords = ref([])
const loading = ref(false)
const errorMessage = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const totalElements = ref(0)

const statusTagType = {
  OPEN: 'danger',
  ACKNOWLEDGED: 'warning',
  INVESTIGATING: 'primary',
  CLOSED: 'success',
  DISMISSED: 'info',
}

const loadHistory = async () => {
  loading.value = true
  errorMessage.value = ''

  try {
    const response = await http.get('/alert-history', {
      params: {
        page: currentPage.value - 1,
        size: pageSize.value,
      },
    })
    historyRecords.value = response.data.content
    totalElements.value = response.data.totalElements
  } catch (error) {
    errorMessage.value = 'Failed to load alert history. Please try again later.'
    ElMessage.error(errorMessage.value)
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page) => {
  currentPage.value = page
  loadHistory()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadHistory()
}

onMounted(loadHistory)
</script>

<template>
  <section class="history-page">
    <h1>Alert History</h1>

    <el-alert
      v-if="errorMessage"
      class="history-error"
      :title="errorMessage"
      type="error"
      show-icon
      :closable="false"
    />

    <el-table
      v-loading="loading"
      :data="historyRecords"
      stripe
      border
      empty-text="No alert history found"
    >
      <el-table-column prop="id" label="History ID" width="120" />
      <el-table-column prop="alertId" label="Alert ID" width="120" />
      <el-table-column label="Old Status" min-width="170">
        <template #default="{ row }">
          <el-tag :type="statusTagType[row.oldStatus] || 'info'">
            {{ row.oldStatus }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="New Status" min-width="170">
        <template #default="{ row }">
          <el-tag :type="statusTagType[row.newStatus] || 'info'">
            {{ row.newStatus }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        prop="changedTime"
        label="Changed Time"
        min-width="210"
      />
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
  </section>
</template>

<style scoped>
.history-page h1 {
  margin-bottom: 24px;
}

.history-error {
  margin-bottom: 20px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
