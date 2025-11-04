/**
 * Load testing script for BSS Invoice API
 * Using k6 (https://k6.io/)
 *
 * Run with: k6 run invoices-api.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');

export const options = {
    stages: [
        // Ramp up from 1 to 50 users over 2 minutes
        { duration: '2m', target: 50 },
        // Stay at 50 users for 5 minutes
        { duration: '5m', target: 50 },
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
const testInvoice = {
    customerId: '00000000-0000-0000-0000-000000000001', // Replace with actual customer ID
    invoiceNumber: `INV-${Date.now()}`,
    invoiceType: 'ONE_TIME',
    issueDate: new Date().toISOString().split('T')[0],
    dueDate: new Date(Date.now() + 14 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
    paidDate: null,
    billingPeriodStart: new Date().toISOString().split('T')[0],
    billingPeriodEnd: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
    subtotal: 100.00,
    discountAmount: 0,
    taxAmount: 23.00,
    totalAmount: 123.00,
    currency: 'PLN',
    paymentTerms: 14,
    lateFee: 0,
    notes: 'Load test invoice',
    pdfUrl: null,
};

export default function () {
    // Test 1: Create Invoice
    const createResponse = http.post(
        `${API_PREFIX}/invoices`,
        JSON.stringify(testInvoice),
        {
            headers: {
                'Content-Type': 'application/json',
                // Authorization: `Bearer ${authToken}`,
            },
        }
    );

    const createSuccess = check(createResponse, {
        'invoice created status is 201': (r) => r.status === 201,
        'invoice created has ID': (r) => {
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
        console.error(`Failed to create invoice: ${createResponse.status} - ${createResponse.body}`);
        return;
    }

    // Extract invoice ID
    let invoiceId;
    try {
        const body = JSON.parse(createResponse.body);
        invoiceId = body.id;
    } catch (e) {
        console.error('Failed to parse invoice ID:', e);
        return;
    }

    sleep(1);

    // Test 2: Get Invoice by ID
    const getResponse = http.get(`${API_PREFIX}/invoices/${invoiceId}`, {
        headers: {
            'Content-Type': 'application/json',
            // Authorization: `Bearer ${authToken}`,
        },
    });

    check(getResponse, {
        'get invoice status is 200': (r) => r.status === 200,
        'get invoice has correct number': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.invoiceNumber === testInvoice.invoiceNumber;
            } catch (e) {
                return false;
            }
        },
    });

    errorRate.add(getResponse.status !== 200);

    sleep(1);

    // Test 3: List Invoices (paginated)
    const listResponse = http.get(`${API_PREFIX}/invoices?page=0&size=20`, {
        headers: {
            'Content-Type': 'application/json',
            // Authorization: `Bearer ${authToken}`,
        },
    });

    check(listResponse, {
        'list invoices status is 200': (r) => r.status === 200,
        'list invoices has content': (r) => {
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

    // Test 4: Search Invoices
    const searchResponse = http.get(`${API_PREFIX}/invoices/search?query=${testInvoice.invoiceNumber}&page=0&size=20`, {
        headers: {
            'Content-Type': 'application/json',
            // Authorization: `Bearer ${authToken}`,
        },
    });

    check(searchResponse, {
        'search invoices status is 200': (r) => r.status === 200,
    });

    errorRate.add(searchResponse.status !== 200);

    sleep(1);

    // Test 5: Update Invoice Status
    const statusChangeData = {
        invoiceId: invoiceId,
        status: 'SENT',
        changeReason: 'Load test status change',
    };

    const statusResponse = http.put(
        `${API_PREFIX}/invoices/${invoiceId}/status`,
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

    // Wait between iterations
    sleep(2);
}

/**
 * Invoice Load Testing Scenarios:
 * 1. Create Invoice - Tests CRUD create operations
 * 2. Get Invoice by ID - Tests single read operation
 * 3. List Invoices - Tests pagination and listing
 * 4. Search Invoices - Tests search functionality
 * 5. Change Status - Tests update operations
 *
 * Expected Performance:
 * - Invoice creation: < 500ms
 * - Invoice retrieval: < 200ms
 * - List operations: < 300ms
 * - Search operations: < 400ms
 * - Status changes: < 300ms
 */
