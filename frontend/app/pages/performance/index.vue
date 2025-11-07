<template>
  <div class="performance-dashboard">
    <div class="dashboard-header">
      <h1>Performance Monitoring</h1>
      <div class="header-actions">
        <button @click="refreshAll" :disabled="loading" class="btn-refresh">
          <i class="pi pi-refresh" :class="{ 'pi-spin': loading }"></i>
          Refresh
        </button>
      </div>
    </div>

    <!-- Alerts Section -->
    <div v-if="alerts.length > 0" class="alerts-section">
      <div v-for="alert in alerts" :key="alert.type" :class="['alert', alert.severity.toLowerCase()]">
        <i class="pi pi-exclamation-triangle"></i>
        <span>{{ alert.message }}</span>
      </div>
    </div>

    <!-- System Metrics Cards -->
    <div class="metrics-grid">
      <div class="metric-card">
        <div class="metric-icon connections">
          <i class="pi pi-users"></i>
        </div>
        <div class="metric-content">
          <div class="metric-label">Active Connections</div>
          <div class="metric-value">{{ systemMetrics?.connections || 0 }}</div>
          <div class="metric-subtitle">Current connections</div>
        </div>
      </div>

      <div class="metric-card">
        <div class="metric-icon locks">
          <i class="pi pi-lock"></i>
        </div>
        <div class="metric-content">
          <div class="metric-label">Waiting Locks</div>
          <div class="metric-value">{{ systemMetrics?.waitingLocks || 0 }}</div>
          <div class="metric-subtitle">Blocked queries</div>
        </div>
      </div>

      <div class="metric-card">
        <div class="metric-icon idle">
          <i class="pi pi-pause"></i>
        </div>
        <div class="metric-content">
          <div class="metric-label">Idle in Transaction</div>
          <div class="metric-value">{{ systemMetrics?.idleInTransaction || 0 }}</div>
          <div class="metric-subtitle">Long transactions</div>
        </div>
      </div>

      <div class="metric-card">
        <div class="metric-icon cache">
          <i class="pi pi-database"></i>
        </div>
        <div class="metric-content">
          <div class="metric-label">Cache Hit Ratio</div>
          <div class="metric-value">{{ dbStats?.cacheHitRatio?.toFixed(2) || 0 }}%</div>
          <div class="metric-subtitle">Buffer cache efficiency</div>
        </div>
      </div>
    </div>

    <!-- Tabs Navigation -->
    <div class="tabs-navigation">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        @click="activeTab = tab.id"
        :class="['tab-btn', { active: activeTab === tab.id }]"
      >
        <i :class="tab.icon"></i>
        {{ tab.label }}
      </button>
    </div>

    <!-- Tab Content -->
    <div class="tab-content">
      <!-- Active Queries Tab -->
      <div v-if="activeTab === 'active-queries'" class="tab-panel">
        <div class="panel-header">
          <h2>Active Queries</h2>
          <div class="panel-actions">
            <button @click="fetchActiveQueries" class="btn-small">Refresh</button>
          </div>
        </div>
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>PID</th>
                <th>User</th>
                <th>Application</th>
                <th>Client</th>
                <th>State</th>
                <th>Duration</th>
                <th>Query</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="query in activeQueries" :key="query.pid">
                <td>{{ query.pid }}</td>
                <td>{{ query.username }}</td>
                <td>{{ query.applicationName }}</td>
                <td>{{ query.clientAddress }}</td>
                <td><span class="badge">{{ query.state }}</span></td>
                <td>{{ formatDuration(query.queryStart) }}</td>
                <td class="query-cell">{{ truncateQuery(query.query) }}</td>
                <td>
                  <button @click="killQuery(query.pid)" class="btn-danger btn-small">
                    Kill
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Long-Running Queries Tab -->
      <div v-if="activeTab === 'long-running'" class="tab-panel">
        <div class="panel-header">
          <h2>Long-Running Queries</h2>
          <div class="panel-actions">
            <input
              v-model.number="longRunningThreshold"
              type="number"
              min="1"
              class="input-small"
              placeholder="Threshold (seconds)"
            />
            <button @click="fetchLongRunningQueries" class="btn-small">Apply</button>
          </div>
        </div>
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>PID</th>
                <th>User</th>
                <th>Duration (s)</th>
                <th>Query</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="query in longRunningQueries" :key="query.pid" class="warning-row">
                <td>{{ query.pid }}</td>
                <td>{{ query.username }}</td>
                <td class="duration-critical">{{ query.durationSeconds }}s</td>
                <td class="query-cell">{{ truncateQuery(query.query) }}</td>
                <td>
                  <button @click="killQuery(query.pid)" class="btn-danger btn-small">
                    Kill
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Lock Analysis Tab -->
      <div v-if="activeTab === 'locks'" class="tab-panel">
        <div class="panel-header">
          <h2>Lock Information</h2>
          <div class="panel-actions">
            <button @click="fetchLockInformation" class="btn-small">Refresh</button>
          </div>
        </div>
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>Lock Type</th>
                <th>Mode</th>
                <th>Granted</th>
                <th>Blocked Query</th>
                <th>Blocking Query</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(lock, index) in lockInformation" :key="index" :class="{ 'warning-row': !lock.granted }">
                <td>{{ lock.lockType }}</td>
                <td>{{ lock.mode }}</td>
                <td>
                  <span :class="['badge', lock.granted ? 'badge-success' : 'badge-danger']">
                    {{ lock.granted ? 'Yes' : 'No' }}
                  </span>
                </td>
                <td class="query-cell">{{ truncateQuery(lock.blockedQuery) }}</td>
                <td class="query-cell">{{ truncateQuery(lock.blockingQuery) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Database Statistics Tab -->
      <div v-if="activeTab === 'database-stats'" class="tab-panel">
        <div class="panel-header">
          <h2>Database Statistics</h2>
          <button @click="fetchDatabaseStatistics" class="btn-small">Refresh</button>
        </div>

        <div class="stats-grid">
          <div class="stat-box">
            <div class="stat-label">Total Connections</div>
            <div class="stat-value">{{ dbStats?.totalConnections || 0 }}</div>
            <div class="stat-breakdown">
              <span>Active: {{ dbStats?.activeConnections || 0 }}</span>
              <span>Idle: {{ dbStats?.idleConnections || 0 }}</span>
            </div>
          </div>

          <div class="stat-box">
            <div class="stat-label">Transactions</div>
            <div class="stat-value">{{ dbStats?.totalCommits || 0 }}</div>
            <div class="stat-subtitle">{{ dbStats?.totalRollbacks || 0 }} rollbacks</div>
          </div>

          <div class="stat-box">
            <div class="stat-label">Blocks Read</div>
            <div class="stat-value">{{ dbStats?.blksRead || 0 }}</div>
            <div class="stat-subtitle">{{ dbStats?.blksHit || 0 }} hits</div>
          </div>

          <div class="stat-box">
            <div class="stat-label">Tuples</div>
            <div class="stat-value">{{ (dbStats?.tupReturned || 0).toLocaleString() }}</div>
            <div class="stat-subtitle">Returned: {{ (dbStats?.tupFetched || 0).toLocaleString() }} fetched</div>
          </div>
        </div>

        <div class="panel-subheader">
          <h3>Query Statistics (Top 20)</h3>
          <button @click="fetchQueryStatistics" class="btn-small">Refresh</button>
        </div>
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>Calls</th>
                <th>Total Time (ms)</th>
                <th>Mean Time (ms)</th>
                <th>Min Time (ms)</th>
                <th>Max Time (ms)</th>
                <th>Rows</th>
                <th>Hit %</th>
                <th>Query</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(stat, index) in queryStatistics.slice(0, 20)" :key="index">
                <td>{{ stat.calls?.toLocaleString() }}</td>
                <td>{{ stat.totalTime?.toFixed(2) }}</td>
                <td>{{ stat.meanTime?.toFixed(2) }}</td>
                <td>{{ stat.minTime?.toFixed(2) }}</td>
                <td>{{ stat.maxTime?.toFixed(2) }}</td>
                <td>{{ stat.rows?.toLocaleString() }}</td>
                <td>
                  <span :class="['badge', getHitPercentClass(stat.hitPercent)]">
                    {{ stat.hitPercent?.toFixed(1) }}%
                  </span>
                </td>
                <td class="query-cell">{{ truncateQuery(stat.query) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Table Statistics Tab -->
      <div v-if="activeTab === 'table-stats'" class="tab-panel">
        <div class="panel-header">
          <h2>Table Statistics</h2>
          <button @click="fetchTableStatistics" class="btn-small">Refresh</button>
        </div>
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>Schema</th>
                <th>Table</th>
                <th>Seq Scans</th>
                <th>Seq Rows</th>
                <th>Idx Scans</th>
                <th>Live Tuples</th>
                <th>Dead Tuples</th>
                <th>Last Vacuum</th>
                <th>Last Analyze</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(table, index) in tableStatistics" :key="index">
                <td>{{ table.schemaName }}</td>
                <td>{{ table.tableName }}</td>
                <td>{{ table.seqScan?.toLocaleString() }}</td>
                <td>{{ table.seqTupRead?.toLocaleString() }}</td>
                <td>{{ table.idxScan?.toLocaleString() }}</td>
                <td>{{ table.nLiveTup?.toLocaleString() }}</td>
                <td :class="{ 'warning-text': (table.nDeadTup || 0) > 1000 }">
                  {{ table.nDeadTup?.toLocaleString() }}
                </td>
                <td>{{ formatTimestamp(table.lastVacuum) }}</td>
                <td>{{ formatTimestamp(table.lastAnalyze) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Deadlocks Tab -->
      <div v-if="activeTab === 'deadlocks'" class="tab-panel">
        <div class="panel-header">
          <h2>Deadlock Events</h2>
          <button @click="fetchDeadlockInformation" class="btn-small">Refresh</button>
        </div>
        <div v-if="deadlockInformation.length === 0" class="empty-state">
          <i class="pi pi-check-circle"></i>
          <p>No deadlock events detected in the last 24 hours</p>
        </div>
        <div v-else class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>PID</th>
                <th>User</th>
                <th>Application</th>
                <th>Query Start</th>
                <th>Query</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(deadlock, index) in deadlockInformation" :key="index" class="critical-row">
                <td>{{ deadlock.pid }}</td>
                <td>{{ deadlock.username }}</td>
                <td>{{ deadlock.applicationName }}</td>
                <td>{{ formatTimestamp(deadlock.queryStart) }}</td>
                <td class="query-cell">{{ truncateQuery(deadlock.query) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useFetch } from '#app'

// Reactive data
const activeTab = ref('active-queries')
const loading = ref(false)
const longRunningThreshold = ref(30)

// Data collections
const alerts = ref<any[]>([])
const systemMetrics = ref<any>(null)
const dbStats = ref<any>(null)
const activeQueries = ref<any[]>([])
const longRunningQueries = ref<any[]>([])
const lockInformation = ref<any[]>([])
const deadlockInformation = ref<any[]>([])
const tableStatistics = ref<any[]>([])
const queryStatistics = ref<any[]>([])

// Tabs configuration
const tabs = [
  { id: 'active-queries', label: 'Active Queries', icon: 'pi pi-play' },
  { id: 'long-running', label: 'Long-Running', icon: 'pi pi-clock' },
  { id: 'locks', label: 'Locks', icon: 'pi pi-lock' },
  { id: 'database-stats', label: 'Database Stats', icon: 'pi pi-chart-bar' },
  { id: 'table-stats', label: 'Table Stats', icon: 'pi pi-table' },
  { id: 'deadlocks', label: 'Deadlocks', icon: 'pi pi-exclamation-triangle' }
]

// Auto-refresh timer
let refreshTimer: NodeJS.Timeout | null = null

// Methods
const fetchData = async (endpoint: string, dataRef: any) => {
  try {
    const { data, error } = await useFetch(endpoint)
    if (error.value) {
      console.error(`Error fetching ${endpoint}:`, error.value)
      return
    }
    dataRef.value = data.value
  } catch (err) {
    console.error(`Error in fetchData for ${endpoint}:`, err)
  }
}

const fetchAll = async () => {
  loading.value = true
  try {
    await Promise.all([
      fetchData('/api/performance/alerts', alerts),
      fetchData('/api/performance/metrics', systemMetrics),
      fetchData('/api/performance/statistics/database', dbStats),
      fetchActiveQueries(),
      fetchLongRunningQueries(),
      fetchLockInformation(),
      fetchDeadlockInformation(),
      fetchTableStatistics(),
      fetchQueryStatistics()
    ])
  } finally {
    loading.value = false
  }
}

const refreshAll = () => {
  fetchAll()
}

const fetchActiveQueries = () => {
  fetchData('/api/performance/queries/active', activeQueries)
}

const fetchLongRunningQueries = () => {
  const threshold = longRunningThreshold.value
  fetchData(`/api/performance/queries/long-running?thresholdSeconds=${threshold}`, longRunningQueries)
}

const fetchLockInformation = () => {
  fetchData('/api/performance/locks', lockInformation)
}

const fetchDeadlockInformation = () => {
  fetchData('/api/performance/deadlocks', deadlockInformation)
}

const fetchTableStatistics = () => {
  fetchData('/api/performance/statistics/tables', tableStatistics)
}

const fetchQueryStatistics = () => {
  fetchData('/api/performance/statistics/queries?limit=100', queryStatistics)
}

const fetchDatabaseStatistics = () => {
  fetchData('/api/performance/statistics/database', dbStats)
}

const killQuery = async (pid: number) => {
  if (!confirm(`Are you sure you want to kill query ${pid}?`)) {
    return
  }

  try {
    const { error } = await useFetch(`/api/performance/queries/${pid}/kill`, {
      method: 'POST'
    })

    if (error.value) {
      alert('Failed to kill query')
      console.error(error.value)
      return
    }

    alert(`Query ${pid} has been terminated`)
    fetchActiveQueries()
    fetchLongRunningQueries()
    fetchLockInformation()
  } catch (err) {
    alert('Error killing query')
    console.error(err)
  }
}

// Utility functions
const formatDuration = (startTime: string) => {
  if (!startTime) return 'N/A'
  const start = new Date(startTime).getTime()
  const now = Date.now()
  const diff = Math.floor((now - start) / 1000)
  const minutes = Math.floor(diff / 60)
  const seconds = diff % 60
  return `${minutes}m ${seconds}s`
}

const formatTimestamp = (timestamp: string | null) => {
  if (!timestamp) return 'Never'
  return new Date(timestamp).toLocaleString()
}

const truncateQuery = (query: string, maxLength: number = 100) => {
  if (!query) return 'N/A'
  return query.length > maxLength ? query.substring(0, maxLength) + '...' : query
}

const getHitPercentClass = (hitPercent: number) => {
  if (!hitPercent) return 'badge-warning'
  if (hitPercent > 95) return 'badge-success'
  if (hitPercent > 80) return 'badge-warning'
  return 'badge-danger'
}

// Lifecycle
onMounted(() => {
  fetchAll()
  // Auto-refresh every 30 seconds
  refreshTimer = setInterval(fetchAll, 30000)
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
})
</script>

<style scoped>
.performance-dashboard {
  padding: 20px;
  max-width: 1600px;
  margin: 0 auto;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.dashboard-header h1 {
  font-size: 28px;
  font-weight: 600;
  color: #1a1a1a;
}

.btn-refresh {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: background 0.2s;
}

.btn-refresh:hover:not(:disabled) {
  background: #2563eb;
}

.btn-refresh:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.alerts-section {
  margin-bottom: 20px;
}

.alert {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 6px;
  margin-bottom: 10px;
  font-weight: 500;
}

.alert.warning {
  background: #fef3c7;
  color: #92400e;
  border-left: 4px solid #f59e0b;
}

.alert.critical {
  background: #fee2e2;
  color: #991b1b;
  border-left: 4px solid #ef4444;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 16px;
  margin-bottom: 30px;
}

.metric-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.metric-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.metric-icon.connections {
  background: #dbeafe;
  color: #1e40af;
}

.metric-icon.locks {
  background: #fce7f3;
  color: #9f1239;
}

.metric-icon.idle {
  background: #fef3c7;
  color: #92400e;
}

.metric-icon.cache {
  background: #d1fae5;
  color: #065f46;
}

.metric-content {
  flex: 1;
}

.metric-label {
  font-size: 12px;
  color: #6b7280;
  text-transform: uppercase;
  font-weight: 600;
  margin-bottom: 4px;
}

.metric-value {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 2px;
}

.metric-subtitle {
  font-size: 12px;
  color: #9ca3af;
}

.tabs-navigation {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  border-bottom: 2px solid #e5e7eb;
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  color: #6b7280;
  transition: all 0.2s;
}

.tab-btn:hover {
  color: #3b82f6;
}

.tab-btn.active {
  color: #3b82f6;
  border-bottom-color: #3b82f6;
}

.tab-content {
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.tab-panel {
  padding: 24px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.panel-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
}

.panel-subheader {
  margin-top: 30px;
  margin-bottom: 15px;
}

.panel-subheader h3 {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
}

.panel-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.btn-small {
  padding: 6px 12px;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
}

.input-small {
  padding: 6px 10px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 13px;
  width: 150px;
}

.btn-danger {
  background: #ef4444;
}

.btn-danger:hover {
  background: #dc2626;
}

.table-container {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table thead {
  background: #f9fafb;
}

.data-table th {
  padding: 12px;
  text-align: left;
  font-weight: 600;
  font-size: 13px;
  color: #6b7280;
  border-bottom: 2px solid #e5e7eb;
}

.data-table td {
  padding: 12px;
  font-size: 13px;
  border-bottom: 1px solid #f3f4f6;
}

.data-table tbody tr:hover {
  background: #f9fafb;
}

.query-cell {
  max-width: 400px;
  word-wrap: break-word;
  font-family: monospace;
  font-size: 12px;
}

.badge {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
}

.badge-success {
  background: #d1fae5;
  color: #065f46;
}

.badge-danger {
  background: #fee2e2;
  color: #991b1b;
}

.badge-warning {
  background: #fef3c7;
  color: #92400e;
}

.warning-row {
  background: #fef3c7;
}

.critical-row {
  background: #fee2e2;
}

.warning-text {
  color: #d97706;
  font-weight: 600;
}

.duration-critical {
  color: #dc2626;
  font-weight: 700;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 30px;
}

.stat-box {
  padding: 20px;
  background: #f9fafb;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.stat-label {
  font-size: 12px;
  color: #6b7280;
  text-transform: uppercase;
  font-weight: 600;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.stat-subtitle {
  font-size: 12px;
  color: #9ca3af;
}

.stat-breakdown {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #6b7280;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #6b7280;
}

.empty-state i {
  font-size: 48px;
  color: #10b981;
  margin-bottom: 16px;
}

.empty-state p {
  font-size: 14px;
}
</style>
