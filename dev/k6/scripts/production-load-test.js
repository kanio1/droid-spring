// K6 Production Load Test
// Tests 400,000 events/minute (6,667 events/sec) target

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const responseTime = new Trend('response_time');
const requestsTotal = new Counter('requests_total');
const eventsPerSecond = new Rate('events_per_second');

// Test configuration
export const options = {
    stages: [
        // Ramp-up phase
        { duration: '2m', target: 1000 },   // Warm-up
        { duration: '5m', target: 2000 },   // Gradual increase
        { duration: '10m', target: 4000 },  // Target load (4,000 users = ~6,667 events/sec)
        { duration: '5m', target: 6000 },   // Stress test
        { duration: '2m', target: 8000 },   // Spike test
        { duration: '5m', target: 0 },      // Ramp-down
    ],
    thresholds: {
        // Error rate should be < 1%
        'errors': ['rate<0.01'],
        // 95% of requests should complete within 2s
        'response_time': ['p(95)<2000'],
        // 99% of requests should complete within 5s
        'response_time': ['p(99)<5000'],
        // Availability should be > 99.9%
        'http_req_duration': ['p(99.9)<5000'],
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_V1 = `${BASE_URL}/api/v1`;

// Test data
const testCustomers = Array.from({ length: 1000 }, (_, i) => ({
    id: `test-customer-${i}`,
    name: `Test Customer ${i}`,
    email: `test${i}@example.com`,
}));

const testOrders = Array.from({ length: 1000 }, (_, i) => ({
    id: `test-order-${i}`,
    customerId: testCustomers[i % testCustomers.length].id,
    total: Math.random() * 1000,
}));

// Default function
export default function () {
    // Simulate real user behavior
    const scenarios = [
        () => testCustomerFlow(),
        () => testOrderFlow(),
        () => testPaymentFlow(),
        () => testInvoiceFlow(),
        () => testReadOperations(),
    ];

    // Randomly select a scenario
    const scenario = scenarios[Math.floor(Math.random() * scenarios.length)];
    scenario();
}

// Test 1: Customer CRUD operations
function testCustomerFlow() {
    const customer = testCustomers[Math.floor(Math.random() * testCustomers.length)];

    // Create customer
    let response = http.post(`${API_V1}/customers`, JSON.stringify(customer), {
        headers: { 'Content-Type': 'application/json' },
        tags: { name: 'create_customer' },
    });

    check(response, {
        'create customer: status is 201 or 200': (r) => r.status === 201 || r.status === 200,
    }) || errorRate.add(1);
    responseTime.add(response.timings.duration);
    requestsTotal.add(1);

    sleep(Math.random() * 2); // Random think time

    // Get customer
    response = http.get(`${API_V1}/customers/${customer.id}`, {
        tags: { name: 'get_customer' },
    });

    check(response, {
        'get customer: status is 200': (r) => r.status === 200,
    }) || errorRate.add(1);
    responseTime.add(response.timings.duration);
    requestsTotal.add(1);

    sleep(Math.random() * 1);

    // Update customer
    response = http.put(`${API_V1}/customers/${customer.id}`, JSON.stringify({ ...customer, name: `${customer.name}-updated` }), {
        headers: { 'Content-Type': 'application/json' },
        tags: { name: 'update_customer' },
    });

    check(response, {
        'update customer: status is 200': (r) => r.status === 200,
    }) || errorRate.add(1);
    responseTime.add(response.timings.duration);
    requestsTotal.add(1);

    sleep(Math.random() * 2);

    // Delete customer
    response = http.del(`${API_V1}/customers/${customer.id}`, null, {
        tags: { name: 'delete_customer' },
    });

    check(response, {
        'delete customer: status is 204 or 200': (r) => r.status === 204 || r.status === 200,
    }) || errorRate.add(1);
    responseTime.add(response.timings.duration);
    requestsTotal.add(1);
}

// Test 2: Order processing flow
function testOrderFlow() {
    const order = testOrders[Math.floor(Math.random() * testOrders.length)];

    // Create order
    let response = http.post(`${API_V1}/orders`, JSON.stringify(order), {
        headers: { 'Content-Type': 'application/json' },
        tags: { name: 'create_order' },
    });

    check(response, {
        'create order: status is 201 or 200': (r) => r.status === 201 || r.status === 200,
    }) || errorRate.add(1);
    responseTime.add(response.timings.duration);
    requestsTotal.add(1);

    sleep(Math.random() * 2);

    // Get order
    response = http.get(`${API_V1}/orders/${order.id}`, {
        tags: { name: 'get_order' },
    });

    check(response, {
        'get order: status is 200': (r) => r.status === 200,
    }) || errorRate.add(1);
    responseTime.add(response.timings.duration);
    requestsTotal.add(1);

    // List orders by customer
    response = http.get(`${API_V1}/orders?customerId=${order.customerId}`, {
        tags: { name: 'list_orders' },
    });

    check(response, {
        'list orders: status is 200': (r) => r.status === 200,
    }) || errorRate.add(1);
    responseTime.add(response.timings.duration);
    requestsTotal.add(1);
}

// Test 3: Payment processing flow
function testPaymentFlow() {
    const payment = {
        id: `test-payment-${Math.floor(Math.random() * 1000)}`,
        orderId: testOrders[Math.floor(Math.random() * testOrders.length)].id,
        amount: Math.random() * 1000,
        status: 'PENDING',
    };

    // Create payment
    let response = http.post(`${API_V1}/payments`, JSON.stringify(payment), {
        headers: { 'Content-Type': 'application/json' },
        tags: { name: 'create_payment' },
    });

    check(response, {
        'create payment: status is 201 or 200': (r) => r.status === 201 || r.status === 200,
    }) || errorRate.add(1);
    responseTime.add(response.timings.duration);
    requestsTotal.add(1);

    sleep(Math.random() * 1);

    // Process payment
    response = http.post(`${API_V1}/payments/${payment.id}/process`, JSON.stringify({}), {
        headers: { 'Content-Type': 'application/json' },
        tags: { name: 'process_payment' },
    });

    check(response, {
        'process payment: status is 200': (r) => r.status === 200,
    }) || errorRate.add(1);
    responseTime.add(response.timings.duration);
    requestsTotal.add(1);

    sleep(Math.random() * 2);

    // Get payment
    response = http.get(`${API_V1}/payments/${payment.id}`, {
        tags: { name: 'get_payment' },
    });

    check(response, {
        'get payment: status is 200': (r) => r.status === 200,
    }) || errorRate.add(1);
    responseTime.add(response.timings.duration);
    requestsTotal.add(1);
}

// Test 4: Invoice operations
function testInvoiceFlow() {
    const invoice = {
        id: `test-invoice-${Math.floor(Math.random() * 1000)}`,
        customerId: testCustomers[Math.floor(Math.random() * testCustomers.length)].id,
        total: Math.random() * 1000,
    };

    // Create invoice
    let response = http.post(`${API_V1}/invoices`, JSON.stringify(invoice), {
        headers: { 'Content-Type': 'application/json' },
        tags: { name: 'create_invoice' },
    });

    check(response, {
        'create invoice: status is 201 or 200': (r) => r.status === 201 || r.status === 200,
    }) || errorRate.add(1);
    responseTime.add(response.timings.duration);
    requestsTotal.add(1);

    sleep(Math.random() * 2);

    // Generate invoice
    response = http.post(`${API_V1}/invoices/${invoice.id}/generate`, JSON.stringify({}), {
        headers: { 'Content-Type': 'application/json' },
        tags: { name: 'generate_invoice' },
    });

    check(response, {
        'generate invoice: status is 200': (r) => r.status === 200,
    }) || errorRate.add(1);
    responseTime.add(response.timings.duration);
    requestsTotal.add(1);

    sleep(Math.random() * 1);

    // Get invoice
    response = http.get(`${API_V1}/invoices/${invoice.id}`, {
        tags: { name: 'get_invoice' },
    });

    check(response, {
        'get invoice: status is 200': (r) => r.status === 200,
    }) || errorRate.add(1);
    responseTime.add(response.timings.duration);
    requestsTotal.add(1);
}

// Test 5: Read-heavy operations
function testReadOperations() {
    const customer = testCustomers[Math.floor(Math.random() * testCustomers.length)];

    // Get customer with addresses
    let response = http.get(`${API_V1}/customers/${customer.id}`, {
        tags: { name: 'get_customer_detail' },
    });

    check(response, {
        'get customer detail: status is 200': (r) => r.status === 200,
    }) || errorRate.add(1);
    responseTime.add(response.timings.duration);
    requestsTotal.add(1);

    sleep(Math.random() * 1);

    // List all customers (pagination)
    for (let page = 1; page <= 3; page++) {
        response = http.get(`${API_V1}/customers?page=${page}&size=20`, {
            tags: { name: 'list_customers' },
        });

        check(response, {
            'list customers: status is 200': (r) => r.status === 200,
        }) || errorRate.add(1);
        responseTime.add(response.timings.duration);
        requestsTotal.add(1);

        sleep(Math.random() * 0.5);
    }

    // Get customer orders summary
    response = http.get(`${API_V1}/customers/${customer.id}/orders?summary=true`, {
        tags: { name: 'get_orders_summary' },
    });

    check(response, {
        'get orders summary: status is 200': (r) => r.status === 200,
    }) || errorRate.add(1);
    responseTime.add(response.timings.duration);
    requestsTotal.add(1);
}

// Setup function (runs once)
export function setup() {
    console.log('Starting BSS Load Test');
    console.log(`Target: ${BASE_URL}`);
    console.log('Stages:');
    options.stages.forEach(stage => {
        console.log(`  - ${stage.duration}: ${stage.target} users`);
    });

    return { baseUrl: BASE_URL };
}

// Teardown function (runs once at the end)
export function teardown(data) {
    console.log('Load test completed');
    console.log(`Total requests: ${requestsTotal.value}`);
    console.log(`Error rate: ${(errorRate.values.rate * 100).toFixed(2)}%`);
}
