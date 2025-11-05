/**
 * API Testing Utilities
 *
 * Provides typed API client with schema validation
 * Supports GraphQL, REST, and WebSocket testing
 *
 * Usage:
 * ```typescript
 * const api = new ApiClient(baseURL, authToken)
 * const customer = await api.customers.getById('123')
 * await expect(api.customers.create(customerData)).toSucceed()
 * ```
 */

import { type APIRequestContext, type APIResponse } from '@playwright/test'
import Ajv from 'ajv'
import addFormats from 'ajv-formats'

export interface ApiClientConfig {
  baseURL: string
  authToken?: string
  defaultHeaders?: Record<string, string>
  timeout?: number
  retries?: number
}

export interface ApiResponse<T = any> {
  status: number
  headers: Record<string, string>
  body: T
  ok: boolean
}

export class ApiClient {
  private request: APIRequestContext
  private config: ApiClientConfig
  private schemaValidator: Ajv

  constructor(config: ApiClientConfig) {
    this.config = {
      timeout: 30000,
      retries: 3,
      defaultHeaders: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      },
      ...config
    }

    this.request = config as any
    this.schemaValidator = new Ajv({ allErrors: true, strict: false })
    addFormats(this.schemaValidator)
  }

  /**
   * GET request
   */
  async get<T = any>(endpoint: string, options?: {
    headers?: Record<string, string>
    params?: Record<string, any>
    schema?: any
  }): Promise<ApiResponse<T>> {
    const { headers, params, schema } = options || {}

    const response = await this.request.get(`${this.config.baseURL}${endpoint}`, {
      headers: { ...this.config.defaultHeaders, ...headers },
      params,
      timeout: this.config.timeout
    })

    const body = await response.json()
    const result = {
      status: response.status(),
      headers: response.headers(),
      body,
      ok: response.ok()
    }

    if (schema) {
      this.validateSchema(schema, body)
    }

    return result
  }

  /**
   * POST request
   */
  async post<T = any>(endpoint: string, data?: any, options?: {
    headers?: Record<string, string>
    schema?: any
  }): Promise<ApiResponse<T>> {
    const { headers, schema } = options || {}

    const response = await this.request.post(`${this.config.baseURL}${endpoint}`, {
      headers: { ...this.config.defaultHeaders, ...headers },
      data: data ? JSON.stringify(data) : undefined,
      timeout: this.config.timeout
    })

    const body = await response.json().catch(() => ({}))
    const result = {
      status: response.status(),
      headers: response.headers(),
      body,
      ok: response.ok()
    }

    if (schema) {
      this.validateSchema(schema, body)
    }

    return result
  }

  /**
   * PUT request
   */
  async put<T = any>(endpoint: string, data?: any, options?: {
    headers?: Record<string, string>
    schema?: any
  }): Promise<ApiResponse<T>> {
    const { headers, schema } = options || {}

    const response = await this.request.put(`${this.config.baseURL}${endpoint}`, {
      headers: { ...this.config.defaultHeaders, ...headers },
      data: data ? JSON.stringify(data) : undefined,
      timeout: this.config.timeout
    })

    const body = await response.json().catch(() => ({}))
    const result = {
      status: response.status(),
      headers: response.headers(),
      body,
      ok: response.ok()
    }

    if (schema) {
      this.validateSchema(schema, body)
    }

    return result
  }

  /**
   * DELETE request
   */
  async delete<T = any>(endpoint: string, options?: {
    headers?: Record<string, string>
    schema?: any
  }): Promise<ApiResponse<T>> {
    const { headers, schema } = options || {}

    const response = await this.request.delete(`${this.config.baseURL}${endpoint}`, {
      headers: { ...this.config.defaultHeaders, ...headers },
      timeout: this.config.timeout
    })

    const body = await response.json().catch(() => ({}))
    const result = {
      status: response.status(),
      headers: response.headers(),
      body,
      ok: response.ok()
    }

    if (schema) {
      this.validateSchema(schema, body)
    }

    return result
  }

  /**
   * Validate response schema
   */
  private validateSchema(schema: any, data: any): void {
    const validate = this.schemaValidator.compile(schema)
    const valid = validate(data)

    if (!valid) {
      throw new Error(`Schema validation failed: ${JSON.stringify(validate.errors, null, 2)}`)
    }
  }

  /**
   * Set authentication token
   */
  setAuthToken(token: string): void {
    this.config.authToken = token
    if (this.request) {
      this.config.defaultHeaders = {
        ...this.config.defaultHeaders,
        'Authorization': `Bearer ${token}`
      }
    }
  }

  /**
   * Customers API
   */
  customers = {
    getById: async (id: string) => {
      return this.get(`/api/customers/${id}`)
    },

    getAll: async (params?: { status?: string; page?: number; limit?: number }) => {
      return this.get('/api/customers', { params })
    },

    create: async (customer: any) => {
      return this.post('/api/customers', customer)
    },

    update: async (id: string, customer: any) => {
      return this.put(`/api/customers/${id}`, customer)
    },

    delete: async (id: string) => {
      return this.delete(`/api/customers/${id}`)
    }
  }

  /**
   * Orders API
   */
  orders = {
    getById: async (id: string) => {
      return this.get(`/api/orders/${id}`)
    },

    getAll: async (params?: { customerId?: string; status?: string }) => {
      return this.get('/api/orders', { params })
    },

    create: async (order: any) => {
      return this.post('/api/orders', order)
    },

    update: async (id: string, order: any) => {
      return this.put(`/api/orders/${id}`, order)
    },

    cancel: async (id: string, reason?: string) => {
      return this.post(`/api/orders/${id}/cancel`, { reason })
    }
  }

  /**
   * Invoices API
   */
  invoices = {
    getById: async (id: string) => {
      return this.get(`/api/invoices/${id}`)
    },

    getAll: async (params?: { customerId?: string; status?: string }) => {
      return this.get('/api/invoices', { params })
    },

    create: async (invoice: any) => {
      return this.post('/api/invoices', invoice)
    },

    markAsPaid: async (id: string, paymentData?: any) => {
      return this.post(`/api/invoices/${id}/mark-paid`, paymentData)
    },

    downloadPdf: async (id: string): Promise<Buffer> => {
      const response = await this.request.get(`${this.config.baseURL}/api/invoices/${id}/pdf`, {
        headers: { ...this.config.defaultHeaders },
        timeout: this.config.timeout
      })
      return await response.body()
    }
  }

  /**
   * Payments API
   */
  payments = {
    getById: async (id: string) => {
      return this.get(`/api/payments/${id}`)
    },

    getAll: async (params?: { customerId?: string; invoiceId?: string }) => {
      return this.get('/api/payments', { params })
    },

    create: async (payment: any) => {
      return this.post('/api/payments', payment)
    },

    process: async (id: string) => {
      return this.post(`/api/payments/${id}/process`)
    },

    refund: async (id: string, amount?: number, reason?: string) => {
      return this.post(`/api/payments/${id}/refund`, { amount, reason })
    }
  }

  /**
   * Subscriptions API
   */
  subscriptions = {
    getById: async (id: string) => {
      return this.get(`/api/subscriptions/${id}`)
    },

    getAll: async (params?: { customerId?: string; status?: string }) => {
      return this.get('/api/subscriptions', { params })
    },

    create: async (subscription: any) => {
      return this.post('/api/subscriptions', subscription)
    },

    cancel: async (id: string, cancelAtPeriodEnd: boolean = true) => {
      return this.post(`/api/subscriptions/${id}/cancel`, { cancelAtPeriodEnd })
    },

    pause: async (id: string) => {
      return this.post(`/api/subscriptions/${id}/pause`)
    },

    resume: async (id: string) => {
      return this.post(`/api/subscriptions/${id}/resume`)
    }
  }

  /**
   * Health check
   */
  async healthCheck(): Promise<boolean> {
    try {
      const response = await this.get('/health')
      return response.status === 200
    } catch (error) {
      return false
    }
  }
}

/**
 * API Response matchers for expect()
 */
export class ApiMatchers {
  static async toSucceed(response: ApiResponse): Promise<{ pass: boolean; message: string }> {
    const pass = response.ok && response.status >= 200 && response.status < 300

    return {
      pass,
      message: pass
        ? `Expected response not to succeed, but it did with status ${response.status}`
        : `Expected response to succeed, but got status ${response.status}`
    }
  }

  static async toHaveStatus(response: ApiResponse, expectedStatus: number): Promise<{ pass: boolean; message: string }> {
    const pass = response.status === expectedStatus

    return {
      pass,
      message: pass
        ? `Expected status ${expectedStatus}, but got ${response.status}`
        : `Expected status ${expectedStatus}, but got ${response.status}`
    }
  }

  static async toHaveProperty(response: ApiResponse, property: string): Promise<{ pass: boolean; message: string }> {
    const pass = response.body.hasOwnProperty(property)

    return {
      pass,
      message: pass
        ? `Expected response to not have property "${property}"`
        : `Expected response to have property "${property}", but it doesn't`
    }
  }

  static async toBePaginated(response: ApiResponse): Promise<{ pass: boolean; message: string }> {
    const hasPagination = response.body.hasOwnProperty('data') &&
                         response.body.hasOwnProperty('total') &&
                         response.body.hasOwnProperty('page') &&
                         response.body.hasOwnProperty('limit')

    return {
      pass: hasPagination,
      message: hasPagination
        ? `Expected response not to be paginated`
        : `Expected response to be paginated with data, total, page, and limit properties`
    }
  }
}

/**
 * GraphQL Client
 */
export class GraphQLClient {
  private apiClient: ApiClient

  constructor(config: ApiClientConfig) {
    this.apiClient = new ApiClient(config)
  }

  async query<T = any>(query: string, variables?: Record<string, any>): Promise<ApiResponse<T>> {
    return this.apiClient.post('/graphql', {
      query,
      variables
    })
  }

  async mutate<T = any>(mutation: string, variables?: Record<string, any>): Promise<ApiResponse<T>> {
    return this.query<T>(mutation, variables)
  }
}

/**
 * WebSocket Client for testing real-time features
 */
export class WebSocketClient {
  private ws: WebSocket | null = null
  private messageQueue: any[] = []

  constructor(private url: string) {}

  async connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.ws = new WebSocket(this.url)

      this.ws.onopen = () => resolve()
      this.ws.onerror = (error) => reject(error)
      this.ws.onmessage = (event) => {
        this.messageQueue.push(JSON.parse(event.data))
      }
    })
  }

  send(message: any): void {
    if (this.ws) {
      this.ws.send(JSON.stringify(message))
    }
  }

  async waitForMessage(predicate: (message: any) => boolean, timeout: number = 5000): Promise<any> {
    const startTime = Date.now()

    while (Date.now() - startTime < timeout) {
      const message = this.messageQueue.find(predicate)
      if (message) {
        return message
      }
      await new Promise(resolve => setTimeout(resolve, 100))
    }

    throw new Error('Timeout waiting for WebSocket message')
  }

  close(): void {
    this.ws?.close()
  }
}
