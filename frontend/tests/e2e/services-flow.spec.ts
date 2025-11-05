import { test, expect } from '@playwright/test'

test.describe('Services Flow', () => {
  test.describe('Services List', () => {
    test.beforeEach(async ({ page }) => {
      await page.route('**/api/services**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            content: [],
            totalElements: 0,
            totalPages: 0,
            number: 0,
            size: 20
          })
        })
      })
    })

    test('should navigate to services page', async ({ page }) => {
      await page.goto('/services')

      await expect(page.locator('h1.page-title')).toContainText('Services')
      await expect(page.locator('text=Manage service catalog')).toBeVisible()
    })

    test('should display services summary cards', async ({ page }) => {
      await page.goto('/services')

      await expect(page.locator('.summary-card')).toHaveCount(4)
      await expect(page.locator('text=Total Services')).toBeVisible()
      await expect(page.locator('text=Active Services')).toBeVisible()
      await expect(page.locator('text=Activations')).toBeVisible()
      await expect(page.locator('text=Pending')).toBeVisible()
    })

    test('should filter by service type', async ({ page }) => {
      await page.goto('/services')

      await page.selectOption('[data-testid="service-type-filter"]', 'INTERNET')
      await page.waitForTimeout(500)

      const calls = page.request.allInterceptions()
      const filterCall = calls.find(c => c.request.url().includes('serviceType'))
      expect(filterCall).toBeDefined()
    })

    test('should filter by category', async ({ page }) => {
      await page.goto('/services')

      await page.selectOption('[data-testid="category-filter"]', 'CONNECTIVITY')
      await page.waitForTimeout(500)

      const calls = page.request.allInterceptions()
      const filterCall = calls.find(c => c.request.url().includes('category'))
      expect(filterCall).toBeDefined()
    })

    test('should search services', async ({ page }) => {
      await page.goto('/services')

      const searchInput = page.locator('input[placeholder*="Search services"]')
      await searchInput.fill('Internet')
      await page.keyboard.press('Enter')
      await page.waitForTimeout(500)

      const calls = page.request.allInterceptions()
      const searchCall = calls.find(c => c.request.url().includes('searchTerm'))
      expect(searchCall).toBeDefined()
    })

    test('should navigate to service activations', async ({ page }) => {
      await page.goto('/services')

      await page.click('button:has-text("View Activations")')
      await expect(page).toHaveURL('/services/activations')

      await expect(page.locator('h1.page-title')).toContainText('Service Activations')
    })
  })

  test.describe('Service Details', () => {
    test('should view service details', async ({ page }) => {
      // Mock single service response
      await page.route('**/api/services/1**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            id: '1',
            serviceCode: 'SVC-INTERNET-001',
            serviceName: 'Premium Internet',
            serviceType: 'INTERNET',
            category: 'CONNECTIVITY',
            status: 'ACTIVE',
            provisioningTime: 30,
            autoProvision: true,
            description: 'High-speed internet service',
            features: [
              {
                name: 'Unlimited bandwidth',
                description: 'No data caps'
              },
              {
                name: '24/7 support',
                description: 'Round-the-clock assistance'
              }
            ],
            dependencies: [
              {
                serviceCode: 'SVC-NETWORK-001',
                serviceName: 'Network Infrastructure',
                status: 'ACTIVE'
              }
            ],
            pricing: {
              monthlyFee: 99.99,
              setupFee: 50.00,
              usageRate: null,
              currency: 'USD'
            },
            provisioningConfig: {
              script: 'provision_internet.sh',
              apiEndpoint: '/api/provision/internet'
            },
            activations: [
              {
                id: 'act-1',
                customerId: 'cust-1',
                customerName: 'John Doe',
                activationDate: '2024-01-15T10:00:00Z',
                status: 'ACTIVE'
              }
            ],
            notes: 'Premium tier service',
            createdAt: '2024-01-01T00:00:00Z',
            updatedAt: '2024-01-01T00:00:00Z',
            version: 1
          })
        })
      })

      await page.goto('/services/1')

      await expect(page.locator('h1.page-title')).toContainText('Service Premium Internet')
      await expect(page.locator('text=Internet - SVC-INTERNET-001')).toBeVisible()
    })

    test('should display service summary', async ({ page }) => {
      await page.route('**/api/services/1**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            id: '1',
            serviceCode: 'SVC-INTERNET-001',
            serviceName: 'Premium Internet',
            serviceType: 'INTERNET',
            category: 'CONNECTIVITY',
            status: 'ACTIVE',
            provisioningTime: 30,
            autoProvision: true,
            createdAt: '2024-01-01T00:00:00Z',
            updatedAt: '2024-01-01T00:00:00Z',
            version: 1
          })
        })
      })

      await page.goto('/services/1')

      const summaryItems = page.locator('.summary-item')
      await expect(summaryItems.first()).toContainText('Service Code')
      await expect(summaryItems.nth(1)).toContainText('Service Name')
      await expect(summaryItems.nth(2)).toContainText('Type')
      await expect(summaryItems.nth(3)).toContainText('Category')
    })

    test('should display service features', async ({ page }) => {
      await page.route('**/api/services/1**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            id: '1',
            serviceCode: 'SVC-INTERNET-001',
            serviceName: 'Premium Internet',
            serviceType: 'INTERNET',
            category: 'CONNECTIVITY',
            status: 'ACTIVE',
            provisioningTime: 30,
            autoProvision: true,
            features: [
              {
                name: 'Unlimited bandwidth',
                description: 'No data caps'
              },
              {
                name: '24/7 support',
                description: 'Round-the-clock assistance'
              },
              {
                name: 'Free installation',
                description: null
              }
            ],
            createdAt: '2024-01-01T00:00:00Z',
            updatedAt: '2024-01-01T00:00:00Z',
            version: 1
          })
        })
      })

      await page.goto('/services/1')

      const featuresList = page.locator('.features-list .feature-item')
      await expect(featuresList).toHaveCount(3)
      await expect(featuresList.first()).toContainText('Unlimited bandwidth')
      await expect(featuresList.nth(1)).toContainText('24/7 support')
      await expect(featuresList.nth(2)).toContainText('Free installation')
    })

    test('should display service dependencies', async ({ page }) => {
      await page.route('**/api/services/1**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            id: '1',
            serviceCode: 'SVC-INTERNET-001',
            serviceName: 'Premium Internet',
            serviceType: 'INTERNET',
            category: 'CONNECTIVITY',
            status: 'ACTIVE',
            provisioningTime: 30,
            autoProvision: true,
            dependencies: [
              {
                serviceCode: 'SVC-NETWORK-001',
                serviceName: 'Network Infrastructure',
                status: 'ACTIVE'
              },
              {
                serviceCode: 'SVC-DNS-001',
                serviceName: 'DNS Service',
                status: 'ACTIVE'
              }
            ],
            createdAt: '2024-01-01T00:00:00Z',
            updatedAt: '2024-01-01T00:00:00Z',
            version: 1
          })
        })
      })

      await page.goto('/services/1')

      const dependenciesList = page.locator('.dependencies-list .dependency-item')
      await expect(dependenciesList).toHaveCount(2)
      await expect(dependenciesList.first()).toContainText('SVC-NETWORK-001')
      await expect(dependenciesList.nth(1)).toContainText('SVC-DNS-001')
    })

    test('should display activation history', async ({ page }) => {
      await page.route('**/api/services/1**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            id: '1',
            serviceCode: 'SVC-INTERNET-001',
            serviceName: 'Premium Internet',
            serviceType: 'INTERNET',
            category: 'CONNECTIVITY',
            status: 'ACTIVE',
            provisioningTime: 30,
            autoProvision: true,
            activations: [
              {
                id: 'act-1',
                customerId: 'cust-1',
                customerName: 'John Doe',
                activationDate: '2024-01-15T10:00:00Z',
                status: 'ACTIVE'
              },
              {
                id: 'act-2',
                customerId: 'cust-2',
                customerName: 'Jane Smith',
                activationDate: '2024-01-20T14:30:00Z',
                status: 'ACTIVE'
              }
            ],
            createdAt: '2024-01-01T00:00:00Z',
            updatedAt: '2024-01-01T00:00:00Z',
            version: 1
          })
        })
      })

      await page.goto('/services/1')

      const activationRows = page.locator('.activation-history .p-datatable-tbody tr')
      await expect(activationRows).toHaveCount(2)
    })

    test('should display pricing information', async ({ page }) => {
      await page.route('**/api/services/1**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            id: '1',
            serviceCode: 'SVC-INTERNET-001',
            serviceName: 'Premium Internet',
            serviceType: 'INTERNET',
            category: 'CONNECTIVITY',
            status: 'ACTIVE',
            provisioningTime: 30,
            autoProvision: true,
            pricing: {
              monthlyFee: 99.99,
              setupFee: 50.00,
              usageRate: '0.05 per GB',
              currency: 'USD'
            },
            createdAt: '2024-01-01T00:00:00Z',
            updatedAt: '2024-01-01T00:00:00Z',
            version: 1
          })
        })
      })

      await page.goto('/services/1')

      const pricingGrid = page.locator('.pricing-grid .pricing-item')
      await expect(pricingGrid).toHaveCount(3)
      await expect(pricingGrid.first()).toContainText('Monthly Fee')
      await expect(pricingGrid.nth(1)).toContainText('Setup Fee')
      await expect(pricingGrid.nth(2)).toContainText('Usage Rate')
    })
  })

  test.describe('Service Activations', () => {
    test.beforeEach(async ({ page }) => {
      await page.route('**/api/services/activations**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            content: [],
            totalElements: 0,
            totalPages: 0,
            number: 0,
            size: 20
          })
        })
      })
    })

    test('should navigate to activations page', async ({ page }) => {
      await page.goto('/services/activations')

      await expect(page.locator('h1.page-title')).toContainText('Service Activations')
      await expect(page.locator('text=Monitor and manage service')).toBeVisible()
    })

    test('should display activations summary cards', async ({ page }) => {
      await page.goto('/services/activations')

      await expect(page.locator('.summary-card')).toHaveCount(4)
      await expect(page.locator('text=Total Activations')).toBeVisible()
      await expect(page.locator('text=Active')).toBeVisible()
      await expect(page.locator('text=Pending')).toBeVisible()
      await expect(page.locator('text=Failed')).toBeVisible()
    })

    test('should filter activations by status', async ({ page }) => {
      await page.goto('/services/activations')

      await page.selectOption('[data-testid="status-filter"]', 'ACTIVE')
      await page.waitForTimeout(500)

      const calls = page.request.allInterceptions()
      const filterCall = calls.find(c => c.request.url().includes('status'))
      expect(filterCall).toBeDefined()
    })

    test('should retry failed activation', async ({ page }) => {
      // Mock activations list with one failed
      await page.route('**/api/services/activations**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            content: [
              {
                id: '1',
                activationId: 'ACT-2024-001',
                serviceId: '1',
                serviceCode: 'SVC-INTERNET-001',
                serviceName: 'Premium Internet',
                customerId: 'cust-1',
                customerName: 'John Doe',
                status: 'FAILED',
                scheduledDate: '2024-01-15T10:00:00Z',
                progress: 0
              }
            ],
            totalElements: 1,
            totalPages: 1,
            number: 0,
            size: 20
          })
        })
      })

      await page.route('**/api/services/activations/1/retry**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true })
        })
      })

      await page.goto('/services/activations')

      const retryButton = page.locator('button[icon="pi pi-refresh"]').first()
      await retryButton.click()

      // Handle confirmation dialog
      page.on('dialog', dialog => dialog.accept())
      await page.waitForTimeout(500)

      await expect(page.locator('text=Activation ACT-2024-001 has been queued for retry')).toBeVisible()
    })

    test('should cancel pending activation', async ({ page }) => {
      // Mock activations list with one pending
      await page.route('**/api/services/activations**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            content: [
              {
                id: '2',
                activationId: 'ACT-2024-002',
                serviceId: '1',
                serviceCode: 'SVC-INTERNET-001',
                serviceName: 'Premium Internet',
                customerId: 'cust-2',
                customerName: 'Jane Smith',
                status: 'PENDING',
                scheduledDate: '2024-01-20T10:00:00Z',
                progress: 0
              }
            ],
            totalElements: 1,
            totalPages: 1,
            number: 0,
            size: 20
          })
        })
      })

      await page.route('**/api/services/activations/2/cancel**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true })
        })
      })

      await page.goto('/services/activations')

      const cancelButton = page.locator('button[icon="pi pi-times"]').first()
      await cancelButton.click()

      // Handle confirmation dialog
      page.on('dialog', dialog => dialog.accept())
      await page.waitForTimeout(500)

      await expect(page.locator('text=Activation ACT-2024-002 has been cancelled')).toBeVisible()
    })

    test('should display activation progress', async ({ page }) => {
      // Mock activations list with different progress levels
      await page.route('**/api/services/activations**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            content: [
              {
                id: '3',
                activationId: 'ACT-2024-003',
                serviceId: '1',
                serviceCode: 'SVC-INTERNET-001',
                serviceName: 'Premium Internet',
                customerId: 'cust-3',
                customerName: 'Bob Johnson',
                status: 'PROVISIONING',
                scheduledDate: '2024-01-20T15:00:00Z',
                progress: 65
              }
            ],
            totalElements: 1,
            totalPages: 1,
            number: 0,
            size: 20
          })
        })
      })

      await page.goto('/services/activations')

      const progressBar = page.locator('.p-progressbar')
      await expect(progressBar).toBeVisible()
      await expect(progressBar).toHaveAttribute('aria-valuenow', '65')
    })

    test('should handle empty state when no activations', async ({ page }) => {
      await page.goto('/services/activations')

      const emptyState = page.locator('.empty-state')
      await expect(emptyState).toBeVisible()
      await expect(emptyState.locator('h3')).toContainText('No activations found')
    })
  })
})
