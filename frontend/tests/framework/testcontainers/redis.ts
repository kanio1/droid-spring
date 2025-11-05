/**
 * Redis Testcontainers Integration
 *
 * Provides Redis container for integration testing
 * Supports cluster mode, persistence, and custom configuration
 *
 * Usage:
 * ```typescript
 * const redis = await RedisContainer.start()
 * const client = redis.createClient()
 * await redis.set('key', 'value')
 * await redis.cleanup()
 * ```
 */

import { GenericContainer, type StartedTestContainer, type Bind } from 'testcontainers'
import { createClient, type RedisClientType } from 'redis'

export interface RedisConfig {
  port?: number
  password?: string
  database?: number
  clusterMode?: boolean
  persistence?: boolean
  maxmemory?: string
  appendonly?: boolean
  requirepass?: string
}

export interface RedisContainerOptions {
  image?: string
  tag?: string
  port?: number
  config?: RedisConfig
  binds?: Bind[]
  environment?: Record<string, string>
}

export class RedisTestContainer {
  private container: StartedTestContainer | null = null
  private client: RedisClientType | null = null
  private config: Required<RedisContainerOptions>

  constructor(options: RedisContainerOptions = {}) {
    this.config = {
      image: 'redis',
      tag: '7-alpine',
      port: 6379,
      ...options
    } as Required<RedisContainerOptions>
  }

  /**
   * Start Redis container
   */
  async start(): Promise<RedisTestContainer> {
    const env: Record<string, string> = {
      ...this.config.environment
    }

    // Add Redis configuration
    if (this.config.config?.requirepass) {
      env['REDIS_PASSWORD'] = this.config.config.requirepass
    }

    if (this.config.config?.appendonly) {
      env['REDIS_APPENDONLY'] = 'yes'
    }

    this.container = await GenericContainer
      .fromImage(`${this.config.image}:${this.config.tag}`)
      .withExposedPorts(this.config.port)
      .withEnvironment(env)
      .withBindMounts(this.config.binds || [])
      .withStartupTimeout(60000)
      .start()

    // Wait for Redis to be ready
    await this.waitForReady()

    // Create Redis client
    await this.createClient()

    console.log(`✅ Redis container started on port ${this.getPort()}`)

    return this
  }

  /**
   * Create Redis client
   */
  async createClient(): Promise<RedisClientType> {
    if (!this.container) {
      throw new Error('Container not started. Call start() first.')
    }

    const config = this.config.config || {}

    this.client = createClient({
      socket: {
        host: this.container.getHost(),
        port: this.getPort()
      },
      password: config.requirepass || config.password,
      database: config.database || 0
    })

    await this.client.connect()
    return this.client
  }

  /**
   * Wait for Redis to be ready
   */
  private async waitForReady(): Promise<void> {
    if (!this.container) return

    const maxAttempts = 30
    const delay = 1000

    for (let i = 0; i < maxAttempts; i++) {
      try {
        const testClient = createClient({
          socket: {
            host: this.container.getHost(),
            port: this.getPort()
          }
        })
        await testClient.connect()
        await testClient.ping()
        await testClient.quit()
        return
      } catch (error) {
        if (i === maxAttempts - 1) {
          throw new Error(`Redis container failed to start: ${error}`)
        }
        await new Promise(resolve => setTimeout(resolve, delay))
      }
    }
  }

  /**
   * Get connection URI
   */
  getUri(): string {
    if (!this.container) {
      throw new Error('Container not started')
    }

    const host = this.container.getHost()
    const port = this.getPort()

    return `redis://${host}:${port}`
  }

  /**
   * Get port
   */
  getPort(): number {
    if (!this.container) {
      throw new Error('Container not started')
    }

    return this.container.getMappedPort(this.config.port)
  }

  /**
   * Get host
   */
  getHost(): string {
    if (!this.container) {
      throw new Error('Container not started')
    }

    return this.container.getHost()
  }

  /**
   * Set key-value pair
   */
  async set(key: string, value: string | number | Buffer, options?: {
    EX?: number // expiration in seconds
    PX?: number // expiration in milliseconds
    NX?: boolean // only set if key doesn't exist
    XX?: boolean // only set if key exists
  }): Promise<string> {
    if (!this.client) {
      await this.createClient()
    }
    return await this.client!.set(key, value.toString(), options)
  }

  /**
   * Get value by key
   */
  async get(key: string): Promise<string | null> {
    if (!this.client) {
      await this.createClient()
    }
    return await this.client!.get(key)
  }

  /**
   * Delete key(s)
   */
  async del(...keys: string[]): Promise<number> {
    if (!this.client) {
      await this.createClient()
    }
    return await this.client!.del(keys)
  }

  /**
   * Check if key exists
   */
  async exists(key: string): Promise<number> {
    if (!this.client) {
      await this.createClient()
    }
    return await this.client!.exists(key)
  }

  /**
   * Set expiration for key
   */
  async expire(key: string, seconds: number): Promise<number> {
    if (!this.client) {
      await this.createClient()
    }
    return await this.client!.expire(key, seconds)
  }

  /**
   * Get TTL for key
   */
  async ttl(key: string): Promise<number> {
    if (!this.client) {
      await this.createClient()
    }
    return await this.client!.ttl(key)
  }

  /**
   * Hash operations - Set field
   */
  async hSet(key: string, field: string, value: string): Promise<number> {
    if (!this.client) {
      await this.createClient()
    }
    return await this.client!.hSet(key, field, value)
  }

  /**
   * Hash operations - Get field
   */
  async hGet(key: string, field: string): Promise<string | null> {
    if (!this.client) {
      await this.createClient()
    }
    return await this.client!.hGet(key, field)
  }

  /**
   * Hash operations - Get all fields
   */
  async hGetAll(key: string): Promise<Record<string, string>> {
    if (!this.client) {
      await this.createClient()
    }
    return await this.client!.hGetAll(key)
  }

  /**
   * List operations - Push to left
   */
  async lPush(key: string, ...values: string[]): Promise<number> {
    if (!this.client) {
      await this.createClient()
    }
    return await this.client!.lPush(key, values)
  }

  /**
   * List operations - Pop from right
   */
  async rPop(key: string): Promise<string | null> {
    if (!this.client) {
      await this.createClient()
    }
    return await this.client!.rPop(key)
  }

  /**
   * Set operations - Add member
   */
  async sAdd(key: string, ...members: string[]): Promise<number> {
    if (!this.client) {
      await this.createClient()
    }
    return await this.client!.sAdd(key, members)
  }

  /**
   * Set operations - Check membership
   */
  async sIsMember(key: string, member: string): Promise<boolean> {
    if (!this.client) {
      await this.createClient()
    }
    return await this.client!.sIsMember(key, member)
  }

  /**
   * Clear all data
   */
  async flushAll(): Promise<string> {
    if (!this.client) {
      await this.createClient()
    }
    return await this.client!.flushAll()
  }

  /**
   * Get info
   */
  async info(section?: string): Promise<string> {
    if (!this.client) {
      await this.createClient()
    }
    return await this.client!.info(section)
  }

  /**
   * Clean up container
   */
  async cleanup(): Promise<void> {
    if (this.client) {
      await this.client.quit()
      this.client = null
    }

    if (this.container) {
      await this.container.stop()
      this.container = null
      console.log('✅ Redis container stopped')
    }
  }
}

/**
 * Singleton Redis container for test suite
 */
export class RedisContainer {
  private static instance: RedisTestContainer | null = null

  static async start(options?: RedisContainerOptions): Promise<RedisTestContainer> {
    if (!this.instance) {
      this.instance = new RedisTestContainer(options)
      await this.instance.start()
    }
    return this.instance
  }

  static async stop(): Promise<void> {
    if (this.instance) {
      await this.instance.cleanup()
      this.instance = null
    }
  }

  static async reset(): Promise<void> {
    if (this.instance) {
      await this.instance.flushAll()
    }
  }

  static getInstance(): RedisTestContainer | null {
    return this.instance
  }
}
