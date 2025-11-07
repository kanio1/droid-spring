/**
 * Soak Testing Suite using k6
 *
 * This script performs soak testing to verify system stability
 * and detect memory leaks over extended periods
 */

import { check, sleep } from 'k6'
import http from 'k6/http'
import { Rate, Trend, Counter } from 'k6/metrics'

// Custom metrics
const memoryLeakDetection = new Rate('potential_memory_leak')
const systemStability = new Trend('system_stability')
const requestCount = new Counter('request_count')
const successfulRequests = new Counter('successful_requests')
const failedRequests = new Counter('failed_requests')
const stabilityScore = new Rate('stability_score')

// Test configuration for soak testing (long duration)
export const options = {
  // Soak test: run for 30 minutes at moderate load
  stages: [
    { duration: '5m', target: 20 },  // Warm up
    { duration: '20m', target: 20 }, // Soak
    { duration: '5m', target: 0 },   // Cool down
  ],
  thresholds: {
    http_req_duration: ['p(95)<3000'], // 95% under 3s
    http_req_failed: ['rate<0.05'],     // Error rate under 5%
  },
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

  const response = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify(payload), {
    headers: { 'Content-Type': 'application/json' },
  })

  requestCount.add(1)

  if (response.status === 200) {
    successfulRequests.add(1)
    return { token: response.json('token'), user: user }
  } else {
    failedRequests.add(1)
    return { token: null, user: user }
  }
}

// Soak Test 1: Continuous customer operations
export default function () {
  const user = USERS[Math.floor(Math.random() * USERS.length)]
  const auth = login(user)

  if (auth.token) {
    const headers = {
      'Authorization': `Bearer ${auth.token}`,
      'Content-Type': 'application/json',
    }

    // List customers
    const response1 = http.get(`${BASE_URL}/api/v1/customers?page=0&size=20`, { headers })
    const success1 = check(response1, {
      'customer list is stable': (r) => r.status === 200,
    })

    stabilityScore.add(success1)

    // Small delay
    sleep(1)

    // Search customers
    const response2 = http.get(`${BASE_URL}/api/v1/customers?search=test&page=0&size=20`, { headers })
    const success2 = check(response2, {
      'customer search is stable': (r) => r.status === 200,
    })

    stabilityScore.add(success2)

    sleep(2)
  } else {
    sleep(1)
  }
}

// Soak Test 2: Continuous order operations
export function soakOrderOperations() {
  const user = USERS[Math.floor(Math.random() * USERS.length)]
  const auth = login(user)

  if (auth.token) {
    const headers = {
      'Authorization': `Bearer ${auth.token}`,
      'Content-Type': 'application/json',
    }

    const response = http.get(`${BASE_URL}/api/v1/orders?page=0&size=20`, { headers })

    const success = check(response, {
      'order operations stable': (r) => r.status === 200,
    })

    stabilityScore.add(success)
    sleep(1)
  }
}

// Soak Test 3: Continuous invoice operations
export function soakInvoiceOperations() {
  const user = USERS[Math.floor(Math.random() * USERS.length)]
  const auth = login(user)

  if (auth.token) {
    const headers = {
      'Authorization': `Bearer ${auth.token}`,
      'Content-Type': 'application/json',
    }

    const response = http.get(`${BASE_URL}/api/v1/invoices?page=0&size=20`, { headers })

    const success = check(response, {
      'invoice operations stable': (r) => r.status === 200,
    })

    stabilityScore.add(success)
    sleep(1)
  }
}

// Soak Test 4: Continuous payment operations
export function soakPaymentOperations() {
  const user = USERS[Math.floor(Math.random() * USERS.length)]
  const auth = login(user)

  if (auth.token) {
    const headers = {
      'Authorization': `Bearer ${auth.token}`,
      'Content-Type': 'application/json',
    }

    const response = http.get(`${BASE_URL}/api/v1/payments?page=0&size=20`, { headers })

    const success = check(response, {
      'payment operations stable': (r) => r.status === 200,
    })

    stabilityScore.add(success)
    sleep(1)
  }
}

// Soak Test 5: Continuous subscription operations
export function soakSubscriptionOperations() {
  const user = USERS[Math.floor(Math.random() * USERS.length)]
  const auth = login(user)

  if (auth.token) {
    const headers = {
      'Authorization': `Bearer ${auth.token}`,
      'Content-Type': 'application/json',
    }

    const response = http.get(`${BASE_URL}/api/v1/subscriptions?page=0&size=20`, { headers })

    const success = check(response, {
      'subscription operations stable': (r) => r.status === 200,
    })

    stabilityScore.add(success)
    sleep(1)
  }
}

// Soak Test 6: Mixed operations cycle
export function soakMixedOperationsCycle() {
  const user = USERS[Math.floor(Math.random() * USERS.length)]
  const auth = login(user)

  if (auth.token) {
    const headers = {
      'Authorization': `Bearer ${auth.token}`,
      'Content-Type': 'application/json',
    }

    // Complete cycle of operations
    const operations = [
      { name: 'dashboard', url: `${BASE_URL}/dashboard` },
      { name: 'customers', url: `${BASE_URL}/api/v1/customers?page=0&size=20` },
      { name: 'orders', url: `${BASE_URL}/api/v1/orders?page=0&size=20` },
      { name: 'invoices', url: `${BASE_URL}/api/v1/invoices?page=0&size=20` },
      { name: 'payments', url: `${BASE_URL}/api/v1/payments?page=0&size=20` },
    ]

    for (const op of operations) {
      const response = http.get(op.url, { headers })

      const success = check(response, {
        [`${op.name} operation stable`]: (r) => r.status === 200,
      })

      stabilityScore.add(success)
      sleep(0.5)
    }
  }
}

// Soak Test 7: Repeated login sessions
export function soakRepeatedLogins() {
  const user = USERS[Math.floor(Math.random() * USERS.length)]

  // Login multiple times to test session management
  for (let i = 0; i < 2; i++) {
    const auth = login(user)

    if (auth.token) {
      stabilityScore.add(1)
    }

    sleep(0.5)
  }
}

// Soak Test 8: Continuous data access patterns
export function soakDataAccessPatterns() {
  const user = USERS[Math.floor(Math.random() * USERS.length)]
  const auth = login(user)

  if (auth.token) {
    const headers = {
      'Authorization': `Bearer ${auth.token}`,
      'Content-Type': 'application/json',
    }

    // Simulate typical user data access pattern
    const patterns = [
      () => http.get(`${BASE_URL}/api/v1/customers?page=0&size=20`, { headers }),
      () => http.get(`${BASE_URL}/api/v1/customers/1`, { headers }),
      () => http.get(`${BASE_URL}/api/v1/orders?page=0&size=20`, { headers }),
    ]

    for (const pattern of patterns) {
      const response = pattern()
      const success = check(response, {
        'data access pattern stable': (r) => r.status < 500,
      })

      stabilityScore.add(success)
      sleep(0.3)
    }
  }
}

// Setup
export function setup() {
  console.log('Starting soak test...')
  console.log(`Base URL: ${BASE_URL}`)
  console.log('Duration: 30 minutes')
  console.log('Purpose: Detect memory leaks and stability issues')
  return { baseUrl: BASEURL, startTime: new Date().toISOString() }
}

// Teardown
export function teardown(data) {
  const endTime = new Date().toISOString()
  console.log('Soak test completed!')
  console.log(`\nStart time: ${data.startTime}`)
  console.log(`End time: ${endTime}`)
  console.log('\n=== SOAK TEST SUMMARY ===')
  console.log(`Total requests: ${requestCount.values}`)
  console.log(`Successful requests: ${successfulRequests.values}`)
  console.log(`Failed requests: ${failedRequests.values}`)
  console.log(`Stability score: ${(stabilityScore.values * 100).toFixed(2)}%`)
  console.log('\nReview the results to check for:')
  console.log('1. Memory leaks over time')
  console.log('2. Performance degradation')
  console.log('3. Resource consumption patterns')
  console.log('4. Long-term stability')
}
