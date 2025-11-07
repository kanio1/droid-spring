#!/usr/bin/env k6
/**
 * Marathon Test for BSS System
 *
 * Long-running endurance test to detect:
 * - Memory leaks
 * - Resource exhaustion
 * - Database connection pool issues
 * - Slow degradation over time
 *
 * This test runs for 12+ hours with:
 * - Moderate load (100-500 VUs)
 * - Continuous operation
 * - Periodic spikes
 *
 * Usage:
 *   k6 run --vus 500 --duration 12h scripts/marathon-test.js
 *
 * Scale: 10M+ events
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// Custom metrics
export let errorRate = new Rate('errors');
export let memoryWarnings = new Rate('memory_warnings');

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_PREFIX = `${BASE_URL}/API_PREFIX`;

// Marathon test stages - 12 hour test
const stages = [
    // Warm up (30 min)
    { duration: '30m', target: 50 },
    // Steady state (2 hours)
    { duration: '2h', target: 100 },
    // Light load (2 hours)
    { duration: '2h', target: 200 },
    // Medium load (1 hour)
    { duration: '1h', target: 300 },
    // Heavy load (30 min)
    { duration: '30m', target: 500 },
    // Spike test (15 min)
    { duration: '15m', target: 1000 },
    // Recovery (1 hour)
    { duration: '1h', target: 200 },
    // Cool down (4 hours)
    { duration: '4h', target: 100 },
    // Final spike (15 min)
    { duration: '15m', target: 800 },
    // End (15 min)
    { duration: '15m', target: 50 },
];

export let options = {
    stages: stages,
    thresholds: {
        http_req_duration: [
            'p(95)<3000', // 95% under 3s for long test
            'p(99)<8000', // 99% under 8s
        ],
        http_req_failed: ['rate<0.05'], // Error rate under 5%
        errors: ['rate<0.03'], // Custom error metric under 3%
        memory_warnings: ['rate<0.01'], // Memory warnings under 1%
    },
    // Extended test settings
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
    // No connection reuse to test fresh connections
    noConnectionReuse: true,
    // Ramping VUs
    rps: 1000, // Max requests per second
    // User agent
    userAgent: 'k6-marathon-test/1.0',
    // Discard test output
    noVCUConnectionReuse: false,
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

// Test data generators
function randomId() {
    return Math.floor(Math.random() * 50000) + 1;
}

function randomString(length = 10) {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz';
    let result = '';
    for (let i = 0; i < length; i++) {
        result += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return result;
}

// Lightweight test scenarios for marathon
function quickCustomerRead() {
    const customerId = randomId();
    const response = http.get(endpoints.customerById(customerId), {
        tags: { name: 'GET /customers/{id}' },
    });

    check(response, {
        'customer read status OK': (r) => r.status === 200 || r.status === 404,
    });

    errorRate.add(response.status >= 400);
}

function quickOrderRead() {
    const orderId = randomId();
    const response = http.get(endpoints.orderById(orderId), {
        tags: { name: 'GET /orders/{id}' },
    });

    check(response, {
        'order read status OK': (r) => r.status === 200 || r.status === 404,
    });

    errorRate.add(response.status >= 400);
}

function quickPaymentRead() {
    const paymentId = randomId();
    const response = http.get(endpoints.paymentById(paymentId), {
        tags: { name: 'GET /payments/{id}' },
    });

    check(response, {
        'payment read status OK': (r) => r.status === 200 || r.status === 404,
    });

    errorRate.add(response.status >= 400);
}

function quickInvoiceRead() {
    const invoiceId = randomId();
    const response = http.get(endpoints.invoiceById(invoiceId), {
        tags: { name: 'GET /invoices/{id}' },
    });

    check(response, {
        'invoice read status OK': (r) => r.status === 200 || r.status === 404,
    });

    errorRate.add(response.status >= 400);
}

function healthCheck() {
    const response = http.get(endpoints.health, {
        tags: { name: 'GET /actuator/health' },
    });

    const success = check(response, {
        'health check OK': (r) => r.status === 200,
    });

    errorRate.add(!success);

    // Check if response indicates memory pressure
    if (response.json('mem.used.percent') && response.json('mem.used.percent') > 80) {
        memoryWarnings.add(1);
        console.warn(`High memory usage: ${response.json('mem.used.percent')}%`);
    }
}

// Mix of read operations
function mixedReadScenario() {
    // Distribute load evenly
    const scenarios = [
        { weight: 40, fn: quickCustomerRead },
        { weight: 30, fn: quickOrderRead },
        { weight: 15, fn: quickPaymentRead },
        { weight: 10, fn: quickInvoiceRead },
        { weight: 5, fn: healthCheck },
    ];

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
}

// Main test loop
export default function () {
    mixedReadScenario();

    // Shorter sleep for higher throughput
    sleep(0.05);
}

// Progress reporting
let lastProgressReport = 0;

export function handleProgress() {
    const now = Date.now();
    if (now - lastProgressReport > 60000) { // Every minute
        const elapsed = (now - __ENV.startedAt) / 1000;
        const totalDuration = getTotalDuration();
        const progress = (elapsed / totalDuration) * 100;

        console.log(`Progress: ${progress.toFixed(2)}% (${Math.floor(elapsed / 60)}/${Math.floor(totalDuration / 60)} minutes)`);
        lastProgressReport = now;
    }
}

function getTotalDuration() {
    return options.stages.reduce((sum, stage) => {
        const value = parseInt(stage.duration);
        const unit = stage.duration.slice(-1);
        switch (unit) {
            case 's': return sum + value;
            case 'm': return sum + value * 60;
            case 'h': return sum + value * 3600;
            default: return sum + value;
        }
    }, 0);
}

// Handle test lifecycle
export function setup() {
    const startTime = Date.now();
    __ENV.startedAt = startTime;

    console.log('=== MARATHON TEST STARTED ===');
    console.log(`Base URL: ${BASE_URL}`);
    console.log(`Test Duration: ${(getTotalDuration() / 3600).toFixed(1)} hours`);
    console.log(`Expected VUs: 50 - 1000`);
    console.log(`Start Time: ${new Date(startTime).toISOString()}`);
    console.log('=============================\n`);

    // Health check
    for (let i = 0; i < 5; i++) {
        healthCheck();
        sleep(1);
    }

    return { startTime };
}

export function teardown(data) {
    const endTime = Date.now();
    const duration = (endTime - data.startTime) / 1000;

    console.log('\n=== MARATHON TEST COMPLETED ===');
    console.log(`Start Time: ${new Date(data.startTime).toISOString()}`);
    console.log(`End Time: ${new Date(endTime).toISOString()}`);
    console.log(`Total Duration: ${(duration / 3600).toFixed(2)} hours`);
    console.log(`Test iterations completed: ${__VU * Math.floor(duration / 5)}`); // Approximate
    console.log('=================================\n`);
}

export function handleSummary(data) {
    // Save detailed results
    return {
        'marathon-test-summary.json': JSON.stringify(data),
        'marathon-test-summary.txt': textSummary(data, {
            indent: ' ',
            enableColors: true,
        }),
    };
}
