/**
 * Test Generator Utility
 * Generate Playwright tests from page objects and routes
 */

import { type Page } from '@playwright/test'
import { faker } from '@faker-js/faker'

export interface TestTemplate {
  name: string
  description: string
  test: (page: Page) => void | Promise<void>
  tags?: string[]
  skip?: boolean
}

class TestGenerator {
  generateCrudTest(entityName: string, config: {
    pagePath: string
    listSelector: string
    createButtonSelector: string
    formFields: Array<{
      name: string
      type: 'input' | 'select' | 'textarea' | 'checkbox'
      selector: string
      required?: boolean
      value?: any
    }>
    apiEndpoint: string
  }): TestTemplate {
    const { pagePath, listSelector, createButtonSelector, formFields, apiEndpoint } = config

    return {
      name: `should create ${entityName}`,
      description: `Create a new ${entityName} with valid data`,
      tags: ['crud', entityName],
      test: async (page) => {
        await page.goto(pagePath)

        // Click create button
        await page.click(createButtonSelector)

        // Fill form fields
        for (const field of formFields) {
          switch (field.type) {
            case 'input':
              await page.fill(field.selector, field.value || faker.lorem.words())
              break
            case 'select':
              await page.selectOption(field.selector, field.value || 0)
              break
            case 'textarea':
              await page.fill(field.selector, field.value || faker.lorem.paragraph())
              break
            case 'checkbox':
              await page.check(field.selector)
              break
          }
        }

        // Submit form
        await page.click('[data-testid="submit"], button[type="submit"]')

        // Verify success
        await expect(page.locator(listSelector)).toBeVisible()
        await expect(page.locator('[data-testid="success-message"]')).toBeVisible()
      }
    }
  }

  generateSearchTest(entityName: string, config: {
    pagePath: string
    searchInputSelector: string
    resultsSelector: string
    searchTerm: string
  }): TestTemplate {
    const { pagePath, searchInputSelector, resultsSelector, searchTerm } = config

    return {
      name: `should search ${entityName}`,
      description: `Search for ${entityName} using search input`,
      tags: ['search', entityName],
      test: async (page) => {
        await page.goto(pagePath)

        // Enter search term
        await page.fill(searchInputSelector, searchTerm)
        await page.keyboard.press(searchInputSelector + 'Enter')

        // Wait for results
        await expect(page.locator(resultsSelector)).toBeVisible()

        // Verify at least one result
        const count = await page.locator(resultsSelector).count()
        expect(count).toBeGreaterThan(0)
      }
    }
  }

  generatePaginationTest(entityName: string, config: {
    pagePath: string
    nextButtonSelector: string
    prevButtonSelector: string
    pageSelector: string
    totalPages: number
  }): TestTemplate {
    const { pagePath, nextButtonSelector, prevButtonSelector, pageSelector, totalPages } = config

    return {
      name: `should paginate ${entityName} results`,
      description: `Navigate through pages of ${entityName} results`,
      tags: ['pagination', entityName],
      test: async (page) => {
        await page.goto(pagePath)

        // Navigate to next page
        for (let i = 1; i < totalPages; i++) {
          await page.click(nextButtonSelector)
          await expect(page.locator(pageSelector.replace('{page}', (i + 1).toString()))).toBeVisible()
        }

        // Navigate back
        for (let i = totalPages - 1; i > 0; i--) {
          await page.click(prevButtonSelector)
          await expect(page.locator(pageSelector.replace('{page}', (i - 1).toString()))).toBeVisible()
        }
      }
    }
  }

  generateValidationTest(entityName: string, config: {
    pagePath: string
    submitButtonSelector: string
    formFields: Array<{
      name: string
      selector: string
      value: any
      expectedError: string
    }>
  }): TestTemplate {
    const { pagePath, submitButtonSelector, formFields } = config

    return {
      name: `should validate ${entityName} form`,
      description: `Show validation errors for invalid ${entityName} data`,
      tags: ['validation', entityName],
      test: async (page) => {
        await page.goto(pagePath)

        // Fill fields with invalid data
        for (const field of formFields) {
          await page.fill(field.selector, field.value)
        }

        // Submit form
        await page.click(submitButtonSelector)

        // Verify validation errors
        for (const field of formFields) {
          await expect(page.locator(`[data-testid="error-${field.name}"]`)).toBeVisible()
          const errorText = await page.locator(`[data-testid="error-${field.name}"]`).textContent()
          expect(errorText).toContain(field.expectedError)
        }
      }
    }
  }

  generateCRUDSuite(entityName: string, config: {
    pagePath: string
    apiEndpoint: string
    listSelector: string
    formFields: Array<{
      name: string
      type: 'input' | 'select' | 'textarea' | 'checkbox'
      selector: string
      required?: boolean
      value?: any
    }>
  }): TestTemplate[] {
    const tests: TestTemplate[] = []

    // Create test
    tests.push(this.generateCrudTest(entityName, config))

    // Update test
    tests.push({
      name: `should update ${entityName}`,
      description: `Update an existing ${entityName}`,
      tags: ['crud', entityName, 'update'],
      test: async (page) => {
        await page.goto(config.pagePath)

        // Click first item
        await page.click(`${config.listSelector} >> nth=0`)

        // Edit form
        for (const field of config.formFields) {
          if (field.type === 'input') {
            await page.fill(field.selector, field.value || 'Updated')
          }
        }

        // Save changes
        await page.click('[data-testid="save"]')

        // Verify update
        await expect(page.locator('[data-testid="success-message"]')).toBeVisible()
      }
    })

    // Delete test
    tests.push({
      name: `should delete ${entityName}`,
      description: `Delete a ${entityName}`,
      tags: ['crud', entityName, 'delete'],
      test: async (page) => {
        await page.goto(config.pagePath)

        // Click first item
        await page.click(`${config.listSelector} >> nth=0`)

        // Delete
        await page.click('[data-testid="delete"]')
        await page.click('[data-testid="confirm-delete"]')

        // Verify deletion
        await expect(page.locator('[data-testid="success-message"]')).toBeVisible()
      }
    })

    return tests
  }

  generateAPITestSuite(entityName: string, config: {
    baseUrl: string
    endpoints: Array<{
      path: string
      method: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'
      expectedStatus: number
      requiredFields?: string[]
    }>
  }): TestTemplate[] {
    return config.endpoints.map(endpoint => ({
      name: `should respond to ${endpoint.method} ${endpoint.path}`,
      description: `API endpoint for ${entityName}`,
      tags: ['api', entityName, endpoint.method],
      test: async (page) => {
        const response = await page.request.fetch(`${config.baseUrl}${endpoint.path}`, {
          method: endpoint.method
        })

        expect(response.status()).toBe(endpoint.expectedStatus)

        if (endpoint.requiredFields && response.headers()['content-type']?.includes('application/json')) {
          const body = await response.json()
          for (const field of endpoint.requiredFields) {
            expect(body).toHaveProperty(field)
          }
        }
      }
    }))
  }

  generatePageTest(url: string, config: {
    title?: string
    elements?: Array<{
      selector: string
      visible?: boolean
    }>
    actions?: Array<{
      description: string
      action: (page: Page) => void | Promise<void>
    }>
  }): TestTemplate {
    return {
      name: `should load ${url}`,
      description: `Basic page load test for ${url}`,
      tags: ['page', 'load'],
      test: async (page) => {
        await page.goto(url)

        if (config.title) {
          await expect(page).toHaveTitle(config.title)
        }

        if (config.elements) {
          for (const element of config.elements) {
            const locator = page.locator(element.selector)
            if (element.visible) {
              await expect(locator).toBeVisible()
            } else {
              await expect(locator).toBeAttached()
            }
          }
        }

        if (config.actions) {
          for (const action of config.actions) {
            await action.action(page)
          }
        }
      }
    }
  }

  exportTestsToFile(tests: TestTemplate[], filename: string) {
    const testCode = tests.map(test => `
test.describe('${test.name}', () => {
  test.describe('${test.description}', () => {
    test('${test.name}', async ({ page }) => {
      ${test.test.toString()}
    })
  })
})
    `).join('\n')

    const fs = require('fs')
    fs.writeFileSync(filename, testCode)
    console.log(`Tests exported to ${filename}`)
  }
}

export const testGenerator = new TestGenerator()

// Faker-based test data generators
export const testData = {
  customer: () => ({
    firstName: faker.person.firstName(),
    lastName: faker.person.lastName(),
    email: faker.internet.email(),
    phone: faker.phone.number(),
    address: faker.location.streetAddress(),
    city: faker.location.city(),
    state: faker.location.state(),
    zipCode: faker.location.zipCode()
  }),

  order: (customerId?: string) => ({
    customerId: customerId || faker.string.uuid(),
    orderNumber: `ORD-${faker.number.int({ min: 100000, max: 999999 })}`,
    totalAmount: faker.number.float({ min: 10, max: 1000, precision: 0.01 }),
    status: faker.helpers.arrayElement(['pending', 'processing', 'shipped', 'delivered'])
  }),

  user: () => ({
    username: faker.internet.userName(),
    email: faker.internet.email(),
    password: faker.internet.password(),
    role: faker.helpers.arrayElement(['admin', 'user', 'viewer'])
  }),

  product: () => ({
    name: faker.commerce.productName(),
    description: faker.commerce.productDescription(),
    price: faker.number.float({ min: 10, max: 1000, precision: 0.01 }),
    category: faker.commerce.department(),
    sku: faker.string.alphanumeric(10).toUpperCase()
  })
}

// Generate test with data
export function withTestData<T>(generator: () => T, testFn: (data: T) => void | Promise<void>) {
  return async (page: Page) => {
    const data = generator()
    await testFn(data)
  }
}
