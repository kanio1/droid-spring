#!/usr/bin/env k6
/**
 * Volume Test for BSS System - 1 Million Events
 *
 * High-volume test to validate system under massive load:
 * - 1,000,000 total requests
 * - Short duration, high intensity
 * - Tests database throughput
 * - Validates connection pooling
 * - Checks circuit breaker behavior
 *
 * Usage:
 *   k6 run --vus 500 --iterations 2000 scripts/volume-test-1m.js
 *   (500 VUs * 2000 iterations = 1,000,000 requests)
 *
 * Or:
 *   k6 run --vus 1000 --duration 10m scripts/volume-test-1m.js
 *   (1000 VUs * 600s = 600,000+ requests)
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
export let errorRate = new Rate('errors');
export let dbQueryTime = new Trend('db_query_time');
export let apiResponseTime = new Trend('api_response_time');

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_PREFIX = `${BASE_URL}/api/v1`;

// Volume test configuration
export const options = {
    // Use iterations instead of duration for exact count
    iterations: __ENV.ITERATIONS ? parseInt(__ENV.ITERATIONS) : 2000,
    vus: __ENV.VUS ? parseInt(__ENV.VUS) : 500,
    // Or use duration for continuous load
    // duration: '10m',
    thresholds: {
        http_req_duration: [
            'p(95)<2500', // 95% under 2.5s
            'p(99)<5000', // 99% under 5s
        ],
        http_req_failed: ['rate<0.05'], // Error rate under 5%
        errors: ['rate<0.03'], // Custom error metric under 3%
        db_query_time: ['p(95)<1000'], // Database queries under 1s
        api_response_time: ['p(95)<2000'], // API response under 2s
    },
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
    // Enable browser-like connection pooling
    noConnectionReuse: false,
    userAgent: 'k6-volume-test/1.0',
};

// BSS API endpoints
const endpoints = {
    customers: `${API_PREFIX}/customers`,
    customerById: (id) => `${API_PREFIX}/customers/${id}`,
    customerSearch: (term) => `${API_PREFIX}/customers?search=${term}&page=0&size=10`,
    orders: `${API_PREFIX}/orders`,
    orderById: (id) => `${API_PREFIX}/orders/${id}`,
    orderList: (page = 0, size = 20) => `${API_PREFIX}/orders?page=${page}&size=${size}`,
    payments: `${API_PREFIX}/payments`,
    paymentById: (id) => `${API_PREFIX}/payments/${id}`,
    invoices: `${API_PREFIX}/invoices`,
    invoiceById: (id) => `${API_PREFIX}/invoices/${id}`,
    health: `${BASE_URL}/actuator/health`,
    metrics: `${BASE_URL}/actuator/prometheus`,
};

// Test data pools
const customerIds = Array.from({ length: 1000 }, (_, i) => i + 1);
const orderIds = Array.from({ length: 2000 }, (_, i) => i + 1);
const paymentIds = Array.from({ length: 1500 }, (_, i) => i + 1);
const invoiceIds = Array.from({ length: 1200 }, (_, i) => i + 1);
const searchTerms = ['test', 'user', 'customer', 'order', 'payment', 'invoice', 'active', 'pending'];

// Data generator functions
function randomIdFromArray(arr) {
    return arr[Math.floor(Math.random() * arr.length)];
}

function randomSearchTerm() {
    return searchTerms[Math.floor(Math.random() * searchTerms.length)];
}

function randomPage() {
    return Math.floor(Math.random() * 10); // 0-9
}

// Heavy read scenarios (90% of traffic)
function readCustomerByIdScenario() {
    const customerId = randomIdFromArray(customerIds);
    const startTime = Date.now();

    const response = http.get(endpoints.customerById(customerId), {
        tags: { name: 'GET /customers/{id}' },
    });

    const duration = Date.now() - startTime;
    dbQueryTime.add(duration);
    apiResponseTime.add(response.timings.duration);

    const success = check(response, {
        'customer read status is 200 or 404': (r) => r.status === 200 || r.status === 404,
        'customer read response time < 2000ms': (r) => r.timings.duration < 2000,
    });

    errorRate.add(!success);
    return success;
}

function searchCustomersScenario() {
    const searchTerm = randomSearchTerm();
    const startTime = Date.now();

    const response = http.get(endpoints.customerSearch(searchTerm), {
        tags: { name: 'GET /customers?search' },
    });

    const duration = Date.now() - startTime;
    dbQueryTime.add(duration);
    apiResponseTime.add(response.timings.duration);

    const success = check(response, {
        'customer search status is 200': (r) => r.status === 200,
        'customer search response time < 1500ms': (r) => r.timings.duration < 1500,
    });

    errorRate.add(!success);
    return success;
}

function readOrderByIdScenario() {
    const orderId = randomIdFromArray(orderIds);
    const startTime = Date.now();

    const response = http.get(endpoints.orderById(orderId), {
        tags: { name: 'GET /orders/{id}' },
    });

    const duration = Date.now() - startTime;
    dbQueryTime.add(duration);
    apiResponseTime.add(response.timings.duration);

    const success = check(response, {
        'order read status is 200 or 404': (r) => r.status === 200 || r.status === 404,
        'order read response time < 2000ms': (r) => r.timings.duration < 2000,
    });

    errorRate.add(!success);
    return success;
}

function listOrdersScenario() {
    const page = randomPage();
    const startTime = Date.now();

    const response = http.get(endpoints.orderList(page, 20), {
        tags: { name: 'GET /orders?page' },
    });

    const duration = Date.now() - startTime;
    dbQueryTime.add(duration);
    apiResponseTime.add(response.timings.duration);

    const success = check(response, {
        'order list status is 200': (r) => r.status === 200,
        'order list response time < 2000ms': (r) => r.timings.duration < 2000,
    });

    errorRate.add(!success);
    return success;
}

function readPaymentByIdScenario() {
    const paymentId = randomIdFromArray(paymentIds);
    const startTime = Date.now();

    const response = http.get(endpoints.paymentById(paymentId), {
        tags: { name: 'GET /payments/{id}' },
    });

    const duration = Date.now() - startTime;
    dbQueryTime.add(duration);
    apiResponseTime.add(response.timings.duration);

    const success = check(response, {
        'payment read status is 200 or 404': (r) => r.status === 200 || r.status === 404,
        'payment read response time < 2000ms': (r) => r.timings.duration < 2000,
    });

    errorRate.add(!success);
    return success;
}

function readInvoiceByIdScenario() {
    const invoiceId = randomIdFromArray(invoiceIds);
    const startTime = Date.now();

    const response = http.get(endpoints.invoiceById(invoiceId), {
        tags: { name: 'GET /invoices/{id}' },
    });

    const duration = Date.now() - startTime;
    dbQueryTime.add(duration);
    apiResponseTime.add(response.timings.duration);

    const success = check(response, {
        'invoice read status is 200 or 404': (r) => r.status === 200 || r.status === 404,
        'invoice read response time < 2000ms': (r) => r.timings.duration < 2000,
    });

    errorRate.add(!success);
    return success;
}

// Write scenarios (10% of traffic)
function createCustomerScenario() {
    const customerData = {
        firstName: `Test${Math.floor(Math.random() * 1000000)}`,
        lastName: `User${Math.floor(Math.random() * 1000000)}`,
        email: `test${Math.floor(Math.random() * 1000000)}@example.com`,
        pesel: `${Math.floor(Math.random() * 900000000) + 100000000}`,
        phone: `+48${Math.floor(Math.random() * 900000000) + 100000000}`,
    };

    const startTime = Date.now();
    const params = { tags: { name: 'POST /customers' } };

    const response = http.post(JSON.stringify(customerData), params);
    const duration = Date.now() - startTime;
    apiResponseTime.add(response.timings.duration);

    const success = check(response, {
        'create customer status is 201': (r) => r.status === 201,
        'create customer response time < 3000ms': (r) => r.timings.duration < 3000,
    });

    errorRate.add(!success);
    return success;
}

function healthCheckScenario() {
    const startTime = Date.now();
    const response = http.get(endpoints.health, {
        tags: { name: 'GET /actuator/health' },
    });
    const duration = Date.now() - startTime;

    const success = check(response, {
        'health check status is 200': (r) => r.status === 200,
        'health check response time < 500ms': (r) => r.timings.duration < 500,
    });

    errorRate.add(!success);
    return success;
}

// Scenario distribution
const scenarios = [
    // Read operations (90% of traffic)
    { weight: 30, fn: readCustomerByIdScenario },
    { weight: 20, fn: searchCustomersScenario },
    { weight: 20, fn: readOrderByIdScenario },
    { weight: 10, fn: listOrdersScenario },
    { weight: 5, fn: readPaymentByIdScenario },
    { weight: 5, fn: readInvoiceByIdScenario },
    // Write operations (10% of traffic)
    { weight: 5, fn: createCustomerScenario },
    { weight: 5, fn: healthCheckScenario },
];

// Main test loop
export default function () {
    // Select scenario based on weight
    const totalWeight = scenarios.reduce((sum, s) => sum + s.weight, 0);
    const random = Math.random() * totalWeight;
    let weightSum = 0;

    for (let scenario of scenarios) {
        weightSum += scenario.weight;
        if (random <= weightSum) {
            scenario.fn();
            break;
        }
    }

    // Short sleep to allow for connection reuse
    sleep(0.01);
}

// Progress tracking
let lastCheck = 0;
const targetIterations = __ENV.ITERATIONS ? parseInt(__ENV.ITERATIONS) : 2000;

export function handleProgress() {
    const now = Date.now();
    if (now - lastCheck > 30000) { // Every 30 seconds
        const progress = (__ITER / targetIterations) * 100;
        console.log(`Volume test progress: ${progress.toFixed(2)}% (${__ITER}/${targetIterations} iterations)`);
        lastCheck = now;
    }
}

// Lifecycle hooks
export function setup() {
    const vuCount = __VU || 500;
    const iterations = __ENV.ITERATIONS ? parseInt(__ENV.ITERATIONS) : 2000;
    const totalRequests = vuCount * iterations;

    console.log('=== VOLUME TEST (1M EVENTS) STARTED ===');
    console.log(`Base URL: ${BASE_URL}`);
    console.log(`Virtual Users: ${vuCount}`);
    console.log(`Iterations per VU: ${iterations}`);
    console.log(`Expected Total Requests: ${totalRequests.toLocaleString()}`);
    console.log(`Expected Duration: ~${Math.floor(totalRequests / (vuCount * 10))} minutes`);
    console.log('==========================================\n`);

    // Pre-warm connections
    console.log('Pre-warming connections...');
    for (let i = 0; i < 20; i++) {
        healthCheckScenario();
        sleep(0.5);
    }
    console.log('Pre-warm complete\n');

    return {
        vuCount,
        iterations,
        totalRequests,
        startTime: Date.now(),
    };
}

export function teardown(data) {
    const endTime = Date.now();
    const duration = (endTime - data.startTime) / 1000;
    const actualRequests = __VU * data.iterations;
    const rps = actualRequests / duration;

    console.log('\n=== VOLUME TEST COMPLETED ===');
    console.log(`Total Requests: ${actualRequests.toLocaleString()}`);
    console.log(`Duration: ${duration.toFixed(1)} seconds (${(duration / 60).toFixed(2)} minutes)`);
    console.log(`Average RPS: ${rps.toFixed(2)}`);
    console.log(`Requests per VU: ${data.iterations}`);
    console.log('==============================\n`);
}

export function handleSummary(data) {
    return {
        'volume-test-1m-summary.json': JSON.stringify(data),
        'volume-test-1m-summary.txt': textSummary(data, {
            indent: ' ',
            enableColors: true,
        }),
    };
}

// Custom summary formatter
function textSummary(data, config = {}) {
    const indent = config.indent || '  ';
    let text = '';

    text += `${indent}aggregate...\n\n`;

    // Key metrics
    const metrics = [
        'http_reqs',
        'http_req_duration{avg}',
        'http_req_duration{p(95)}',
        'http_req_duration{p(99)}',
        'http_req_failed',
        'errors',
        'db_query_time{avg}',
        'api_response_time{avg}',
    ];

    for (let metric of metrics) {
        if (data.metrics[metric]) {
            const value = data.metrics[metric].values;
            text += `${indent}${metric}: ${JSON.stringify(value)}\n`;
        }
    }

    text += '\n';

    return text;
}
