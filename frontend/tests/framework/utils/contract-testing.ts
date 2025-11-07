/**
 * Contract Testing Utilities
 * Support for Pact.js and schema validation
 */

import { type Page } from '@playwright/test'
import Ajv from 'ajv'
import addFormats from 'ajv-formats'

export interface ContractTest {
  consumer: string
  provider: string
  interaction: {
    description: string
    request: {
      method: string
      path: string
      headers?: Record<string, string>
      body?: any
    }
    response: {
      status: number
      headers?: Record<string, string>
      body: any
    }
  }
  providerState?: string
}

export interface SchemaDefinition {
  $id?: string
  $schema?: string
  type: 'object' | 'array' | 'string' | 'number' | 'boolean' | 'null'
  properties?: Record<string, any>
  required?: string[]
  items?: any
}

class ContractTester {
  private ajv: Ajv

  constructor() {
    this.ajv = new Ajv({ allErrors: true, verbose: true })
    addFormats(this.ajv)
  }

  validateSchema(data: any, schema: SchemaDefinition): { valid: boolean; errors: any[] } {
    const validate = this.ajv.compile(schema)
    const valid = validate(data) as boolean

    return {
      valid,
      errors: validate.errors || []
    }
  }

  async testAPIContract(page: Page, contract: ContractTest) {
    const { interaction } = contract
    const { request, response } = interaction

    console.log(`Testing contract: ${interaction.description}`)

    // Make the request
    const apiResponse = await page.request.fetch(`http://localhost:3000${request.path}`, {
      method: request.method,
      headers: request.headers,
      data: request.body
    })

    // Validate status
    if (apiResponse.status() !== response.status) {
      throw new Error(`Expected status ${response.status}, got ${apiResponse.status()}`)
    }

    // Validate body structure if schema is provided
    if (response.body && typeof response.body === 'object') {
      const body = await apiResponse.json()
      const schema = this.extractSchemaFromContract(response.body)

      if (schema) {
        const validation = this.validateSchema(body, schema)
        if (!validation.valid) {
          throw new Error(`Schema validation failed: ${JSON.stringify(validation.errors, null, 2)}`)
        }
      }
    }

    return {
      status: apiResponse.status(),
      headers: apiResponse.headers(),
      body: await apiResponse.json()
    }
  }

  private extractSchemaFromContract(body: any): SchemaDefinition | null {
    // This is a simplified schema extraction
    // In a real implementation, you'd use JSON Schema inference
    if (Array.isArray(body)) {
      return {
        type: 'array',
        items: body.length > 0 ? this.inferType(body[0]) : {}
      }
    } else if (typeof body === 'object' && body !== null) {
      const properties: Record<string, any> = {}
      const required: string[] = []

      for (const [key, value] of Object.entries(body)) {
        properties[key] = this.inferType(value)
        required.push(key)
      }

      return {
        type: 'object',
        properties,
        required
      }
    }

    return null
  }

  private inferType(value: any): any {
    if (Array.isArray(value)) {
      return { type: 'array', items: value.length > 0 ? this.inferType(value[0]) : {} }
    }
    if (typeof value === 'object' && value !== null) {
      const properties: Record<string, any> = {}
      for (const [key, val] of Object.entries(value)) {
        properties[key] = this.inferType(val)
      }
      return { type: 'object', properties }
    }
    if (typeof value === 'string') {
      // Try to detect date strings
      if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/.test(value)) {
        return { type: 'string', format: 'date-time' }
      }
      // Try to detect emails
      if (value.includes('@') && value.includes('.')) {
        return { type: 'string', format: 'email' }
      }
      return { type: 'string' }
    }
    if (typeof value === 'number') {
      return Number.isInteger(value) ? { type: 'integer' } : { type: 'number' }
    }
    if (typeof value === 'boolean') {
      return { type: 'boolean' }
    }
    return { type: 'string' }
  }

  generatePactFile(contracts: ContractTest[], outputPath: string) {
    const pact = {
      consumer: { name: contracts[0]?.consumer || 'frontend' },
      provider: { name: contracts[0]?.provider || 'backend' },
      interactions: contracts.map(contract => ({
        description: contract.interaction.description,
        providerState: contract.providerState,
        request: contract.interaction.request,
        response: contract.interaction.response
      })),
      metadata: {
        pactSpecification: { version: '2.0.0' },
        pagination: 'none',
        method: 'mock'
      }
    }

    const fs = require('fs')
    fs.writeFileSync(outputPath, JSON.stringify(pact, null, 2))
    console.log(`Pact file generated: ${outputPath}`)
  }

  async validateResponse(page: Page, config: {
    url: string
    method?: string
    expectedSchema?: SchemaDefinition
    expectedStatus?: number
    expectedFields?: string[]
  }) {
    const response = await page.request.fetch(config.url, {
      method: config.method || 'GET'
    })

    if (config.expectedStatus && response.status() !== config.expectedStatus) {
      throw new Error(`Expected status ${config.expectedStatus}, got ${response.status()}`)
    }

    let body: any = null
    if (response.headers()['content-type']?.includes('application/json')) {
      body = await response.json()

      if (config.expectedSchema) {
        const validation = this.validateSchema(body, config.expectedSchema)
        if (!validation.valid) {
          throw new Error(`Schema validation failed: ${JSON.stringify(validation.errors, null, 2)}`)
        }
      }

      if (config.expectedFields) {
        for (const field of config.expectedFields) {
          if (!this.hasNestedProperty(body, field)) {
            throw new Error(`Expected field "${field}" not found in response`)
          }
        }
      }
    }

    return {
      status: response.status(),
      headers: response.headers(),
      body
    }
  }

  private hasNestedProperty(obj: any, path: string): boolean {
    const keys = path.split('.')
    let current = obj
    for (const key of keys) {
      if (current === null || current === undefined || !(key in current)) {
        return false
      }
      current = current[key]
    }
    return true
  }
}

export const contractTester = new ContractTester()

// Pre-defined schemas for common API endpoints
export const customerSchema: SchemaDefinition = {
  type: 'object',
  properties: {
    id: { type: 'string' },
    firstName: { type: 'string' },
    lastName: { type: 'string' },
    email: { type: 'string', format: 'email' },
    phone: { type: 'string' },
    status: { type: 'string', enum: ['active', 'inactive', 'pending'] },
    createdAt: { type: 'string', format: 'date-time' }
  },
  required: ['id', 'firstName', 'lastName', 'email', 'status', 'createdAt']
}

export const orderSchema: SchemaDefinition = {
  type: 'object',
  properties: {
    id: { type: 'string' },
    customerId: { type: 'string' },
    orderNumber: { type: 'string' },
    totalAmount: { type: 'number' },
    status: { type: 'string' },
    createdAt: { type: 'string', format: 'date-time' }
  },
  required: ['id', 'customerId', 'orderNumber', 'totalAmount', 'status', 'createdAt']
}

export const errorSchema: SchemaDefinition = {
  type: 'object',
  properties: {
    error: { type: 'string' },
    code: { type: 'string' },
    message: { type: 'string' }
  },
  required: ['error', 'message']
}

// Helper to create contract tests
export function createContract(
  consumer: string,
  provider: string,
  description: string,
  request: ContractTest['interaction']['request'],
  response: ContractTest['interaction']['response'],
  providerState?: string
): ContractTest {
  return {
    consumer,
    provider,
    interaction: {
      description,
      request,
      response
    },
    providerState
  }
}

// OpenAPI schema validation
export async function validateOpenAPISchema(page: Page, openApiUrl: string) {
  try {
    const response = await page.request.get(openApiUrl)
    const schema = await response.json()

    // Basic OpenAPI validation
    if (!schema.openapi && !schema.swagger) {
      throw new Error('Not a valid OpenAPI/Swagger document')
    }

    // Check for required fields
    const requiredFields = ['info', 'paths']
    for (const field of requiredFields) {
      if (!schema[field]) {
        throw new Error(`Missing required OpenAPI field: ${field}`)
      }
    }

    return {
      valid: true,
      version: schema.openapi || schema.swagger,
      paths: Object.keys(schema.paths || {})
    }
  } catch (error) {
    return {
      valid: false,
      error: error instanceof Error ? error.message : String(error)
    }
  }
}
