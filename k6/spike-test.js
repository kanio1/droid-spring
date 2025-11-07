import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';

export let errors = new Counter('spike_errors');
export let errorRate = new Rate('spike_error_rate');

export let options = {
    stages: [
        { duration: '10s', target: 10 },
        { duration: '20s', target: 1000 },
        { duration: '30s', target: 1000 },
        { duration: '20s', target: 10 },
        { duration: '10s', target: 0 },
    ],
    thresholds: {
        http_req_duration: ['p(95)<2000'],
        http_req_failed: ['rate<0.1'],
        spike_error_rate: ['rate<0.2'],
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_PREFIX = BASE_URL + '/api';

export function setup() {
    console.log(`Starting spike test against ${BASE_URL}`);
    return { baseUrl: BASE_URL, apiPrefix: API_PREFIX };
}

export default function(data) {
    let customerId = Math.floor(Math.random() * 100) + 1;

    let responses = http.batch([
        ['GET', `${data.apiPrefix}/monitoring/metrics?customerId=${customerId}`],
        ['GET', `${data.apiPrefix}/monitoring/alerts`],
        ['GET', `${data.apiPrefix}/cost/calculations`],
        ['GET', `${data.apiPrefix}/monitoring/customer-resource-configurations/customer/${customerId}`],
        ['GET', `${data.apiPrefix}/monitoring/cost-forecasts`],
    ]);

    responses.forEach((response, index) => {
        let success = check(response, {
            [`spike test status check ${index}`]: (r) => r.status < 500,
            [`spike test response time < 2000ms ${index}`]: (r) => r.timings.duration < 2000,
        });

        if (!success) {
            errors.add(1);
            errorRate.add(1);
        } else {
            errorRate.add(0);
        }
    });

    sleep(0.1);
}

export function teardown(data) {
    console.log('Spike test completed');
}
