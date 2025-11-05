import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  vus: 1, // 1 virtual user
  iterations: 1,
  thresholds: {
    http_req_duration: ['p(95)<200'], // 95% of requests must complete within 200ms
    http_req_failed: ['rate<0.01'],   // Error rate must be less than 1%
  },
};

export default function() {
  console.log('Starting smoke test...');

  // Test health endpoint
  const healthRes = http.get(`${BASE_URL}/actuator/health`);
  check(healthRes, {
    'health endpoint responds': (r) => r.status === 200,
    'health status is UP': (r) => r.json('status') === 'UP',
  });
  console.log('Health check passed');

  // Test customer list
  const customersRes = http.get(`${BASE_URL}/api/customers?page=0&size=10`);
  check(customersRes, {
    'customers endpoint responds': (r) => r.status === 200,
  });
  console.log('Customers endpoint passed');

  // Test products list
  const productsRes = http.get(`${BASE_URL}/api/products?page=0&size=10`);
  check(productsRes, {
    'products endpoint responds': (r) => r.status === 200,
  });
  console.log('Products endpoint passed');

  // Test services list
  const servicesRes = http.get(`${BASE_URL}/api/services?page=0&size=10`);
  check(servicesRes, {
    'services endpoint responds': (r) => r.status === 200,
  });
  console.log('Services endpoint passed');

  console.log('Smoke test completed successfully');
}
