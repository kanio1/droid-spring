<template>
  <div class="customer-insights" data-testid="customer-insights">
    <div v-if="loading" class="loading-state" data-testid="insights-loading">
      <div class="spinner"></div>
      <p>Loading AI insights...</p>
    </div>

    <div v-else-if="error" class="error-state" data-testid="insights-error">
      <p class="error-message">{{ error }}</p>
      <button @click="loadInsights" class="retry-button">Retry</button>
    </div>

    <div v-else-if="insights.length === 0" class="empty-state" data-testid="insights-empty">
      <div class="empty-icon">🤖</div>
      <h3>No AI insights available</h3>
      <p>AI insights will appear here when customer data is analyzed.</p>
    </div>

    <div v-else class="insights-container">
      <div class="insights-header">
        <h2>AI-Powered Insights</h2>
        <div class="insight-count">{{ insights.length }} active insights</div>
      </div>

      <div class="insights-grid">
        <div
          v-for="insight in sortedInsights"
          :key="insight.id"
          class="insight-card"
          :class="[
            `insight-type-${insight.insightType.toLowerCase()}`,
            { 'high-confidence': insight.confidenceScore >= 0.8 },
            { 'high-priority': insight.priority >= 8 }
          ]"
          :data-testid="`insight-${insight.id}`"
        >
          <div class="insight-header">
            <div class="insight-type-badge">
              {{ formatInsightType(insight.insightType) }}
            </div>
            <div class="insight-priority" v-if="insight.priority >= 8">
              ⚡ High Priority
            </div>
          </div>

          <h3 class="insight-title">{{ insight.title }}</h3>
          <p class="insight-description">{{ insight.description }}</p>

          <div class="insight-metadata">
            <div class="confidence-score">
              <span class="label">Confidence:</span>
              <span class="value">{{ formatPercentage(insight.confidenceScore) }}</span>
            </div>
            <div class="model-info">
              <span class="label">Model:</span>
              <span class="value">{{ insight.modelName }}</span>
            </div>
          </div>

          <div v-if="insight.data && Object.keys(insight.data).length > 0" class="insight-data">
            <h4>Details</h4>
            <div class="data-grid">
              <div
                v-for="(value, key) in insight.data"
                :key="key"
                class="data-item"
              >
                <span class="data-key">{{ formatDataKey(key) }}:</span>
                <span class="data-value">{{ value }}</span>
              </div>
            </div>
          </div>

          <div class="insight-actions">
            <button
              v-if="insight.status === 'ACTIVE'"
              @click="markAsViewed(insight.id)"
              class="btn-view"
              data-testid="btn-view-insight"
            >
              Mark as Viewed
            </button>
            <button
              v-if="insight.status === 'ACTIVE'"
              @click="dismissInsight(insight.id)"
              class="btn-dismiss"
              data-testid="btn-dismiss-insight"
            >
              Dismiss
            </button>
            <span v-else class="status-badge" :class="`status-${insight.status.toLowerCase()}`">
              {{ insight.status }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'

interface InsightData {
  [key: string]: string
}

interface CustomerInsight {
  id: string
  customerId: string
  tenantId: string
  insightType: 'CHURN_RISK' | 'LIFETIME_VALUE' | 'CROSS_SELL_OPPORTUNITY' | 'BEHAVIORAL_PATTERN' | 'PURCHASE_PREDICTION' | string
  title: string
  description: string
  confidenceScore: number
  priority: number
  category: string
  status: 'ACTIVE' | 'VIEWED' | 'DISMISSED' | 'EXPIRED' | 'IMPLEMENTED'
  expiresAt?: string
  modelName: string
  modelVersion: string
  createdAt: string
  data?: InsightData
}

const props = defineProps<{
  customerId: string
  tenantId: string
}>()

const insights = ref<CustomerInsight[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

const sortedInsights = computed(() => {
  return [...insights.value].sort((a, b) => {
    // First by priority (high to low)
    if (b.priority !== a.priority) {
      return b.priority - a.priority
    }
    // Then by confidence (high to low)
    if (b.confidenceScore !== a.confidenceScore) {
      return b.confidenceScore - a.confidenceScore
    }
    // Finally by creation date (newest first)
    return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
  })
})

async function loadInsights() {
  loading.value = true
  error.value = null

  try {
    // In real implementation, would use API call
    // For now, simulate loading
    await new Promise(resolve => setTimeout(resolve, 1000))

    // Mock data - in real app would fetch from API
    insights.value = [
      {
        id: '1',
        customerId: props.customerId,
        tenantId: props.tenantId,
        insightType: 'CHURN_RISK',
        title: 'High Churn Risk Detected',
        description: 'Customer shows multiple warning signs including decreased engagement over the last 30 days.',
        confidenceScore: 0.87,
        priority: 9,
        category: 'risk',
        status: 'ACTIVE',
        modelName: 'ChurnRiskModel-v2.1',
        modelVersion: '2.1.0',
        createdAt: new Date().toISOString(),
        data: {
          risk_score: '73.5',
          risk_level: 'HIGH',
          main_factors: 'low_engagement,recent_cancellations,support_tickets',
          recommended_actions: 'engagement_campaign,personal_outreach,loyalty_program'
        }
      },
      {
        id: '2',
        customerId: props.customerId,
        tenantId: props.tenantId,
        insightType: 'LIFETIME_VALUE',
        title: 'Predicted Customer Lifetime Value',
        description: 'AI model predicts this customer\'s lifetime value at $12,450 over the next 12 months.',
        confidenceScore: 0.92,
        priority: 8,
        category: 'financial',
        status: 'ACTIVE',
        modelName: 'LTVPredictionModel-v3.2',
        modelVersion: '3.2.0',
        createdAt: new Date().toISOString(),
        data: {
          predicted_ltv: '12450.00',
          confidence_interval: '$9,960 - $14,940',
          timeframe: '12 months'
        }
      },
      {
        id: '3',
        customerId: props.customerId,
        tenantId: props.tenantId,
        insightType: 'CROSS_SELL_OPPORTUNITY',
        title: 'Cross-Sell Opportunity: Premium',
        description: 'Based on customer behavior, there\'s a 78% likelihood this customer would be interested in premium products.',
        confidenceScore: 0.78,
        priority: 7,
        category: 'revenue',
        status: 'ACTIVE',
        modelName: 'CrossSellModel-v1.8',
        modelVersion: '1.8.3',
        createdAt: new Date().toISOString(),
        data: {
          likelihood: '0.78',
          product_category: 'premium',
          expected_value: '$345.00',
          timeframe: '30 days'
        }
      }
    ]
  } catch (err) {
    error.value = 'Failed to load insights. Please try again.'
    console.error('Error loading insights:', err)
  } finally {
    loading.value = false
  }
}

async function markAsViewed(insightId: string) {
  try {
    // In real implementation, would call API
    const insight = insights.value.find(i => i.id === insightId)
    if (insight) {
      insight.status = 'VIEWED'
    }
  } catch (err) {
    console.error('Error marking insight as viewed:', err)
  }
}

async function dismissInsight(insightId: string) {
  try {
    // In real implementation, would call API
    const insight = insights.value.find(i => i.id === insightId)
    if (insight) {
      insight.status = 'DISMISSED'
      // Remove from active list
      insights.value = insights.value.filter(i => i.id !== insightId)
    }
  } catch (err) {
    console.error('Error dismissing insight:', err)
  }
}

function formatInsightType(type: string): string {
  return type.replace(/_/g, ' ').toLowerCase()
    .split(' ')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
}

function formatPercentage(value: number): string {
  return `${Math.round(value * 100)}%`
}

function formatDataKey(key: string): string {
  return key.replace(/_/g, ' ')
    .split(' ')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
}

onMounted(() => {
  loadInsights()
})
</script>

<style scoped>
.customer-insights {
  padding: 1.5rem;
  max-width: 1400px;
  margin: 0 auto;
}

.loading-state,
.error-state,
.empty-state {
  text-align: center;
  padding: 3rem 1rem;
}

.spinner {
  border: 3px solid #f3f3f3;
  border-top: 3px solid #3498db;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  animation: spin 1s linear infinite;
  margin: 0 auto 1rem;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.insights-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.insights-header h2 {
  font-size: 1.75rem;
  font-weight: 600;
  color: #2c3e50;
  margin: 0;
}

.insight-count {
  background: #e8f4f8;
  color: #2c3e50;
  padding: 0.5rem 1rem;
  border-radius: 20px;
  font-size: 0.9rem;
  font-weight: 500;
}

.insights-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 1.5rem;
}

.insight-card {
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.insight-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  transform: translateY(-2px);
}

.insight-card.high-confidence {
  border-left: 4px solid #27ae60;
}

.insight-card.high-priority {
  background: linear-gradient(to right, #fff5f5 0%, white 10%);
}

.insight-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.insight-type-badge {
  background: #e8f4f8;
  color: #2c3e50;
  padding: 0.25rem 0.75rem;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.insight-priority {
  color: #e74c3c;
  font-size: 0.85rem;
  font-weight: 600;
}

.insight-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #2c3e50;
  margin: 0 0 0.75rem 0;
}

.insight-description {
  color: #555;
  line-height: 1.6;
  margin: 0 0 1rem 0;
}

.insight-metadata {
  display: flex;
  gap: 1.5rem;
  padding: 0.75rem;
  background: #f8f9fa;
  border-radius: 6px;
  margin-bottom: 1rem;
  font-size: 0.9rem;
}

.insight-metadata .label {
  color: #666;
  font-weight: 500;
  margin-right: 0.5rem;
}

.insight-metadata .value {
  color: #2c3e50;
  font-weight: 600;
}

.insight-data {
  margin-bottom: 1rem;
}

.insight-data h4 {
  font-size: 0.95rem;
  font-weight: 600;
  color: #2c3e50;
  margin: 0 0 0.75rem 0;
}

.data-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 0.5rem;
}

.data-item {
  display: flex;
  justify-content: space-between;
  padding: 0.5rem;
  background: #f8f9fa;
  border-radius: 4px;
  font-size: 0.9rem;
}

.data-key {
  color: #666;
  font-weight: 500;
}

.data-value {
  color: #2c3e50;
  font-weight: 600;
  text-align: right;
}

.insight-actions {
  display: flex;
  gap: 0.75rem;
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #e0e0e0;
}

.btn-view,
.btn-dismiss {
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 6px;
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-view {
  background: #3498db;
  color: white;
}

.btn-view:hover {
  background: #2980b9;
}

.btn-dismiss {
  background: #e74c3c;
  color: white;
}

.btn-dismiss:hover {
  background: #c0392b;
}

.status-badge {
  padding: 0.5rem 1rem;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.status-active {
  background: #d4edda;
  color: #155724;
}

.status-viewed {
  background: #d1ecf1;
  color: #0c5460;
}

.status-dismissed {
  background: #f8d7da;
  color: #721c24;
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
}

.empty-state h3 {
  color: #2c3e50;
  margin: 0 0 0.5rem 0;
}

.empty-state p {
  color: #666;
}

.error-message {
  color: #e74c3c;
  margin-bottom: 1rem;
}

.retry-button {
  background: #3498db;
  color: white;
  border: none;
  padding: 0.75rem 1.5rem;
  border-radius: 6px;
  font-size: 1rem;
  cursor: pointer;
  transition: background 0.2s ease;
}

.retry-button:hover {
  background: #2980b9;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .insights-grid {
    grid-template-columns: 1fr;
  }

  .insights-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }

  .insight-metadata {
    flex-direction: column;
    gap: 0.5rem;
  }
}
</style>
