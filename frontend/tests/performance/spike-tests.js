/**
 * Spike Testing Suite using k6
 *
 * This script performs spike testing to verify system behavior
 * with sudden bursts of traffic
 */

import { check, sleep } from 'k6'
import http from 'k6/http'
import { Rate, Trend, Counter } from 'k6/metrics'

// Custom metrics
const spikeDetection = new Rate('spike_detected')
const recoveryRate = new Rate('recovery_rate')
const spikeResponseTime = new Trend('spike_response_time')
const requestCount = new Counter('request_count')
const spikeRequestCount = new Counter('spike_request_count')

// Test configuration for spike testing
export const options = {
  // Spike test: sudden burst of traffic
  stages: [
    { duration: '1m', target: 10 },   // Normal load
    { duration: '10s', target: 200 }, // SPIKE!
    { duration: '30s', target: 200 }, // Stay at spike
    { duration: '1m', target: 10 },   // Back to normal
    { duration: '10s', target: 200 }, // Another spike
    { duration: '30s', target: 200 }, // Stay at spike
    { duration: '1m', target: 10 },   // Recover
  ],
}

// Test data
const BASE_URL = __ENV.BASE_URL || 'http://localhost:3000'
const USERS = [
  { username: 'admin', password: 'password' },
  { username: 'user1', password: 'password123' },
]

function login(user) {
  const payload = {
    username: user.username,
    password: user.password,
  }

  const startTime = Date.now()
  const response = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify(payload), {
    headers: { 'Content-Type': 'application/json' },
  })
  const endTime = Date.now()

  spikeResponseTime.add(endTime - startTime)
  requestCount.add(1)

  if (response.status === 200) {
    return { token: response.json('token'), user: user }
  }

  return { token: null, user: user }
}

// Spike Test 1: Customer operations during spike
export default function () {
  const user = USERS[Math.floor(Math.random() * USERS.length)]
  const auth = login(user)

  if (auth.token) {
    const headers = {
      'Authorization': `Bearer ${auth.token}`,
      'Content-Type': 'application/json',
    }

    // Spike request
    const startTime = Date.now()
    const response = http.get(`${BASE_URL}/api/v1/customers?page=0&size=20`, { headers })
    const endTime = Date.now()

    spikeResponseTime.add(endTime - startTime)
    spikeRequestCount.add(1)

    const success = check(response, {
      'customer list during spike': (r) => r.status < 500,
    })

    spikeDetection.add(!success)

    // During spike, minimal sleep
    if (Math.random() < 0.5) {
      sleep(0.1)
    }
  }
}

// Spike Test 2: Order operations during spike
export function spikeOrderOperations() {
  const user = USERS[Math.floor(Math.random() * USERS.length)]
  const auth = login(user)

  if (auth.token) {
    const headers = {
      'Authorization': `Bearer ${auth.token}`,
      'Content-Type': 'application/json',
    }

    const response = http.get(`${BASE_URL}/api/v1/orders?page=0&size=20`, { headers })

    check(response, {
      'orders during spike': (r) => r.status < 500,
    })

    spikeRequestCount.add(1)
  }
}

// Spike Test 3: Invoice operations during spike
export function spikeInvoiceOperations() {
  const user = USERS[Math.floor(Math.random() * USERS.length)]
  const auth = login(user)

  if (auth.token) {
    const headers = {
      'Authorization': `Bearer ${auth.token}`,
      'Content-Type': 'application/json',
    }

    const response = http.get(`${BASE_URL}/api/v1/invoices?page=0&size=20`, { headers })

    check(response, {
      'invoices during spike': (r) => r.status < 500,
    })

    spikeRequestCount.add(1)
  }
}

// Spike Test 4: Payment operations during spike
export function spikePaymentOperations() {
  const user = USERS[Math.floor(Math.random() * USERS.length)]
  const auth = login(user)

  if (auth.token) {
    const headers = {
      'Authorization': `Bearer ${auth.token}`,
      'Content-Type': 'application/json',
    }

    const response = http.get(`${BASE_URL}/api/v1/payments?page=0&size=20`, { headers })

    check(response, {
      'payments during spike': (r) => r.status < 500,
    })

    spikeRequestCount.add(1)
  }
}

// Spike Test 5: Subscription operations during spike
export function spikeSubscriptionOperations() {
  const user = USERS[Math.floor(Math.random() * USERS.length)]
  const auth = login(user)

  if (auth.token) {
    const headers = {
      'Authorization': `Bearer ${auth.token}`,
      'Content-Type': 'application/json',
    }

    const response = http.get(`${BASE_URL}/api/v1/subscriptions?page=0&size=20`, { headers })

    check(response, {
      'subscriptions during spike': (r) => r.status < 500,
    })

    spikeRequestCount.add(1)
  }
}

// Spike Test 6: Dashboard during spike
export function spikeDashboardAccess() {
  const user = USERS[Math.floor(Math.random() * USERS.length)]
  const auth = login(user)

  if (auth.token) {
    const headers = {
      'Authorization': `Bearer ${auth.token}`,
    }

    const response = http.get(`${BASE_URL}/dashboard`, { headers })

    check(response, {
      'dashboard during spike': (r) => r.status < 500,
    })

    spikeRequestCount.add(1)
  }
}

// Spike Test 7: Burst of different operations
export function spikeBurstOperations() {
  const user = USERS[Math.floor(Math.random() * USERS.length)]
  const auth = login(user)

  if (auth.token) {
    const headers = {
      'Authorization': `Bearer ${auth.token}`,
      'Content-Type': 'application/json',
    }

    // Burst: multiple operations in quick succession
    const operations = [
      () => http.get(`${BASE_URL}/api/v1/customers?page=0&size=20`, { headers }),
      () => http.get(`${BASE_URL}/api/v1/orders?page=0&size=20`, { headers }),
      () => http.get(`${BASE_URL}/api/v1/invoices?page=0&size=20`, { headers }),
    ]

    for (const operation of operations) {
      const response = operation()

      if (response.status < 500) {
        spikeRequestCount.add(1)
      }
    }
  }
}

// Setup
export function setup() {
  console.log('Starting spike test...')
  console.log(`Base URL: ${BASE_URL}`)
  console.log('This test simulates sudden traffic spikes')
  return { baseUrl: BASE_URL }
}

// Teardown
export function teardown(data) {
  console.log('Spike test completed!')
  console.log('\n=== SPIKE TEST SUMMARY ===')
  console.log(`Total requests: ${requestCount.values}`)
  console.log(`Spike requests: ${spikeRequestCount.values}`)
  console.log(`Review metrics to assess:`)
  console.log('1. System recovery after spikes')
  console.log('2. Response time degradation')
  console.log('3. Error rates during spikes')
}
