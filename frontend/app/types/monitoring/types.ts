// Monitoring & Alert System Types

export interface ResourceMetric {
  id: number
  customerId: number
  resourceId: number
  metricType: string
  value: number
  unit: string
  timestamp: string
  source: string
}

export interface Alert {
  id: number
  customerId: number
  resourceId: number
  metricType: string
  severity: 'WARNING' | 'CRITICAL' | 'OK'
  status: 'OPEN' | 'ACKNOWLEDGED' | 'RESOLVED'
  currentValue: number
  thresholdValue: number
  thresholdType: string
  message: string
  triggeredAt: string
  resolvedAt?: string
  acknowledgedBy?: string
  acknowledgedAt?: string
  resolvedBy?: string
  source: string
}

export interface ResourceThreshold {
  id: number
  customerId: number
  resourceId: number
  metricType: string
  warningThreshold: number
  criticalThreshold: number
  operator: 'GT' | 'LT'
  consecutiveViolations: number
  enabled: boolean
  createdAt: string
  updatedAt: string
}

export interface NotificationPreference {
  id: number
  customerId: number
  email?: string
  phoneNumber?: string
  slackChannel?: string
  emailEnabled: boolean
  smsEnabled: boolean
  slackEnabled: boolean
  criticalAlertsOnly: boolean
  createdAt: string
  updatedAt: string
}

export interface CustomerResource {
  id: number
  customerId: number
  resourceCatalogId: string
  resourceName: string
  resourceType: string
  unit: string
  currentUsage: number
  limitValue?: number
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED'
  createdAt: string
  updatedAt: string
}

export interface MetricSummary {
  metricType: string
  currentValue: number
  unit: string
  averageValue: number
  minValue: number
  maxValue: number
  trend: 'UP' | 'DOWN' | 'STABLE'
  lastUpdated: string
}

export interface AlertStatistics {
  total: number
  open: number
  acknowledged: number
  resolved: number
  critical: number
  warning: number
}

export interface ResourceUsageStatistics {
  resourceId: number
  resourceName: string
  resourceType: string
  currentUsage: number
  limitValue?: number
  usagePercentage: number
  status: 'NORMAL' | 'WARNING' | 'CRITICAL'
}
