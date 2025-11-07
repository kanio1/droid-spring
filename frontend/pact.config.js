/**
 * Pact Configuration for Contract Testing
 *
 * This file contains the configuration for the Pact framework,
 * including broker settings, consumer/provider info, and test settings
 */

const { Pact } = require('@pact-foundation/pact')

// Consumer configuration
const consumer = {
  name: 'BSS Frontend',
  version: process.env.npm_package_version || '1.0.0'
}

// Provider configuration
const provider = {
  name: 'BSS Backend API',
  version: process.env.GITHUB_SHA || process.env.GIT_COMMIT || 'dev'
}

// Pact broker configuration
const broker = {
  baseUrl: process.env.PACT_BROKER_BASE_URL || 'http://localhost:9292',
  token: process.env.PACT_BROKER_TOKEN,
  enablePending: true,
  includeWipPactsSince: '2025-01-01',
  consumerVersionSelectors: [
    { tag: 'main', latest: true },
    { tag: 'develop', latest: true },
    { tag: 'staging', latest: true }
  ],
  providerVersionBranch: process.env.GITHUB_REF_NAME || 'main',
  publishVersion: process.env.CI ? consumer.version : undefined,
  autoDetectVersionProperties: true
}

// Test configuration
const testConfig = {
  host: 'localhost',
  port: 1234,
  log: 'logs/pact.log',
  dir: 'pacts',
  logLevel: 'info',
  timeout: 60000,
  cors: false
}

// Environment validation
function validateConfig() {
  const required = []
  const missing = required.filter((key) => !process.env[key])

  if (missing.length > 0) {
    console.warn(`Warning: Missing environment variables: ${missing.join(', ')}`)
  }

  return {
    isValid: missing.length === 0,
    missing
  }
}

// Export configuration
module.exports = {
  consumer,
  provider,
  broker,
  testConfig,
  validateConfig,

  // Factory function to create new pact instance
  createPact(consumerName = consumer.name, providerName = provider.name) {
    return new Pact({
      ...testConfig,
      consumer: consumerName,
      provider: providerName
    })
  }
}
