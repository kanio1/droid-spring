/**
 * Keycloak Testcontainers Integration
 *
 * Provides Keycloak container for OIDC authentication testing
 * Supports realm import, user creation, and client configuration
 *
 * Usage:
 * ```typescript
 * const keycloak = await KeycloakContainer.start()
 * await keycloak.importRealm(realmConfig)
 * const token = await keycloak.getAccessToken('username', 'password')
 * ```
 */

import { GenericContainer, type StartedTestContainer } from 'testcontainers'

export interface KeycloakUser {
  username: string
  password: string
  email?: string
  firstName?: string
  lastName?: string
  enabled?: boolean
  emailVerified?: boolean
  realmRoles?: string[]
  clientRoles?: Record<string, string[]>
}

export interface KeycloakClient {
  clientId: string
  secret?: string
  publicClient?: boolean
  directAccessGrantsEnabled?: boolean
  standardFlowEnabled?: boolean
  redirectUris?: string[]
  webOrigins?: string[]
  scopes?: string[]
}

export interface KeycloakRealmConfig {
  realm: string
  users?: KeycloakUser[]
  clients?: KeycloakClient[]
  roles?: string[]
}

export class KeycloakTestContainer {
  private container: StartedTestContainer | null = null
  private realmUrl: string = ''
  private adminUrl: string = ''

  /**
   * Start Keycloak container
   */
  async start(options: {
    adminUsername?: string
    adminPassword?: string
    importRealm?: KeycloakRealmConfig
    extraEnv?: Record<string, string>
  } = {}): Promise<KeycloakTestContainer> {
    const {
      adminUsername = 'admin',
      adminPassword = 'admin',
      extraEnv = {}
    } = options

    const env: Record<string, string> = {
      KEYCLOAK_ADMIN: adminUsername,
      KEYCLOAK_ADMIN_PASSWORD: adminPassword,
      KC_HTTP_RELATIVE_PATH: '/',
      ...extraEnv
    }

    this.container = await GenericContainer
      .fromImage('quay.io/keycloak/keycloak:26.0')
      .withExposedPorts(8080)
      .withEnvironment(env)
      .withCommand(['start-dev'])
      .withStartupTimeout(120000)
      .start()

    const host = this.container.getHost()
    const port = this.container.getMappedPort(8080)

    this.realmUrl = `http://${host}:${port}`
    this.adminUrl = `${this.realmUrl}/admin`

    // Wait for Keycloak to be ready
    await this.waitForReady()

    // Import realm if provided
    if (options.importRealm) {
      await this.importRealm(options.importRealm, adminUsername, adminPassword)
    }

    console.log(`✅ Keycloak container started at ${this.realmUrl}`)

    return this
  }

  /**
   * Wait for Keycloak to be ready
   */
  private async waitForReady(): Promise<void> {
    if (!this.container) return

    const maxAttempts = 60
    const delay = 2000

    for (let i = 0; i < maxAttempts; i++) {
      try {
        const response = await fetch(`${this.realmUrl}/realms/master`)
        if (response.ok) {
          return
        }
      } catch (error) {
        if (i === maxAttempts - 1) {
          throw new Error(`Keycloak container failed to start: ${error}`)
        }
        await new Promise(resolve => setTimeout(resolve, delay))
      }
    }
  }

  /**
   * Import realm configuration
   */
  async importRealm(
    config: KeycloakRealmConfig,
    adminUsername: string = 'admin',
    adminPassword: string = 'admin'
  ): Promise<void> {
    // Get admin access token
    const token = await this.getAdminToken(adminUsername, adminPassword)

    // Create realm
    await fetch(`${this.adminUrl}/realms`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        realm: config.realm,
        enabled: true
      })
    })

    // Create roles
    if (config.roles) {
      for (const role of config.roles) {
        await this.createRole(config.realm, role, token)
      }
    }

    // Create clients
    if (config.clients) {
      for (const client of config.clients) {
        await this.createClient(config.realm, client, token)
      }
    }

    // Create users
    if (config.users) {
      for (const user of config.users) {
        await this.createUser(config.realm, user, token)
      }
    }

    console.log(`✅ Realm "${config.realm}" imported successfully`)
  }

  /**
   * Get admin access token
   */
  private async getAdminToken(
    adminUsername: string,
    adminPassword: string
  ): Promise<string> {
    const response = await fetch(`${this.realmUrl}/realms/master/protocol/openid-connect/token`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      body: new URLSearchParams({
        client_id: 'admin-cli',
        username: adminUsername,
        password: adminPassword,
        grant_type: 'password'
      })
    })

    if (!response.ok) {
      throw new Error(`Failed to get admin token: ${response.statusText}`)
    }

    const data = await response.json()
    return data.access_token
  }

  /**
   * Create role in realm
   */
  private async createRole(realm: string, roleName: string, token: string): Promise<void> {
    await fetch(`${this.adminUrl}/realms/${realm}/roles`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        name: roleName
      })
    })
  }

  /**
   * Create client in realm
   */
  private async createClient(
    realm: string,
    client: KeycloakClient,
    token: string
  ): Promise<void> {
    const clientConfig = {
      clientId: client.clientId,
      publicClient: client.publicClient || false,
      secret: client.secret || undefined,
      directAccessGrantsEnabled: client.directAccessGrantsEnabled || true,
      standardFlowEnabled: client.standardFlowEnabled !== false,
      redirectUris: client.redirectUris || [],
      webOrigins: client.webOrigins || [],
      scope: client.scopes?.join(' ') || 'openid profile email'
    }

    const response = await fetch(`${this.adminUrl}/realms/${realm}/clients`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(clientConfig)
    })

    if (!response.ok) {
      throw new Error(`Failed to create client: ${response.statusText}`)
    }

    console.log(`✅ Client "${client.clientId}" created`)
  }

  /**
   * Create user in realm
   */
  private async createUser(
    realm: string,
    user: KeycloakUser,
    token: string
  ): Promise<void> {
    const userData = {
      username: user.username,
      email: user.email || `${user.username}@example.test`,
      firstName: user.firstName || user.username,
      lastName: user.lastName || 'User',
      enabled: user.enabled !== false,
      emailVerified: user.emailVerified !== false,
      credentials: [
        {
          type: 'password',
          value: user.password,
          temporary: false
        }
      ]
    }

    const response = await fetch(`${this.adminUrl}/realms/${realm}/users`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(userData)
    })

    if (!response.ok) {
      throw new Error(`Failed to create user: ${response.statusText}`)
    }

    // Get user ID
    const userId = await this.getUserId(realm, user.username, token)

    // Add realm roles
    if (user.realmRoles?.length) {
      await this.addRealmRoles(realm, userId, user.realmRoles, token)
    }

    // Add client roles
    if (user.clientRoles) {
      for (const [clientId, roles] of Object.entries(user.clientRoles)) {
        await this.addClientRoles(realm, userId, clientId, roles, token)
      }
    }

    console.log(`✅ User "${user.username}" created`)
  }

  /**
   * Get user ID by username
   */
  private async getUserId(
    realm: string,
    username: string,
    token: string
  ): Promise<string> {
    const response = await fetch(
      `${this.adminUrl}/realms/${realm}/users?username=${encodeURIComponent(username)}`,
      {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      }
    )

    if (!response.ok) {
      throw new Error(`Failed to get user: ${response.statusText}`)
    }

    const users = await response.json()
    if (!users.length) {
      throw new Error(`User "${username}" not found`)
    }

    return users[0].id
  }

  /**
   * Add realm roles to user
   */
  private async addRealmRoles(
    realm: string,
    userId: string,
    roles: string[],
    token: string
  ): Promise<void> {
    const roleData = roles.map(role => ({ name: role }))

    await fetch(`${this.adminUrl}/realms/${realm}/users/${userId}/role-mappings/realm`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(roleData)
    })
  }

  /**
   * Add client roles to user
   */
  private async addClientRoles(
    realm: string,
    userId: string,
    clientId: string,
    roles: string[],
    token: string
  ): Promise<void> {
    const roleData = roles.map(role => ({ name: role }))

    await fetch(
      `${this.adminUrl}/realms/${realm}/users/${userId}/role-mappings/clients/${clientId}`,
      {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(roleData)
      }
    )
  }

  /**
   * Get access token for user
   */
  async getAccessToken(
    realm: string,
    clientId: string,
    username: string,
    password: string,
    clientSecret?: string
  ): Promise<string> {
    const body = new URLSearchParams({
      client_id: clientId,
      username,
      password,
      grant_type: 'password'
    })

    if (clientSecret) {
      body.append('client_secret', clientSecret)
    }

    const response = await fetch(`${this.realmUrl}/realms/${realm}/protocol/openid-connect/token`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      body
    })

    if (!response.ok) {
      throw new Error(`Failed to get access token: ${response.statusText}`)
    }

    const data = await response.json()
    return data.access_token
  }

  /**
   * Get realm URL
   */
  getRealmUrl(realm: string = 'master'): string {
    return `${this.realmUrl}/realms/${realm}`
  }

  /**
   * Get admin URL
   */
  getAdminUrl(): string {
    return this.adminUrl
  }

  /**
   * Clean up container
   */
  async cleanup(): Promise<void> {
    if (this.container) {
      await this.container.stop()
      this.container = null
      console.log('✅ Keycloak container stopped')
    }
  }
}

/**
 * Singleton Keycloak container for test suite
 */
export class KeycloakContainer {
  private static instance: KeycloakTestContainer | null = null

  static async start(options?: Parameters<KeycloakTestContainer['start']>[0]): Promise<KeycloakTestContainer> {
    if (!this.instance) {
      this.instance = new KeycloakTestContainer()
      await this.instance.start(options)
    }
    return this.instance
  }

  static async stop(): Promise<void> {
    if (this.instance) {
      await this.instance.cleanup()
      this.instance = null
    }
  }

  static getInstance(): KeycloakTestContainer | null {
    return this.instance
  }
}
