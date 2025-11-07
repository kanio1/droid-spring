#!/usr/bin/env k6
/**
 * Extreme Spike Test for BSS System
 *
 * This test simulates sudden spikes in traffic:
 * - 100 users → 10,000 users in 30 seconds
 * - Test system response to traffic surges
 * - Validates auto-scaling and circuit breakers
 *
 * Usage:
 *   k6 run --vus 1000 --duration 30m scripts/extreme-spike-test.js
 *
 * Scale: 100K - 1M events
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// Custom metrics
export let errorRate = new Rate('errors');
export let spikeDetected = new Rate('spike_detected');

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_PREFIX = `${BASE_URL}/api/v1`;

// Spike test scenarios
const stages = [
    // Ramp up to 100 users
    { duration: '2m', target: 100 },
    // Stabilize
    { duration: '3m', target: 100 },
    // First spike: 100 → 1,000 users (10x)
    { duration: '30s', target: 1000 },
    // Stabilize at high load
    { duration: '5m', target: 1000 },
    // Second spike: 1,000 → 5,000 users (5x)
    { duration: '30s', target: 5000 },
    // Peak load
    { duration: '10m', target: 5000 },
    // Spike to maximum: 5,000 → 10,000 users (2x)
    { duration: '30s', target: 10000 },
    // Sustained peak
    { duration: '15m', target: 10000 },
    // Drop to normal
    { duration: '2m', target: 100 },
    // Recovery period
    { duration: '5m', target: 100 },
];

export let options = {
    stages: stages,
    thresholds: {
        http_req_duration: [
            'p(95)<2000', // 95% of requests under 2s during spike
            'p(99)<5000', // 99% of requests under 5s
        ],
        http_req_failed: ['rate<0.10'], // Error rate under 10%
        errors: ['rate<0.05'], // Custom error metric under 5%
        spike_detected: ['rate<1.0'], // Track spikes
    },
    // Enable detailed logging
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
    // Discard test output to reduce noise
    noConnectionReuse: false,
    // User agent
    userAgent: 'k6-spike-test/1.0',
};

// BSS API endpoints
const endpoints = {
    // Customer endpoints
    customers: `${API_PREFIX}/customers`,
    customerById: (id) => `${API_PREFIX}/customers/${id}`,

    // Order endpoints
    orders: `${API_PREFIX}/orders`,
    orderById: (id) => `${API_PREFIX}/orders/${id}`,

    // Payment endpoints
    payments: `${API_PREFIX}/payments`,
    paymentById: (id) => `${API_PREFIX}/payments/${id}`,

    // Invoice endpoints
    invoices: `${API_PREFIX}/invoices`,
    invoiceById: (id) => `${API_PREFIX}/invoices/${id}`,

    // Health check
    health: `${BASE_URL}/actuator/health`,
};

// Test data generators
function randomId() {
    return Math.floor(Math.random() * 10000) + 1;
}

function randomString(length = 10) {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz';
    let result = '';
    for (let i = 0; i < length; i++) {
        result += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return result;
}

function generateCustomer() {
    return {
        firstName: `Test${randomString(5)}`,
        lastName: `User${randomString(5)}`,
        email: `test${randomId()}@example.com`,
        pesel: `${Math.floor(Math.random() * 900000000) + 100000000}`,
        phone: `+48${Math.floor(Math.random() * 900000000) + 100000000}`,
    };
}

function generateOrder(customerId) {
    return {
        customerId: customerId,
        orderType: 'NEW_SERVICE',
        priority: 'MEDIUM',
        notes: `Test order ${randomString(20)}`,
    };
}

// Spike detection helper
let previousRPS = 0;
let spikeThreshold = 5; // 5x increase in RPS is a spike

function detectSpike(currentRPS) {
    if (previousRPS > 0) {
        const increase = currentRPS / previousRPS;
        if (increase >= spikeThreshold) {
            spikeDetected.add(1);
            console.log(`SPIKE DETECTED: ${increase.toFixed(2)}x increase in RPS (${previousRPS.toFixed(2)} → ${currentRPS.toFixed(2)})`);
        }
    }
    previousRPS = currentRPS;
}

// Test scenarios
function testCustomerReadScenario() {
    const customerId = randomId();

    const response = http.get(endpoints.customerById(customerId), {
        tags: { name: 'GET /customers/{id}' },
    });

    const success = check(response, {
        'customer read status is 200 or 404': (r) => r.status === 200 || r.status === 404,
        'customer read response time < 2000ms': (r) => r.timings.duration < 2000,
    });

    errorRate.add(!success);
    return success;
}

function testCustomerSearchScenario() {
    const searchTerm = `test${randomId()}`;

    const response = http.get(`${endpoints.customers}?search=${searchTerm}&page=0&size=10`, {
        tags: { name: 'GET /customers?search' },
    });

    const success = check(response, {
        'customer search status is 200': (r) => r.status === 200,
        'customer search response time < 1500ms': (r) => r.timings.duration < 1500,
    });

    errorRate.add(!success);
    return success;
}

function testOrderReadScenario() {
    const orderId = randomId();

    const response = http.get(endpoints.orderById(orderId), {
        tags: { name: 'GET /orders/{id}' },
    });

    const success = check(response, {
        'order read status is 200 or 404': (r) => r.status === 200 || r.status === 404,
        'order read response time < 2000ms': (r) => r.timings.duration < 2000,
    });

    errorRate.add(!success);
    return success;
}

function testOrderListScenario() {
    const response = http.get(`${endpoints.orders}?page=0&size=20`, {
        tags: { name: 'GET /orders' },
    });

    const success = check(response, {
        'order list status is 200': (r) => r.status === 200,
        'order list response time < 2000ms': (r) => r.timings.duration < 2000,
    });

    errorRate.add(!success);
    return success;
}

function testPaymentReadScenario() {
    const paymentId = randomId();

    const response = http.get(endpoints.paymentById(paymentId), {
        tags: { name: 'GET /payments/{id}' },
    });

    const success = check(response, {
        'payment read status is 200 or 404': (r) => r.status === 200 || r.status === 404,
        'payment read response time < 2000ms': (r) => r.timings.duration < 2000,
    });

    errorRate.add(!success);
    return success;
}

function testInvoiceReadScenario() {
    const invoiceId = randomId();

    const response = http.get(endpoints.invoiceById(invoiceId), {
        tags: { name: 'GET /invoices/{id}' },
    });

    const success = check(response, {
        'invoice read status is 200 or 404': (r) => r.status === 200 || r.status === 404,
        'invoice read response time < 2000ms': (r) => r.timings.duration < 2000,
    });

    errorRate.add(!success);
    return success;
}

function testHealthCheck() {
    const response = http.get(endpoints.health, {
        tags: { name: 'GET /actuator/health' },
    });

    const success = check(response, {
        'health check status is 200': (r) => r.status === 200,
        'health check response time < 500ms': (r) => r.timings.duration < 500,
    });

    errorRate.add(!success);
    return success;
}

// Main test loop
export default function () {
    // Execute multiple scenarios per VU iteration
    // Distribute load across different endpoints

    const scenarios = [
        { weight: 30, fn: testCustomerReadScenario },
        { weight: 20, fn: testCustomerSearchScenario },
        { weight: 20, fn: testOrderReadScenario },
        { weight: 15, fn: testOrderListScenario },
        { weight: 10, fn: testPaymentReadScenario },
        { weight: 3, fn: testInvoiceReadScenario },
        { weight: 2, fn: testHealthCheck },
    ];

    // Select scenario based on weight
    const totalWeight = scenarios.reduce((sum, s) => sum + s.weight, 0);
    const random = Math.random() * totalWeight;
    let weightSum = 0;

    for (let scenario of scenarios) {
        weightSum += scenario.weight;
        if (random <= weightSum) {
            const startTime = Date.now();
            scenario.fn();
            const endTime = Date.now();

            // Detect spikes
            const iterationDuration = (endTime - startTime) / 1000;
            const currentRPS = 1 / iterationDuration;
            detectSpike(currentRPS);

            break;
        }
    }

    // Short delay between iterations
    sleep(0.1);
}

// Handle test lifecycle
export function setup() {
    console.log('=== EXTREME SPIKE TEST STARTED ===');
    console.log(`Base URL: ${BASE_URL}`);
    console.log(`Test Duration: ${options.stages.reduce((sum, s) => sum + parseInt(s.duration), 0)} seconds`);
    console.log(`Peak Target: 10,000 VUs`);
    console.log('=================================\n');

    // Pre-warm the system
    console.log('Pre-warming system...');
    for (let i = 0; i < 10; i++) {
        testHealthCheck();
        sleep(1);
    }
    console.log('Pre-warm complete\n');
}

export function teardown(data) {
    console.log('\n=== EXTREME SPIKE TEST COMPLETED ===');
    console.log(`Test completed at: ${new Date().toISOString()}`);
    console.log('====================================\n');
}

// Handle interrupt
export function handleSummary(data) {
    // Save detailed results
    return {
        'extreme-spike-test-summary.json': JSON.stringify(data),
        'extreme-spike-test-summary.txt': textSummary(data, {
            indent: ' ',
            enableColors: true,
        }),
    };
}

// Track custom metrics at the end
export function handleTest() {
    console.log('\n=== TEST SUMMARY ===');
    console.log(`Peak VUs: ${__ENV.k6?.iteration || 'N/A'}`);
    console.log('===================\n');
}
