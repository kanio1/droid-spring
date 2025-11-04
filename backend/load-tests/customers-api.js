/**
 * Load testing script for BSS Customer API
 * Using k6 (https://k6.io/)
 *
 * Run with: k6 run customers-api.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');

export const options = {
    stages: [
        // Ramp up from 1 to 100 users over 2 minutes
        { duration: '2m', target: 100 },
        // Stay at 100 users for 5 minutes
        { duration: '5m', target: 100 },
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
const testCustomer = {
    firstName: 'LoadTest',
    lastName: 'User',
    email: `loadtest-${Date.now()}@example.com`,
    pesel: `12345678901`,
    phoneNumber: '+48123456789',
    address: {
        street: 'Load Test St. 1',
        city: 'TestCity',
        postalCode: '00-000',
        country: 'PL',
    },
};

export function setup() {
    // Create test data before running tests
    console.log('Setting up load test data...');

    // Note: In real scenario, you would authenticate here
    // const authResponse = http.post(`${BASE_URL}/auth/login`, JSON.stringify(authData), {
    //     headers: { 'Content-Type': 'application/json' },
    // });

    return { customerId: null };
}

export default function (data) {
    // Test 1: Create Customer
    const createResponse = http.post(
        `${API_PREFIX}/customers`,
        JSON.stringify(testCustomer),
        {
            headers: {
                'Content-Type': 'application/json',
                // Authorization: `Bearer ${authToken}`,
            },
        }
    );

    const createSuccess = check(createResponse, {
        'customer created status is 201': (r) => r.status === 201,
        'customer created has ID': (r) => {
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
        console.error(`Failed to create customer: ${createResponse.status} - ${createResponse.body}`);
        return;
    }

    // Extract customer ID
    let customerId;
    try {
        const body = JSON.parse(createResponse.body);
        customerId = body.id;
    } catch (e) {
        console.error('Failed to parse customer ID:', e);
        return;
    }

    sleep(1);

    // Test 2: Get Customer by ID
    const getResponse = http.get(`${API_PREFIX}/customers/${customerId}`, {
        headers: {
            'Content-Type': 'application/json',
            // Authorization: `Bearer ${authToken}`,
        },
    });

    check(getResponse, {
        'get customer status is 200': (r) => r.status === 200,
        'get customer has correct email': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.email === testCustomer.email;
            } catch (e) {
                return false;
            }
        },
    });

    errorRate.add(getResponse.status !== 200);

    sleep(1);

    // Test 3: List Customers (paginated)
    const listResponse = http.get(`${API_PREFIX}/customers?page=0&size=20`, {
        headers: {
            'Content-Type': 'application/json',
            // Authorization: `Bearer ${authToken}`,
        },
    });

    check(listResponse, {
        'list customers status is 200': (r) => r.status === 200,
        'list customers has content': (r) => {
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

    // Test 4: Search Customers
    const searchResponse = http.get(`${API_PREFIX}/customers/search?searchTerm=LoadTest&page=0&size=20`, {
        headers: {
            'Content-Type': 'application/json',
            // Authorization: `Bearer ${authToken}`,
        },
    });

    check(searchResponse, {
        'search customers status is 200': (r) => r.status === 200,
        'search returns results': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.content !== undefined;
            } catch (e) {
                return false;
            }
        },
    });

    errorRate.add(searchResponse.status !== 200);

    sleep(1);

    // Test 5: Update Customer
    const updateData = {
        ...testCustomer,
        phoneNumber: '+48987654321',
    };

    const updateResponse = http.put(
        `${API_PREFIX}/customers/${customerId}`,
        JSON.stringify(updateData),
        {
            headers: {
                'Content-Type': 'application/json',
                // Authorization: `Bearer ${authToken}`,
            },
        }
    );

    check(updateResponse, {
        'update customer status is 200': (r) => r.status === 200,
    });

    errorRate.add(updateResponse.status !== 200);

    sleep(1);

    // Test 6: Delete Customer
    const deleteResponse = http.del(`${API_PREFIX}/customers/${customerId}`, null, {
        headers: {
            'Content-Type': 'application/json',
            // Authorization: `Bearer ${authToken}`,
        },
    });

    check(deleteResponse, {
        'delete customer status is 204': (r) => r.status === 204,
    });

    errorRate.add(deleteResponse.status !== 204);

    // Wait between iterations
    sleep(2);
}

export function teardown(data) {
    // Cleanup after tests
    console.log('Tearing down load test...');
}

/**
 * To run this test:
 *
 * 1. Install k6: https://k6.io/docs/getting-started/installation/
 *
 * 2. Start the BSS backend:
 *    mvn spring-boot:run
 *
 * 3. Run the load test:
 *    k6 run customers-api.js
 *
 * 4. Run with custom BASE_URL:
 *    BASE_URL=http://localhost:8080 k6 run customers-api.js
 *
 * Expected Results:
 * - 95% of requests should complete within 500ms
 * - Error rate should be less than 5%
 * - All CRUD operations should work correctly
 */
