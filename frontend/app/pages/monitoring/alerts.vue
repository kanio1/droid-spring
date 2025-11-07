<template>
  <div class="alerts-page">
    <div class="page-header">
      <h1>Alert Management</h1>
      <div class="header-actions">
        <Button
          label="Refresh"
          icon="pi pi-refresh"
          :loading="loading"
          @click="refreshData"
        />
      </div>
    </div>

    <!-- Filters -->
    <div class="filters">
      <Dropdown
        v-model="selectedSeverity"
        :options="severityOptions"
        placeholder="Filter by Severity"
        option-label="label"
        option-value="value"
        show-clear
      />
      <Dropdown
        v-model="selectedStatus"
        :options="statusOptions"
        placeholder="Filter by Status"
        option-label="label"
        option-value="value"
        show-clear
      />
      <Calendar
        v-model="dateRange"
        selection-mode="range"
        placeholder="Date Range"
        :show-icon="true"
      />
    </div>

    <!-- Alerts Table -->
    <DataTable
      :value="filteredAlerts"
      :loading="loading"
      :paginator="true"
      :rows="10"
      :rows-per-page-options="[10, 25, 50]"
      responsive-layout="scroll"
      striped-rows
    >
      <Column field="id" header="ID" sortable style="width: 100px" />
      <Column field="severity" header="Severity" sortable>
        <template #body="{ data }">
          <Badge
            :value="data.severity"
            :severity="getSeveritySeverity(data.severity)"
          />
        </template>
      </Column>
      <Column field="status" header="Status" sortable>
        <template #body="{ data }">
          <Badge
            :value="data.status"
            :severity="getStatusSeverity(data.status)"
          />
        </template>
      </Column>
      <Column field="metricType" header="Metric Type" sortable />
      <Column field="message" header="Message" />
      <Column field="triggeredAt" header="Triggered At" sortable>
        <template #body="{ data }">
          {{ formatDateTime(data.triggeredAt) }}
        </template>
      </Column>
      <Column header="Actions" style="width: 200px">
        <template #body="{ data }">
          <div class="action-buttons">
            <Button
              v-if="data.status === 'OPEN'"
              icon="pi pi-check"
              size="small"
              outlined
              @click="handleAcknowledge(data.id)"
              v-tooltip.top="'Acknowledge'"
            />
            <Button
              v-if="data.status !== 'RESOLVED'"
              icon="pi pi-times"
              size="small"
              severity="success"
              @click="handleResolve(data.id)"
              v-tooltip.top="'Resolve'"
            />
            <Button
              icon="pi pi-trash"
              size="small"
              severity="danger"
              outlined
              @click="handleDelete(data.id)"
              v-tooltip.top="'Delete'"
            />
          </div>
        </template>
      </Column>
    </DataTable>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useAlertsStore } from '~/stores/monitoring/alertsStore'
import type { Alert } from '~/types/monitoring/types'

definePageMeta({
  layout: 'default'
})

// Store
const alertsStore = useAlertsStore()

// State
const loading = ref(false)
const selectedSeverity = ref<string>()
const selectedStatus = ref<string>()
const dateRange = ref<Date[] | null>(null)
const customerId = ref(1)

// Options
const severityOptions = [
  { label: 'Critical', value: 'CRITICAL' },
  { label: 'Warning', value: 'WARNING' },
  { label: 'OK', value: 'OK' }
]

const statusOptions = [
  { label: 'Open', value: 'OPEN' },
  { label: 'Acknowledged', value: 'ACKNOWLEDGED' },
  { label: 'Resolved', value: 'RESOLVED' }
]

// Computed
const alerts = computed(() => alertsStore.alerts)

const filteredAlerts = computed(() => {
  let filtered = alerts.value

  if (selectedSeverity.value) {
    filtered = filtered.filter(a => a.severity === selectedSeverity.value)
  }

  if (selectedStatus.value) {
    filtered = filtered.filter(a => a.status === selectedStatus.value)
  }

  if (dateRange.value && dateRange.value.length === 2) {
    const [start, end] = dateRange.value
    if (start && end) {
      filtered = filtered.filter(a => {
        const triggeredAt = new Date(a.triggeredAt)
        return triggeredAt >= start && triggeredAt <= end
      })
    }
  }

  return filtered
})

// Methods
const refreshData = async () => {
  loading.value = true

  try {
    await alertsStore.fetchAlerts(customerId.value)
  } catch (error) {
    console.error('Error fetching alerts:', error)
  } finally {
    loading.value = false
  }
}

const handleAcknowledge = async (alertId: number) => {
  try {
    await alertsStore.acknowledgeAlert(alertId, 'current-user')
  } catch (error) {
    console.error('Error acknowledging alert:', error)
  }
}

const handleResolve = async (alertId: number) => {
  try {
    await alertsStore.resolveAlert(alertId, 'current-user')
  } catch (error) {
    console.error('Error resolving alert:', error)
  }
}

const handleDelete = async (alertId: number) => {
  if (confirm('Are you sure you want to delete this alert?')) {
    try {
      await alertsStore.deleteAlert(alertId)
    } catch (error) {
      console.error('Error deleting alert:', error)
    }
  }
}

const getSeveritySeverity = (severity: string) => {
  switch (severity) {
    case 'CRITICAL':
      return 'danger'
    case 'WARNING':
      return 'warning'
    default:
      return 'success'
  }
}

const getStatusSeverity = (status: string) => {
  switch (status) {
    case 'OPEN':
      return 'danger'
    case 'ACKNOWLEDGED':
      return 'warning'
    case 'RESOLVED':
      return 'success'
    default:
      return 'info'
  }
}

const formatDateTime = (timestamp: string) => {
  return new Date(timestamp).toLocaleString()
}

// Lifecycle
onMounted(() => {
  refreshData()
})
</script>

<style scoped>
.alerts-page {
  padding: 2rem;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}

.page-header h1 {
  margin: 0;
  font-size: 2rem;
  font-weight: 700;
  color: #1f2937;
}

.header-actions {
  display: flex;
  gap: 1rem;
}

.filters {
  display: flex;
  gap: 1rem;
  margin-bottom: 2rem;
  background: white;
  padding: 1rem;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.action-buttons {
  display: flex;
  gap: 0.5rem;
}
</style>
