import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const responseTime = new Trend('response_time');

// Test configuration
export const options = {
  stages: [
    { duration: '2m', target: 10 }, // Ramp up to 10 users
    { duration: '5m', target: 10 }, // Stay at 10 users
    { duration: '2m', target: 20 }, // Ramp up to 20 users
    { duration: '5m', target: 20 }, // Stay at 20 users
    { duration: '2m', target: 0 },  // Ramp down to 0 users
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% of requests must complete within 500ms
    http_req_failed: ['rate<0.05'],   // Error rate must be less than 5%
    errors: ['rate<0.1'],             // Custom error rate less than 10%
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Test data
const testCustomers = [
  { email: 'test1@example.com', name: 'Test Customer 1' },
  { email: 'test2@example.com', name: 'Test Customer 2' },
  { email: 'test3@example.com', name: 'Test Customer 3' },
];

export function setup() {
  // Create test data
  const createdCustomers = [];

  for (const customer of testCustomers) {
    const payload = JSON.stringify({
      email: customer.email,
      name: customer.name,
      phone: '+1234567890',
      address: {
        street: '123 Main St',
        city: 'Test City',
        state: 'TS',
        zipCode: '12345',
        country: 'US'
      }
    });

    const params = {
      headers: {
        'Content-Type': 'application/json',
      },
    };

    const res = http.post(`${BASE_URL}/api/customers`, payload, params);
    if (res.status === 201) {
      createdCustomers.push(JSON.parse(res.body));
      sleep(1);
    }
  }

  return { createdCustomers };
}

export default function(data) {
  // Test scenarios
  testCustomerList();
  sleep(1);

  testCustomerSearch();
  sleep(1);

  testCustomerDetails(data);
  sleep(1);

  testOrders(data);
  sleep(1);

  testInvoices(data);
  sleep(1);

  testPayments(data);
  sleep(1);

  testSubscriptions(data);
  sleep(1);

  testProducts();
  sleep(1);

  testServices();
  sleep(1);

  testAssets();
  sleep(1);
}

function testCustomerList() {
  const res = http.get(`${BASE_URL}/api/customers?page=0&size=20`);

  const success = check(res, {
    'customer list status is 200': (r) => r.status === 200,
    'customer list has content': (r) => JSON.parse(r.body).content !== undefined,
  });

  errorRate.add(!success);
  responseTime.add(res.timings.duration);
}

function testCustomerSearch() {
  const res = http.get(`${BASE_URL}/api/customers/search?query=test`);

  const success = check(res, {
    'customer search status is 200': (r) => r.status === 200,
  });

  errorRate.add(!success);
  responseTime.add(res.timings.duration);
}

function testCustomerDetails(data) {
  if (!data.createdCustomers || data.createdCustomers.length === 0) return;

  const customerId = data.createdCustomers[0].id;
  const res = http.get(`${BASE_URL}/api/customers/${customerId}`);

  const success = check(res, {
    'customer details status is 200': (r) => r.status === 200,
    'customer details has id': (r) => JSON.parse(r.body).id !== undefined,
  });

  errorRate.add(!success);
  responseTime.add(res.timings.duration);
}

function testOrders(data) {
  if (!data.createdCustomers || data.createdCustomers.length === 0) return;

  const res = http.get(`${BASE_URL}/api/orders?customerId=${data.createdCustomers[0].id}&page=0&size=20`);

  const success = check(res, {
    'orders list status is 200': (r) => r.status === 200,
  });

  errorRate.add(!success);
  responseTime.add(res.timings.duration);
}

function testInvoices(data) {
  if (!data.createdCustomers || data.createdCustomers.length === 0) return;

  const res = http.get(`${BASE_URL}/api/invoices?customerId=${data.createdCustomers[0].id}&page=0&size=20`);

  const success = check(res, {
    'invoices list status is 200': (r) => r.status === 200,
  });

  errorRate.add(!success);
  responseTime.add(res.timings.duration);
}

function testPayments(data) {
  if (!data.createdCustomers || data.createdCustomers.length === 0) return;

  const res = http.get(`${BASE_URL}/api/payments?customerId=${data.createdCustomers[0].id}&page=0&size=20`);

  const success = check(res, {
    'payments list status is 200': (r) => r.status === 200,
  });

  errorRate.add(!success);
  responseTime.add(res.timings.duration);
}

function testSubscriptions(data) {
  if (!data.createdCustomers || data.createdCustomers.length === 0) return;

  const res = http.get(`${BASE_URL}/api/subscriptions?customerId=${data.createdCustomers[0].id}&page=0&size=20`);

  const success = check(res, {
    'subscriptions list status is 200': (r) => r.status === 200,
  });

  errorRate.add(!success);
  responseTime.add(res.timings.duration);
}

function testProducts() {
  const res = http.get(`${BASE_URL}/api/products?page=0&size=20`);

  const success = check(res, {
    'products list status is 200': (r) => r.status === 200,
  });

  errorRate.add(!success);
  responseTime.add(res.timings.duration);
}

function testServices() {
  const res = http.get(`${BASE_URL}/api/services?page=0&size=20`);

  const success = check(res, {
    'services list status is 200': (r) => r.status === 200,
  });

  errorRate.add(!success);
  responseTime.add(res.timings.duration);
}

function testAssets() {
  const res = http.get(`${BASE_URL}/api/assets?page=0&size=20`);

  const success = check(res, {
    'assets list status is 200': (r) => r.status === 200,
  });

  errorRate.add(!success);
  responseTime.add(res.timings.duration);
}

export function teardown(data) {
  // Clean up test data
  if (data.createdCustomers) {
    for (const customer of data.createdCustomers) {
      http.del(`${BASE_URL}/api/customers/${customer.id}`);
      sleep(1);
    }
  }
}
