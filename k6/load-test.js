import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

export let errorRate = new Rate('errors');

export let options = {
    stages: [
        { duration: '2m', target: 100 },
        { duration: '5m', target: 100 },
        { duration: '2m', target: 200 },
        { duration: '5m', target: 200 },
        { duration: '2m', target: 0 },
    ],
    thresholds: {
        http_req_duration: ['p(95)<500', 'p(99)<1000'],
        http_req_failed: ['rate<0.01'],
        errors: ['rate<0.05'],
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_PREFIX = BASE_URL + '/api';

export function setup() {
    console.log(`Starting load test against ${BASE_URL}`);
    return { baseUrl: BASE_URL, apiPrefix: API_PREFIX };
}

export default function(data) {
    let responses = http.batch([
        ['GET', `${data.apiPrefix}/monitoring/metrics`],
        ['GET', `${data.apiPrefix}/monitoring/alerts`],
        ['GET', `${data.apiPrefix}/cost/calculations`],
    ]);

    responses.forEach((response, index) => {
        check(response, {
            [`status is 200 for endpoint ${index}`]: (r) => r.status === 200,
            [`response time < 500ms for endpoint ${index}`]: (r) => r.timings.duration < 500,
        }) || errorRate.add(1);
    });

    sleep(1);
}

export function teardown(data) {
    console.log('Load test completed');
}
