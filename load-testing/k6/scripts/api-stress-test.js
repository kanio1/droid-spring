/**
 * API Stress Test
 *
 * Tests API endpoints under high load
 * Measures throughput, latency, and error rates
 *
 * Usage:
 * k6 run --vus 100 --duration 10m api-stress-test.js
 */

import { check, sleep } from 'k6'
import http from 'k6/http'
import { Counter, Rate, Trend } from 'k6/metrics'

// Custom metrics
const successfulRequests = new Counter('successful_requests')
const failedRequests = new Counter('failed_requests')
const requestRate = new Rate('request_rate')
const errorRate = new Rate('errors')
const responseTime = new Trend('response_time')

// Test configuration
export const options = {
  stages: [
    { duration: '2m', target: 50 },
    { duration: '5m', target: 50 },
    { duration: '2m', target: 100 },
    { duration: '5m', target: 100 },
    { duration: '2m', target: 200 },
    { duration: '5m', target: 200 },
    { duration: '2m', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(99)<1000'],
    http_req_failed: ['rate<0.05'],
    successful_requests: ['count>0'],
    failed_requests: ['count<100'],
  },
}

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080'
const API_BASE = `${BASE_URL}/api`

// Test data
const CUSTOMER_IDS = Array.from({ length: 100 }, (_, i) => `cust-${i + 1}`)
const ORDER_IDS = Array.from({ length: 100 }, (_, i) => `order-${i + 1}`)

// Get random ID
function getRandomId(ids) {
  return ids[Math.floor(Math.random() * ids.length)]
}

// GET /api/customers
function testGetCustomers() {
  const response = http.get(`${API_BASE}/customers`)

  const success = check(response, {
    'customers retrieved': (r) => r.status === 200,
    'response time OK': (r) => r.timings.duration < 500,
  })

  requestRate.add(1)
  responseTime.add(response.timings.duration)

  if (success) {
    successfulRequests.add(1)
  } else {
    failedRequests.add(1)
    errorRate.add(1)
  }
}

// GET /api/customers/{id}
function testGetCustomer() {
  const customerId = getRandomId(CUSTOMER_IDS)
  const response = http.get(`${API_BASE}/customers/${customerId}`)

  const success = check(response, {
    'customer retrieved': (r) => r.status === 200 || r.status === 404,
    'response time OK': (r) => r.timings.duration < 300,
  })

  requestRate.add(1)
  responseTime.add(response.timings.duration)

  if (success) {
    successfulRequests.add(1)
  } else {
    failedRequests.add(1)
    errorRate.add(1)
  }
}

// GET /api/orders
function testGetOrders() {
  const response = http.get(`${API_BASE}/orders`)

  const success = check(response, {
    'orders retrieved': (r) => r.status === 200,
    'response time OK': (r) => r.timings.duration < 500,
  })

  requestRate.add(1)
  responseTime.add(response.timings.duration)

  if (success) {
    successfulRequests.add(1)
  } else {
    failedRequests.add(1)
    errorRate.add(1)
  }
}

// GET /api/orders/{id}
function testGetOrder() {
  const orderId = getRandomId(ORDER_IDS)
  const response = http.get(`${API_BASE}/orders/${orderId}`)

  const success = check(response, {
    'order retrieved': (r) => r.status === 200 || r.status === 404,
    'response time OK': (r) => r.timings.duration < 300,
  })

  requestRate.add(1)
  responseTime.add(response.timings.duration)

  if (success) {
    successfulRequests.add(1)
  } else {
    failedRequests.add(1)
    errorRate.add(1)
  }
}

// GET /api/invoices
function testGetInvoices() {
  const response = http.get(`${API_BASE}/invoices`)

  const success = check(response, {
    'invoices retrieved': (r) => r.status === 200,
    'response time OK': (r) => r.timings.duration < 500,
  })

  requestRate.add(1)
  responseTime.add(response.timings.duration)

  if (success) {
    successfulRequests.add(1)
  } else {
    failedRequests.add(1)
    errorRate.add(1)
  }
}

// GET /api/payments
function testGetPayments() {
  const response = http.get(`${API_BASE}/payments`)

  const success = check(response, {
    'payments retrieved': (r) => r.status === 200,
    'response time OK': (r) => r.timings.duration < 500,
  })

  requestRate.add(1)
  responseTime.add(response.timings.duration)

  if (success) {
    successfulRequests.add(1)
  } else {
    failedRequests.add(1)
    errorRate.add(1)
  }
}

// GET /health
function testHealthCheck() {
  const response = http.get(`${BASE_URL}/actuator/health`)

  const success = check(response, {
    'health check OK': (r) => r.status === 200,
    'response time OK': (r) => r.timings.duration < 100,
  })

  requestRate.add(1)
  responseTime.add(response.timings.duration)

  if (success) {
    successfulRequests.add(1)
  } else {
    failedRequests.add(1)
    errorRate.add(1)
  }
}

// GET /actuator/metrics
function testMetricsEndpoint() {
  const response = http.get(`${BASE_URL}/actuator/metrics`)

  const success = check(response, {
    'metrics retrieved': (r) => r.status === 200,
    'response time OK': (r) => r.timings.duration < 200,
  })

  requestRate.add(1)
  responseTime.add(response.timings.duration)

  if (success) {
    successfulRequests.add(1)
  } else {
    failedRequests.add(1)
    errorRate.add(1)
  }
}

// Main test function - randomly select endpoint to test
export default function () {
  const endpoints = [
    testGetCustomers,
    testGetCustomer,
    testGetOrders,
    testGetOrder,
    testGetInvoices,
    testGetPayments,
    testHealthCheck,
    testMetricsEndpoint,
  ]

  // Select random endpoint
  const endpoint = endpoints[Math.floor(Math.random() * endpoints.length)]
  endpoint()

  // Random sleep between requests
  sleep(Math.random() * 2)
}
