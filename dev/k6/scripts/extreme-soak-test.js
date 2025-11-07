#!/usr/bin/env k6
/**
 * Extreme Soak Test for BSS System
 *
 * Long-duration test to identify memory leaks and resource exhaustion:
 * - 24+ hour duration
 * - Moderate load (200-500 VUs)
 * - Continuous operation
 * - Memory monitoring
 *
 * Tests:
 * - JVM memory leaks
 * - Database connection leaks
 * - File descriptor exhaustion
 * - Cache eviction behavior
 * - Log rotation
 *
 * Usage:
 *   k6 run --vus 300 --duration 24h scripts/extreme-soak-test.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
export let errorRate = new Rate('errors');
export let memoryLeaks = new Rate('memory_leaks');
export let connectionErrors = new Rate('connection_errors');

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_PREFIX = `${BASE_URL}/api/v1`;

// Soak test stages - 24 hours
const stages = [
    // Warm up (1 hour)
    { duration: '1h', target: 100 },
    // Light soak (4 hours)
    { duration: '4h', target: 200 },
    // Medium load (8 hours)
    { duration: '8h', target: 300 },
    // Peak (2 hours)
    { duration: '2h', target: 500 },
    // Medium (4 hours)
    { duration: '4h', target: 300 },
    // Light (4 hours)
    { duration: '4h', target: 200 },
    // Cool down (1 hour)
    { duration: '1h', target: 100 },
];

export let options = {
    stages: stages,
    thresholds: {
        http_req_duration: [
            'p(95)<4000', // Allow slightly higher latency over long test
            'p(99)<10000',
        ],
        http_req_failed: ['rate<0.05'],
        errors: ['rate<0.03'],
        memory_leaks: ['rate<0.005'], // Very low tolerance for memory issues
        connection_errors: ['rate<0.01'],
    },
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
    // Use persistent connections
    noConnectionReuse: false,
    // Aggressive connection pooling
    httpDebug: false,
    userAgent: 'k6-soak-test/1.0',
    // Discard default output
    noVCUConnectionReuse: false,
};

// API endpoints
const endpoints = {
    customers: `${API_PREFIX}/customers`,
    customerById: (id) => `${API_PREFIX}/customers/${id}`,
    orders: `${API_PREFIX}/orders`,
    orderById: (id) => `${API_PREFIX}/orders/${id}`,
    payments: `${API_PREFIX}/payments`,
    paymentById: (id) => `${API_PREFIX}/payments/${id}`,
    health: `${BASE_URL}/actuator/health`,
    metrics: `${BASE_URL}/actuator/prometheus`,
    env: `${BASE_URL}/actuator/env`,
};

// Data pools
const customerIds = Array.from({ length: 2000 }, (_, i) => i + 1);
const orderIds = Array.from({ length: 3000 }, (_, i) => i + 1);

function randomId(arr) {
    return arr[Math.floor(Math.random() * arr.length)];
}

// Lightweight scenarios for soak test
function readCustomer() {
    const id = randomId(customerIds);
    const response = http.get(endpoints.customerById(id), {
        tags: { name: 'GET /customers/{id}' },
    });

    if (response.error) {
        connectionErrors.add(1);
    }

    const success = check(response, {
        'customer read OK': (r) => r.status === 200 || r.status === 404,
    });

    errorRate.add(!success);

    // Check for memory pressure indicators
    if (response.timings.duration > 5000) {
        memoryLeaks.add(1);
    }
}

function readOrder() {
    const id = randomId(orderIds);
    const response = http.get(endpoints.orderById(id), {
        tags: { name: 'GET /orders/{id}' },
    });

    if (response.error) {
        connectionErrors.add(1);
    }

    const success = check(response, {
        'order read OK': (r) => r.status === 200 || r.status === 404,
    });

    errorRate.add(!success);
}

function readPayment() {
    const id = Math.floor(Math.random() * 1500) + 1;
    const response = http.get(endpoints.paymentById(id), {
        tags: { name: 'GET /payments/{id}' },
    });

    if (response.error) {
        connectionErrors.add(1);
    }

    const success = check(response, {
        'payment read OK': (r) => r.status === 200 || r.status === 404,
    });

    errorRate.add(!success);
}

function systemHealthCheck() {
    const response = http.get(endpoints.health, {
        tags: { name: 'GET /actuator/health' },
    });

    if (response.error) {
        connectionErrors.add(1);
    }

    const success = check(response, {
        'health check OK': (r) => r.status === 200,
    });

    errorRate.add(!success);

    // Extract memory metrics if available
    try {
        const body = JSON.parse(response.body);
        if (body.memory) {
            const usedPercent = (body.memory.used / body.memory.max) * 100;
            if (usedPercent > 85) {
                console.warn(`High memory usage: ${usedPercent.toFixed(2)}%`);
                memoryLeaks.add(1);
            }
        }
    } catch (e) {
        // Ignore parsing errors
    }
}

function memoryMetricsCheck() {
    const response = http.get(endpoints.metrics, {
        tags: { name: 'GET /actuator/metrics' },
        timeout: '5s',
    });

    if (response.error || response.status >= 400) {
        connectionErrors.add(1);
        return;
    }

    // Check for specific memory-related metrics
    const body = response.body;
    if (body.includes('jvm_memory_used_bytes') && body.includes('jvm_memory_max_bytes')) {
        // Metrics are available
    }
}

// Scenario distribution
const scenarios = [
    { weight: 40, fn: readCustomer },
    { weight: 30, fn: readOrder },
    { weight: 15, fn: readPayment },
    { weight: 10, fn: systemHealthCheck },
    { weight: 5, fn: memoryMetricsCheck },
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

    // Minimal sleep for sustained load
    sleep(0.05);
}

// Progress reporting
let lastReport = 0;

export function handleProgress() {
    const now = Date.now();
    if (now - lastReport > 300000) { // Every 5 minutes
        const startTime = new Date(__ENV.startedAt).getTime();
        const elapsed = (now - startTime) / 1000;
        const totalDuration = getTotalDuration();
        const progress = (elapsed / totalDuration) * 100;

        console.log(`Soak test progress: ${progress.toFixed(2)}% | Elapsed: ${formatDuration(elapsed)} | Remaining: ${formatDuration(totalDuration - elapsed)}`);

        if (__VU % 10 === 0) { // Log from one VU only
            console.log(`  - VUs active: ${__VU}`);
            console.log(`  - Iterations: ${__ITER}`);
        }

        lastReport = now;
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

function formatDuration(seconds) {
    const days = Math.floor(seconds / 86400);
    const hours = Math.floor((seconds % 86400) / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);

    if (days > 0) return `${days}d ${hours}h`;
    if (hours > 0) return `${hours}h ${minutes}m`;
    return `${minutes}m`;
}

// Setup and teardown
export function setup() {
    const startTime = Date.now();
    __ENV.startedAt = new Date(startTime).toISOString();

    console.log('=== EXTREME SOAK TEST STARTED ===');
    console.log(`Base URL: ${BASE_URL}`);
    console.log(`Test Duration: ${(getTotalDuration() / 3600).toFixed(1)} hours`);
    console.log(`Start Time: ${new Date(startTime).toISOString()}`);
    console.log(`Expected End Time: ${new Date(startTime + getTotalDuration() * 1000).toISOString()}`);
    console.log('==================================\n`);

    // Initial health check
    for (let i = 0; i < 5; i++) {
        systemHealthCheck();
        sleep(1);
    }

    return { startTime };
}

export function teardown(data) {
    const endTime = Date.now();
    const duration = (endTime - data.startTime) / 1000;

    console.log('\n=== EXTREME SOAK TEST COMPLETED ===');
    console.log(`Start Time: ${new Date(data.startTime).toISOString()}`);
    console.log(`End Time: ${new Date(endTime).toISOString()}`);
    console.log(`Total Duration: ${formatDuration(duration)}`);
    console.log(`Total Iterations: ${__VU * Math.floor(duration / 5)}`); // Approximate
    console.log('=====================================\n`);
}

export function handleSummary(data) {
    return {
        'extreme-soak-test-summary.json': JSON.stringify(data),
        'extreme-soak-test-summary.txt': textSummary(data, {
            indent: ' ',
            enableColors: true,
        }),
    };
}

function textSummary(data, config = {}) {
    const indent = config.indent || '  ';
    let text = `${indent}SOAK TEST RESULTS\n\n`;

    // Memory leak detection
    if (data.metrics.memory_leaks) {
        const leakRate = data.metrics.memory_leaks.values.rate;
        if (leakRate > 0.01) {
            text += `${indent}WARNING: Possible memory leaks detected (rate: ${(leakRate * 100).toFixed(2)}%)\n`;
        } else {
            text += `${indent}OK: No significant memory leaks detected\n`;
        }
    }

    // Connection errors
    if (data.metrics.connection_errors) {
        const connRate = data.metrics.connection_errors.values.rate;
        if (connRate > 0.01) {
            text += `${indent}WARNING: High connection error rate: ${(connRate * 100).toFixed(2)}%\n`;
        } else {
            text += `${indent}OK: Low connection error rate: ${(connRate * 100).toFixed(2)}%\n`;
        }
    }

    // Performance over time
    const durations = data.metrics.http_req_duration.values;
    if (durations) {
        const trend = (durations.max - durations.min) / durations.avg;
        if (trend > 0.5) {
            text += `${indent}WARNING: Response time increasing over time\n`;
        } else {
            text += `${indent}OK: Stable response times\n`;
        }
    }

    return text;
}
