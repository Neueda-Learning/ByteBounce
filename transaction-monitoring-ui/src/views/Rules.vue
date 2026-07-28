<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Delete,
  Edit,
  Plus,
  Setting,
  SwitchButton,
} from '@element-plus/icons-vue'
import http from '../api/axios'

const rules = ref([])
const loading = ref(false)
const errorMessage = ref('')
const dialogVisible = ref(false)
const submitting = ref(false)
const editingRuleId = ref(null)
const formRef = ref()

const ruleTypes = [
  'AMOUNT_THRESHOLD',
  'VELOCITY',
  'NEW_PAYEE',
  'DAILY_LIMIT',
]
const severityOptions = ['LOW', 'MEDIUM', 'HIGH']

const typeTagType = {
  AMOUNT_THRESHOLD: 'danger',
  VELOCITY: 'warning',
  NEW_PAYEE: 'primary',
  DAILY_LIMIT: 'success',
}

const severityTagType = {
  LOW: 'info',
  MEDIUM: 'warning',
  HIGH: 'danger',
}

const emptyForm = () => ({
  name: '',
  type: '',
  description: '',
  threshold: null,
  timeWindow: null,
  maxCount: null,
  severity: '',
  enabled: true,
})

const ruleForm = reactive(emptyForm())

const formRules = {
  name: [{ required: true, message: 'Rule name is required', trigger: 'blur' }],
  type: [{ required: true, message: 'Rule type is required', trigger: 'change' }],
  severity: [
    { required: true, message: 'Severity is required', trigger: 'change' },
  ],
}

const dialogTitle = computed(() =>
  editingRuleId.value ? 'Edit Rule' : 'Create Rule',
)

const loadRules = async () => {
  loading.value = true
  errorMessage.value = ''

  try {
    const response = await http.get('/rules')
    rules.value = response.data
  } catch (error) {
    errorMessage.value = 'Failed to load rules. Please try again later.'
    ElMessage.error(errorMessage.value)
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  Object.assign(ruleForm, emptyForm())
  editingRuleId.value = null
  nextTick(() => formRef.value?.clearValidate())
}

const openCreateDialog = () => {
  resetForm()
  dialogVisible.value = true
}

const openEditDialog = (rule) => {
  editingRuleId.value = rule.id
  Object.assign(ruleForm, {
    name: rule.name,
    type: rule.type,
    description: rule.description || '',
    threshold: rule.threshold,
    timeWindow: rule.timeWindow,
    maxCount: rule.maxCount,
    severity: rule.severity,
    enabled: rule.enabled,
  })
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const validateTypeParameters = () => {
  if (
    ['AMOUNT_THRESHOLD', 'DAILY_LIMIT'].includes(ruleForm.type) &&
    !(ruleForm.threshold > 0)
  ) {
    ElMessage.warning('Threshold must be greater than zero for this rule type.')
    return false
  }

  if (
    ruleForm.type === 'VELOCITY' &&
    (!(ruleForm.timeWindow > 0) || !(ruleForm.maxCount > 0))
  ) {
    ElMessage.warning(
      'Time window and max count must be greater than zero for VELOCITY.',
    )
    return false
  }

  return true
}

const normalizePayload = () => ({
  name: ruleForm.name.trim(),
  type: ruleForm.type,
  description: ruleForm.description || null,
  threshold: ['AMOUNT_THRESHOLD', 'DAILY_LIMIT'].includes(ruleForm.type)
    ? ruleForm.threshold
    : null,
  timeWindow: ruleForm.type === 'VELOCITY' ? ruleForm.timeWindow : null,
  maxCount: ruleForm.type === 'VELOCITY' ? ruleForm.maxCount : null,
  severity: ruleForm.severity,
  enabled: ruleForm.enabled,
})

const saveRule = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid || !validateTypeParameters()) {
    return
  }

  submitting.value = true

  try {
    const payload = normalizePayload()

    if (editingRuleId.value) {
      await http.put(`/rules/${editingRuleId.value}`, payload)
      ElMessage.success('Rule updated successfully.')
    } else {
      await http.post('/rules', payload)
      ElMessage.success('Rule created successfully.')
    }

    dialogVisible.value = false
    await loadRules()
  } catch (error) {
    const message =
      error.response?.data?.message || 'Failed to save the monitoring rule.'
    ElMessage.error(message)
  } finally {
    submitting.value = false
  }
}

const toggleRuleStatus = async (rule) => {
  try {
    await http.patch(`/rules/${rule.id}/status`, {
      enabled: !rule.enabled,
    })
    ElMessage.success(`Rule ${rule.enabled ? 'disabled' : 'enabled'} successfully.`)
    await loadRules()
  } catch (error) {
    const message =
      error.response?.data?.message || 'Failed to update rule status.'
    ElMessage.error(message)
  }
}

const deleteRule = async (rule) => {
    try {
      await ElMessageBox.confirm(
      `Remove "${rule.name}" from active monitoring? The rule will be retained as Disabled so existing alerts remain readable.`,
      'Disable Rule',
      {
        confirmButtonText: 'Disable',
        cancelButtonText: 'Cancel',
        type: 'warning',
      },
    )

    await http.delete(`/rules/${rule.id}`)
    ElMessage.success('Rule disabled successfully.')
    await loadRules()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }

    const message =
      error.response?.data?.message || 'Failed to delete the monitoring rule.'
    ElMessage.error(message)
  }
}

onMounted(loadRules)
</script>

<template>
  <section class="rules-page">
    <div class="page-header">
      <div class="page-heading">
        <span class="page-icon">
          <el-icon><Setting /></el-icon>
        </span>
        <div>
          <h1>Rules</h1>
          <p>Configure the controls used to evaluate transaction risk.</p>
        </div>
      </div>
      <el-button
        type="primary"
        :icon="Plus"
        class="create-button"
        @click="openCreateDialog"
      >
        Create Rule
      </el-button>
    </div>

    <el-alert
      v-if="errorMessage"
      class="rules-error"
      :title="errorMessage"
      type="error"
      show-icon
      :closable="false"
    />

    <el-card class="table-card" shadow="never">
      <el-table
        v-loading="loading"
        :data="rules"
        stripe
        empty-text="No rules found"
      >
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="Name" min-width="210" />
      <el-table-column label="Type" min-width="180">
        <template #default="{ row }">
          <el-tag
            :type="typeTagType[row.type] || 'info'"
            effect="light"
            round
          >
            {{ row.type }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        prop="description"
        label="Description"
        min-width="260"
        show-overflow-tooltip
      />
      <el-table-column prop="threshold" label="Threshold" width="120" />
      <el-table-column prop="timeWindow" label="Time Window" width="130" />
      <el-table-column prop="maxCount" label="Max Count" width="110" />
      <el-table-column label="Severity" width="110">
        <template #default="{ row }">
          <el-tag
            :type="severityTagType[row.severity] || 'info'"
            effect="dark"
            round
          >
            {{ row.severity }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Enabled" width="100">
        <template #default="{ row }">
          <el-tag
            :type="row.enabled ? 'success' : 'info'"
            effect="light"
            round
          >
            {{ row.enabled ? 'Enabled' : 'Disabled' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        prop="createdTime"
        label="Created Time"
        min-width="190"
      />
      <el-table-column label="Actions" width="300" fixed="right">
        <template #default="{ row }">
          <div class="action-buttons">
            <el-button
              type="primary"
              size="small"
              plain
              :icon="Edit"
              @click="openEditDialog(row)"
            >
              Edit
            </el-button>
            <el-button
              :type="row.enabled ? 'warning' : 'success'"
              size="small"
              plain
              :icon="SwitchButton"
              @click="toggleRuleStatus(row)"
            >
              {{ row.enabled ? 'Disable' : 'Enable' }}
            </el-button>
            <el-button
              type="danger"
              size="small"
              plain
              :icon="Delete"
              @click="deleteRule(row)"
            >
              Delete
            </el-button>
          </div>
        </template>
      </el-table-column>
        <template #empty>
          <el-empty
            description="No monitoring rules configured"
            :image-size="92"
          />
        </template>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      class="management-dialog"
      @closed="resetForm"
    >
      <el-form
        ref="formRef"
        :model="ruleForm"
        :rules="formRules"
        label-position="top"
      >
        <el-form-item label="Name" prop="name">
          <el-input
            v-model="ruleForm.name"
            maxlength="100"
            placeholder="Enter rule name"
          />
        </el-form-item>

        <el-form-item label="Type" prop="type">
          <el-select
            v-model="ruleForm.type"
            class="form-control"
            placeholder="Select rule type"
          >
            <el-option
              v-for="type in ruleTypes"
              :key="type"
              :label="type"
              :value="type"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="Description">
          <el-input
            v-model="ruleForm.description"
            type="textarea"
            :rows="3"
            maxlength="500"
            placeholder="Enter rule description"
          />
        </el-form-item>

        <el-form-item
          v-if="['AMOUNT_THRESHOLD', 'DAILY_LIMIT'].includes(ruleForm.type)"
          label="Threshold"
          required
        >
          <div class="threshold-field">
            <el-input-number
              v-model="ruleForm.threshold"
              class="form-control"
              :min="0.01"
              :precision="2"
              :controls="false"
              placeholder="Enter threshold"
            />
            <div class="field-hint">
              Amount-based thresholds are compared in the base currency (USD).
            </div>
          </div>
        </el-form-item>

        <template v-if="ruleForm.type === 'VELOCITY'">
          <el-form-item label="Time Window (minutes)" required>
            <el-input-number
              v-model="ruleForm.timeWindow"
              class="form-control"
              :min="1"
              :precision="0"
              :controls="false"
              placeholder="Enter time window"
            />
          </el-form-item>

          <el-form-item label="Max Count" required>
            <el-input-number
              v-model="ruleForm.maxCount"
              class="form-control"
              :min="1"
              :precision="0"
              :controls="false"
              placeholder="Enter max count"
            />
          </el-form-item>
        </template>

        <el-form-item label="Severity" prop="severity">
          <el-select
            v-model="ruleForm.severity"
            class="form-control"
            placeholder="Select severity"
          >
            <el-option
              v-for="severity in severityOptions"
              :key="severity"
              :label="severity"
              :value="severity"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="Enabled">
          <el-switch v-model="ruleForm.enabled" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">
          Cancel
        </el-button>
        <el-button
          type="primary"
          :loading="submitting"
          @click="saveRule"
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
  color: #7c3aed;
  font-size: 23px;
  background: #f3e8ff;
}

.create-button {
  height: 40px;
  padding: 0 18px;
  border-radius: 10px;
  box-shadow: 0 7px 16px rgb(37 99 235 / 20%);
}

.threshold-field {
  width: 100%;
}

.field-hint {
  margin-top: 8px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.rules-error {
  margin-bottom: 20px;
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

.action-buttons {
  display: flex;
  align-items: center;
  gap: 6px;
}

.action-buttons :deep(.el-button + .el-button) {
  margin-left: 0;
}

.form-control {
  width: 100%;
}

:deep(.el-table__body tr:hover > td.el-table__cell) {
  background: #f8f5ff !important;
}
</style>
