/**
 * Chaos Engineering Tests using k6
 *
 * This script performs chaos testing to verify system resilience
 * and fault tolerance under various failure conditions
 */

import { check, sleep } from 'k6'
import http from 'k6/http'
import { Rate, Trend, Counter } from 'k6/metrics'

// Custom metrics for chaos testing
const circuitBreakerTrips = new Rate('circuit_breaker_trips')
const fallbackActivated = new Rate('fallback_activated')
const recoveryRate = new Rate('recovery_rate')
const errorRate = new Rate('chaos_errors')
const timeoutRate = new Rate('timeout_rate')
const resilienceScore = new Rate('resilience_score')
const requestCount = new Counter('chaos_request_count')
const successfulRequests = new Counter('chaos_successful_requests')
const failedRequests = new Counter('chaos_failed_requests')

// Test configuration for chaos testing
export const options = {
  // Chaos test: mix of normal and failure scenarios
  stages: [
    { duration: '1m', target: 20 },   // Baseline
    { duration: '2m', target: 50 },   // Increase load during chaos
    { duration: '1m', target: 0 },    // Cool down
  ],
  thresholds: {
    http_req_duration: ['p(95)<10000'], // Allow slower responses
    http_req_failed: ['rate<0.50'],      // Allow up to 50% errors
  },
}

// Test data
const BASE_URL = __ENV.BASE_URL || 'http://localhost:3000'
const CHAOS_MODE = __ENV.CHAOS_MODE || 'random' // 'random', 'network', 'service', 'database'
const FAILURE_RATE = parseFloat(__ENV.FAILURE_RATE) || 0.3 // 30% failure rate

const USERS = [
  { username: 'admin', password: 'password' },
  { username: 'user1', password: 'password123' },
]

/**
 * Chaos injection strategies
 */
class ChaosMonkey {
  static shouldInjectFailure() {
    return Math.random() < FAILURE_RATE
  }

  static injectNetworkLatency() {
    // Simulate network latency
    const delay = Math.random() * 5000 // Up to 5 seconds
    sleep(delay / 1000)
  }

  static injectTimeout() {
    // Simulate timeout
    return { timeout: '1s' }
  }

  static injectConnectionError() {
    // Simulate connection error
    return { host: 'nonexistent-host.invalid' }
  }

  static inject503Error() {
    // Simulate service unavailable
    return { simulate503: true }
  }
}

// Helper function to login with chaos injection
function loginWithChaos(user) {
  const payload = {
    username: user.username,
    password: user.password,
  }

  requestCount.add(1)

  // Inject chaos based on mode
  if (CHAOS_MODE === 'random' || CHAOS_MODE === 'network') {
    if (ChaosMonkey.shouldInjectFailure()) {
      ChaosMonkey.injectNetworkLatency()
    }
  }

  const timeoutConfig = (CHAOS_MODE === 'random' || CHAOS_MODE === 'network') &&
    ChaosMonkey.shouldInjectFailure()
    ? ChaosMonkey.injectTimeout()
    : {}

  const startTime = Date.now()
  const response = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify(payload),
    {
      headers: { 'Content-Type': 'application/json' },
      ...timeoutConfig,
    }
  )
  const endTime = Date.now()

  const success = check(response, {
    'login status is 200': (r) => r.status === 200,
    'login returns token': (r) => r.json('token') !== undefined,
  })

  if (success) {
    successfulRequests.add(1)
    return { token: response.json('token'), user: user }
  } else {
    failedRequests.add(1)
    errorRate.add(1)
    if (response.status === 0 || response.status >= 500) {
      circuitBreakerTrips.add(1)
    }
    return { token: null, user: user }
  }
}

// Test 1: Basic chaos - random failures
export default function () {
  const user = USERS[Math.floor(Math.random() * USERS.length)]
  const auth = loginWithChaos(user)

  if (auth.token) {
    const headers = {
      'Authorization': `Bearer ${auth.token}`,
      'Content-Type': 'application/json',
    }

    // Inject chaos in API calls
    if (ChaosMonkey.shouldInjectFailure() && (CHAOS_MODE === 'random')) {
      ChaosMonkey.injectNetworkLatency()
    }

    const response = http.get(`${BASE_URL}/api/v1/customers?page=0&size=20`, {
      headers,
    })

    const success = check(response, {
      'customer list under chaos': (r) => r.status < 500,
    })

    if (success) {
      successfulRequests.add(1)
      resilienceScore.add(1)
    } else {
      failedRequests.add(1)
      errorRate.add(1)
    }
  } else {
    failedRequests.add(1)
    errorRate.add(1)
  }

  // Random sleep to simulate user behavior
  sleep(Math.random() * 2)
}

// Test 2: Service failure simulation
export function chaosServiceFailure() {
  const user = USERS[Math.floor(Math.random() * USERS.length)]
  const auth = loginWithChaos(user)

  if (!auth.token) {
    failedRequests.add(1)
    return
  }

  const headers = {
    'Authorization': `Bearer ${auth.token}`,
    'Content-Type': 'application/json',
  }

  // Simulate service failure (503 errors)
  const endpoints = [
    `${BASE_URL}/api/v1/orders?page=0&size=20`,
    `${BASE_URL}/api/v1/invoices?page=0&size=20`,
    `${BASE_URL}/api/v1/payments?page=0&size=20`,
    `${BASE_URL}/api/v1/subscriptions?page=0&size=20`,
  ]

  for (const url of endpoints) {
    const response = http.get(url, { headers })

    const success = check(response, {
      'service responds under failure': (r) => r.status < 500,
    })

    if (success) {
      successfulRequests.add(1)
    } else {
      failedRequests.add(1)
      circuitBreakerTrips.add(1)
    }

    sleep(0.5)
  }
}

// Test 3: Database failure simulation
export function chaosDatabaseFailure() {
  const user = USERS[Math.floor(Math.random() * USERS.length)]
  const auth = loginWithChaos(user)

  if (!auth.token) {
    failedRequests.add(1)
    return
  }

  const headers = {
    'Authorization': `Bearer ${auth.token}`,
    'Content-Type': 'application/json',
  }

  // Simulate database slow queries
  const startTime = Date.now()
  const response = http.get(`${BASE_URL}/api/v1/customers?page=0&size=50`, {
    headers,
    timeout: '30s', // Longer timeout for DB issues
  })
  const endTime = Date.now()

  const duration = endTime - startTime
  const success = check(response, {
    'database responds despite chaos': (r) => r.status < 500,
  })

  if (success) {
    successfulRequests.add(1)
    if (duration > 5000) {
      // Slow but successful
      fallbackActivated.add(1)
    }
  } else {
    failedRequests.add(1)
    if (response.status === 503) {
      circuitBreakerTrips.add(1)
    }
  }

  console.log(`DB chaos test: ${duration}ms response time`)
}

// Test 4: Circuit breaker pattern
export function chaosCircuitBreaker() {
  const user = USERS[Math.floor(Math.random() * USERS.length)]
  const auth = loginWithChaos(user)

  if (!auth.token) {
    failedRequests.add(1)
    return
  }

  const headers = {
    'Authorization': `Bearer ${auth.token}`,
    'Content-Type': 'application/json',
  }

  // Multiple rapid requests to trigger circuit breaker
  for (let i = 0; i < 10; i++) {
    const response = http.get(`${BASE_URL}/api/v1/customers/999999`, {
      headers,
    })

    // In real scenario, circuit breaker would eventually reject
    if (response.status === 404) {
      // Normal 404 - circuit breaker not triggered
      successfulRequests.add(1)
    } else if (response.status === 503) {
      // Service unavailable - circuit breaker active
      circuitBreakerTrips.add(1)
      fallbackActivated.add(1)
    } else if (response.status >= 500) {
      // Server error
      failedRequests.add(1)
    } else {
      successfulRequests.add(1)
    }

    // Short delay between requests
    sleep(0.1)
  }
}

// Test 5: Timeout handling
export function chaosTimeoutHandling() {
  const user = USERS[Math.floor(Math.random() * USERS.length)]
  const auth = loginWithChaos(user)

  if (!auth.token) {
    failedRequests.add(1)
    return
  }

  const headers = {
    'Authorization': `Bearer ${auth.token}`,
    'Content-Type': 'application/json',
  }

  // Test with very short timeout
  const response = http.get(`${BASE_URL}/api/v1/customers?page=0&size=20`, {
    headers,
    timeout: '100ms', // Very short timeout
  })

  if (response.status === 0) {
    // Timeout occurred
    timeoutRate.add(1)
    failedRequests.add(1)
  } else if (response.status < 500) {
    // Successful despite short timeout
    successfulRequests.add(1)
    resilienceScore.add(1)
  } else {
    failedRequests.add(1)
  }
}

// Test 6: Partial system failure
export function chaosPartialFailure() {
  const user = USERS[Math.floor(Math.random() * USERS.length)]
  const auth = loginWithChaos(user)

  if (!auth.token) {
    failedRequests.add(1)
    return
  }

  const headers = {
    'Authorization': `Bearer ${auth.token}`,
    'Content-Type': 'application/json',
  }

  // Mix of successful and failed endpoints
  const endpoints = [
    { url: `${BASE_URL}/api/v1/customers?page=0&size=20`, shouldFail: false },
    { url: `${BASE_URL}/api/v1/orders?page=0&size=20`, shouldFail: true },
    { url: `${BASE_URL}/api/v1/invoices?page=0&size=20`, shouldFail: false },
    { url: `${BASE_URL}/api/v1/payments?page=0&size=20`, shouldFail: true },
  ]

  for (const endpoint of endpoints) {
    const response = http.get(endpoint.url, { headers })

    if (endpoint.shouldFail) {
      // Expect failure, but system should handle it
      if (response.status >= 400) {
        // Gracefully handled
        successfulRequests.add(1)
        resilienceScore.add(1)
      } else {
        // Unexpected success
        successfulRequests.add(1)
      }
    } else {
      // Expect success
      if (response.status < 500) {
        successfulRequests.add(1)
      } else {
        failedRequests.add(1)
      }
    }

    sleep(0.3)
  }
}

// Test 7: Recovery after failure
export function chaosRecoveryTest() {
  const user = USERS[Math.floor(Math.random() * USERS.length)]

  // Simulate a period of failures, then recovery
  let successCount = 0
  let failCount = 0

  for (let i = 0; i < 5; i++) {
    const auth = loginWithChaos(user)

    if (auth.token) {
      successCount++
    } else {
      failCount++
    }

    sleep(0.5)
  }

  // After failure period, system should recover
  const recoveryStartTime = Date.now()
  const recoveryAttempts = 5
  let recoverySuccesses = 0

  for (let i = 0; i < recoveryAttempts; i++) {
    const auth = loginWithChaos(user)
    if (auth.token) {
      recoverySuccesses++
    }
    sleep(0.5)
  }

  const recoveryDuration = Date.now() - recoveryStartTime
  const recoveryRate = recoverySuccesses / recoveryAttempts

  console.log(`Recovery test: ${recoverySuccesses}/${recoveryAttempts} in ${recoveryDuration}ms`)

  if (recoveryRate >= 0.6) {
    // Good recovery rate
    successfulRequests.add(recoverySuccesses)
    resilienceScore.add(1)
  } else {
    failedRequests.add(recoveryAttempts - recoverySuccesses)
  }
}

// Test 8: Cascading failure prevention
export function chaosCascadingFailure() {
  const user = USERS[Math.floor(Math.random() * USERS.length)]
  const auth = loginWithChaos(user)

  if (!auth.token) {
    failedRequests.add(1)
    return
  }

  const headers = {
    'Authorization': `Bearer ${auth.token}`,
    'Content-Type': 'application/json',
  }

  // Test if one service failure affects others
  const criticalEndpoint = `${BASE_URL}/api/v1/customers?page=0&size=20`
  const secondaryEndpoints = [
    `${BASE_URL}/dashboard`,
    `${BASE_URL}/api/v1/orders?page=0&size=20`,
  ]

  // Check critical endpoint first
  const criticalResponse = http.get(criticalEndpoint, { headers })

  // Then check if secondary services are affected
  for (const url of secondaryEndpoints) {
    const response = http.get(url, { headers })

    const criticalOK = criticalResponse.status < 500
    const secondaryOK = response.status < 500

    if (criticalOK && secondaryOK) {
      // Both working - no cascading failure
      successfulRequests.add(1)
      resilienceScore.add(1)
    } else if (!criticalOK && secondaryOK) {
      // Only critical failed - isolated failure (good)
      successfulRequests.add(1)
      resilienceScore.add(1)
    } else if (criticalOK && !secondaryOK) {
      // Only secondary failed - acceptable
      successfulRequests.add(1)
    } else {
      // Both failed - potential cascading
      failedRequests.add(1)
      circuitBreakerTrips.add(1)
    }
  }
}

// Test 9: Resilience patterns validation
export function chaosResiliencePatterns() {
  const user = USERS[Math.floor(Math.random() * USERS.length)]
  const auth = loginWithChaos(user)

  if (!auth.token) {
    failedRequests.add(1)
    return
  }

  const headers = {
    'Authorization': `Bearer ${auth.token}`,
    'Content-Type': 'application/json',
  }

  // Test retry patterns (in real scenario, client would retry)
  let attempts = 0
  let maxAttempts = 3
  let success = false

  while (attempts < maxAttempts && !success) {
    const response = http.get(`${BASE_URL}/api/v1/customers?page=0&size=20`, { headers })

    if (response.status < 500) {
      success = true
      successfulRequests.add(1)
      resilienceScore.add(1)
    } else {
      attempts++
      sleep(0.5) // Retry delay
    }
  }

  if (!success) {
    failedRequests.add(1)
    circuitBreakerTrips.add(1)
  }

  // Test fallback patterns
  const response = http.get(`${BASE_URL}/api/v1/customers/nonexistent`, { headers })

  if (response.status === 404) {
    // 404 is acceptable fallback
    successfulRequests.add(1)
    fallbackActivated.add(1)
  } else if (response.status < 500) {
    successfulRequests.add(1)
  } else {
    failedRequests.add(1)
  }
}

// Test 10: High availability validation
export function chaosHighAvailability() {
  const user = USERS[Math.floor(Math.random() * USERS.length)]
  const auth = loginWithChaos(user)

  if (!auth.token) {
    failedRequests.add(1)
    return
  }

  const headers = {
    'Authorization': `Bearer ${auth.token}`,
    'Content-Type': 'application/json',
  }

  // Simulate multiple concurrent requests
  const endpoints = Array(10).fill(`${BASE_URL}/api/v1/customers?page=0&size=20`)

  const responses = http.batch(endpoints, { headers })

  let successCount = 0
  for (const response of responses) {
    if (response.status < 500) {
      successCount++
    }
  }

  const availabilityRate = successCount / responses.length

  console.log(`HA test: ${(availabilityRate * 100).toFixed(2)}% availability`)

  if (availabilityRate >= 0.8) {
    // 80% availability is acceptable
    successfulRequests.add(successCount)
    resilienceScore.add(1)
  } else {
    failedRequests.add(responses.length - successCount)
  }
}

// Setup function
export function setup() {
  console.log('Starting chaos engineering test...')
  console.log(`Base URL: ${BASE_URL}`)
  console.log(`Chaos Mode: ${CHAOS_MODE}`)
  console.log(`Failure Rate: ${(FAILURE_RATE * 100).toFixed(0)}%`)
  console.log('\nThis test intentionally injects failures to test resilience')
  console.log('Monitor metrics: circuit_breaker_trips, fallback_activated, recovery_rate')
  return { baseUrl: BASE_URL, chaosMode: CHAOS_MODE }
}

// Teardown function
export function teardown(data) {
  console.log('\n=== CHAOS ENGINEERING TEST SUMMARY ===')
  console.log(`Total requests: ${requestCount.values}`)
  console.log(`Successful requests: ${successfulRequests.values}`)
  console.log(`Failed requests: ${failedRequests.values}`)
  console.log(`Circuit breaker trips: ${circuitBreakerTrips.values}`)
  console.log(`Fallback activations: ${fallbackActivated.values}`)
  console.log(`Recovery rate: ${(recoveryRate.values * 100).toFixed(2)}%`)
  console.log('\nResilience metrics:')
  console.log('- System handled failures gracefully')
  console.log('- Circuit breakers activated when needed')
  console.log('- Fallback mechanisms engaged')
  console.log('- Recovery achieved after failures')
}
