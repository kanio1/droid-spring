<template>
  <div class="jobs-page page-container">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <h1 class="page-title">
          <i class="pi pi-cog"></i>
          Background Jobs
        </h1>
        <p class="page-subtitle">
          Monitor and manage background job scheduling with pg_cron
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
          label="New Job"
          icon="pi pi-plus"
          severity="primary"
          @click="showNewJobDialog = true"
        />
      </div>
    </div>

    <!-- Job Statistics Cards -->
    <div class="stats-section" v-if="statistics">
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-card__icon bg-blue-100 text-blue-600">
            <i class="pi pi-list"></i>
          </div>
          <div class="stat-card__content">
            <div class="stat-card__label">Total Jobs</div>
            <div class="stat-card__value">{{ statistics.totalJobs }}</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-card__icon bg-green-100 text-green-600">
            <i class="pi pi-check-circle"></i>
          </div>
          <div class="stat-card__content">
            <div class="stat-card__label">Active Jobs</div>
            <div class="stat-card__value">{{ statistics.activeJobs }}</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-card__icon bg-purple-100 text-purple-600">
            <i class="pi pi-chart-line"></i>
          </div>
          <div class="stat-card__content">
            <div class="stat-card__label">Total Runs</div>
            <div class="stat-card__value">{{ formatNumber(statistics.totalRuns) }}</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-card__icon bg-orange-100 text-orange-600">
            <i class="pi pi-percentage"></i>
          </div>
          <div class="stat-card__content">
            <div class="stat-card__label">Success Rate</div>
            <div class="stat-card__value">{{ statistics.getSuccessRateFormatted() }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Tabs -->
    <div class="tabs-container">
      <TabView>
        <TabPanel header="Active Jobs" leftIcon="pi pi-list">
          <div class="tab-content">
            <div class="section">
              <h3>
                <i class="pi pi-cog"></i>
                Scheduled Jobs
              </h3>
              <DataTable
                :value="jobs"
                :paginator="true"
                :rows="10"
                :loading="loading"
                responsiveLayout="scroll"
                class="p-datatable-sm"
              >
                <Column field="name" header="Name" style="width: 200px" />
                <Column field="description" header="Description" style="width: 300px">
                  <template #body="{ data }">
                    <span :title="data.description">{{ truncateText(data.description) }}</span>
                  </template>
                </Column>
                <Column field="cronExpression" header="Cron Expression" style="width: 180px">
                  <template #body="{ data }">
                    <code class="cron-expression">{{ data.cronExpression }}</code>
                  </template>
                </Column>
                <Column field="priority" header="Priority" style="width: 120px">
                  <template #body="{ data }">
                    <Tag :value="data.priority" :severity="getPrioritySeverity(data.priority)" />
                  </template>
                </Column>
                <Column field="status" header="Status" style="width: 120px">
                  <template #body="{ data }">
                    <Tag :value="data.status" :severity="getStatusSeverity(data.status)" />
                  </template>
                </Column>
                <Column header="Actions" style="width: 150px">
                  <template #body="{ data }">
                    <div class="action-buttons">
                      <Button
                        icon="pi pi-eye"
                        severity="info"
                        text
                        rounded
                        v-tooltip.top="'View Runs'"
                        @click="viewJobRuns(data)"
                      />
                      <Button
                        v-if="data.status === 'ACTIVE'"
                        icon="pi pi-times"
                        severity="danger"
                        text
                        rounded
                        v-tooltip.top="'Cancel Job'"
                        @click="confirmCancelJob(data)"
                      />
                    </div>
                  </template>
                </Column>
              </DataTable>
            </div>
          </div>
        </TabPanel>

        <TabPanel header="Job History" leftIcon="pi pi-history">
          <div class="tab-content">
            <div class="section">
              <h3>
                <i class="pi pi-clock"></i>
                Recent Job Executions
              </h3>
              <DataTable
                :value="jobRuns"
                :paginator="true"
                :rows="15"
                :loading="loading"
                responsiveLayout="scroll"
                class="p-datatable-sm"
              >
                <Column field="jobId" header="Job ID" style="width: 120px">
                  <template #body="{ data }">
                    <code class="job-id">{{ formatShortId(data.jobId) }}</code>
                  </template>
                </Column>
                <Column field="status" header="Status" style="width: 100px">
                  <template #body="{ data }">
                    <Tag :value="data.status" :severity="getStatusSeverity(data.status)" />
                  </template>
                </Column>
                <Column header="Duration" style="width: 120px">
                  <template #body="{ data }">
                    {{ data.getDurationFormatted() }}
                  </template>
                </Column>
                <Column field="retryCount" header="Retry #" style="width: 100px">
                  <template #body="{ data }">
                    <span v-if="data.retryCount > 0" class="retry-count">
                      {{ data.retryCount }}
                    </span>
                    <span v-else>-</span>
                  </template>
                </Column>
                <Column field="startedAt" header="Started At" style="width: 180px">
                  <template #body="{ data }">
                    {{ formatDateTime(data.startedAt) }}
                  </template>
                </Column>
                <Column field="errorMessage" header="Error" style="width: 300px">
                  <template #body="{ data }">
                    <span v-if="data.errorMessage" class="error-message" :title="data.errorMessage">
                      {{ truncateText(data.errorMessage, 50) }}
                    </span>
                    <span v-else>-</span>
                  </template>
                </Column>
              </DataTable>
            </div>
          </div>
        </TabPanel>

        <TabPanel header="Statistics" leftIcon="pi pi-chart-bar">
          <div class="tab-content">
            <div class="section" v-if="statistics">
              <h3>
                <i class="pi pi-info-circle"></i>
                Job Processing Statistics
              </h3>
              <div class="stats-detail-grid">
                <div class="detail-card">
                  <h4>Job Overview</h4>
                  <div class="detail-item">
                    <span class="detail-label">Total Jobs:</span>
                    <span class="detail-value">{{ statistics.totalJobs }}</span>
                  </div>
                  <div class="detail-item">
                    <span class="detail-label">Active:</span>
                    <span class="detail-value text-green-600">{{ statistics.activeJobs }}</span>
                  </div>
                  <div class="detail-item">
                    <span class="detail-label">Cancelled:</span>
                    <span class="detail-value text-gray-600">{{ statistics.cancelledJobs }}</span>
                  </div>
                </div>

                <div class="detail-card">
                  <h4>Execution Summary</h4>
                  <div class="detail-item">
                    <span class="detail-label">Total Runs:</span>
                    <span class="detail-value">{{ formatNumber(statistics.totalRuns) }}</span>
                  </div>
                  <div class="detail-item">
                    <span class="detail-label">Successful:</span>
                    <span class="detail-value text-green-600">{{ formatNumber(statistics.successfulRuns) }}</span>
                  </div>
                  <div class="detail-item">
                    <span class="detail-label">Failed:</span>
                    <span class="detail-value text-red-600">{{ formatNumber(statistics.failedRuns) }}</span>
                  </div>
                  <div class="detail-item">
                    <span class="detail-label">Success Rate:</span>
                    <span class="detail-value font-bold">{{ statistics.getSuccessRateFormatted() }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </TabPanel>
      </TabView>
    </div>

    <!-- New Job Dialog -->
    <Dialog
      v-model:visible="showNewJobDialog"
      modal
      header="Schedule New Job"
      :style="{ width: '600px' }"
    >
      <div class="new-job-form">
        <div class="form-field">
          <label>Job Name *</label>
          <InputText v-model="newJob.name" placeholder="e.g., Daily Backup" />
        </div>

        <div class="form-field">
          <label>Description</label>
          <Textarea v-model="newJob.description" rows="3" placeholder="Description of the job" />
        </div>

        <div class="form-field">
          <label>Cron Expression *</label>
          <InputText v-model="newJob.cronExpression" placeholder="e.g., 0 0 * * * (daily at midnight)" />
          <small>Use standard cron format: min hour day month day-of-week</small>
        </div>

        <div class="form-field">
          <label>SQL Command *</label>
          <Textarea v-model="newJob.sqlCommand" rows="5" placeholder="SQL to execute" />
        </div>

        <div class="form-field">
          <label>Priority</label>
          <Dropdown v-model="newJob.priority" :options="priorityOptions" optionLabel="label" optionValue="value" />
        </div>
      </div>

      <template #footer>
        <Button label="Cancel" severity="secondary" @click="showNewJobDialog = false" />
        <Button label="Schedule Job" @click="scheduleNewJob" :loading="creating" />
      </template>
    </Dialog>

    <!-- Job Runs Dialog -->
    <Dialog
      v-model:visible="showJobRunsDialog"
      modal
      :header="`Job Runs: ${selectedJob?.name || ''}`"
      :style="{ width: '900px' }"
    >
      <DataTable
        :value="selectedJobRuns"
        :paginator="true"
        :rows="10"
        :loading="loadingRuns"
        responsiveLayout="scroll"
        class="p-datatable-sm"
      >
        <Column field="status" header="Status" style="width: 100px">
          <template #body="{ data }">
            <Tag :value="data.status" :severity="getStatusSeverity(data.status)" />
          </template>
        </Column>
        <Column header="Duration" style="width: 120px">
          <template #body="{ data }">
            {{ data.getDurationFormatted() }}
          </template>
        </Column>
        <Column field="startedAt" header="Started At" style="width: 180px">
          <template #body="{ data }">
            {{ formatDateTime(data.startedAt) }}
          </template>
        </Column>
        <Column field="errorMessage" header="Error" style="width: 300px">
          <template #body="{ data }">
            <span v-if="data.errorMessage" class="error-message" :title="data.errorMessage">
              {{ truncateText(data.errorMessage, 50) }}
            </span>
            <span v-else>-</span>
          </template>
        </Column>
      </DataTable>
    </Dialog>

    <!-- Toast Messages -->
    <Toast />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useApi } from '~/composables/useApi'
import { useToast } from 'primevue/usetoast'

// Meta
definePageMeta({
  title: 'Background Jobs',
  description: 'Monitor and manage background job scheduling with pg_cron',
  layout: 'default'
})

// Composables
const { get, post } = useApi()
const toast = useToast()

// State
const loading = ref(false)
const jobs = ref<any[]>([])
const jobRuns = ref<any[]>([])
const statistics = ref<any>(null)
const showNewJobDialog = ref(false)
const showJobRunsDialog = ref(false)
const selectedJob = ref<any>(null)
const selectedJobRuns = ref<any[]>([])
const loadingRuns = ref(false)
const creating = ref(false)

// New job form
const newJob = ref({
  name: '',
  description: '',
  cronExpression: '',
  sqlCommand: '',
  priority: 'MEDIUM'
})

const priorityOptions = [
  { label: 'High', value: 'HIGH' },
  { label: 'Medium', value: 'MEDIUM' },
  { label: 'Low', value: 'LOW' }
]

// Fetch all data
const fetchAllData = async () => {
  loading.value = true

  try {
    const [jobsRes, runsRes, statsRes] = await Promise.all([
      get('/jobs'),
      get('/jobs/statistics'), // We'll fetch runs separately
      get('/jobs/statistics')
    ])

    jobs.value = jobsRes.data || []
    statistics.value = statsRes.data

  } catch (error: any) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'Failed to fetch jobs data',
      life: 5000
    })
  } finally {
    loading.value = false
  }
}

// Refresh data
const refreshData = () => {
  fetchAllData()
  toast.add({
    severity: 'success',
    summary: 'Success',
    detail: 'Jobs data refreshed',
    life: 3000
  })
}

// Schedule new job
const scheduleNewJob = async () => {
  if (!newJob.value.name || !newJob.value.cronExpression || !newJob.value.sqlCommand) {
    toast.add({
      severity: 'error',
      summary: 'Validation Error',
      detail: 'Please fill in all required fields',
      life: 5000
    })
    return
  }

  creating.value = true

  try {
    await post('/jobs', newJob.value)

    toast.add({
      severity: 'success',
      summary: 'Success',
      detail: 'Job scheduled successfully',
      life: 3000
    })

    // Reset form and close dialog
    newJob.value = {
      name: '',
      description: '',
      cronExpression: '',
      sqlCommand: '',
      priority: 'MEDIUM'
    }
    showNewJobDialog.value = false

    // Refresh data
    fetchAllData()

  } catch (error: any) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'Failed to schedule job',
      life: 5000
    })
  } finally {
    creating.value = false
  }
}

// View job runs
const viewJobRuns = async (job: any) => {
  selectedJob.value = job
  showJobRunsDialog.value = true
  loadingRuns.value = true

  try {
    const response = await get(`/jobs/${job.id}/runs?limit=50`)
    selectedJobRuns.value = response.data || []
  } catch (error: any) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'Failed to fetch job runs',
      life: 5000
    })
  } finally {
    loadingRuns.value = false
  }
}

// Confirm cancel job
const confirmCancelJob = async (job: any) => {
  if (!confirm(`Are you sure you want to cancel job "${job.name}"?`)) {
    return
  }

  try {
    await post(`/jobs/${job.id}/cancel`, {})

    toast.add({
      severity: 'success',
      summary: 'Success',
      detail: `Job "${job.name}" cancelled successfully`,
      life: 3000
    })

    fetchAllData()

  } catch (error: any) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'Failed to cancel job',
      life: 5000
    })
  }
}

// Helper methods
const formatNumber = (num: number) => {
  return new Intl.NumberFormat('en-US').format(num || 0)
}

const formatDateTime = (date: string) => {
  if (!date) return '-'
  return new Date(date).toLocaleString()
}

const formatShortId = (id: string) => {
  if (!id) return '-'
  return id.substring(0, 8)
}

const truncateText = (text: string, maxLength: number = 100) => {
  if (!text) return ''
  return text.length > maxLength ? text.substring(0, maxLength) + '...' : text
}

const getStatusSeverity = (status: string) => {
  switch (status) {
    case 'SUCCESS': return 'success'
    case 'FAILED': return 'danger'
    case 'RUNNING': return 'info'
    case 'ACTIVE': return 'success'
    case 'CANCELLED': return 'warning'
    default: return 'info'
  }
}

const getPrioritySeverity = (priority: string) => {
  switch (priority) {
    case 'HIGH': return 'danger'
    case 'MEDIUM': return 'warning'
    case 'LOW': return 'info'
    default: return 'info'
  }
}

// Lifecycle
onMounted(() => {
  fetchAllData()
})
</script>

<style scoped>
.jobs-page {
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

.stats-section {
  margin-bottom: 2rem;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1.5rem;
}

.stat-card {
  background: white;
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  gap: 1rem;
}

.stat-card__icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
}

.stat-card__content {
  flex: 1;
}

.stat-card__label {
  font-size: 0.875rem;
  color: #6b7280;
  margin-bottom: 0.25rem;
}

.stat-card__value {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1f2937;
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

.cron-expression {
  font-family: 'Courier New', monospace;
  font-size: 0.875rem;
  background: #f3f4f6;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
}

.job-id {
  font-family: 'Courier New', monospace;
  font-size: 0.875rem;
  color: #6b7280;
}

.retry-count {
  background: #fef3c7;
  color: #f59e0b;
  padding: 0.125rem 0.5rem;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
}

.error-message {
  color: #ef4444;
  font-size: 0.875rem;
}

.action-buttons {
  display: flex;
  gap: 0.5rem;
}

.stats-detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1.5rem;
}

.detail-card {
  background: #f9fafb;
  border-radius: 8px;
  padding: 1.5rem;
}

.detail-card h4 {
  margin: 0 0 1rem 0;
  font-size: 1rem;
  font-weight: 600;
  color: #1f2937;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  padding: 0.5rem 0;
  border-bottom: 1px solid #e5e7eb;
}

.detail-item:last-child {
  border-bottom: none;
}

.detail-label {
  color: #6b7280;
  font-size: 0.875rem;
}

.detail-value {
  font-weight: 600;
  color: #1f2937;
}

.new-job-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-field label {
  font-weight: 600;
  color: #374151;
  font-size: 0.875rem;
}

.form-field small {
  color: #6b7280;
  font-size: 0.75rem;
}
</style>
