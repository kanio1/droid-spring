import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';

export let errors = new Counter('errors');
export let errorRate = new Rate('error_rate');

export let options = {
    stages: [
        { duration: '30s', target: 50 },
        { duration: '1m', target: 100 },
        { duration: '1m', target: 200 },
        { duration: '1m', target: 500 },
        { duration: '2m', target: 1000 },
        { duration: '1m', target: 0 },
    ],
    thresholds: {
        http_req_duration: ['p(95)<1000', 'p(99)<2000'],
        http_req_failed: ['rate<0.05'],
        error_rate: ['rate<0.1'],
        errors: ['count<100'],
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_PREFIX = BASE_URL + '/api';

export function setup() {
    console.log(`Starting stress test against ${BASE_URL}`);
    return { baseUrl: BASE_URL, apiPrefix: API_PREFIX };
}

export default function(data) {
    let testData = {
        customerId: Math.floor(Math.random() * 1000) + 1,
        resourceType: ['CPU', 'MEMORY', 'DISK', 'NETWORK'][Math.floor(Math.random() * 4)],
        resourceId: `res-${Math.floor(Math.random() * 10000)}`,
        region: ['us-east-1', 'us-west-2', 'eu-west-1'][Math.floor(Math.random() * 3)],
    };

    let payloads = [
        {
            method: 'GET',
            url: `${data.apiPrefix}/monitoring/metrics?customerId=${testData.customerId}`,
            params: { tags: { name: 'get_metrics' } },
        },
        {
            method: 'GET',
            url: `${data.apiPrefix}/monitoring/alerts?status=OPEN`,
            params: { tags: { name: 'get_alerts' } },
        },
        {
            method: 'GET',
            url: `${data.apiPrefix}/cost/calculations?customerId=${testData.customerId}`,
            params: { tags: { name: 'get_cost_calculations' } },
        },
        {
            method: 'GET',
            url: `${data.apiPrefix}/monitoring/customer-resource-configurations/customer/${testData.customerId}`,
            params: { tags: { name: 'get_configurations' } },
        },
    ];

    let responses = http.batch(payloads);

    responses.forEach((response, index) => {
        let success = check(response, {
            [`status is 200/201/404 for request ${index}`]: (r) =>
                [200, 201, 404].includes(r.status),
            [`response time < 1000ms for request ${index}`]: (r) => r.timings.duration < 1000,
            [`response has valid body for request ${index}`]: (r) => r.body !== null,
        });

        if (!success) {
            errors.add(1);
            errorRate.add(1);
        } else {
            errorRate.add(0);
        }
    });

    sleep(0.5);
}

export function teardown(data) {
    console.log('Stress test completed');
}
