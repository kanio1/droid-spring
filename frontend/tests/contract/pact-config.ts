/**
 * Pact Configuration
 *
 * Central configuration for contract testing
 * Defines pact file locations, provider/consumer names, and broker settings
 */

import { Pact } from '@pact-foundation/pact'
import path from 'path'

export const PACT_CONFIG = {
  // Consumer (Frontend) Configuration
  consumer: {
    name: 'bss-frontend'
  },

  // Provider (Backend) Configuration
  provider: {
    name: 'bss-backend'
  },

  // Pact File Configuration
  pactFile: {
    dir: path.resolve(process.cwd(), 'tests/contract/pacts'),
    fileName: '${REPLACED_AT_RUNTIME}.json'
  },

  // Pact Broker Configuration
  broker: {
    url: process.env.PACT_BROKER_URL || 'http://localhost:9292',
    username: process.env.PACT_BROKER_USERNAME,
    password: process.env.PACT_BROKER_PASSWORD,
    enablePending: true,
    includeWipPactsSince: '2025-01-01',
    autoPublish: process.env.CI === 'true',
    branch: process.env.GITHUB_REF_NAME || 'main',
    buildUrl: process.env.GITHUB_RUN_URL || 'http://localhost:3000'
  },

  // Test Configuration
  test: {
    timeout: 30000,
    logLevel: 'INFO' as const,
    port: 0 // Random port for provider
  },

  // Request Filtering
  requestFilter: {
    // Add headers for all requests (auth tokens, etc.)
    requestHeaders: {
      'Content-Type': 'application/json'
    }
  }
}

/**
 * Create a new Pact consumer instance
 */
export function createConsumerPact(consumerName: string, providerName: string): Pact {
  return new Pact({
    consumer: consumerName,
    provider: providerName,
    port: PACT_CONFIG.test.port,
    log: path.resolve(process.cwd(), 'tests/contract/logs', `${consumerName}-${providerName}.log`),
    dir: PACT_CONFIG.pactFile.dir,
    logLevel: PACT_CONFIG.test.logLevel,
    spec: 2
  })
}

/**
 * Create a new Pact provider instance
 */
export function createProviderPact(providerName: string): Pact {
  return new Pact({
    provider: providerName,
    port: PACT_CONFIG.test.port,
    log: path.resolve(process.cwd(), 'tests/contract/logs', `${providerName}.log`),
    dir: PACT_CONFIG.pactFile.dir,
    logLevel: PACT_CONFIG.test.logLevel,
    spec: 2
  })
}

/**
 * Get pact file path
 */
export function getPactFilePath(consumer: string, provider: string): string {
  return path.resolve(
    PACT_CONFIG.pactFile.dir,
    `${consumer}-${provider}.json`
  )
}

/**
 * Verify environment
 */
export function validateEnvironment(): { valid: boolean; errors: string[] } {
  const errors: string[] = []

  // Check if PACT_BROKER_URL is set for publishing
  if (process.env.CI === 'true' && !process.env.PACT_BROKER_URL) {
    errors.push('PACT_BROKER_URL must be set in CI environment')
  }

  // Check if credentials are set
  if (process.env.PACT_BROKER_URL && (!process.env.PACT_BROKER_USERNAME || !process.env.PACT_BROKER_PASSWORD)) {
    errors.push('PACT_BROKER_USERNAME and PACT_BROKER_PASSWORD must be set when PACT_BROKER_URL is provided')
  }

  return {
    valid: errors.length === 0,
    errors
  }
}
