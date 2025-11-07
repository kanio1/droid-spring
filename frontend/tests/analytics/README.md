# Test Analytics Dashboard

Real-time test metrics, trends, and insights for the BSS application testing framework.

## Overview

The Test Analytics Dashboard provides comprehensive visualization and analysis of test execution data from multiple sources including Playwright, Allure, and k6. It offers real-time metrics, historical trends, and quality gates to help teams monitor test health and make data-driven decisions.

## Features

### 📊 Real-Time Metrics
- **Total Tests** - Count of all executed tests
- **Pass Rate** - Percentage of passing tests
- **Average Duration** - Mean test execution time
- **Flaky Tests** - Count of unstable tests

### 📈 Visual Analytics
- **Test Results Distribution** - Doughnut chart showing pass/fail/skip breakdown
- **Pass Rate Trend** - Line chart showing trends over 7 days
- **Severity Distribution** - Bar chart of tests by severity level
- **Suite Performance** - Bar chart comparing test suite pass rates

### 📋 Test Management
- **Recent Test Executions Table** - Filterable list of latest test runs
- **Test Suite Filter** - Filter by smoke, regression, security, resilience
- **Time Range Filter** - View data from last 7/14/30/90 days
- **Status Tracking** - Real-time status updates

### 🎯 Quality Gates
- **Pass Rate Threshold** - Minimum 90% pass rate
- **Duration Threshold** - Maximum 30s average duration
- **Critical Test Check** - Zero failed critical tests
- **Automated Quality Assessment** - Pass/Fail status for each gate

## Data Sources

### 1. Playwright Results
```json
{
  "suites": [
    {
      "title": "Customer Tests",
      "specs": [
        {
          "title": "customer.spec.ts",
          "tests": [
            {
              "title": "Can create customer",
              "results": [
                {
                  "status": "passed",
                  "duration": 2500,
                  "startTime": "2025-11-06T10:00:00Z"
                }
              ]
            }
          ]
        }
      ]
    }
  ]
}
```

### 2. Allure Results
```json
{
  "name": "Login Test",
  "status": "passed",
  "start": 1699267200000000,
  "stop": 1699267202500000,
  "labels": [
    { "name": "severity", "value": "critical" },
    { "name": "owner", "value": "QA Team" }
  ]
}
```

### 3. k6 Performance Results
```json
{
  "metrics": {
    "http_reqs": { "count": 1000 },
    "http_req_duration": {
      "avg": 150.5,
      "min": 50,
      "max": 500,
      "p(95)": 300
    },
    "http_req_failed": { "rate": 0.01 }
  }
}
```

## Architecture

```
Test Execution
      ↓
┌─────────────────┐
│  Test Runners   │
│  - Playwright   │
│  - Allure       │
│  - k6          │
└─────────────────┘
      ↓
┌─────────────────┐
│  Results        │
│  - results.json │
│  - allure-      │
│    results/     │
│  - k6-          │
│    results.json │
└─────────────────┘
      ↓
┌─────────────────┐
│  Metrics        │
│  Collection     │
│  collect-       │
│    test-        │
│    metrics.js   │
└─────────────────┘
      ↓
┌─────────────────┐
│  Analytics      │
│  Data           │
│  - test-        │
│    metrics.json │
│  - trends.json  │
│  - suite-       │
│    performance  │
│    .json        │
└─────────────────┘
      ↓
┌─────────────────┐
│  Dashboard      │
│  - HTML/JS      │
│  - Chart.js     │
│  - Real-time    │
└─────────────────┘
```

## Usage

### 1. Run Tests and Collect Metrics

```bash
# Run all test types
pnpm test

# Collect metrics
node tests/analytics/collect-test-metrics.js collect

# Or use npm script
pnpm test:metrics:collect
```

### 2. Start Analytics Dashboard

```bash
# Serve the dashboard locally
python3 -m http.server 8080 --directory tests/analytics

# Open in browser
open http://localhost:8080/test-analytics-dashboard.html
```

### 3. Automated Collection (CI/CD)

```bash
# Collect from CI results
RESULTS_DIR=./test-results \
EXPORT_DIR=./tests/analytics/data \
node tests/analytics/collect-test-metrics.js collect
```

## Configuration

### Environment Variables

```bash
# Base URL for application
BASE_URL=http://localhost:3000

# Results directory (default: ./results)
RESULTS_DIR=./results

# Export directory (default: ./tests/analytics/data)
EXPORT_DIR=./tests/analytics/data
```

### Customization

**Update Refresh Interval:**
```javascript
// In test-analytics-dashboard.html
setInterval(refreshDashboard, 5 * 60 * 1000); // 5 minutes
```

**Add New Metrics:**
```javascript
// In collect-test-metrics.js
function collectCustomMetrics() {
  // Your custom metric collection logic
  return metrics
}
```

**Add New Charts:**
```javascript
// In test-analytics-dashboard.html
function updateCustomChart() {
  const ctx = document.getElementById('customChart').getContext('2d')
  new Chart(ctx, {
    // Chart configuration
  })
}
```

## File Structure

```
tests/analytics/
├── README.md                          # This file
├── test-analytics-dashboard.html      # Main dashboard UI
├── collect-test-metrics.js            # Metrics collection script
├── data/                              # Generated analytics data
│   ├── test-metrics.json              # Summary metrics
│   ├── raw-metrics.json               # Raw test data
│   ├── trends.json                    # Historical trends
│   └── suite-performance.json         # Test suite performance
└── public/                            # Static assets
    ├── css/
    └── js/
```

## Metrics Explained

### Test Metrics

| Metric | Description | Formula |
|--------|-------------|---------|
| Total Tests | All executed tests | count(all tests) |
| Pass Rate | Percentage of passing tests | (passed / total) * 100 |
| Avg Duration | Mean execution time | sum(duration) / count |
| Flaky Tests | Tests with inconsistent results | count(failed_then_passed) |

### Quality Gates

| Gate | Threshold | Purpose |
|------|-----------|---------|
| Pass Rate | ≥ 90% | Ensure high test reliability |
| Duration | ≤ 30s | Maintain test performance |
| Critical Tests | 0 failures | Protect critical functionality |

### Trend Analysis

| Trend | Window | Purpose |
|-------|--------|---------|
| Pass Rate | 7 days | Detect quality regression |
| Duration | 7 days | Monitor performance degradation |
| Test Count | 7 days | Track test suite growth |

## Dashboard Components

### 1. Statistics Cards
```html
<div class="stat-card">
  <h3>Total Tests</h3>
  <div class="value" id="totalTests">0</div>
  <div class="change" id="totalTestsChange">-</div>
</div>
```

### 2. Chart Cards
```html
<div class="chart-card">
  <h2>Test Results Distribution</h2>
  <div class="chart-container">
    <canvas id="resultsChart"></canvas>
  </div>
</div>
```

### 3. Filter Bar
```html
<div class="filter-bar">
  <select id="testSuiteFilter">
    <option value="all">All Test Suites</option>
  </select>
  <button class="refresh-btn" onclick="refreshDashboard()">🔄 Refresh</button>
</div>
```

### 4. Data Table
```html
<table>
  <thead>
    <tr>
      <th>Test Name</th>
      <th>Suite</th>
      <th>Status</th>
      <th>Duration</th>
      <th>Severity</th>
      <th>Owner</th>
      <th>Date</th>
    </tr>
  </thead>
  <tbody id="recentTests">
    <!-- Test data rows -->
  </tbody>
</table>
```

## CI/CD Integration

### GitHub Actions

```yaml
name: Test Analytics

on:
  push:
    branches: [main]
  schedule:
    - cron: '0 */6 * * *' # Every 6 hours

jobs:
  collect-metrics:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Run tests
        run: pnpm test

      - name: Collect metrics
        run: node tests/analytics/collect-test-metrics.js collect

      - name: Upload analytics data
        uses: actions/upload-artifact@v3
        with:
          name: test-analytics
          path: tests/analytics/data/

      - name: Deploy dashboard
        if: github.ref == 'refs/heads/main'
        run: |
          # Deploy to GitHub Pages or internal server
```

### Jenkins Pipeline

```groovy
pipeline {
  agent any

  stages {
    stage('Test') {
      steps {
        sh 'pnpm test'
      }
    }

    stage('Analytics') {
      steps {
        sh 'node tests/analytics/collect-test-metrics.js collect'
      }
    }

    stage('Publish Dashboard') {
      steps {
        publishHTML([
          allowMissing: false,
          alwaysLinkToLastBuild: true,
          keepAll: true,
          reportDir: 'tests/analytics',
          reportFiles: 'test-analytics-dashboard.html',
          reportName: 'Test Analytics'
        ])
      }
    }
  }
}
```

## Advanced Features

### 1. Custom Metrics

Add custom metrics to your tests:

```javascript
// In your test file
test('Custom metric example', async ({ page }) => {
  // Your test logic
  const startTime = Date.now()

  await page.goto('/dashboard')
  await page.waitForSelector('.dashboard-card')

  const duration = Date.now() - startTime

  // Log custom metric
  console.log(JSON.stringify({
    type: 'custom_metric',
    name: 'dashboard_load_time',
    value: duration,
    timestamp: new Date().toISOString()
  }))
})
```

Collect in metrics script:

```javascript
function collectCustomMetrics() {
  // Parse custom metrics from logs
  return customMetrics
}
```

### 2. Historical Comparison

Compare current run with previous:

```javascript
function compareWithHistory(currentMetrics, historicalMetrics) {
  return {
    passRateChange: currentMetrics.passRate - historicalMetrics.passRate,
    durationChange: currentMetrics.avgDuration - historicalMetrics.avgDuration,
    testCountChange: currentMetrics.total - historicalMetrics.total
  }
}
```

### 3. Alerting

Set up alerts for quality gate failures:

```javascript
function checkAlerts(metrics, qualityGates) {
  const alerts = []

  if (qualityGates.passRate.status === 'fail') {
    alerts.push({
      type: 'critical',
      message: `Pass rate ${metrics.passRate}% below threshold 90%`
    })
  }

  if (qualityGates.avgDuration.status === 'fail') {
    alerts.push({
      type: 'warning',
      message: `Average duration ${metrics.avgDuration}s exceeds 30s`
    })
  }

  return alerts
}
```

### 4. Export Data

Export analytics data for external systems:

```javascript
function exportToCSV(metrics) {
  const csv = [
    'Test Name,Status,Duration,Date',
    ...metrics.map(m => `${m.name},${m.status},${m.duration},${m.timestamp}`)
  ].join('\n')

  return csv
}

function exportToJSON(metrics) {
  return JSON.stringify(metrics, null, 2)
}
```

## Performance Optimization

### 1. Data Compression
```javascript
// Compress large datasets
const compressed = LZString.compress(JSON.stringify(metrics))
```

### 2. Caching
```javascript
// Cache results for 5 minutes
const cache = new Map()
const CACHE_TTL = 5 * 60 * 1000

function getCachedMetrics(key) {
  const cached = cache.get(key)
  if (cached && Date.now() - cached.timestamp < CACHE_TTL) {
    return cached.data
  }
  return null
}
```

### 3. Lazy Loading
```javascript
// Load chart data on demand
function loadChartData(chartType) {
  return import(`./charts/${chartType}.js`)
}
```

## Troubleshooting

### Dashboard Not Loading

**Problem:** Dashboard shows blank page

**Solution:**
1. Check browser console for errors
2. Verify Chart.js is loaded
3. Ensure data files exist in `data/` directory

```bash
# Check data files
ls -la tests/analytics/data/

# Verify JSON is valid
cat tests/analytics/data/test-metrics.json | jq .
```

### No Metrics Data

**Problem:** "Loading..." message persists

**Solution:**
1. Run metrics collection
2. Check file permissions
3. Verify export directory exists

```bash
# Collect metrics
node tests/analytics/collect-test-metrics.js collect

# Check permissions
chmod 644 tests/analytics/data/*.json

# Ensure directory exists
mkdir -p tests/analytics/data
```

### Charts Not Rendering

**Problem:** Charts show "No data available"

**Solution:**
1. Check data format matches expected schema
2. Verify Chart.js version compatibility
3. Check for JavaScript errors

```javascript
// Debug data
console.log('Raw metrics:', metrics)
console.log('Chart data:', chartData)
```

### Slow Performance

**Problem:** Dashboard loads slowly

**Solution:**
1. Reduce data retention window
2. Implement pagination
3. Use data compression

```javascript
// Reduce data points
const recentData = data.slice(0, 100) // Last 100 records
```

## Best Practices

### 1. Data Collection
- Collect metrics immediately after test execution
- Validate data format before export
- Use consistent naming conventions
- Include timestamps for all metrics

### 2. Dashboard Usage
- Refresh dashboard regularly for real-time data
- Use filters to focus on relevant data
- Monitor quality gates for automated quality checks
- Set up alerts for critical metrics

### 3. Test Organization
- Tag tests with severity levels
- Group related tests into suites
- Document test purposes and expected outcomes
- Track ownership for test maintenance

### 4. Performance
- Keep test execution time under 30s average
- Optimize slow tests
- Run tests in parallel when possible
- Use proper setup/teardown for isolation

### 5. Monitoring
- Track pass rate trends over time
- Monitor for flaky test increases
- Watch for duration performance degradation
- Alert on quality gate failures

## API Reference

### collect-test-metrics.js

#### `collectPlaywrightMetrics()`
Collects test results from Playwright test results.

**Returns:** `Array<TestMetric>`

#### `collectAllureMetrics()`
Collects test results from Allure results directory.

**Returns:** `Array<TestMetric>`

#### `collectK6Metrics()`
Collects performance metrics from k6 results.

**Returns:** `Array<PerformanceMetric>`

#### `collectTrends()`
Generates historical trend data.

**Returns:** `Trends`

#### `collectQualityGates(metrics, trends)`
Calculates quality gate status.

**Parameters:**
- `metrics: Array<TestMetric>` - Test execution metrics
- `Trends` - Historical trend data

**Returns:** `QualityGates`

### Dashboard Data Format

#### TestMetric
```typescript
interface TestMetric {
  name: string
  suite: string
  status: 'passed' | 'failed' | 'skipped'
  duration: number
  timestamp: string
  severity?: string
  owner?: string
  tags?: string[]
  steps?: number
  attachments?: number
}
```

#### QualityGates
```typescript
interface QualityGates {
  passRate: GateStatus
  avgDuration: GateStatus
  criticalTests: GateStatus
}

interface GateStatus {
  value: number | string
  threshold: number
  status: 'pass' | 'fail'
}
```

## Examples

### 1. Daily Test Report

Generate daily summary:

```bash
#!/bin/bash
# daily-report.sh

DATE=$(date +%Y-%m-%d)
RESULTS_DIR="./results/${DATE}"

# Run tests
pnpm test --output-dir="${RESULTS_DIR}"

# Collect metrics
RESULTS_DIR="${RESULTS_DIR}" \
EXPORT_DIR="./analytics/data/${DATE}" \
node tests/analytics/collect-test-metrics.js collect

# Generate report
echo "Daily report generated for ${DATE}"
```

### 2. Quality Gate Check

Fail build on quality gate failure:

```bash
#!/bin/bash
# quality-check.sh

node tests/analytics/collect-test-metrics.js collect

# Read pass rate
PASS_RATE=$(jq -r '.qualityGates.passRate.status' tests/analytics/data/test-metrics.json)

if [ "$PASS_RATE" = "fail" ]; then
  echo "❌ Quality gate failed: Pass rate below 90%"
  exit 1
else
  echo "✅ All quality gates passed"
  exit 0
fi
```

### 3. Historical Analysis

Compare with last week's data:

```javascript
// historical-analysis.js
const currentData = require('./data/test-metrics.json')
const lastWeekData = require('./data/2025-10-30/test-metrics.json')

const comparison = {
  passRateChange: currentData.overview.passRate - lastWeekData.overview.passRate,
  testCountChange: currentData.overview.total - lastWeekData.overview.total,
  avgDurationChange: currentData.qualityGates.avgDuration.value - lastWeekData.qualityGates.avgDuration.value
}

console.log('Week-over-week changes:', comparison)
```

## Security Considerations

### 1. Data Sanitization
- Remove sensitive data from test results
- Sanitize test names and descriptions
- Avoid logging credentials or PII

### 2. Access Control
- Restrict dashboard access to authorized users
- Use authentication for CI/CD dashboards
- Protect exported data files

### 3. Data Retention
- Implement data retention policies
- Clean up old analytics data
- Compress historical data

## Future Enhancements

### Planned Features

1. **Machine Learning Predictions**
   - Predict test failures
   - Identify flaky test patterns
   - Recommend test optimizations

2. **Advanced Filtering**
   - Multi-dimensional filters
   - Custom filter saved searches
   - Dynamic filter builder

3. **Team Dashboards**
   - Team-specific metrics
   - Ownership tracking
   - Performance leaderboards

4. **Integration Expansion**
   - Jira integration
   - Slack/Teams notifications
   - Custom webhook support

5. **Mobile Support**
   - Responsive dashboard design
   - Mobile app for metrics
   - Offline data viewing

### Contributing

To add new features:

1. Fork the repository
2. Create feature branch
3. Add tests for new functionality
4. Update documentation
5. Submit pull request

## Support

For issues or questions:

1. Check troubleshooting section
2. Review test logs
3. Verify configuration
4. Create GitHub issue

## License

Internal BSS Application Testing Framework

---

**Last Updated:** 2025-11-06
**Version:** 1.0.0
**Maintainer:** QA Team
