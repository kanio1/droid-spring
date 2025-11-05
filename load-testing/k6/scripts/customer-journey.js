/**
 * Customer Journey Load Test
 *
 * Simulates complete customer lifecycle:
 * 1. Register/Login
 * 2. Create Order
 * 3. View Invoice
 * 4. Make Payment
 * 5. Subscribe to Service
 *
 * Usage:
 * k6 run --vus 10 --duration 5m customer-journey.js
 */

import { check, sleep } from 'k6'
import http from 'k6/http'
import { Rate } from 'k6/metrics'

// Custom metrics
const errorRate = new Rate('errors')

// Test configuration
export const options = {
  stages: [
    { duration: '2m', target: 10 }, // Ramp up
    { duration: '5m', target: 10 }, // Stay at 10 users
    { duration: '2m', target: 50 }, // Ramp up to 50 users
    { duration: '5m', target: 50 }, // Stay at 50 users
    { duration: '2m', target: 0 },  // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% of requests must complete below 500ms
    http_req_failed: ['rate<0.1'],    // Error rate must be less than 10%
    errors: ['rate<0.1'],
  },
}

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080'
const API_BASE = `${BASE_URL}/api`

// Test data
const TEST_USERS = [
  { email: 'test1@example.com', password: 'password123' },
  { email: 'test2@example.com', password: 'password123' },
  { email: 'test3@example.com', password: 'password123' },
]

// Get random test user
function getRandomUser() {
  return TEST_USERS[Math.floor(Math.random() * TEST_USERS.length)]
}

// Authentication
function login(email, password) {
  const payload = JSON.stringify({
    email: email,
    password: password,
  })

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  }

  const response = http.post(`${API_BASE}/auth/login`, payload, params)

  const success = check(response, {
    'login successful': (r) => r.status === 200,
    'token received': (r) => r.json('token') !== '',
  })

  errorRate.add(!success)

  if (!success) {
    return null
  }

  return response.json('token')
}

// Create customer
function createCustomer(token) {
  const payload = JSON.stringify({
    email: `customer_${Date.now()}@example.com`,
    firstName: 'LoadTest',
    lastName: 'User',
    phone: '+1234567890',
  })

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
  }

  const response = http.post(`${API_BASE}/customers`, payload, params)

  check(response, {
    'customer created': (r) => r.status === 201,
    'customer ID received': (r) => r.json('id') !== '',
  })

  return response.json('id')
}

// Create order
function createOrder(customerId, token) {
  const payload = JSON.stringify({
    customerId: customerId,
    items: [
      {
        productId: 'prod-1',
        quantity: 1,
        unitPrice: 99.99,
      },
    ],
  })

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
  }

  const response = http.post(`${API_BASE}/orders`, payload, params)

  check(response, {
    'order created': (r) => r.status === 201,
    'order ID received': (r) => r.json('id') !== '',
  })

  return response.json('id')
}

// View invoice
function viewInvoice(invoiceId, token) {
  const params = {
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  }

  const response = http.get(`${API_BASE}/invoices/${invoiceId}`, params)

  check(response, {
    'invoice retrieved': (r) => r.status === 200,
    'invoice has data': (r) => r.json('id') === invoiceId,
  })

  return response
}

// Make payment
function makePayment(invoiceId, token) {
  const payload = JSON.stringify({
    invoiceId: invoiceId,
    amount: 99.99,
    method: 'credit_card',
    methodData: {
      type: 'card',
      lastFourDigits: '4242',
    },
  })

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
  }

  const response = http.post(`${API_BASE}/payments`, payload, params)

  check(response, {
    'payment created': (r) => r.status === 201,
    'payment processing': (r) => r.json('status') === 'processing',
  })

  return response.json('id')
}

// Subscribe to service
function subscribeToService(customerId, token) {
  const payload = JSON.stringify({
    customerId: customerId,
    planId: 'plan-basic',
    billingCycle: 'monthly',
  })

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
  }

  const response = http.post(`${API_BASE}/subscriptions`, payload, params)

  check(response, {
    'subscription created': (r) => r.status === 201,
    'subscription active': (r) => r.json('status') === 'active',
  })

  return response.json('id')
}

// Main test function
export default function () {
  const user = getRandomUser()

  // Step 1: Login
  const token = login(user.email, user.password)

  if (!token) {
    errorRate.add(1)
    return
  }

  sleep(1)

  // Step 2: Create Customer
  const customerId = createCustomer(token)

  if (!customerId) {
    errorRate.add(1)
    return
  }

  sleep(1)

  // Step 3: Create Order
  const orderId = createOrder(customerId, token)

  if (!orderId) {
    errorRate.add(1)
    return
  }

  sleep(1)

  // Step 4: View Invoice (simulate invoice generation)
  // In real scenario, invoice would be created automatically
  const invoiceId = `inv-${Date.now()}`

  viewInvoice(invoiceId, token)

  sleep(1)

  // Step 5: Make Payment
  const paymentId = makePayment(invoiceId, token)

  if (!paymentId) {
    errorRate.add(1)
    return
  }

  sleep(1)

  // Step 6: Subscribe to Service
  const subscriptionId = subscribeToService(customerId, token)

  if (!subscriptionId) {
    errorRate.add(1)
    return
  }

  sleep(2)

  // Simulate user think time
  sleep(Math.random() * 3)
}
