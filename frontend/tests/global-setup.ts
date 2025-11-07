/**
 * Global Setup for Playwright Tests
 *
 * Initializes test environment before all tests
 * - Starts Keycloak for OIDC authentication testing
 * - Starts Redis for caching and session testing
 * - Sets up test data
 *
 * Run once per test session
 */

import { FullConfig } from '@playwright/test'
import { KeycloakContainer } from './framework/testcontainers/keycloak'
import { RedisContainer } from './framework/testcontainers/redis'

async function globalSetup(config: FullConfig) {
  console.log('🚀 Starting global test environment setup...')

  // Start Keycloak container for OIDC authentication
  console.log('📦 Starting Keycloak container...')
  const keycloakContainer = await KeycloakContainer.start({
    importRealm: {
      realm: 'bss-test',
      users: [
        {
          username: 'testuser',
          password: 'testpass',
          email: 'testuser@example.com',
          firstName: 'Test',
          lastName: 'User',
          enabled: true,
          emailVerified: true,
          realmRoles: ['user']
        },
        {
          username: 'admin',
          password: 'admin',
          email: 'admin@example.com',
          firstName: 'Admin',
          lastName: 'User',
          enabled: true,
          emailVerified: true,
          realmRoles: ['admin', 'user']
        }
      ],
      clients: [
        {
          clientId: 'bss-frontend',
          publicClient: false,
          directAccessGrantsEnabled: true,
          standardFlowEnabled: true,
          redirectUris: ['http://localhost:3000/*'],
          webOrigins: ['http://localhost:3000'],
          scopes: ['openid', 'profile', 'email']
        }
      ],
      roles: ['admin', 'user', 'customer', 'manager']
    }
  })

  // Start Redis container for caching and session testing
  console.log('📦 Starting Redis container...')
  const redisContainer = await RedisContainer.start({
    port: 6379,
    config: {
      appendonly: true,
      requirepass: 'testpass'
    }
  })

  // Store container URLs in environment variables for tests
  process.env.KEYCLOAK_URL = keycloakContainer.getRealmUrl('bss-test')
  process.env.KEYCLOAK_ADMIN_URL = keycloakContainer.getAdminUrl()
  process.env.KEYCLOAK_CLIENT_ID = 'bss-frontend'

  process.env.REDIS_URL = redisContainer.getUri()
  process.env.REDIS_HOST = redisContainer.getHost()
  process.env.REDIS_PORT = redisContainer.getPort().toString()

  console.log('✅ Global setup completed')
  console.log(`   Keycloak URL: ${process.env.KEYCLOAK_URL}`)
  console.log(`   Redis URL: ${process.env.REDIS_URL}`)
  console.log('✅ Test environment ready')

  return async () => {
    console.log('🧹 Cleaning up global test environment...')
    // Cleanup is handled by global-teardown.ts
  }
}

export default globalSetup
