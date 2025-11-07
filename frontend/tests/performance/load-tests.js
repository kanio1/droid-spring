/**
 * Load Testing Suite using k6
 *
 * This script performs load testing on the BSS application
 * to verify performance under normal and peak load conditions
 */

import { check, sleep } from 'k6'
import http from 'k6/http'
import { Rate, Trend, Counter } from 'k6/metrics'

// Custom metrics
const errorRate = new Rate('errors')
const loginSuccess = new Rate('login_success')
const pageLoadTime = new Trend('page_load_time')
const apiResponseTime = new Trend('api_response_time')
const requestCount = new Counter('request_count')

// Test configuration
export const options = {
  // Load test: ramp up to 50 VUs over 2 minutes, stay at 50 for 5 minutes
  stages: [
    { duration: '2m', target: 25 }, // Ramp-up
    { duration: '5m', target: 50 }, // Stay at peak
    { duration: '2m', target: 0 },  // Ramp-down
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'], // 95% of requests under 2s
    http_req_failed: ['rate<0.05'],     // Error rate under 5%
    errors: ['rate<0.1'],               // Custom error rate under 10%
  },
}

// Test data
const BASE_URL = __ENV.BASE_URL || 'http://localhost:3000'
const USERS = [
  { username: 'admin', password: 'password' },
  { username: 'user1', password: 'password123' },
  { username: 'user2', password: 'password123' },
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
  })
  const endTime = Date.now()

  apiResponseTime.add(endTime - startTime)
  requestCount.add(1)

  const success = check(response, {
    'login status is 200': (r) => r.status === 200,
    'login returns token': (r) => r.json('token') !== '',
  })

  loginSuccess.add(success)

  if (success) {
    return { token: response.json('token'), user: user }
  }

  return { token: null, user: user }
}

// Test 1: Load test - Customer operations
export default function () {
  const user = USERS[Math.floor(Math.random() * USERS.length)]
  const auth = login(user)

  if (!auth.token) {
    errorRate.add(1)
    return
  }

  const headers = {
    'Authorization': `Bearer ${auth.token}`,
    'Content-Type': 'application/json',
  }

  // Test 1.1: Load customer list
  {
    const startTime = Date.now()
    const response = http.get(`${BASE_URL}/api/v1/customers?page=0&size=20`, { headers })
    const endTime = Date.now()

    pageLoadTime.add(endTime - startTime)

    const success = check(response, {
      'customer list status is 200': (r) => r.status === 200,
      'customer list has data': (r) => r.json('data') !== undefined,
    })

    errorRate.add(!success)
    sleep(1)
  }

  // Test 1.2: Load customer details
  {
    const response = http.get(`${BASE_URL}/api/v1/customers/123e4567-e89b-12d3-a456-426614174000`, { headers })

    check(response, {
      'customer details status is 200 or 404': (r) => r.status === 200 || r.status === 404,
    })

    sleep(0.5)
  }

  // Test 1.3: Search customers
  {
    const response = http.get(`${BASE_URL}/api/v1/customers?search=test&page=0&size=20`, { headers })

    check(response, {
      'search customers status is 200': (r) => r.status === 200,
    })

    sleep(0.5)
  }

  // Test 1.4: Create customer
  {
    const payload = {
      firstName: `LoadTest${Date.now()}`,
      lastName: 'User',
      email: `load${Date.now()}@example.com`,
      phone: '+1-555-0123',
    }

    const response = http.post(`${BASE_URL}/api/v1/customers`, JSON.stringify(payload), { headers })

    check(response, {
      'create customer status is 201': (r) => r.status === 201,
    })

    sleep(1)
  }

  sleep(2)
}

// Test 2: Load test - Order operations
export function orderLoadTest() {
  const user = USERS[Math.floor(Math.random() * USERS.length)]
  const auth = login(user)

  if (!auth.token) {
    errorRate.add(1)
    return
  }

  const headers = {
    'Authorization': `Bearer ${auth.token}`,
    'Content-Type': 'application/json',
  }

  // Load order list
  const response = http.get(`${BASE_URL}/api/v1/orders?page=0&size=20`, { headers })

  check(response, {
    'order list status is 200': (r) => r.status === 200,
  })

  sleep(0.5)
}

// Test 3: Load test - Invoice operations
export function invoiceLoadTest() {
  const user = USERS[Math.floor(Math.random() * USERS.length)]
  const auth = login(user)

  if (!auth.token) {
    errorRate.add(1)
    return
  }

  const headers = {
    'Authorization': `Bearer ${auth.token}`,
    'Content-Type': 'application/json',
  }

  // Load invoice list
  const response = http.get(`${BASE_URL}/api/v1/invoices?page=0&size=20`, { headers })

  check(response, {
    'invoice list status is 200': (r) => r.status === 200,
  })

  sleep(0.5)
}

// Test 4: Load test - Payment operations
export function paymentLoadTest() {
  const user = USERS[Math.floor(Math.random() * USERS.length)]
  const auth = login(user)

  if (!auth.token) {
    errorRate.add(1)
    return
  }

  const headers = {
    'Authorization': `Bearer ${auth.token}`,
    'Content-Type': 'application/json',
  }

  // Load payment list
  const response = http.get(`${BASE_URL}/api/v1/payments?page=0&size=20`, { headers })

  check(response, {
    'payment list status is 200': (r) => r.status === 200,
  })

  sleep(0.5)
}

// Test 5: Load test - Subscription operations
export function subscriptionLoadTest() {
  const user = USERS[Math.floor(Math.random() * USERS.length)]
  const auth = login(user)

  if (!auth.token) {
    errorRate.add(1)
    return
  }

  const headers = {
    'Authorization': `Bearer ${auth.token}`,
    'Content-Type': 'application/json',
  }

  // Load subscription list
  const response = http.get(`${BASE_URL}/api/v1/subscriptions?page=0&size=20`, { headers })

  check(response, {
    'subscription list status is 200': (r) => r.status === 200,
  })

  sleep(0.5)
}

// Test 6: Load test - Mixed operations
export function mixedLoadTest() {
  const user = USERS[Math.floor(Math.random() * USERS.length)]
  const auth = login(user)

  if (!auth.token) {
    errorRate.add(1)
    return
  }

  const headers = {
    'Authorization': `Bearer ${auth.token}`,
    'Content-Type': 'application/json',
  }

  // Simulate user workflow: view dashboard, customers, orders
  const pages = [
    `${BASE_URL}/dashboard`,
    `${BASE_URL}/customers`,
    `${BASE_URL}/orders`,
    `${BASE_URL}/invoices`,
  ]

  for (const page of pages) {
    const startTime = Date.now()
    const response = http.get(page, { headers })
    const endTime = Date.now()

    pageLoadTime.add(endTime - startTime)

    check(response, {
      'page loads successfully': (r) => r.status === 200,
    })

    sleep(1)
  }
}

// Test 7: Load test - Concurrent user simulation
export function concurrentUsersTest() {
  // This test simulates multiple users doing different operations
  const operations = [
    'customer list',
    'order list',
    'invoice list',
    'payment list',
    'subscription list',
  ]

  const operation = operations[Math.floor(Math.random() * operations.length)]

  const user = USERS[Math.floor(Math.random() * USERS.length)]
  const auth = login(user)

  if (!auth.token) {
    errorRate.add(1)
    return
  }

  const headers = {
    'Authorization': `Bearer ${auth.token}`,
    'Content-Type': 'application/json',
  }

  const endpoints = {
    'customer list': `${BASE_URL}/api/v1/customers?page=0&size=20`,
    'order list': `${BASE_URL}/api/v1/orders?page=0&size=20`,
    'invoice list': `${BASE_URL}/api/v1/invoices?page=0&size=20`,
    'payment list': `${BASE_URL}/api/v1/payments?page=0&size=20`,
    'subscription list': `${BASE_URL}/api/v1/subscriptions?page=0&size=20`,
  }

  const response = http.get(endpoints[operation], { headers })

  check(response, {
    [`${operation} loads successfully`]: (r) => r.status === 200,
  })

  sleep(1)
}

// Setup function
export function setup() {
  console.log('Starting load test setup...')
  console.log(`Base URL: ${BASE_URL}`)
  console.log(`Test duration will be approximately 9 minutes`)
  return { baseUrl: BASE_URL }
}

// Teardown function
export function teardown(data) {
  console.log('Load test completed')
  console.log('Summary:')
  console.log('- Check thresholds for pass/fail criteria')
  console.log('- Review metrics for performance insights')
}
