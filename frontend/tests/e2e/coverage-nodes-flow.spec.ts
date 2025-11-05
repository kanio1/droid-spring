/**
 * Coverage Nodes E2E Flow Tests
 *
 * Complete test coverage for coverage node management functionality
 * Tests CRUD operations, equipment management, coverage area creation, and map visualization
 *
 * Priority: 🔴 HIGH
 * Learning Value: ⭐⭐⭐⭐
 */

import { test, expect } from '@playwright/test'

test.describe('Coverage Nodes Flow', () => {
  test.describe('Coverage Nodes List', () => {
    test.beforeEach(async ({ page }) => {
      // Mock API responses
      await page.route('**/api/coverage-nodes**', async (route) => {
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

    test('should navigate to coverage nodes page', async ({ page }) => {
      await page.goto('/coverage-nodes')

      await expect(page.locator('h1.page-title')).toContainText('Coverage Nodes')
      await expect(page.locator('text=Manage network coverage areas')).toBeVisible()
    })

    test('should display coverage nodes summary cards', async ({ page }) => {
      await page.goto('/coverage-nodes')

      await expect(page.locator('.summary-card')).toHaveCount(5)
      await expect(page.locator('text=Total Nodes')).toBeVisible()
      await expect(page.locator('text=Active')).toBeVisible()
      await expect(page.locator('text=Inactive')).toBeVisible()
      await expect(page.locator('text=Equipment Online')).toBeVisible()
      await expect(page.locator('text=Coverage Area')).toBeVisible()
    })

    test('should filter by coverage type', async ({ page }) => {
      await page.goto('/coverage-nodes')

      await page.selectOption('[data-testid="coverage-type-filter"]', '5G')
      await page.waitForTimeout(500)

      const calls = page.request.allInterceptions()
      const filterCall = calls.find(c => c.request.url().includes('coverageType'))
      expect(filterCall).toBeDefined()
    })

    test('should search coverage nodes', async ({ page }) => {
      await page.goto('/coverage-nodes')

      const searchInput = page.locator('input[placeholder*="Search nodes"]')
      await searchInput.fill('Node-5G-001')
      await page.keyboard.press('Enter')
      await page.waitForTimeout(500)

      const calls = page.request.allInterceptions()
      const searchCall = calls.find(c => c.request.url().includes('searchTerm'))
      expect(searchCall).toBeDefined()
    })

    test('should display empty state when no coverage nodes', async ({ page }) => {
      await page.goto('/coverage-nodes')

      await expect(page.locator('.empty-state')).toBeVisible()
      await expect(page.locator('text=No coverage nodes found')).toBeVisible()
      await expect(page.locator('[data-testid="create-node-btn"]')).toBeVisible()
    })

    test('should toggle map view', async ({ page }) => {
      await page.goto('/coverage-nodes')

      await page.click('[data-testid="map-view-toggle"]')

      await expect(page.locator('[data-testid="coverage-map"]')).toBeVisible()
      await expect(page.locator('.map-marker')).toBeVisible()
    })
  })

  test.describe('Create Coverage Node', () => {
    test.beforeEach(async ({ page }) => {
      await page.route('**/api/coverage-nodes**', async (route) => {
        if (route.request().method() === 'POST') {
          await route.fulfill({
            status: 201,
            contentType: 'application/json',
            body: JSON.stringify({
              id: 'node-123',
              name: '5G Node Downtown',
              latitude: 40.7128,
              longitude: -74.0060,
              coverageType: '5G',
              radius: 5000,
              status: 'ACTIVE',
              equipmentCount: 3,
              coverageArea: 78.5
            })
          })
        } else {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({ content: [], totalElements: 0 })
          })
        }
      })
    })

    test('should open create coverage node modal', async ({ page }) => {
      await page.goto('/coverage-nodes')
      await page.click('[data-testid="create-node-btn"]')

      await expect(page.locator('[data-testid="coverage-node-modal"]')).toBeVisible()
      await expect(page.locator('h2.modal-title')).toContainText('Create Coverage Node')
    })

    test('should create new coverage node with valid data', async ({ page }) => {
      await page.goto('/coverage-nodes')
      await page.click('[data-testid="create-node-btn"]')

      // Fill form
      await page.fill('[name="name"]', '5G Node Downtown')
      await page.fill('[name="latitude"]', '40.7128')
      await page.fill('[name="longitude"]', '-74.0060')
      await page.selectOption('[name="coverageType"]', '5G')
      await page.fill('[name="radius"]', '5000')

      // Submit
      await page.click('[data-testid="save-node-btn"]')

      // Verify success
      await expect(page.locator('[data-testid="toast-success"]')).toContainText('Coverage node created successfully')
      await expect(page.locator('[data-testid="coverage-node-modal"]')).not.toBeVisible()
    })

    test('should validate required fields', async ({ page }) => {
      await page.goto('/coverage-nodes')
      await page.click('[data-testid="create-node-btn"]')

      // Try to submit without filling required fields
      await page.click('[data-testid="save-node-btn"]')

      // Verify validation errors
      await expect(page.locator('[name="name"]')).toHaveAttribute('aria-invalid', 'true')
      await expect(page.locator('[name="latitude"]')).toHaveAttribute('aria-invalid', 'true')
      await expect(page.locator('.error-message')).toContainText('This field is required')
    })

    test('should validate coordinate ranges', async ({ page }) => {
      await page.goto('/coverage-nodes')
      await page.click('[data-testid="create-node-btn"]')

      await page.fill('[name="latitude"]', '999')
      await page.fill('[name="longitude"]', '999')

      await page.click('[data-testid="save-node-btn"]')

      await expect(page.locator('.error-message')).toContainText('must be between')
    })
  })

  test.describe('Edit Coverage Node', () => {
    test.beforeEach(async ({ page }) => {
      // Mock get node
      await page.route('**/api/coverage-nodes/node-123', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            id: 'node-123',
            name: '5G Node Downtown',
            latitude: 40.7128,
            longitude: -74.0060,
            coverageType: '5G',
            radius: 5000,
            status: 'ACTIVE',
            equipmentCount: 3,
            coverageArea: 78.5
          })
        })
      })

      // Mock update
      await page.route('**/api/coverage-nodes/node-123', async (route) => {
        if (route.request().method() === 'PUT') {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              id: 'node-123',
              name: '5G Node Downtown Updated',
              latitude: 40.7128,
              longitude: -74.0060,
              coverageType: '5G',
              radius: 6000,
              status: 'ACTIVE',
              equipmentCount: 3,
              coverageArea: 78.5
            })
          })
        }
      })

      // Mock list
      await page.route('**/api/coverage-nodes**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            content: [
              {
                id: 'node-123',
                name: '5G Node Downtown',
                latitude: 40.7128,
                longitude: -74.0060,
                coverageType: '5G',
                radius: 5000,
                status: 'ACTIVE',
                equipmentCount: 3,
                coverageArea: 78.5
              }
            ],
            totalElements: 1,
            totalPages: 1,
            number: 0,
            size: 20
          })
        })
      })
    })

    test('should open edit modal from list', async ({ page }) => {
      await page.goto('/coverage-nodes')

      await page.click('[data-testid="edit-node-node-123"]')

      await expect(page.locator('[data-testid="coverage-node-modal"]')).toBeVisible()
      await expect(page.locator('h2.modal-title')).toContainText('Edit Coverage Node')
      await expect(page.locator('[name="name"]')).toHaveValue('5G Node Downtown')
    })

    test('should update coverage node successfully', async ({ page }) => {
      await page.goto('/coverage-nodes')
      await page.click('[data-testid="edit-node-node-123"]')

      // Update radius
      await page.fill('[name="radius"]', '6000')
      await page.click('[data-testid="save-node-btn"]')

      // Verify success
      await expect(page.locator('[data-testid="toast-success"]'))
        .toContainText('Coverage node updated successfully')
      await expect(page.locator('[data-testid="coverage-node-modal"]')).not.toBeVisible()
    })
  })

  test.describe('Delete Coverage Node', () => {
    test.beforeEach(async ({ page }) => {
      await page.route('**/api/coverage-nodes**', async (route) => {
        if (route.request().method() === 'GET') {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              content: [
                {
                  id: 'node-123',
                  name: '5G Node Downtown',
                  latitude: 40.7128,
                  longitude: -74.0060,
                  coverageType: '5G',
                  radius: 5000,
                  status: 'ACTIVE',
                  equipmentCount: 3,
                  coverageArea: 78.5
                }
              ],
              totalElements: 1,
              totalPages: 1,
              number: 0,
              size: 20
            })
          })
        } else if (route.request().method() === 'DELETE') {
          await route.fulfill({
            status: 204
          })
        }
      })
    })

    test('should delete coverage node after confirmation', async ({ page }) => {
      await page.goto('/coverage-nodes')

      await page.click('[data-testid="delete-node-node-123"]')

      // Confirm deletion
      await expect(page.locator('[data-testid="confirm-dialog"]')).toBeVisible()
      await page.click('[data-testid="confirm-delete-btn"]')

      // Verify success
      await expect(page.locator('[data-testid="toast-success"]'))
        .toContainText('Coverage node deleted successfully')
    })

    test('should not delete when cancel is clicked', async ({ page }) => {
      await page.goto('/coverage-nodes')

      await page.click('[data-testid="delete-node-node-123"]')
      await page.click('[data-testid="cancel-delete-btn"]')

      // Verify dialog is closed
      await expect(page.locator('[data-testid="confirm-dialog"]')).not.toBeVisible()
    })

    test('should prevent deletion if equipment is active', async ({ page }) => {
      await page.route('**/api/coverage-nodes**', async (route) => {
        if (route.request().method() === 'GET') {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              content: [
                {
                  id: 'node-123',
                  name: '5G Node Downtown',
                  latitude: 40.7128,
                  longitude: -74.0060,
                  coverageType: '5G',
                  radius: 5000,
                  status: 'ACTIVE',
                  equipmentCount: 3,
                  coverageArea: 78.5,
                  hasActiveEquipment: true
                }
              ],
              totalElements: 1,
              totalPages: 1,
              number: 0,
              size: 20
            })
          })
        }
      })

      await page.goto('/coverage-nodes')

      await page.click('[data-testid="delete-node-node-123"]')

      // Should show warning
      await expect(page.locator('[data-testid="warning-dialog"]')).toBeVisible()
      await expect(page.locator('text=Cannot delete node with active equipment')).toBeVisible()
    })
  })

  test.describe('Equipment Management', () => {
    test.beforeEach(async ({ page }) => {
      await page.route('**/api/coverage-nodes/node-123/equipment**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([
            {
              id: 'eq-001',
              name: '5G Antenna A1',
              type: 'ANTENNA',
              status: 'ONLINE',
              lastMaintenance: '2024-01-15'
            },
            {
              id: 'eq-002',
              name: 'Base Station B2',
              type: 'BASE_STATION',
              status: 'ONLINE',
              lastMaintenance: '2024-01-10'
            }
          ])
        })
      })
    })

    test('should display equipment list for node', async ({ page }) => {
      await page.goto('/coverage-nodes/node-123')

      await expect(page.locator('h1.page-title')).toContainText('Coverage Node Details')

      // Navigate to equipment tab
      await page.click('[data-testid="equipment-tab"]')

      await expect(page.locator('[data-testid="equipment-list"]')).toBeVisible()
      await expect(page.locator('[data-testid="equipment-eq-001"]')).toBeVisible()
      await expect(page.locator('[data-testid="equipment-eq-002"]')).toBeVisible()
    })

    test('should add new equipment', async ({ page }) => {
      await page.route('**/api/coverage-nodes/node-123/equipment**', async (route) => {
        if (route.request().method() === 'POST') {
          await route.fulfill({
            status: 201,
            contentType: 'application/json',
            body: JSON.stringify({
              id: 'eq-003',
              name: 'New Equipment',
              type: 'ROUTER',
              status: 'ONLINE'
            })
          })
        }
      })

      await page.goto('/coverage-nodes/node-123')
      await page.click('[data-testid="equipment-tab"]')

      await page.click('[data-testid="add-equipment-btn"]')

      await page.fill('[name="name"]', 'New Equipment')
      await page.selectOption('[name="type"]', 'ROUTER')

      await page.click('[data-testid="save-equipment-btn"]')

      await expect(page.locator('[data-testid="toast-success"]'))
        .toContainText('Equipment added successfully')
    })

    test('should update equipment status', async ({ page }) => {
      await page.route('**/api/coverage-nodes/node-123/equipment/eq-001**', async (route) => {
        if (route.request().method() === 'PATCH') {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              id: 'eq-001',
              name: '5G Antenna A1',
              type: 'ANTENNA',
              status: 'MAINTENANCE'
            })
          })
        }
      })

      await page.goto('/coverage-nodes/node-123')
      await page.click('[data-testid="equipment-tab"]')

      await page.click('[data-testid="equipment-eq-001"] [data-testid="status-toggle"]')

      await expect(page.locator('[data-testid="toast-success"]'))
        .toContainText('Equipment status updated')
    })
  })

  test.describe('Coverage Area Visualization', () => {
    test.beforeEach(async ({ page }) => {
      await page.route('**/api/coverage-nodes/node-123/coverage**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            area: 78.5,
            boundaries: [
              { lat: 40.7128, lng: -74.0060 },
              { lat: 40.7228, lng: -74.0060 },
              { lat: 40.7228, lng: -73.9960 },
              { lat: 40.7128, lng: -73.9960 }
            ]
          })
        })
      })
    })

    test('should display coverage map with node location', async ({ page }) => {
      await page.goto('/coverage-nodes/node-123')

      await page.click('[data-testid="coverage-tab"]')

      await expect(page.locator('[data-testid="coverage-map"]')).toBeVisible()
      await expect(page.locator('.map-marker')).toBeVisible()
    })

    test('should show coverage statistics', async ({ page }) => {
      await page.goto('/coverage-nodes/node-123')

      await page.click('[data-testid="coverage-tab"]')

      await expect(page.locator('[data-testid="coverage-area"]')).toContainText('78.5')
      await expect(page.locator('[data-testid="coverage-area-unit"]')).toContainText('km²')
    })

    test('should allow drawing coverage area', async ({ page }) => {
      await page.goto('/coverage-nodes/node-123')

      await page.click('[data-testid="coverage-tab"]')
      await page.click('[data-testid="draw-coverage-btn"]')

      // Simulate drawing on map
      await page.click('[data-testid="map-canvas"]', { position: { x: 100, y: 100 } })
      await page.click('[data-testid="map-canvas"]', { position: { x: 150, y: 100 } })
      await page.click('[data-testid="map-canvas"]', { position: { x: 150, y: 150 } })
      await page.click('[data-testid="map-canvas"]', { position: { x: 100, y: 150 } })
      await page.click('[data-testid="save-coverage-btn"]')

      await expect(page.locator('[data-testid="toast-success"]'))
        .toContainText('Coverage area saved')
    })
  })

  test.describe('Node Details', () => {
    test.beforeEach(async ({ page }) => {
      await page.route('**/api/coverage-nodes/node-123', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            id: 'node-123',
            name: '5G Node Downtown',
            latitude: 40.7128,
            longitude: -74.0060,
            coverageType: '5G',
            radius: 5000,
            status: 'ACTIVE',
            equipmentCount: 3,
            coverageArea: 78.5,
            createdAt: '2024-01-15T10:00:00Z',
            updatedAt: '2024-01-16T10:00:00Z'
          })
        })
      })
    })

    test('should navigate to coverage node details page', async ({ page }) => {
      await page.goto('/coverage-nodes/node-123')

      await expect(page.locator('h1.page-title')).toContainText('Coverage Node Details')
      await expect(page.locator('[data-testid="node-name"]')).toContainText('5G Node Downtown')
      await expect(page.locator('[data-testid="node-coordinates"]')).toContainText('40.7128, -74.0060')
      await expect(page.locator('[data-testid="node-type"]')).toContainText('5G')
    })

    test('should display node metadata', async ({ page }) => {
      await page.goto('/coverage-nodes/node-123')

      await expect(page.locator('[data-testid="created-at"]')).toContainText('2024-01-15')
      await expect(page.locator('[data-testid="updated-at"]')).toContainText('2024-01-16')
    })

    test('should show node status indicator', async ({ page }) => {
      await page.goto('/coverage-nodes/node-123')

      const statusBadge = page.locator('[data-testid="status-badge"]')
      await expect(statusBadge).toContainText('ACTIVE')
      await expect(statusBadge).toHaveClass(/bg-success/)
    })
  })

  test.describe('Filter and Sort', () => {
    test('should filter nodes by status', async ({ page }) => {
      await page.route('**/api/coverage-nodes**', async (route) => {
        const url = new URL(route.request().url())
        const status = url.searchParams.get('status')

        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            content: status === 'ACTIVE' ? [
              { id: 'node-123', status: 'ACTIVE' }
            ] : [],
            totalElements: status === 'ACTIVE' ? 1 : 0
          })
        })
      })

      await page.goto('/coverage-nodes')
      await page.selectOption('[data-testid="status-filter"]', 'ACTIVE')

      const calls = page.request.allInterceptions()
      const filterCall = calls.find(c => c.request.url().includes('status=ACTIVE'))
      expect(filterCall).toBeDefined()
    })

    test('should sort nodes by coverage area', async ({ page }) => {
      await page.route('**/api/coverage-nodes**', async (route) => {
        const url = new URL(route.request().url())
        const sortBy = url.searchParams.get('sortBy')

        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            content: [],
            totalElements: 0
          })
        })
      })

      await page.goto('/coverage-nodes')
      await page.click('[data-testid="sort-coverage-area"]')

      const calls = page.request.allInterceptions()
      const sortCall = calls.find(c => c.request.url().includes('sortBy=coverageArea'))
      expect(sortCall).toBeDefined()
    })
  })

  test.describe('Accessibility', () => {
    test('should support keyboard navigation', async ({ page }) => {
      await page.goto('/coverage-nodes')

      // Tab through interactive elements
      await page.keyboard.press('Tab')
      await page.keyboard.press('Tab')
      await page.keyboard.press('Tab')

      // Verify focus is visible
      const focusedElement = await page.evaluate(() => document.activeElement?.getAttribute('data-testid'))
      expect(focusedElement).toBeDefined()
    })

    test('should have proper ARIA labels', async ({ page }) => {
      await page.goto('/coverage-nodes')

      await expect(page.locator('[data-testid="create-node-btn"]'))
        .toHaveAttribute('aria-label', 'Create new coverage node')
    })

    test('should support keyboard shortcuts', async ({ page }) => {
      await page.goto('/coverage-nodes')

      // Press 'n' to create new node
      await page.keyboard.press('n')

      await expect(page.locator('[data-testid="coverage-node-modal"]')).toBeVisible()
    })
  })
})
