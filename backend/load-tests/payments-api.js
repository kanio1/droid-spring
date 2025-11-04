/**
 * Load testing script for BSS Payment API
 * Using k6 (https://k6.io/)
 *
 * Run with: k6 run payments-api.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');

export const options = {
    stages: [
        // Ramp up from 1 to 30 users over 2 minutes
        { duration: '2m', target: 30 },
        // Stay at 30 users for 5 minutes
        { duration: '5m', target: 30 },
        // Ramp down to 0 users over 2 minutes
        { duration: '2m', target: 0 },
    ],
    thresholds: {
        http_req_duration: ['p(95)<500'], // 95% of requests must complete within 500ms
        http_req_failed: ['rate<0.05'], // Error rate must be less than 5%
        errors: ['rate<0.1'], // Custom error rate must be less than 10%
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_PREFIX = `${BASE_URL}/api`;

// Test data
const testPayment = {
    customerId: '00000000-0000-0000-0000-000000000001', // Replace with actual customer ID
    invoiceId: '00000000-0000-0000-0000-000000000002', // Replace with actual invoice ID
    amount: 123.00,
    currency: 'PLN',
    paymentMethod: 'CREDIT_CARD',
    paymentDate: new Date().toISOString().split('T')[0],
    dueDate: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
    transactionId: `TXN-${Date.now()}`,
    gatewayResponse: {
        gateway: 'STRIPE',
        transactionId: `GW-${Date.now()}`,
        status: 'SUCCESS',
    },
    metadata: {
        source: 'load-test',
        testRunId: `${Date.now()}`,
    },
};

export default function () {
    // Test 1: Create Payment
    const createResponse = http.post(
        `${API_PREFIX}/payments`,
        JSON.stringify(testPayment),
        {
            headers: {
                'Content-Type': 'application/json',
                // Authorization: `Bearer ${authToken}`,
            },
        }
    );

    const createSuccess = check(createResponse, {
        'payment created status is 201': (r) => r.status === 201,
        'payment created has ID': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.id !== undefined;
            } catch (e) {
                return false;
            }
        },
    });

    errorRate.add(!createSuccess);

    if (!createSuccess) {
        console.error(`Failed to create payment: ${createResponse.status} - ${createResponse.body}`);
        return;
    }

    // Extract payment ID
    let paymentId;
    try {
        const body = JSON.parse(createResponse.body);
        paymentId = body.id;
    } catch (e) {
        console.error('Failed to parse payment ID:', e);
        return;
    }

    sleep(1);

    // Test 2: Get Payment by ID
    const getResponse = http.get(`${API_PREFIX}/payments/${paymentId}`, {
        headers: {
            'Content-Type': 'application/json',
            // Authorization: `Bearer ${authToken}`,
        },
    });

    check(getResponse, {
        'get payment status is 200': (r) => r.status === 200,
        'get payment has correct amount': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.amount === testPayment.amount;
            } catch (e) {
                return false;
            }
        },
    });

    errorRate.add(getResponse.status !== 200);

    sleep(1);

    // Test 3: List Payments (paginated)
    const listResponse = http.get(`${API_PREFIX}/payments?page=0&size=20`, {
        headers: {
            'Content-Type': 'application/json',
            // Authorization: `Bearer ${authToken}`,
        },
    });

    check(listResponse, {
        'list payments status is 200': (r) => r.status === 200,
        'list payments has content': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.content !== undefined && Array.isArray(body.content);
            } catch (e) {
                return false;
            }
        },
    });

    errorRate.add(listResponse.status !== 200);

    sleep(1);

    // Test 4: Change Payment Status
    const statusChangeData = {
        paymentId: paymentId,
        status: 'COMPLETED',
        changeReason: 'Load test status change',
    };

    const statusResponse = http.put(
        `${API_PREFIX}/payments/${paymentId}/status`,
        JSON.stringify(statusChangeData),
        {
            headers: {
                'Content-Type': 'application/json',
                // Authorization: `Bearer ${authToken}`,
            },
        }
    );

    check(statusResponse, {
        'change status status is 200': (r) => r.status === 200,
    });

    errorRate.add(statusResponse.status !== 200);

    sleep(1);

    // Test 5: Get Payment by Transaction ID
    const txnResponse = http.get(`${API_PREFIX}/payments/transaction/${testPayment.transactionId}`, {
        headers: {
            'Content-Type': 'application/json',
            // Authorization: `Bearer ${authToken}`,
        },
    });

    check(txnResponse, {
        'get by transaction status is 200': (r) => r.status === 200,
    });

    // Wait between iterations
    sleep(2);
}

/**
 * Payment Load Testing Scenarios:
 * 1. Create Payment - Tests payment processing
 * 2. Get Payment by ID - Tests single read operation
 * 3. List Payments - Tests pagination
 * 4. Change Status - Tests status updates
 * 5. Get by Transaction ID - Tests gateway integration
 *
 * Expected Performance:
 * - Payment creation: < 500ms
 * - Payment retrieval: < 200ms
 * - List operations: < 300ms
 * - Status changes: < 300ms
 * - Transaction lookup: < 300ms
 */
