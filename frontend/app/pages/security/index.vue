<template>
  <div class="security-page page-container">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <h1 class="page-title">
          <i class="pi pi-shield"></i>
          Security & Compliance
        </h1>
        <p class="page-subtitle">
          Advanced security monitoring with RLS, encryption, and audit logging
        </p>
      </div>
      <div class="page-header__actions">
        <Button
          label="Refresh"
          icon="pi pi-refresh"
          severity="secondary"
          outlined
          @click="refreshData"
          :loading="loading"
        />
        <Button
          label="Cleanup Data"
          icon="pi pi-trash"
          severity="warning"
          outlined
          @click="cleanupData"
        />
      </div>
    </div>

    <!-- Security Grade -->
    <div class="grade-card" v-if="metrics">
      <div class="grade-card__icon" :class="'grade-' + metrics.overallSecurityGrade">
        <i class="pi" :class="getGradeIcon(metrics.overallSecurityGrade)"></i>
      </div>
      <div class="grade-card__content">
        <div class="grade-title">Security Grade</div>
        <div class="grade-value">{{ metrics.overallSecurityGrade }}</div>
        <div class="grade-summary">{{ metrics.summary }}</div>
      </div>
    </div>

    <!-- Security Tabs -->
    <div class="tabs-container">
      <TabView>
        <TabPanel header="Security Metrics" leftIcon="pi pi-chart-line">
          <div class="tab-content">
            <div class="section" v-if="metrics">
              <h3>
                <i class="pi pi-info-circle"></i>
                Security Overview
              </h3>
              <div class="metrics-grid">
                <div class="metric-card">
                  <div class="metric-card__label">Encryption Coverage</div>
                  <div class="metric-card__value" :class="metrics.isEncryptionCompliant() ? 'text-green-600' : 'text-red-600'">
                    {{ metrics.encryptionCoveragePercent?.toFixed(1) }}%
                  </div>
                  <div class="metric-card__detail">
                    {{ metrics.encryptedColumns }}/{{ metrics.totalClassifiedColumns }} columns
                  </div>
                  <ProgressBar :value="metrics.encryptionCoveragePercent" :showValue="false" style="height: 8px" />
                </div>

                <div class="metric-card">
                  <div class="metric-card__label">RLS Coverage</div>
                  <div class="metric-card__value" :class="metrics.isRLSCompliant() ? 'text-green-600' : 'text-red-600'">
                    {{ metrics.rlsCoveragePercent?.toFixed(1) }}%
                  </div>
                  <div class="metric-card__detail">
                    {{ metrics.rlsEnabledTables }}/{{ metrics.totalTables }} tables
                  </div>
                  <ProgressBar :value="metrics.rlsCoveragePercent" :showValue="false" style="height: 8px" />
                </div>

                <div class="metric-card">
                  <div class="metric-card__label">Events (24h)</div>
                  <div class="metric-card__value">{{ formatNumber(metrics.eventsLast24h) }}</div>
                  <div class="metric-card__detail">
                    Failed: {{ formatNumber(metrics.failedEvents24h) }}
                  </div>
                </div>

                <div class="metric-card">
                  <div class="metric-card__label">Events (7d)</div>
                  <div class="metric-card__value">{{ formatNumber(metrics.eventsLast7d) }}</div>
                  <div class="metric-card__detail">Total audit volume</div>
                </div>
              </div>
            </div>

            <div class="section">
              <h3>
                <i class="pi pi-check-circle"></i>
                Compliance Status
              </h3>
              <DataTable
                :value="compliance"
                :paginator="false"
                :loading="loading"
                responsiveLayout="scroll"
                class="p-datatable-sm"
              >
                <Column field="checkType" header="Check Type" style="width: 200px">
                  <template #body="{ data }">
                    <strong>{{ formatCheckType(data.checkType) }}</strong>
                  </template>
                </Column>
                <Column field="passed" header="Passed" style="width: 120px">
                  <template #body="{ data }">
                    <Tag :value="data.passed" severity="success" />
                  </template>
                </Column>
                <Column field="failed" header="Failed" style="width: 120px">
                  <template #body="{ data }">
                    <Tag v-if="data.failed > 0" :value="data.failed" severity="danger" />
                    <span v-else>-</span>
                  </template>
                </Column>
                <Column field="compliancePercent" header="Compliance %" style="width: 150px">
                  <template #body="{ data }">
                    <span :class="data.isCompliant() ? 'text-green-600' : 'text-red-600'">
                      {{ data.compliancePercent?.toFixed(1) }}%
                    </span>
                  </template>
                </Column>
                <Column field="complianceStatus" header="Status" style="width: 150px">
                  <template #body="{ data }">
                    <Tag :value="data.complianceStatus" :severity="getComplianceSeverity(data.complianceStatus)" />
                  </template>
                </Column>
              </DataTable>
            </div>
          </div>
        </TabPanel>

        <TabPanel header="RLS Policies" leftIcon="pi pi-lock">
          <div class="tab-content">
            <div class="section">
              <h3>
                <i class="pi pi-list"></i>
                Row Level Security Status
              </h3>
              <DataTable
                :value="rlsPolicies"
                :paginator="true"
                :rows="10"
                :loading="loading"
                responsiveLayout="scroll"
                class="p-datatable-sm"
              >
                <Column field="tableName" header="Table" style="width: 200px" />
                <Column field="policyCount" header="Policies" style="width: 120px" />
                <Column field="isEnabled" header="RLS Enabled" style="width: 150px">
                  <template #body="{ data }">
                    <Tag :value="data.isEnabled ? 'Enabled' : 'Disabled'"
                         :severity="data.isEnabled ? 'success' : 'danger'" />
                  </template>
                </Column>
                <Column header="Actions" style="width: 200px">
                  <template #body="{ data }">
                    <Button
                      v-if="!data.isEnabled"
                      label="Enable"
                      icon="pi pi-lock-open"
                      severity="success"
                      size="small"
                      @click="toggleRLS(data.tableName, true)"
                    />
                    <Button
                      v-else
                      label="Disable"
                      icon="pi pi-lock"
                      severity="danger"
                      size="small"
                      outlined
                      @click="toggleRLS(data.tableName, false)"
                    />
                  </template>
                </Column>
              </DataTable>
            </div>
          </div>
        </TabPanel>

        <TabPanel header="Data Classification" leftIcon="pi pi-database">
          <div class="tab-content">
            <div class="section">
              <h3>
                <i class="pi pi-tags"></i>
                PII Data Classification
              </h3>
              <DataTable
                :value="dataClassification"
                :paginator="true"
                :rows="15"
                :loading="loading"
                responsiveLayout="scroll"
                class="p-datatable-sm"
              >
                <Column field="tableName" header="Table" style="width: 150px" />
                <Column field="columnName" header="Column" style="width: 150px" />
                <Column field="classificationLevel" header="Level" style="width: 150px">
                  <template #body="{ data }">
                    <Tag :value="data.classificationLevel" :severity="getClassificationSeverity(data.classificationLevel)" />
                  </template>
                </Column>
                <Column field="piiType" header="PII Type" style="width: 150px">
                  <template #body="{ data }">
                    <span v-if="data.piiType">{{ data.piiType }}</span>
                    <span v-else>-</span>
                  </template>
                </Column>
                <Column field="isEncrypted" header="Encrypted" style="width: 120px">
                  <template #body="{ data }">
                    <i :class="data.isEncrypted ? 'pi pi-check-circle text-green-600' : 'pi pi-times-circle text-red-600'"></i>
                  </template>
                </Column>
                <Column field="isMasked" header="Masked" style="width: 120px">
                  <template #body="{ data }">
                    <i :class="data.isMasked ? 'pi pi-check-circle text-green-600' : 'pi pi-times-circle text-red-600'"></i>
                  </template>
                </Column>
                <Column field="retentionPeriod" header="Retention" style="width: 150px">
                  <template #body="{ data }">
                    <span v-if="data.retentionPeriod">{{ data.retentionPeriod }}</span>
                    <span v-else>-</span>
                  </template>
                </Column>
              </DataTable>
            </div>
          </div>
        </TabPanel>

        <TabPanel header="Audit Logs" leftIcon="pi pi-history">
          <div class="tab-content">
            <div class="section">
              <h3>
                <i class="pi pi-list"></i>
                Recent Audit Events
              </h3>
              <DataTable
                :value="auditLogs"
                :paginator="true"
                :rows="15"
                :loading="loading"
                responsiveLayout="scroll"
                class="p-datatable-sm"
              >
                <Column field="eventTime" header="Time" style="width: 180px">
                  <template #body="{ data }">
                    {{ formatDateTime(data.eventTime) }}
                  </template>
                </Column>
                <Column field="userName" header="User" style="width: 150px" />
                <Column field="eventType" header="Event" style="width: 120px">
                  <template #body="{ data }">
                    <Tag :value="data.eventType" :severity="getEventTypeSeverity(data.eventType)" />
                  </template>
                </Column>
                <Column field="tableName" header="Table" style="width: 150px" />
                <Column field="operation" header="Operation" style="width: 100px" />
                <Column field="success" header="Status" style="width: 100px">
                  <template #body="{ data }">
                    <Tag :value="data.success ? 'Success' : 'Failed'"
                         :severity="data.success ? 'success' : 'danger'" />
                  </template>
                </Column>
                <Column field="ipAddress" header="IP Address" style="width: 150px" />
              </DataTable>
            </div>
          </div>
        </TabPanel>

        <TabPanel header="Security Alerts" leftIcon="pi pi-exclamation-triangle">
          <div class="tab-content">
            <div class="section">
              <h3>
                <i class="pi pi-bell"></i>
                Active Security Alerts
              </h3>
              <div v-if="alerts.length === 0" class="no-alerts">
                <i class="pi pi-check-circle"></i>
                <p>No security alerts at this time</p>
              </div>
              <div v-else class="alerts-list">
                <div v-for="alert in alerts" :key="alert.alertId" class="alert-item" :class="'alert-' + getAlertSeverity(alert.severity)">
                  <div class="alert-header">
                    <i :class="getAlertIcon(alert.severity)"></i>
                    <div class="alert-title">
                      <strong>{{ alert.eventType }}</strong>
                      <span class="alert-time">{{ formatDateTime(alert.timestamp) }}</span>
                    </div>
                    <Tag :value="alert.severity" :severity="getAlertSeverity(alert.severity)" />
                  </div>
                  <div class="alert-details">
                    <div v-if="alert.tableName">Table: {{ alert.tableName }}</div>
                    <div v-if="alert.userName">User: {{ alert.userName }}</div>
                    <div v-if="alert.ipAddress">IP: {{ alert.ipAddress }}</div>
                    <div v-if="alert.message">{{ alert.message }}</div>
                  </div>
                </div>
              </div>
            </div>

            <div class="section">
              <h3>
                <i class="pi pi-users"></i>
                Failed Login Attempts (24h)
              </h3>
              <DataTable
                :value="failedLogins"
                :paginator="true"
                :rows="10"
                :loading="loading"
                responsiveLayout="scroll"
                class="p-datatable-sm"
              >
                <Column field="userName" header="Username" style="width: 200px" />
                <Column field="attemptCount" header="Attempts" style="width: 120px">
                  <template #body="{ data }">
                    <Tag :value="data.attemptCount" severity="danger" />
                  </template>
                </Column>
                <Column field="lastAttempt" header="Last Attempt" style="width: 180px">
                  <template #body="{ data }">
                    {{ formatDateTime(data.lastAttempt) }}
                  </template>
                </Column>
              </DataTable>
            </div>
          </div>
        </TabPanel>
      </TabView>
    </div>

    <!-- Toast Messages -->
    <Toast />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useApi } from '~/composables/useApi'
import { useToast } from 'primevue/usetoast'

// Meta
definePageMeta({
  title: 'Security & Compliance',
  description: 'Advanced security monitoring with RLS, encryption, and audit logging',
  layout: 'default'
})

// Composables
const { get, post } = useApi()
const toast = useToast()

// State
const loading = ref(false)
const metrics = ref<any>(null)
const compliance = ref<any[]>([])
const rlsPolicies = ref<any[]>([])
const dataClassification = ref<any[]>([])
const auditLogs = ref<any[]>([])
const alerts = ref<any[]>([])
const failedLogins = ref<any[]>([])

// Methods
const fetchAllData = async () => {
  loading.value = true

  try {
    const [metricsRes, complianceRes, rlsRes, classificationRes, logsRes, alertsRes, failedRes] = await Promise.all([
      get('/security/metrics'),
      get('/security/compliance'),
      get('/security/rls-policies'),
      get('/security/data-classification'),
      get('/security/audit-logs?limit=100'),
      get('/security/alerts'),
      get('/security/failed-logins?since=' + new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString())
    ])

    metrics.value = metricsRes.data
    compliance.value = complianceRes.data || []
    rlsPolicies.value = rlsRes.data || []
    dataClassification.value = classificationRes.data || []
    auditLogs.value = logsRes.data || []
    alerts.value = alertsRes.data || []
    failedLogins.value = failedRes.data || []

  } catch (error: any) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'Failed to fetch security data',
      life: 5000
    })
  } finally {
    loading.value = false
  }
}

const refreshData = () => {
  fetchAllData()
  toast.add({
    severity: 'success',
    summary: 'Success',
    detail: 'Security data refreshed',
    life: 3000
  })
}

const cleanupData = async () => {
  if (!confirm('Are you sure you want to run data cleanup? This will permanently delete expired data.')) {
    return
  }

  try {
    await post('/security/cleanup', {})
    toast.add({
      severity: 'success',
      summary: 'Success',
      detail: 'Data cleanup completed',
      life: 3000
    })
    fetchAllData()
  } catch (error: any) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'Failed to cleanup data',
      life: 5000
    })
  }
}

const toggleRLS = async (tableName: string, enable: boolean) => {
  const action = enable ? 'enable' : 'disable'
  const endpoint = `/security/rls/${action}/${tableName}`

  try {
    await post(endpoint, {})
    toast.add({
      severity: 'success',
      summary: 'Success',
      detail: `RLS ${enable ? 'enabled' : 'disabled'} on ${tableName}`,
      life: 3000
    })
    fetchAllData()
  } catch (error: any) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: error.message || `Failed to ${action} RLS`,
      life: 5000
    })
  }
}

const formatNumber = (num: number) => {
  return new Intl.NumberFormat('en-US').format(num || 0)
}

const formatDateTime = (date: string) => {
  if (!date) return '-'
  return new Date(date).toLocaleString()
}

const formatCheckType = (type: string) => {
  return type.replace(/_/g, ' ').toUpperCase()
}

const getGradeIcon = (grade: string) => {
  if (grade === 'A' || grade === 'B') return 'pi-check-circle'
  if (grade === 'C') return 'pi-info-circle'
  return 'pi-exclamation-triangle'
}

const getComplianceSeverity = (status: string) => {
  switch (status) {
    case 'COMPLIANT': return 'success'
    case 'PARTIAL': return 'warning'
    case 'NON_COMPLIANT': return 'danger'
    default: return 'info'
  }
}

const getClassificationSeverity = (level: string) => {
  switch (level) {
    case 'RESTRICTED': return 'danger'
    case 'CONFIDENTIAL': return 'warning'
    case 'INTERNAL': return 'info'
    case 'PUBLIC': return 'success'
    default: return 'info'
  }
}

const getEventTypeSeverity = (type: string) => {
  switch (type) {
    case 'INSERT': return 'success'
    case 'UPDATE': return 'info'
    case 'DELETE': return 'danger'
    case 'LOGIN': return 'info'
    case 'LOGOUT': return 'warning'
    default: return 'info'
  }
}

const getAlertSeverity = (severity: string) => {
  switch (severity) {
    case 'CRITICAL': return 'danger'
    case 'ERROR': return 'danger'
    case 'WARNING': return 'warning'
    default: return 'info'
  }
}

const getAlertIcon = (severity: string) => {
  switch (severity) {
    case 'CRITICAL': return 'pi-times-circle text-red-600'
    case 'ERROR': return 'pi-exclamation-circle text-red-600'
    case 'WARNING': return 'pi-exclamation-triangle text-yellow-600'
    default: return 'pi-info-circle text-blue-600'
  }
}

// Lifecycle
onMounted(() => {
  fetchAllData()
})
</script>

<style scoped>
.security-page {
  padding: 2rem;
  max-width: 1600px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}

.page-title {
  margin: 0;
  font-size: 2rem;
  font-weight: 700;
  color: #1f2937;
  display: flex;
  align-items: center;
  gap: 1rem;
}

.page-subtitle {
  margin: 0.5rem 0 0 0;
  color: #6b7280;
  font-size: 1rem;
}

.page-header__actions {
  display: flex;
  gap: 1rem;
}

.grade-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  padding: 2rem;
  margin-bottom: 2rem;
  display: flex;
  align-items: center;
  gap: 2rem;
  color: white;
}

.grade-card__icon {
  width: 100px;
  height: 100px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 3rem;
  background: rgba(255, 255, 255, 0.2);
}

.grade-card__content {
  flex: 1;
}

.grade-title {
  font-size: 1.25rem;
  opacity: 0.9;
  margin-bottom: 0.5rem;
}

.grade-value {
  font-size: 4rem;
  font-weight: 800;
  margin-bottom: 0.5rem;
}

.grade-summary {
  font-size: 1rem;
  opacity: 0.9;
}

.tabs-container {
  background: white;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.tab-content {
  padding: 2rem;
}

.section {
  margin-bottom: 3rem;
}

.section h3 {
  margin: 0 0 1.5rem 0;
  font-size: 1.25rem;
  font-weight: 600;
  color: #1f2937;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.metric-card {
  background: #f9fafb;
  border-radius: 8px;
  padding: 1.5rem;
}

.metric-card__label {
  font-size: 0.875rem;
  color: #6b7280;
  margin-bottom: 0.5rem;
}

.metric-card__value {
  font-size: 2rem;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 0.25rem;
}

.metric-card__detail {
  font-size: 0.875rem;
  color: #6b7280;
  margin-bottom: 1rem;
}

.text-green-600 {
  color: #10b981;
}

.text-red-600 {
  color: #ef4444;
}

.no-alerts {
  text-align: center;
  padding: 3rem;
  color: #10b981;
}

.no-alerts i {
  font-size: 4rem;
  margin-bottom: 1rem;
}

.alerts-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-bottom: 2rem;
}

.alert-item {
  background: #f9fafb;
  border-radius: 8px;
  padding: 1.5rem;
  border-left: 4px solid;
}

.alert-item.alert-CRITICAL {
  border-left-color: #ef4444;
}

.alert-item.alert-ERROR {
  border-left-color: #ef4444;
}

.alert-item.alert-WARNING {
  border-left-color: #f59e0b;
}

.alert-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 0.75rem;
}

.alert-header i {
  font-size: 1.5rem;
}

.alert-title {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.alert-title strong {
  color: #1f2937;
}

.alert-time {
  font-size: 0.75rem;
  color: #6b7280;
}

.alert-details {
  color: #374151;
  font-size: 0.875rem;
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
}

.alert-details div {
  background: white;
  padding: 0.25rem 0.75rem;
  border-radius: 4px;
}
</style>
