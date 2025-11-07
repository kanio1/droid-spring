<template>
  <div class="dashboard">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <h1 class="page-title">Dashboard</h1>
        <p class="page-subtitle">Welcome back! Here's an overview of your BSS system.</p>
      </div>
      <div class="page-header__actions">
        <NuxtLink to="/customers/create">
          <AppButton variant="primary" icon="➕">
            Add Customer
          </AppButton>
        </NuxtLink>
      </div>
    </div>

    <!-- Stats Cards -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-card__icon stat-card__icon--primary">
          👥
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ stats.customers.total }}</div>
          <div class="stat-card__label">Total Customers</div>
          <div class="stat-card__change" :class="getChangeClass(stats.customers.change)">
            {{ stats.customers.change > 0 ? '+' : '' }}{{ stats.customers.change }} today
          </div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-card__icon stat-card__icon--success">
          ✅
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ stats.activeCustomers }}</div>
          <div class="stat-card__label">Active Customers</div>
          <div class="stat-card__change stat-card__change--positive">
            85% of total
          </div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-card__icon stat-card__icon--warning">
          📍
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ stats.addresses.total }}</div>
          <div class="stat-card__label">Addresses</div>
          <div class="stat-card__change" :class="getChangeClass(stats.addresses.change)">
            {{ stats.addresses.change > 0 ? '+' : '' }}{{ stats.addresses.change }} today
          </div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-card__icon stat-card__icon--info">
          🗺️
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ stats.coverageNodes }}</div>
          <div class="stat-card__label">Coverage Nodes</div>
          <div class="stat-card__change stat-card__change--neutral">
            Network infrastructure
          </div>
        </div>
      </div>
    </div>

    <!-- Content Grid -->
    <div class="content-grid">
      <!-- Recent Activity -->
      <div class="content-card">
        <div class="content-card__header">
          <h2>Recent Activity</h2>
          <NuxtLink to="/customers" class="view-all-link">
            View All
          </NuxtLink>
        </div>
        <div class="content-card__content">
          <div v-if="loading" class="loading-state">
            <div class="loading-spinner"></div>
            <p>Loading recent activity...</p>
          </div>
          <div v-else-if="recentActivity.length === 0" class="empty-state">
            <div class="empty-icon">📊</div>
            <p>No recent activity</p>
          </div>
          <div v-else class="activity-list">
            <div
              v-for="activity in recentActivity"
              :key="activity.id"
              class="activity-item"
            >
              <div class="activity-icon">
                {{ getActivityIcon(activity.type) }}
              </div>
              <div class="activity-content">
                <div class="activity-description">{{ activity.description }}</div>
                <div class="activity-time">{{ formatTimeAgo(activity.timestamp) }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Quick Actions -->
      <div class="content-card">
        <div class="content-card__header">
          <h2>Quick Actions</h2>
        </div>
        <div class="content-card__content">
          <div class="quick-actions">
            <NuxtLink to="/customers/create" class="quick-action">
              <div class="quick-action__icon">👤</div>
              <div class="quick-action__content">
                <div class="quick-action__title">Add Customer</div>
                <div class="quick-action__description">Create a new customer account</div>
              </div>
            </NuxtLink>

            <NuxtLink to="/customers" class="quick-action">
              <div class="quick-action__icon">👥</div>
              <div class="quick-action__content">
                <div class="quick-action__title">Manage Customers</div>
                <div class="quick-action__description">View and edit customer data</div>
              </div>
            </NuxtLink>

            <NuxtLink to="/addresses" class="quick-action quick-action--disabled">
              <div class="quick-action__icon">📍</div>
              <div class="quick-action__content">
                <div class="quick-action__title">Address Management</div>
                <div class="quick-action__description">Coming soon</div>
              </div>
            </NuxtLink>

            <NuxtLink to="/coverage-nodes" class="quick-action quick-action--disabled">
              <div class="quick-action__icon">🗺️</div>
              <div class="quick-action__content">
                <div class="quick-action__title">Coverage Nodes</div>
                <div class="quick-action__description">Coming soon</div>
              </div>
            </NuxtLink>
          </div>
        </div>
      </div>

      <!-- System Status -->
      <div class="content-card content-card--full-width">
        <div class="content-card__header">
          <h2>System Status</h2>
        </div>
        <div class="content-card__content">
          <div class="system-status">
            <div class="status-item">
              <div class="status-indicator status-indicator--healthy"></div>
              <div class="status-content">
                <div class="status-title">Backend API</div>
                <div class="status-description">All systems operational</div>
              </div>
            </div>

            <div class="status-item">
              <div class="status-indicator status-indicator--healthy"></div>
              <div class="status-content">
                <div class="status-title">Database</div>
                <div class="status-description">Connected and healthy</div>
              </div>
            </div>

            <div class="status-item">
              <div class="status-indicator status-indicator--healthy"></div>
              <div class="status-content">
                <div class="status-title">Authentication</div>
                <div class="status-description">Keycloak integration active</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// Page meta with SEO
definePageMeta({
  title: 'Dashboard',
  description: 'BSS Dashboard - Monitor customers, orders, billing, and system status with real-time analytics and insights.',
  ogTitle: 'BSS Dashboard - System Overview',
  ogDescription: 'Monitor your Business Support System with real-time analytics, customer insights, and system health monitoring.',
  ogImage: '/images/dashboard-og.png',
  twitterCard: 'summary_large_image'
})

// Composables
const { get } = useApi()

// Reactive state
const loading = ref(true)
const recentActivity = ref([
  {
    id: '1',
    type: 'customer_created',
    description: 'New customer Jan Kowalski was added',
    timestamp: new Date(Date.now() - 2 * 60 * 1000).toISOString() // 2 minutes ago
  },
  {
    id: '2',
    type: 'customer_updated',
    description: 'Anna Nowak updated her contact information',
    timestamp: new Date(Date.now() - 15 * 60 * 1000).toISOString() // 15 minutes ago
  },
  {
    id: '3',
    type: 'status_changed',
    description: 'Piotr Wiśniewski status changed to ACTIVE',
    timestamp: new Date(Date.now() - 45 * 60 * 1000).toISOString() // 45 minutes ago
  },
  {
    id: '4',
    type: 'customer_created',
    description: 'New customer Maria Kaczmarek was added',
    timestamp: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString() // 2 hours ago
  }
])

const stats = ref({
  customers: {
    total: 1234,
    change: 12
  },
  activeCustomers: 1049,
  addresses: {
    total: 5678,
    change: 45
  },
  coverageNodes: 89
})

// Methods
const getChangeClass = (change: number) => {
  if (change > 0) return 'stat-card__change--positive'
  if (change < 0) return 'stat-card__change--negative'
  return 'stat-card__change--neutral'
}

const getActivityIcon = (type: string) => {
  const icons = {
    customer_created: '👤',
    customer_updated: '✏️',
    status_changed: '🔄',
    customer_deleted: '🗑️'
  }
  return icons[type as keyof typeof icons] || '📊'
}

const formatTimeAgo = (timestamp: string) => {
  const now = new Date()
  const time = new Date(timestamp)
  const diffInMinutes = Math.floor((now.getTime() - time.getTime()) / (1000 * 60))

  if (diffInMinutes < 1) return 'Just now'
  if (diffInMinutes < 60) return `${diffInMinutes} minute${diffInMinutes === 1 ? '' : 's'} ago`
  
  const diffInHours = Math.floor(diffInMinutes / 60)
  if (diffInHours < 24) return `${diffInHours} hour${diffInHours === 1 ? '' : 's'} ago`
  
  const diffInDays = Math.floor(diffInHours / 24)
  return `${diffInDays} day${diffInDays === 1 ? '' : 's'} ago`
}

const fetchDashboardData = async () => {
  loading.value = true
  
  try {
    // In a real app, you would fetch actual data from the API
    // For now, we'll use the mock data
    
    // Example API calls that would be made:
    // const customersResponse = await get('/customers?size=1')
    // const statsResponse = await get('/dashboard/stats')
    // const activityResponse = await get('/dashboard/recent-activity')
    
    // Simulate API delay
    await new Promise(resolve => setTimeout(resolve, 1000))
    
  } catch (error) {
    console.error('Failed to fetch dashboard data:', error)
  } finally {
    loading.value = false
  }
}

// Lifecycle
onMounted(() => {
  fetchDashboardData()
})
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

/* Page Header */
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-6);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--color-border);
}

.page-header__content {
  flex: 1;
}

.page-title {
  margin: 0 0 var(--space-1) 0;
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
}

.page-subtitle {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-base);
}

.page-header__actions {
  flex-shrink: 0;
}

/* Stats Grid */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: var(--space-4);
}

.stat-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
  display: flex;
  align-items: center;
  gap: var(--space-4);
  transition: all var(--transition-base) var(--transition-timing);
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.stat-card__icon {
  width: 60px;
  height: 60px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-2xl);
  flex-shrink: 0;
}

.stat-card__icon--primary {
  background: var(--color-primary-light);
  color: var(--color-primary);
}

.stat-card__icon--success {
  background: var(--color-success-light);
  color: var(--color-success);
}

.stat-card__icon--warning {
  background: var(--color-warning-light);
  color: var(--color-warning);
}

.stat-card__icon--info {
  background: var(--color-primary-light);
  color: var(--color-primary);
}

.stat-card__content {
  flex: 1;
}

.stat-card__value {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  line-height: 1;
  margin-bottom: var(--space-1);
}

.stat-card__label {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-1);
}

.stat-card__change {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
}

.stat-card__change--positive {
  color: var(--color-success);
}

.stat-card__change--negative {
  color: var(--color-danger);
}

.stat-card__change--neutral {
  color: var(--color-text-muted);
}

/* Content Grid */
.content-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: var(--space-6);
}

.content-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.content-card--full-width {
  grid-column: 1 / -1;
}

.content-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4) var(--space-6);
  background: var(--color-surface-alt);
  border-bottom: 1px solid var(--color-border);
}

.content-card__header h2 {
  margin: 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.view-all-link {
  color: var(--color-primary);
  text-decoration: none;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  transition: color var(--transition-fast) var(--transition-timing);
}

.view-all-link:hover {
  color: var(--color-primary-hover);
}

.content-card__content {
  padding: var(--space-6);
}

/* Recent Activity */
.activity-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.activity-item {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
  padding: var(--space-3);
  border-radius: var(--radius-md);
  transition: background-color var(--transition-fast) var(--transition-timing);
}

.activity-item:hover {
  background: var(--color-surface-alt);
}

.activity-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-full);
  background: var(--color-primary-light);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-base);
  flex-shrink: 0;
}

.activity-content {
  flex: 1;
  min-width: 0;
}

.activity-description {
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
  margin-bottom: var(--space-1);
  line-height: var(--line-height-tight);
}

.activity-time {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

/* Quick Actions */
.quick-actions {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.quick-action {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  text-decoration: none;
  color: inherit;
  transition: all var(--transition-fast) var(--transition-timing);
}

.quick-action:hover {
  background: var(--color-surface-alt);
  border-color: var(--color-primary);
  transform: translateY(-1px);
}

.quick-action--disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.quick-action--disabled:hover {
  transform: none;
  border-color: var(--color-border);
}

.quick-action__icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  background: var(--color-primary-light);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-lg);
  flex-shrink: 0;
}

.quick-action__content {
  flex: 1;
}

.quick-action__title {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
  margin-bottom: var(--space-1);
}

.quick-action__description {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

/* System Status */
.system-status {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--space-4);
}

.status-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3);
  border-radius: var(--radius-md);
  background: var(--color-surface-alt);
}

.status-indicator {
  width: 12px;
  height: 12px;
  border-radius: var(--radius-full);
  flex-shrink: 0;
}

.status-indicator--healthy {
  background: var(--color-success);
  box-shadow: 0 0 0 2px var(--color-success-light);
}

.status-content {
  flex: 1;
}

.status-title {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
  margin-bottom: var(--space-1);
}

.status-description {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

/* Loading and Empty States */
.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--space-8);
  text-align: center;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--color-border);
  border-top: 3px solid var(--color-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: var(--space-3);
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: var(--space-3);
  opacity: 0.5;
}

.loading-state p,
.empty-state p {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

/* Mobile Responsive */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
    gap: var(--space-4);
  }
  
  .page-title {
    font-size: var(--font-size-2xl);
  }
  
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .content-grid {
    grid-template-columns: 1fr;
  }
  
  .content-card__header {
    padding: var(--space-3) var(--space-4);
  }
  
  .content-card__content {
    padding: var(--space-4);
  }
  
  .stat-card {
    padding: var(--space-4);
  }
  
  .stat-card__icon {
    width: 48px;
    height: 48px;
  }
}

/* Tablet adjustments */
@media (min-width: 769px) and (max-width: 1024px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
  
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
