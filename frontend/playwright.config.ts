import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './tests/e2e',
  outputDir: './test-results',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 1,
  workers: process.env.CI ? 1 : undefined,
  timeout: 30000,
  expect: {
    timeout: 10000,
    toHaveScreenshot: {
      maxDiffPixelRatio: 0.01,
    },
  },
  reporter: [
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
    ['json', { outputFile: 'test-results/results.json' }],
    ['junit', { outputFile: 'test-results/results.xml' }],
    ['blob', { outputFile: 'test-results/blob-report' }],
    ['list'],
    process.env.CI ? ['github'] : [],
    process.env.ALLURE_RESULTS_PATH ? ['allure-playwright', {
      outputFolder: process.env.ALLURE_RESULTS_PATH,
      detail: true,
      quiet: false
    }] : []
  ].filter(Boolean),
  use: {
    baseURL: 'http://localhost:3000',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    actionTimeout: 10000,
    navigationTimeout: 15000,
    headless: true,
  },
  // UI Mode configuration
  ui: {
    enabled: true,
    port: 9323,
    host: 'localhost',
  },
  // Test sharding for CI
  shard: process.env.PW_SHARD ? {
    current: parseInt(process.env.PW_SHARD.split('/')[0]),
    total: parseInt(process.env.PW_SHARD.split('/')[1])
  } : undefined,
  projects: [
    // Smoke Tests - Fast running critical path tests
    {
      name: 'smoke',
      testDir: './tests/e2e/smoke',
      use: { ...devices['Desktop Chrome'] },
      timeout: 60000,
      retries: 0, // No retries for smoke tests - should pass every time
    },

    // Desktop Browsers
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
    },
    {
      name: 'edge',
      use: { ...devices['Desktop Edge'] },
    },

    // Mobile Browsers
    {
      name: 'mobile-chrome',
      use: { ...devices['Pixel 5'] },
    },
    {
      name: 'mobile-safari',
      use: { ...devices['iPhone 12'] },
    },

    // Tablet
    {
      name: 'ipad',
      use: { ...devices['iPad Pro'] },
    },

    // Regression Tests - Comprehensive test suite
    {
      name: 'regression',
      testDir: './tests/e2e/regression',
      use: { ...devices['Desktop Chrome'] },
      timeout: 60000,
      retries: 1,
    },

    // Security Tests - Vulnerability and security validation
    {
      name: 'security',
      testDir: './tests/security',
      testMatch: /.*\.spec\.ts/,
      use: { ...devices['Desktop Chrome'] },
      timeout: 300000, // 5 minutes for security scans
      retries: 0, // No retries for security tests
    },

    // Resilience Tests - Chaos engineering and fault tolerance
    {
      name: 'resilience',
      testDir: './tests/resilience',
      testMatch: /.*\.spec\.ts/,
      use: { ...devices['Desktop Chrome'] },
      timeout: 120000, // 2 minutes for resilience tests
      retries: 0, // No retries for resilience tests
    },

    // Visual Regression Tests - Percy-based visual testing
    {
      name: 'visual',
      testDir: './tests/visual',
      use: { ...devices['Desktop Chrome'] },
      timeout: 60000,
      retries: 0, // No retries for visual tests
    },

    // API Testing - GraphQL, REST, Contract Testing
    {
      name: 'api',
      testDir: './tests/examples',
      testMatch: /api-advanced\.spec\.ts/,
      use: { ...devices['Desktop Chrome'] },
      timeout: 60000,
      retries: 1,
    },

    // Network Testing - Offline, throttling, request modification
    {
      name: 'network',
      testDir: './tests/examples',
      testMatch: /network-advanced\.spec\.ts/,
      use: { ...devices['Desktop Chrome'] },
      timeout: 60000,
      retries: 0,
    },

    // Security Testing - Advanced security scenarios
    {
      name: 'security-advanced',
      testDir: './tests/examples',
      testMatch: /security-advanced\.spec\.ts/,
      use: { ...devices['Desktop Chrome'] },
      timeout: 120000, // 2 minutes for complex security tests
      retries: 0,
    },

    // AI/ML Testing - OCR, search, recommendations
    {
      name: 'ai-ml',
      testDir: './tests/examples',
      testMatch: /ai-ml\.spec\.ts/,
      use: { ...devices['Desktop Chrome'] },
      timeout: 90000,
      retries: 0,
    },
  ],
  webServer: {
    command: 'pnpm run dev',
    url: 'http://localhost:3000',
    reuseExistingServer: !process.env.CI,
    timeout: 120000,
  },
  globalSetup: require.resolve('./tests/global-setup.ts'),
  globalTeardown: require.resolve('./tests/global-teardown.ts'),
})
