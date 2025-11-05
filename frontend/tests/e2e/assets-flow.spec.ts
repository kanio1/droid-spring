import { test, expect } from '@playwright/test'

test.describe('Assets Flow', () => {
  test.describe('Assets List', () => {
    test.beforeEach(async ({ page }) => {
      await page.route('**/api/assets**', async (route) => {
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

    test('should navigate to assets page', async ({ page }) => {
      await page.goto('/assets')

      await expect(page.locator('h1.page-title')).toContainText('Assets')
      await expect(page.locator('text=Manage network assets')).toBeVisible()
    })

    test('should display assets summary cards', async ({ page }) => {
      await page.goto('/assets')

      await expect(page.locator('.summary-card')).toHaveCount(4)
      await expect(page.locator('text=Total Assets')).toBeVisible()
      await expect(page.locator('text=Active')).toBeVisible()
      await expect(page.locator('text=Maintenance')).toBeVisible()
      await expect(page.locator('text=Inactive')).toBeVisible()
    })

    test('should display quick access cards', async ({ page }) => {
      await page.goto('/assets')

      const networkElementsCard = page.locator('.category-card').first()
      await expect(networkElementsCard).toContainText('Network Elements')
      await expect(networkElementsCard).toContainText('Routers, switches, base stations')

      const simCardsCard = page.locator('.category-card').nth(1)
      await expect(simCardsCard).toContainText('SIM Cards')
      await expect(simCardsCard).toContainText('Mobile and IoT SIM cards')
    })

    test('should filter by asset type', async ({ page }) => {
      await page.goto('/assets')

      await page.selectOption('[data-testid="asset-type-filter"]', 'NETWORK_ELEMENT')
      await page.waitForTimeout(500)

      const calls = page.request.allInterceptions()
      const filterCall = calls.find(c => c.request.url().includes('assetType'))
      expect(filterCall).toBeDefined()
    })

    test('should search assets', async ({ page }) => {
      await page.goto('/assets')

      const searchInput = page.locator('input[placeholder*="Search assets"]')
      await searchInput.fill('Switch-001')
      await page.keyboard.press('Enter')
      await page.waitForTimeout(500)

      const calls = page.request.allInterceptions()
      const searchCall = calls.find(c => c.request.url().includes('searchTerm'))
      expect(searchCall).toBeDefined()
    })
  })

  test.describe('Network Elements', () => {
    test.beforeEach(async ({ page }) => {
      await page.route('**/api/assets**', async (route) => {
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

    test('should navigate to network elements page', async ({ page }) => {
      await page.goto('/assets/network-elements')

      await expect(page.locator('h1.page-title')).toContainText('Network Elements')
      await expect(page.locator('text=Manage network infrastructure')).toBeVisible()
    })

    test('should display network elements summary cards', async ({ page }) => {
      await page.goto('/assets/network-elements')

      await expect(page.locator('.summary-card')).toHaveCount(4)
      await expect(page.locator('text=Total Elements')).toBeVisible()
      await expect(page.locator('text=Online')).toBeVisible()
      await expect(page.locator('text=Warning')).toBeVisible()
      await expect(page.locator('text=Offline')).toBeVisible()
    })

    test('should filter by element type', async ({ page }) => {
      await page.goto('/assets/network-elements')

      await page.selectOption('[data-testid="element-type-filter"]', 'ROUTER')
      await page.waitForTimeout(500)

      const calls = page.request.allInterceptions()
      const filterCall = calls.find(c => c.request.url().includes('subType'))
      expect(filterCall).toBeDefined()
    })

    test('should ping network element', async ({ page }) => {
      // Mock network elements list
      await page.route('**/api/assets**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            content: [
              {
                id: '1',
                assetId: 'NE-2024-001',
                name: 'Core Router 1',
                assetType: 'NETWORK_ELEMENT',
                subType: 'ROUTER',
                status: 'ACTIVE',
                location: 'Data Center 1',
                ipAddress: '192.168.1.1',
                manufacturer: 'Cisco',
                model: 'Catalyst 2960',
                serialNumber: 'SN123456',
                customerId: null,
                customerName: null,
                createdAt: '2024-01-01T00:00:00Z',
                updatedAt: '2024-01-01T00:00:00Z',
                version: 1
              }
            ],
            totalElements: 1,
            totalPages: 1,
            number: 0,
            size: 20
          })
        })
      })

      await page.goto('/assets/network-elements')

      const pingButton = page.locator('button[icon="pi pi-signal"]').first()
      await pingButton.click()
      await page.waitForTimeout(500)

      await expect(page.locator('text=Element Core Router 1 is reachable')).toBeVisible()
    })
  })

  test.describe('SIM Cards', () => {
    test.beforeEach(async ({ page }) => {
      await page.route('**/api/assets**', async (route) => {
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

    test('should navigate to SIM cards page', async ({ page }) => {
      await page.goto('/assets/sim-cards')

      await expect(page.locator('h1.page-title')).toContainText('SIM Cards')
      await expect(page.locator('text=Manage mobile and IoT SIM')).toBeVisible()
    })

    test('should display SIM cards summary cards', async ({ page }) => {
      await page.goto('/assets/sim-cards')

      await expect(page.locator('.summary-card')).toHaveCount(4)
      await expect(page.locator('text=Total SIMs')).toBeVisible()
      await expect(page.locator('text=Active')).toBeVisible()
      await expect(page.locator('text=Reserved')).toBeVisible()
      await expect(page.locator('text=Suspended')).toBeVisible()
    })

    test('should activate SIM card', async ({ page }) => {
      // Mock SIM cards list
      await page.route('**/api/assets**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            content: [
              {
                id: '1',
                assetId: 'SIM-2024-001',
                name: 'SIM Card 1',
                assetType: 'SIM_CARD',
                subType: 'PHYSICAL_SIM',
                status: 'INACTIVE',
                iccid: '1234567890123456789',
                imsi: '123456789012345',
                phoneNumber: '+1234567890',
                operator: 'VERIZON',
                location: null,
                manufacturer: null,
                model: null,
                serialNumber: 'SIM123456',
                customerId: null,
                customerName: null,
                createdAt: '2024-01-01T00:00:00Z',
                updatedAt: '2024-01-01T00:00:00Z',
                version: 1
              }
            ],
            totalElements: 1,
            totalPages: 1,
            number: 0,
            size: 20
          })
        })
      })

      await page.goto('/assets/sim-cards')

      const activateButton = page.locator('button[icon="pi pi-play"]').first()
      await activateButton.click()
      await page.waitForTimeout(500)

      await expect(page.locator('text=SIM card 1234567890123456789 has been activated')).toBeVisible()
    })
  })

  test.describe('Create Asset', () => {
    test('should navigate to create asset page', async ({ page }) => {
      await page.goto('/assets/create')

      await expect(page.locator('h1.page-title')).toContainText('Create New Asset')
      await expect(page.locator('input[placeholder*="e.g., ASSET-2024-001"]')).toBeVisible()
    })

    test('should create network element asset', async ({ page }) => {
      await page.goto('/assets/create')

      // Fill form
      await page.fill('input[placeholder*="e.g., ASSET-2024-001"]', 'NE-2024-002')
      await page.selectOption('[data-testid="asset-type"]', 'NETWORK_ELEMENT')
      await page.fill('input[placeholder*="Enter asset name"]', 'Edge Switch 1')
      await page.selectOption('[data-testid="status"]', 'ACTIVE')
      await page.fill('input[placeholder*="e.g., Data Center 1"]', 'Data Center 2')
      await page.fill('input[placeholder*="Enter serial number"]', 'SN789012')
      await page.fill('input[placeholder*="e.g., Cisco"]', 'Cisco')
      await page.fill('input[placeholder*="e.g., Catalyst 2960"]', 'Catalyst 3850')
      await page.selectOption('[data-testid="element-type"]', 'SWITCH')
      await page.fill('input[placeholder*="e.g., 192.168.1.1"]', '192.168.2.10')

      // Mock API response
      await page.route('**/api/assets**', async (route) => {
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({ id: '2', success: true })
        })
      })

      await page.click('button:has-text("Create Asset")')
      await page.waitForTimeout(500)

      await expect(page.locator('text=Asset Edge Switch 1 has been created successfully')).toBeVisible()
    })

    test('should create SIM card asset', async ({ page }) => {
      await page.goto('/assets/create')

      // Fill form
      await page.fill('input[placeholder*="e.g., ASSET-2024-001"]', 'SIM-2024-002')
      await page.selectOption('[data-testid="asset-type"]', 'SIM_CARD')
      await page.fill('input[placeholder*="Enter asset name"]', 'SIM Card 2')
      await page.selectOption('[data-testid="status"]', 'INACTIVE')
      await page.fill('input[placeholder*="Enter ICCID"]', '9876543210987654321')
      await page.fill('input[placeholder*="Enter IMSI"]', '987654321098765')
      await page.fill('input[placeholder*="e.g., +1234567890"]', '+1987654321')
      await page.selectOption('[data-testid="sim-type"]', 'PHYSICAL_SIM')
      await page.selectOption('[data-testid="operator"]', 'AT&T')

      await page.route('**/api/assets**', async (route) => {
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({ id: '2', success: true })
        })
      })

      await page.click('button:has-text("Create Asset")')
      await page.waitForTimeout(500)

      await expect(page.locator('text=Asset SIM Card 2 has been created successfully')).toBeVisible()
    })

    test('should add technical specifications', async ({ page }) => {
      await page.goto('/assets/create')

      // Fill basic info
      await page.fill('input[placeholder*="e.g., ASSET-2024-001"]', 'HW-2024-001')
      await page.selectOption('[data-testid="asset-type"]', 'HARDWARE')
      await page.fill('input[placeholder*="Enter asset name"]', 'Server 1')

      // Add specifications
      await page.click('button:has-text("Add Specification")')
      const specRows = page.locator('.spec-row')
      await specRows.first().locator('input').first().fill('CPU')
      await specRows.first().locator('input').nth(1).fill('Intel Xeon')

      await page.click('button:has-text("Add Specification")')
      const secondSpec = page.locator('.spec-row').nth(1)
      await secondSpec.locator('input').first().fill('RAM')
      await secondSpec.locator('input').nth(1).fill('32GB')

      const specs = page.locator('.spec-row')
      await expect(specs).toHaveCount(2)
    })
  })

  test.describe('Asset Details', () => {
    test('should view asset details', async ({ page }) => {
      // Mock single asset response
      await page.route('**/api/assets/1**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            id: '1',
            assetId: 'NE-2024-001',
            name: 'Core Router 1',
            assetType: 'NETWORK_ELEMENT',
            subType: 'ROUTER',
            status: 'ACTIVE',
            location: 'Data Center 1',
            ipAddress: '192.168.1.1',
            manufacturer: 'Cisco',
            model: 'Catalyst 2960',
            serialNumber: 'SN123456',
            customerId: null,
            customerName: null,
            warrantyStart: '2024-01-01T00:00:00Z',
            warrantyEnd: '2025-01-01T00:00:00Z',
            warrantyProvider: 'Cisco',
            specifications: {
              'Port Count': '24',
              'Throughput': '10 Gbps'
            },
            assignments: [],
            maintenanceRecords: [],
            notes: 'Primary core router',
            createdAt: '2024-01-01T00:00:00Z',
            updatedAt: '2024-01-01T00:00:00Z',
            version: 1
          })
        })
      })

      await page.goto('/assets/1')

      await expect(page.locator('h1.page-title')).toContainText('Asset NE-2024-001')
      await expect(page.locator('text=Core Router 1 - Network Element')).toBeVisible()
    })

    test('should display technical specifications', async ({ page }) => {
      await page.route('**/api/assets/1**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            id: '1',
            assetId: 'NE-2024-001',
            name: 'Core Router 1',
            assetType: 'NETWORK_ELEMENT',
            subType: 'ROUTER',
            status: 'ACTIVE',
            location: 'Data Center 1',
            manufacturer: 'Cisco',
            model: 'Catalyst 2960',
            serialNumber: 'SN123456',
            specifications: {
              'Port Count': '24',
              'Throughput': '10 Gbps',
              'Power Consumption': '500W'
            },
            createdAt: '2024-01-01T00:00:00Z',
            updatedAt: '2024-01-01T00:00:00Z',
            version: 1
          })
        })
      })

      await page.goto('/assets/1')

      const specsGrid = page.locator('.specs-grid .spec-item')
      await expect(specsGrid).toHaveCount(3)
      await expect(specsGrid.first()).toContainText('Port Count')
      await expect(specsGrid.nth(1)).toContainText('Throughput')
      await expect(specsGrid.nth(2)).toContainText('Power Consumption')
    })
  })
})
