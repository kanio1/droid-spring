/**
 * Database Load Test
 *
 * Tests database operations under load
 * Simulates heavy read/write operations
 *
 * Usage:
 * k6 run --vus 20 --duration 10m database-load-test.js
 */

import { check, sleep } from 'k6'
import http from 'k6/http'
import { Counter, Rate, Trend } from 'k6/metrics'

// Custom metrics
const dbOperations = new Counter('db_operations')
const dbErrors = new Counter('db_errors')
const dbLatency = new Trend('db_latency')

export const options = {
  stages: [
    { duration: '2m', target: 20 },
    { duration: '5m', target: 20 },
    { duration: '2m', target: 50 },
    { duration: '5m', target: 50 },
    { duration: '2m', target: 0 },
  ],
  thresholds: {
    db_operations: ['count>1000'],
    db_errors: ['count<50'],
    db_latency: ['p(95)<1000'],
  },
}

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080'
const API_BASE = `${BASE_URL}/api`

// Test customer queries with various filters
function testCustomerQueries() {
  const queries = [
    () => http.get(`${API_BASE}/customers?status=active`),
    () => http.get(`${API_BASE}/customers?status=inactive`),
    () => http.get(`${API_BASE}/customers?page=1&limit=50`),
    () => http.get(`${API_BASE}/customers?sort=createdAt&order=desc`),
    () => http.get(`${API_BASE}/customers?createdAfter=2024-01-01`),
  ]

  const response = queries[Math.floor(Math.random() * queries.length)]()
  const startTime = Date.now()

  const success = check(response, {
    'query successful': (r) => r.status === 200,
    'response has data': (r) => r.json('data') !== undefined,
  })

  const endTime = Date.now()
  dbLatency.add(endTime - startTime)

  if (success) {
    dbOperations.add(1)
  } else {
    dbErrors.add(1)
  }
}

// Test complex joins (orders with customers)
function testComplexJoins() {
  const response = http.get(`${API_BASE}/orders?includeCustomer=true&page=1&limit=20`)

  const success = check(response, {
    'join query successful': (r) => r.status === 200,
    'includes customer data': (r) => {
      const data = r.json('data')
      return data && data.length > 0 && data[0].customer !== undefined
    },
  })

  if (success) {
    dbOperations.add(1)
  } else {
    dbErrors.add(1)
  }
}

// Test aggregation queries
function testAggregationQueries() {
  const response = http.get(`${API_BASE}/analytics/revenue?period=monthly&year=2024`)

  const success = check(response, {
    'aggregation successful': (r) => r.status === 200,
    'has aggregated data': (r) => r.json('data') !== undefined,
  })

  if (success) {
    dbOperations.add(1)
  } else {
    dbErrors.add(1)
  }
}

// Test invoice generation (write-heavy operation)
function testInvoiceGeneration() {
  const payload = JSON.stringify({
    customerId: `cust-${Math.floor(Math.random() * 100)}`,
    orderId: `order-${Math.floor(Math.random() * 100)}`,
    dueDate: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString(),
  })

  const response = http.post(`${API_BASE}/invoices`, payload, {
    headers: { 'Content-Type': 'application/json' },
  })

  const success = check(response, {
    'invoice generated': (r) => r.status === 201,
    'invoice has ID': (r) => r.json('id') !== undefined,
  })

  if (success) {
    dbOperations.add(1)
  } else {
    dbErrors.add(1)
  }
}

// Test subscription queries
function testSubscriptionQueries() {
  const queries = [
    () => http.get(`${API_BASE}/subscriptions?status=active`),
    () => http.get(`${API_BASE}/subscriptions?customerId=cust-1`),
    () => http.get(`${API_BASE}/subscriptions?expiringInDays=30`),
  ]

  const response = queries[Math.floor(Math.random() * queries.length)]()

  const success = check(response, {
    'subscription query successful': (r) => r.status === 200,
    'has subscription data': (r) => r.json('data') !== undefined,
  })

  if (success) {
    dbOperations.add(1)
  } else {
    dbErrors.add(1)
  }
}

// Main test function
export default function () {
  const operations = [
    { fn: testCustomerQueries, weight: 40 },
    { fn: testComplexJoins, weight: 20 },
    { fn: testAggregationQueries, weight: 10 },
    { fn: testInvoiceGeneration, weight: 20 },
    { fn: testSubscriptionQueries, weight: 10 },
  ]

  // Weighted random selection
  const totalWeight = operations.reduce((sum, op) => sum + op.weight, 0)
  let random = Math.random() * totalWeight

  for (const op of operations) {
    if (random < op.weight) {
      op.fn()
      break
    }
    random -= op.weight
  }

  sleep(Math.random() * 3)
}
