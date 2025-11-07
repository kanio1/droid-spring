#!/usr/bin/env k6
/**
 * Distributed Load Test for BSS System
 *
 * This script is designed to run across multiple K6 instances
 * to simulate very high loads (10,000+ VUs).
 *
 * Master script coordinates multiple worker instances.
 *
 * Usage:
 *   # On master node
 *   k6 run --out cloud scripts/distributed-test.js
 *
 *   # Or with multiple local instances
 *   k6 run --vus 3000 --duration 30m scripts/distributed-test.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// Custom metrics
export let errorRate = new Rate('errors');

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_PREFIX = `${BASE_URL}/api/v1`;
const WORKER_ID = __ENV.WORKER_ID || 'default';

// Distributed test stages
const stages = [
    { duration: '2m', target: 1000 },
    { duration: '5m', target: 5000 },
    { duration: '10m', target: 10000 },
    { duration: '5m', target: 15000 },
    { duration: '5m', target: 20000 },
    { duration: '10m', target: 10000 },
    { duration: '5m', target: 5000 },
    { duration: '2m', target: 1000 },
];

export let options = {
    stages: stages,
    thresholds: {
        http_req_duration: [
            'p(95)<3000',
            'p(99)<8000',
        ],
        http_req_failed: ['rate<0.10'],
        errors: ['rate<0.05'],
    },
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
    noConnectionReuse: false,
    userAgent: `k6-distributed-test/1.0 (worker:${WORKER_ID})`,
    // Enable browser-like caching
    httpDebug: 'full',
};

// BSS API endpoints
const endpoints = {
    customers: `${API_PREFIX}/customers`,
    customerById: (id) => `${API_PREFIX}/customers/${id}`,
    orders: `${API_PREFIX}/orders`,
    orderById: (id) => `${API_PREFIX}/orders/${id}`,
    payments: `${API_PREFIX}/payments`,
    paymentById: (id) => `${API_PREFIX}/payments/${id}`,
    invoices: `${API_PREFIX}/invoices`,
    invoiceById: (id) => `${API_PREFIX}/invoices/${id}`,
    health: `${BASE_URL}/actuator/health`,
};

// Data pools for distributed testing
const customerIds = Array.from({ length: 5000 }, (_, i) => i + 1);
const orderIds = Array.from({ length: 10000 }, (_, i) => i + 1);

function randomId(arr) {
    return arr[Math.floor(Math.random() * arr.length)];
}

// Core test scenarios
function customerReadTest() {
    const id = randomId(customerIds);
    const response = http.get(endpoints.customerById(id), {
        tags: { name: 'GET /customers/{id}', worker: WORKER_ID },
    });

    const success = check(response, {
        'customer read OK': (r) => r.status === 200 || r.status === 404,
    });

    errorRate.add(!success);
}

function orderReadTest() {
    const id = randomId(orderIds);
    const response = http.get(endpoints.orderById(id), {
        tags: { name: 'GET /orders/{id}', worker: WORKER_ID },
    });

    const success = check(response, {
        'order read OK': (r) => r.status === 200 || r.status === 404,
    });

    errorRate.add(!success);
}

function paymentReadTest() {
    const id = Math.floor(Math.random() * 5000) + 1;
    const response = http.get(endpoints.paymentById(id), {
        tags: { name: 'GET /payments/{id}', worker: WORKER_ID },
    });

    const success = check(response, {
        'payment read OK': (r) => r.status === 200 || r.status === 404,
    });

    errorRate.add(!success);
}

function healthTest() {
    const response = http.get(endpoints.health, {
        tags: { name: 'GET /actuator/health', worker: WORKER_ID },
    });

    const success = check(response, {
        'health check OK': (r) => r.status === 200,
    });

    errorRate.add(!success);
}

// Scenario distribution for distributed test
const scenarios = [
    { weight: 40, fn: customerReadTest },
    { weight: 30, fn: orderReadTest },
    { weight: 20, fn: paymentReadTest },
    { weight: 10, fn: healthTest },
];

export default function () {
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

    // Minimal sleep for maximum throughput
    sleep(0.01);
}

export function setup() {
    console.log(`=== DISTRIBUTED TEST STARTED (Worker: ${WORKER_ID}) ===`);
    console.log(`Base URL: ${BASE_URL}`);
    console.log(`Worker ID: ${WORKER_ID}`);
    console.log(`Virtual Users: ${__VU || 'N/A'}`);
    console.log('========================================================\n`);

    return { workerId: WORKER_ID, startTime: Date.now() };
}

export function teardown(data) {
    console.log(`\n=== DISTRIBUTED TEST COMPLETED (Worker: ${WORKER_ID}) ===`);
    console.log(`Worker ID: ${data.workerId}`);
    console.log(`Duration: ${((Date.now() - data.startTime) / 1000).toFixed(2)}s`);
    console.log('========================================================\n`);
}

export function handleSummary(data) {
    return {
        [`distributed-test-${WORKER_ID}-summary.json`]: JSON.stringify(data),
    };
}
