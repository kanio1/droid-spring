/**
 * Stress Testing Suite using k6
 *
 * This script performs stress testing to find breaking points
 * and verify system behavior under extreme load conditions
 */

import { check, sleep } from 'k6'
import http from 'k6/http'
import { Rate, Trend, Counter } from 'k6/metrics'

// Custom metrics
const errorRate = new Rate('errors')
const breakingPoint = new Rate('breaking_point')
const timeoutRate = new Rate('timeout_rate')
const pageLoadTime = new Trend('page_load_time')
const apiResponseTime = new Trend('api_response_time')
const requestCount = new Counter('request_count')
const successfulRequests = new Counter('successful_requests')
const failedRequests = new Counter('failed_requests')

// Test configuration for stress testing
export const options = {
  // Stress test: ramp up to 200 VUs very quickly
  stages: [
    { duration: '30s', target: 50 },   // Quick ramp-up
    { duration: '1m', target: 100 },   // Increase load
    { duration: '2m', target: 200 },   // Peak stress
    { duration: '1m', target: 200 },   // Stay at breaking point
    { duration: '30s', target: 0 },    // Quick ramp-down
  ],
  thresholds: {
    // More relaxed thresholds for stress testing
    http_req_duration: ['p(95)<5000'], // 95% under 5s
    http_req_failed: ['rate<0.20'],     // Allow up to 20% errors
  },
}

// Test data
const BASE_URL = __ENV.BASE_URL || 'http://localhost:3000'
const USERS = [
  { username: 'admin', password: 'password' },
  { username: 'user1', password: 'password123' },
  { username: 'user2', password: 'password123' },
  { username: 'user3', password: 'password123' },
  { username: 'user4', password: 'password123' },
]

// Helper function to login
function login(user) {
  const payload = {
    username: user.username,
    password: user.password,
  }

  const startTime = Date.now()
  const response = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify(payload), {
    headers: { 'Content-Type': 'application/json' },
    timeout: '10s', // Add timeout for stress test
  })
  const endTime = Date.now()

  apiResponseTime.add(endTime - startTime)
  requestCount.add(1)

  const success = check(response, {
    'login status is 200': (r) => r.status === 200,
    'login returns token': (r) => r.json('token') !== undefined,
  })

  if (success) {
    successfulRequests.add(1)
    return { token: response.json('token'), user: user }
  } else {
    failedRequests.add(1)
    return { token: null, user: user }
  }
}

// Test 1: Stress test - Rapid requests
export default function () {
  const user = USERS[Math.floor(Math.random() * USERS.length)]

  try {
    // Try login without waiting (stress test)
    const auth = login(user)

    if (auth.token) {
      const headers = {
        'Authorization': `Bearer ${auth.token}`,
        'Content-Type': 'application/json',
      }

      // Stress test: rapid customer list requests
      const response = http.get(`${BASE_URL}/api/v1/customers?page=0&size=20`, {
        headers,
        timeout: '10s',
      })

      const success = check(response, {
        'customer list responds': (r) => r.status !== 0,
        'customer list status is OK': (r) => r.status < 500,
      })

      if (success) {
        successfulRequests.add(1)
      } else {
        failedRequests.add(1)
        errorRate.add(1)
      }
    } else {
      failedRequests.add(1)
      errorRate.add(1)
    }
  } catch (error) {
    failedRequests.add(1)
    errorRate.add(1)
    breakingPoint.add(1)
  }

  // No sleep - maximum stress
}

// Test 2: Stress test - Database operations
export function stressDatabaseOperations() {
  const user = USERS[Math.floor(Math.random() * USERS.length)]
  const auth = login(user)

  if (!auth.token) {
    failedRequests.add(1)
    return
  }

  const headers = {
    'Authorization': `Bearer ${auth.token}`,
    'Content-Type': 'application/json',
  }

  // Stress test: create customers rapidly
  for (let i = 0; i < 5; i++) {
    const payload = {
      firstName: `Stress${Date.now()}${i}`,
      lastName: 'User',
      email: `stress${Date.now()}${i}@example.com`,
    }

    const response = http.post(`${BASE_URL}/api/v1/customers`, JSON.stringify(payload), {
      headers,
      timeout: '5s',
    })

    const success = check(response, {
      'create customer responds': (r) => r.status < 500,
    })

    if (success) {
      successfulRequests.add(1)
    } else {
      failedRequests.add(1)
    }

    // No sleep between requests
  }
}

// Test 3: Stress test - Concurrent API calls
export function stressConcurrentAPIs() {
  const user = USERS[Math.floor(Math.random() * USERS.length)]
  const auth = login(user)

  if (!auth.token) {
    failedRequests.add(1)
    return
  }

  const headers = {
    'Authorization': `Bearer ${auth.token}`,
    'Content-Type': 'application/json',
  }

  // Make concurrent requests to different endpoints
  const endpoints = [
    `${BASE_URL}/api/v1/customers?page=0&size=20`,
    `${BASE_URL}/api/v1/orders?page=0&size=20`,
    `${BASE_URL}/api/v1/invoices?page=0&size=20`,
    `${BASE_URL}/api/v1/payments?page=0&size=20`,
    `${BASE_URL}/api/v1/subscriptions?page=0&size=20`,
  ]

  const responses = http.batch(endpoints, { headers, timeout: '10s' })

  for (const response of responses) {
    const success = check(response, {
      'endpoint responds': (r) => r.status < 500,
    })

    if (success) {
      successfulRequests.add(1)
    } else {
      failedRequests.add(1)
    }
  }
}

// Test 4: Stress test - Search operations
export function stressSearchOperations() {
  const user = USERS[Math.floor(Math.random() * USERS.length)]
  const auth = login(user)

  if (!auth.token) {
    failedRequests.add(1)
    return
  }

  const headers = {
    'Authorization': `Bearer ${auth.token}`,
    'Content-Type': 'application/json',
  }

  // Rapid search requests with different queries
  const queries = ['test', 'user', 'customer', 'order', 'invoice']

  for (const query of queries) {
    const response = http.get(`${BASE_URL}/api/v1/customers?search=${query}&page=0&size=20`, {
      headers,
      timeout: '5s',
    })

    check(response, {
      'search responds': (r) => r.status < 500,
    })

    if (response.status < 500) {
      successfulRequests.add(1)
    } else {
      failedRequests.add(1)
    }

    // No sleep
  }
}

// Test 5: Stress test - File uploads (simulated)
export function stressFileOperations() {
  const user = USERS[Math.floor(Math.random() * USERS.length)]
  const auth = login(user)

  if (!auth.token) {
    failedRequests.add(1)
    return
  }

  const headers = {
    'Authorization': `Bearer ${auth.token}`,
  }

  // Simulate file upload (just the request)
  const response = http.post(`${BASE_URL}/api/upload`, 'test data', {
    headers,
    timeout: '10s',
  })

  check(response, {
    'upload responds': (r) => r.status < 500,
  })

  if (response.status < 500) {
    successfulRequests.add(1)
  } else {
    failedRequests.add(1)
  }
}

// Test 6: Stress test - Memory and CPU intensive operations
export function stressResourceIntensive() {
  const user = USERS[Math.floor(Math.random() * USERS.length)]
  const auth = login(user)

  if (!auth.token) {
    failedRequests.add(1)
    return
  }

  const headers = {
    'Authorization': `Bearer ${auth.token}`,
    'Content-Type': 'application/json',
  }

  // Request large datasets
  const response = http.get(`${BASE_URL}/api/v1/customers?page=0&size=1000`, {
    headers,
    timeout: '15s',
  })

  const success = check(response, {
    'large dataset responds': (r) => r.status < 500,
  })

  if (success) {
    successfulRequests.add(1)
  } else {
    failedRequests.add(1)
  }
}

// Test 7: Stress test - Authentication stress
export function stressAuthentication() {
  // Rapid login attempts
  for (let i = 0; i < 3; i++) {
    const user = USERS[Math.floor(Math.random() * USERS.length)]
    const auth = login(user)

    if (auth.token) {
      successfulRequests.add(1)
    } else {
      failedRequests.add(1)
    }

    // No sleep
  }
}

// Test 8: Stress test - Error scenarios
export function stressErrorScenarios() {
  const headers = {
    'Content-Type': 'application/json',
  }

  // Send invalid requests
  const invalidPayloads = [
    {},
    { invalid: 'data' },
    { username: '', password: '' },
  ]

  for (const payload of invalidPayloads) {
    const response = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify(payload), {
      headers,
      timeout: '5s',
    })

    // For error scenarios, we expect non-2xx status
    const success = check(response, {
      'invalid request returns error': (r) => r.status >= 400,
    })

    if (success) {
      successfulRequests.add(1)
    } else {
      failedRequests.add(1)
    }
  }
}

// Test 9: Stress test - Long running operations
export function stressLongRunningOperations() {
  const user = USERS[Math.floor(Math.random() * USERS.length)]
  const auth = login(user)

  if (!auth.token) {
    failedRequests.add(1)
    return
  }

  const headers = {
    'Authorization': `Bearer ${auth.token}`,
  }

  // Request operations that might take longer
  const response = http.get(`${BASE_URL}/api/reports/comprehensive`, {
    headers,
    timeout: '30s',
  })

  const success = check(response, {
    'long running operation responds': (r) => r.status < 500,
  })

  if (success) {
    successfulRequests.add(1)
  } else {
    failedRequests.add(1)
  }
}

// Test 10: Stress test - Mixed workflows
export function stressMixedWorkflows() {
  const user = USERS[Math.floor(Math.random() * USERS.length)]
  const auth = login(user)

  if (!auth.token) {
    failedRequests.add(1)
    return
  }

  const headers = {
    'Authorization': `Bearer ${auth.token}`,
    'Content-Type': 'application/json',
  }

  // Simulate complete user workflow rapidly
  const workflow = [
    () => http.get(`${BASE_URL}/dashboard`, { headers }),
    () => http.get(`${BASE_URL}/api/v1/customers?page=0&size=20`, { headers }),
    () => http.get(`${BASE_URL}/api/v1/orders?page=0&size=20`, { headers }),
  ]

  for (const step of workflow) {
    const response = step()

    if (response.status < 500) {
      successfulRequests.add(1)
    } else {
      failedRequests.add(1)
    }
  }
}

// Setup function
export function setup() {
  console.log('Starting stress test...')
  console.log(`Base URL: ${BASE_URL}`)
  console.log('This test will push the system to its limits')
  console.log('Duration: approximately 5 minutes')
  return { baseUrl: BASE_URL }
}

// Teardown function
export function teardown(data) {
  console.log('Stress test completed!')
  console.log('\n=== STRESS TEST SUMMARY ===')
  console.log(`Total requests: ${requestCount.values}`)
  console.log(`Successful requests: ${successfulRequests.values}`)
  console.log(`Failed requests: ${failedRequests.values}`)
  console.log(`Error rate: ${(errorRate.values * 100).toFixed(2)}%`)
  console.log('\nReview the results to identify:')
  console.log('1. System breaking points')
  console.log('2. Performance degradation')
  console.log('3. Error patterns')
  console.log('4. Resource limits')
}
