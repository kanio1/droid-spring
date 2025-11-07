/**
 * Global Teardown for Playwright Tests
 *
 * Cleans up test environment after all tests complete
 * - Stops Keycloak container
 * - Stops Redis container
 * - Removes any test artifacts
 *
 * Run once per test session
 */

import { FullConfig } from '@playwright/test'
import { KeycloakContainer } from './framework/testcontainers/keycloak'
import { RedisContainer } from './framework/testcontainers/redis'
import * as fs from 'fs'
import * as path from 'path'

async function globalTeardown(config: FullConfig) {
  console.log('🧹 Starting global test environment teardown...')

  try {
    // Stop Keycloak container
    console.log('📦 Stopping Keycloak container...')
    await KeycloakContainer.stop()
    console.log('✅ Keycloak container stopped')

    // Stop Redis container
    console.log('📦 Stopping Redis container...')
    await RedisContainer.stop()
    console.log('✅ Redis container stopped')

    // Clean up test results directory if in CI
    if (process.env.CI) {
      const testResultsDir = path.join(process.cwd(), 'test-results')
      if (fs.existsSync(testResultsDir)) {
        console.log('🧹 Cleaning up test results...')
        // Keep test results for analysis but could archive them
        console.log('📊 Test results preserved for CI reporting')
      }
    }

    console.log('✅ Global teardown completed successfully')

  } catch (error) {
    console.error('❌ Error during global teardown:', error)
    throw error
  }
}

export default globalTeardown
