/**
 * Products Page Tests
 *
 * Comprehensive test coverage for products page functionality
 * Tests product listing, product details, CRUD operations, filtering, search, and bulk actions
 *
 * Priority: 🔴 HIGH
 * Learning Value: ⭐⭐⭐⭐⭐
 */

import { test, expect } from '@playwright/test'

test.describe('Products Page', () => {
  test.describe('Products List View', () => {
    test.beforeEach(async ({ page }) => {
      // Mock products list API response
      await page.route('**/api/products**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            content: [
              {
                id: 'prod-001',
                name: '5G Wireless Router',
                sku: 'WR-5G-001',
                category: 'Networking',
                price: 299.99,
                stock: 150,
                status: 'ACTIVE',
                image: '/images/router.jpg',
                rating: 4.5,
                salesCount: 342
              },
              {
                id: 'prod-002',
                name: 'Fiber Optic Cable 100m',
                sku: 'FOC-100M',
                category: 'Cables',
                price: 89.99,
                stock: 75,
                status: 'ACTIVE',
                image: '/images/cable.jpg',
                rating: 4.8,
                salesCount: 567
              },
              {
                id: 'prod-003',
                name: 'Network Switch 24-Port',
                sku: 'NS-24P',
                category: 'Networking',
                price: 599.99,
                stock: 0,
                status: 'OUT_OF_STOCK',
                image: '/images/switch.jpg',
                rating: 4.6,
                salesCount: 234
              }
            ],
            totalElements: 3,
            totalPages: 1,
            number: 0,
            size: 20
          })
        })
      })
    })

    test('should navigate to products page', async ({ page }) => {
      await page.goto('/products')

      await expect(page.locator('h1.page-title')).toContainText('Products')
      await expect(page.locator('[data-testid="products-count"]')).toContainText('3')
    })

    test('should display products in grid view', async ({ page }) => {
      await page.goto('/products')

      await expect(page.locator('[data-testid="view-toggle"]')).toHaveValue('grid')
      await expect(page.locator('[data-testid="products-grid"]')).toBeVisible()
      await expect(page.locator('[data-testid="product-card"]')).toHaveCount(3)
    })

    test('should switch to list view', async ({ page }) => {
      await page.goto('/products')

      await page.selectOption('[data-testid="view-toggle"]', 'list')

      await expect(page.locator('[data-testid="products-list"]')).toBeVisible()
      await expect(page.locator('[data-testid="product-row"]')).toHaveCount(3)
    })

    test('should display product information in grid view', async ({ page }) => {
      await page.goto('/products')

      const firstProduct = page.locator('[data-testid="product-card"]').first()

      await expect(firstProduct.locator('[data-testid="product-image"]')).toBeVisible()
      await expect(firstProduct.locator('[data-testid="product-name"]')).toContainText('5G Wireless Router')
      await expect(firstProduct.locator('[data-testid="product-sku"]')).toContainText('WR-5G-001')
      await expect(firstProduct.locator('[data-testid="product-price"]')).toContainText('299.99')
      await expect(firstProduct.locator('[data-testid="product-stock"]')).toContainText('150')
    })

    test('should display product information in list view', async ({ page }) => {
      await page.goto('/products')

      await page.selectOption('[data-testid="view-toggle"]', 'list')

      const firstProduct = page.locator('[data-testid="product-row"]').first()

      await expect(firstProduct.locator('[data-testid="product-name"]')).toContainText('5G Wireless Router')
      await expect(firstProduct.locator('[data-testid="product-category"]')).toContainText('Networking')
      await expect(firstProduct.locator('[data-testid="product-price"]')).toContainText('299.99')
      await expect(firstProduct.locator('[data-testid="product-stock"]')).toContainText('150')
      await expect(firstProduct.locator('[data-testid="product-sales"]')).toContainText('342')
    })

    test('should filter by category', async ({ page }) => {
      await page.route('**/api/products**', async (route) => {
        const url = new URL(route.request().url())
        const category = url.searchParams.get('category')

        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            content: category === 'Networking' ? [
              { id: 'prod-001', name: '5G Wireless Router', category: 'Networking', price: 299.99, stock: 150, status: 'ACTIVE' }
            ] : [],
            totalElements: category === 'Networking' ? 1 : 0
          })
        })
      })

      await page.goto('/products')

      await page.selectOption('[data-testid="category-filter"]', 'Networking')

      await expect(page.locator('[data-testid="product-card"]')).toHaveCount(1)
    })

    test('should filter by status', async ({ page }) => {
      await page.goto('/products')

      await page.selectOption('[data-testid="status-filter"]', 'ACTIVE')

      const products = page.locator('[data-testid="product-card"]')
      const count = await products.count()

      for (let i = 0; i < count; i++) {
        await expect(products.nth(i)).toHaveClass(/active/)
      }
    })

    test('should filter by price range', async ({ page }) => {
      await page.goto('/products')

      await page.fill('[data-testid="price-min"]', '0')
      await page.fill('[data-testid="price-max"]', '300')

      await page.click('[data-testid="apply-filters"]')

      // Verify all products are under 300
      const products = page.locator('[data-testid="product-card"]')
      const count = await products.count()

      for (let i = 0; i < count; i++) {
        const priceText = await products.nth(i).locator('[data-testid="product-price"]').textContent()
        const price = parseFloat(priceText?.replace(/[^0-9.]/g, '') || '0')
        expect(price).toBeLessThanOrEqual(300)
      }
    })

    test('should search products', async ({ page }) => {
      await page.route('**/api/products**', async (route) => {
        const url = new URL(route.request().url())
        const search = url.searchParams.get('search')

        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            content: search === 'router' ? [
              { id: 'prod-001', name: '5G Wireless Router', category: 'Networking', price: 299.99, stock: 150, status: 'ACTIVE' }
            ] : [],
            totalElements: search === 'router' ? 1 : 0
          })
        })
      })

      await page.goto('/products')

      await page.fill('[data-testid="search-input"]', 'router')
      await page.keyboard.press('Enter')

      await expect(page.locator('[data-testid="product-card"]')).toHaveCount(1)
      await expect(page.locator('[data-testid="product-card"]')).toContainText('router')
    })

    test('should sort products', async ({ page }) => {
      await page.goto('/products')

      await page.selectOption('[data-testid="sort-select"]', 'price-asc')

      const calls = page.request.allInterceptions()
      const sortCall = calls.find(c => c.request.url().includes('sortBy=price'))
      expect(sortCall).toBeDefined()
    })

    test('should clear all filters', async ({ page }) => {
      await page.goto('/products')

      // Apply some filters
      await page.selectOption('[data-testid="category-filter"]', 'Networking')
      await page.fill('[data-testid="price-min"]', '0')

      await page.click('[data-testid="clear-filters"]')

      // Verify filters are cleared
      await expect(page.locator('[data-testid="category-filter"]')).toHaveValue('')
      await expect(page.locator('[data-testid="price-min"]')).toHaveValue('')
    })

    test('should display out of stock products', async ({ page }) => {
      await page.goto('/products')

      const outOfStockProduct = page.locator('[data-testid="product-row"]').filter({ hasText: 'Network Switch' })

      await expect(outOfStockProduct.locator('[data-testid="product-status"]')).toContainText('Out of Stock')
      await expect(outOfStockProduct).toHaveClass(/out-of-stock/)
    })
  })

  test.describe('Product Details', () => {
    test.beforeEach(async ({ page }) => {
      await page.route('**/api/products/prod-001**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            id: 'prod-001',
            name: '5G Wireless Router',
            sku: 'WR-5G-001',
            description: 'High-performance 5G wireless router with advanced security features',
            category: 'Networking',
            price: 299.99,
            cost: 150.00,
            stock: 150,
            minStock: 10,
            status: 'ACTIVE',
            images: ['/images/router-1.jpg', '/images/router-2.jpg', '/images/router-3.jpg'],
            specifications: {
              frequency: '5GHz',
              range: 'Up to 100m',
              ports: '4 x Ethernet, 1 x WAN',
              dimensions: '200 x 150 x 40mm',
              weight: '500g'
            },
            variants: [
              { id: 'var-001', name: 'Black', sku: 'WR-5G-001-BLK', stock: 75 },
              { id: 'var-002', name: 'White', sku: 'WR-5G-001-WHT', stock: 75 }
            ],
            relatedProducts: [
              { id: 'prod-004', name: 'Ethernet Cable', price: 19.99 }
            ]
          })
        })
      })
    })

    test('should view product details', async ({ page }) => {
      await page.goto('/products/prod-001')

      await expect(page.locator('h1.page-title')).toContainText('5G Wireless Router')
      await expect(page.locator('[data-testid="product-sku"]')).toContainText('WR-5G-001')
      await expect(page.locator('[data-testid="product-price"]')).toContainText('299.99')
      await expect(page.locator('[data-testid="product-description"]')).toBeVisible()
      await expect(page.locator('[data-testid="product-stock"]')).toContainText('150')
    })

    test('should display product images gallery', async ({ page }) => {
      await page.goto('/products/prod-001')

      await expect(page.locator('[data-testid="product-gallery"]')).toBeVisible()
      await expect(page.locator('[data-testid="product-image-main"]')).toBeVisible()
      await expect(page.locator('[data-testid="product-thumbnails"]')).toBeVisible()
      await expect(page.locator('[data-testid="product-thumbnail"]')).toHaveCount(3)
    })

    test('should switch product images', async ({ page }) => {
      await page.goto('/products/prod-001')

      // Click on thumbnail
      await page.click('[data-testid="product-thumbnail"]').nth(1)

      // Main image should change
      await expect(page.locator('[data-testid="product-image-main"]'))
        .toHaveAttribute('src', '/images/router-2.jpg')
    })

    test('should display product specifications', async ({ page }) => {
      await page.goto('/products/prod-001')

      await page.click('[data-testid="specifications-tab"]')

      await expect(page.locator('[data-testid="spec-frequency"]')).toContainText('5GHz')
      await expect(page.locator('[data-testid="spec-range"]')).toContainText('Up to 100m')
      await expect(page.locator('[data-testid="spec-ports"]')).toContainText('4 x Ethernet, 1 x WAN')
      await expect(page.locator('[data-testid="spec-dimensions"]')).toContainText('200 x 150 x 40mm')
    })

    test('should display product variants', async ({ page }) => {
      await page.goto('/products/prod-001')

      await page.click('[data-testid="variants-tab"]')

      await expect(page.locator('[data-testid="variant-option"]')).toHaveCount(2)
      await expect(page.locator('[data-testid="variant-option"]').first()).toContainText('Black')
      await expect(page.locator('[data-testid="variant-option"]').nth(1)).toContainText('White')
    })

    test('should select product variant', async ({ page }) => {
      await page.goto('/products/prod-001')

      await page.click('[data-testid="variants-tab"]')
      await page.click('[data-testid="variant-option"]').first()

      await expect(page.locator('[data-testid="variant-option"]').first()).toHaveClass(/selected/)
      await expect(page.locator('[data-testid="selected-variant-sku"]')).toContainText('WR-5G-001-BLK')
    })

    test('should show related products', async ({ page }) => {
      await page.goto('/products/prod-001')

      await page.click('[data-testid="related-products-tab"]')

      await expect(page.locator('[data-testid="related-product-card"]')).toHaveCount(1)
      await expect(page.locator('[data-testid="related-product-name"]')).toContainText('Ethernet Cable')
    })

    test('should navigate to related product', async ({ page }) => {
      await page.goto('/products/prod-001')

      await page.click('[data-testid="related-products-tab"]')
      await page.click('[data-testid="related-product-card"]')

      await expect(page).toHaveURL('/products/prod-004')
    })

    test('should display stock status indicator', async ({ page }) => {
      await page.goto('/products/prod-001')

      await expect(page.locator('[data-testid="stock-indicator"]')).toContainText('In Stock')
      await expect(page.locator('[data-testid="stock-indicator"]')).toHaveClass(/in-stock/)

      // Mock low stock
      await page.route('**/api/products/prod-001**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            id: 'prod-001',
            name: '5G Wireless Router',
            stock: 5,
            minStock: 10
          })
        })
      })

      await page.reload()

      await expect(page.locator('[data-testid="stock-indicator"]')).toContainText('Low Stock')
      await expect(page.locator('[data-testid="stock-indicator"]')).toHaveClass(/low-stock/)
    })
  })

  test.describe('Create Product', () => {
    test.beforeEach(async ({ page }) => {
      await page.route('**/api/products**', async (route) => {
        if (route.request().method() === 'POST') {
          await route.fulfill({
            status: 201,
            contentType: 'application/json',
            body: JSON.stringify({
              id: 'prod-999',
              name: 'New Product',
              sku: 'NEW-PROD-001',
              price: 99.99
            })
          })
        }
      })
    })

    test('should navigate to create product page', async ({ page }) => {
      await page.goto('/products')
      await page.click('[data-testid="create-product-btn"]')

      await expect(page).toHaveURL('/products/create')
      await expect(page.locator('h1')).toContainText('Create Product')
    })

    test('should create product with valid data', async ({ page }) => {
      await page.goto('/products/create')

      // Fill form
      await page.fill('[data-testid="product-name"]', 'New Product')
      await page.fill('[data-testid="product-sku"]', 'NEW-PROD-001')
      await page.fill('[data-testid="product-price"]', '99.99')
      await page.fill('[data-testid="product-stock"]', '100')
      await page.selectOption('[data-testid="product-category"]', 'Networking')
      await page.fill('[data-testid="product-description"]', 'Test product description')

      await page.click('[data-testid="save-product-btn"]')

      await expect(page.locator('[data-testid="success-message"]'))
        .toContainText('Product created successfully')
      await expect(page).toHaveURL('/products')
    })

    test('should validate required fields', async ({ page }) => {
      await page.goto('/products/create')

      await page.click('[data-testid="save-product-btn"]')

      await expect(page.locator('[data-testid="error-name"]'))
        .toContainText('required')
      await expect(page.locator('[data-testid="error-sku"]'))
        .toContainText('required')
      await expect(page.locator('[data-testid="error-price"]'))
        .toContainText('required')
    })

    test('should validate SKU uniqueness', async ({ page }) => {
      await page.route('**/api/products/sku/check**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ exists: true })
        })
      })

      await page.goto('/products/create')

      await page.fill('[data-testid="product-sku"]', 'DUPLICATE-SKU')
      await page.keyboard.press('Tab') // Trigger validation

      await expect(page.locator('[data-testid="error-sku"]'))
        .toContainText('already exists')
    })

    test('should upload product images', async ({ page }) => {
      await page.goto('/products/create')

      // Create test image
      await page.evaluate(() => {
        const fs = require('fs')
        fs.writeFileSync('test-image.png', 'fake-image-data')
      })

      await page.setInputFiles('[data-testid="image-upload"]', 'test-image.png')

      await expect(page.locator('[data-testid="image-preview"]')).toBeVisible()
      await expect(page.locator('[data-testid="image-preview"]')).toHaveAttribute('src')
    })

    test('should remove uploaded image', async ({ page }) => {
      await page.goto('/products/create')

      // Mock image upload
      await page.route('**/api/upload**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ url: '/images/product-123.jpg' })
        })
      })

      await page.evaluate(() => {
        const fs = require('fs')
        fs.writeFileSync('test-image.png', 'fake-image-data')
      })

      await page.setInputFiles('[data-testid="image-upload"]', 'test-image.png')
      await page.waitForTimeout(500)

      await page.click('[data-testid="remove-image"]')

      await expect(page.locator('[data-testid="image-preview"]')).not.toBeVisible()
    })

    test('should add product specification', async ({ page }) => {
      await page.goto('/products/create')

      await page.click('[data-testid="add-specification"]')

      await page.fill('[data-testid="spec-key-0"]', 'Color')
      await page.fill('[data-testid="spec-value-0"]', 'Black')

      await page.click('[data-testid="add-specification"]')

      await page.fill('[data-testid="spec-key-1"]', 'Material')
      await page.fill('[data-testid="spec-value-1"]', 'Plastic')

      await expect(page.locator('[data-testid="spec-row"]')).toHaveCount(2)
    })

    test('should add product variant', async ({ page }) => {
      await page.goto('/products/create')

      await page.click('[data-testid="add-variant"]')

      await page.fill('[data-testid="variant-name-0"]', 'Black')
      await page.fill('[data-testid="variant-sku-0"]', 'VAR-BLACK')
      await page.fill('[data-testid="variant-price-0]"]', '99.99')
      await page.fill('[data-testid="variant-stock-0]"]', '50')

      await page.click('[data-testid="add-variant"]')

      await page.fill('[data-testid="variant-name-1"]', 'White')
      await page.fill('[data-testid="variant-sku-1"]', 'VAR-WHITE')
      await page.fill('[data-testid="variant-price-1]"]', '99.99')
      await page.fill('[data-testid="variant-stock-1]"]', '50')

      await expect(page.locator('[data-testid="variant-row"]')).toHaveCount(2)
    })

    test('should cancel product creation', async ({ page }) => {
      await page.goto('/products/create')

      await page.fill('[data-testid="product-name"]', 'Draft Product')

      await page.click('[data-testid="cancel-btn"]')

      await expect(page.locator('[data-testid="confirm-dialog"]')).toBeVisible()
      await page.click('[data-testid="confirm-cancel"]')

      await expect(page).toHaveURL('/products')
    })
  })

  test.describe('Edit Product', () => {
    test.beforeEach(async ({ page }) => {
      // Mock get product
      await page.route('**/api/products/prod-001**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            id: 'prod-001',
            name: '5G Wireless Router',
            sku: 'WR-5G-001',
            price: 299.99,
            stock: 150,
            category: 'Networking'
          })
        })
      })

      // Mock update product
      await page.route('**/api/products/prod-001**', async (route) => {
        if (route.request().method() === 'PUT') {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              id: 'prod-001',
              name: 'Updated Router',
              sku: 'WR-5G-001',
              price: 319.99,
              stock: 150
            })
          })
        }
      })
    })

    test('should open edit product page', async ({ page }) => {
      await page.goto('/products/prod-001/edit')

      await expect(page.locator('h1')).toContainText('Edit Product')
      await expect(page.locator('[data-testid="product-name"]')).toHaveValue('5G Wireless Router')
    })

    test('should update product successfully', async ({ page }) => {
      await page.goto('/products/prod-001/edit')

      await page.fill('[data-testid="product-name"]', 'Updated Router')
      await page.fill('[data-testid="product-price"]', '319.99')

      await page.click('[data-testid="save-product-btn"]')

      await expect(page.locator('[data-testid="success-message"]'))
        .toContainText('Product updated successfully')
      await expect(page).toHaveURL('/products/prod-001')
    })

    test('should validate price change', async ({ page }) => {
      await page.goto('/products/prod-001/edit')

      await page.fill('[data-testid="product-price"]', '-10')

      await page.click('[data-testid="save-product-btn"]')

      await expect(page.locator('[data-testid="error-price"]'))
        .toContainText('must be greater than 0')
    })
  })

  test.describe('Delete Product', () => {
    test.beforeEach(async ({ page }) => {
      await page.route('**/api/products/prod-001**', async (route) => {
        if (route.request().method() === 'DELETE') {
          await route.fulfill({
            status: 204
          })
        }
      })
    })

    test('should delete product after confirmation', async ({ page }) => {
      await page.goto('/products/prod-001')

      await page.click('[data-testid="delete-product-btn"]')

      await expect(page.locator('[data-testid="confirm-dialog"]')).toBeVisible()
      await page.click('[data-testid="confirm-delete"]')

      await expect(page.locator('[data-testid="success-message"]'))
        .toContainText('Product deleted successfully')
      await expect(page).toHaveURL('/products')
    })

    test('should cancel deletion', async ({ page }) => {
      await page.goto('/products/prod-001')

      await page.click('[data-testid="delete-product-btn"]')
      await page.click('[data-testid="cancel-delete"]')

      await expect(page.locator('[data-testid="confirm-dialog"]')).not.toBeVisible()
      await expect(page).toHaveURL('/products/prod-001')
    })
  })

  test.describe('Bulk Actions', () => {
    test('should select multiple products', async ({ page }) => {
      await page.goto('/products')

      // Select first product
      await page.click('[data-testid="select-product-0"]')
      await expect(page.locator('[data-testid="product-0"]')).toHaveClass(/selected/)

      // Select second product
      await page.click('[data-testid="select-product-1"]')
      await expect(page.locator('[data-testid="product-1"]')).toHaveClass(/selected/)

      // Verify bulk actions bar appears
      await expect(page.locator('[data-testid="bulk-actions-bar"]')).toBeVisible()
      await expect(page.locator('[data-testid="selected-count"]')).toContainText('2')
    })

    test('should select all products', async ({ page }) => {
      await page.goto('/products')

      await page.click('[data-testid="select-all"]')

      const products = page.locator('[data-testid="product-card"]')
      const count = await products.count()

      for (let i = 0; i < count; i++) {
        await expect(products.nth(i)).toHaveClass(/selected/)
      }
    })

    test('should bulk delete products', async ({ page }) => {
      await page.route('**/api/products/bulk-delete**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ deleted: 2 })
        })
      })

      await page.goto('/products')

      // Select products
      await page.click('[data-testid="select-product-0"]')
      await page.click('[data-testid="select-product-1"]')

      await page.click('[data-testid="bulk-delete-btn"]')

      await expect(page.locator('[data-testid="confirm-dialog"]')).toBeVisible()
      await page.click('[data-testid="confirm-delete"]')

      await expect(page.locator('[data-testid="success-message"]'))
        .toContainText('2 products deleted')
    })

    test('should bulk update status', async ({ page }) => {
      await page.route('**/api/products/bulk-status**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ updated: 2 })
        })
      })

      await page.goto('/products')

      // Select products
      await page.click('[data-testid="select-product-0"]')
      await page.click('[data-testid="select-product-1"]')

      await page.selectOption('[data-testid="bulk-status-select"]', 'INACTIVE')
      await page.click('[data-testid="bulk-update-btn"]')

      await expect(page.locator('[data-testid="success-message"]'))
        .toContainText('2 products updated')
    })

    test('should export selected products', async ({ page }) => {
      await page.goto('/products')

      // Select products
      await page.click('[data-testid="select-product-0"]')

      const downloadPromise = page.waitForEvent('download')
      await page.click('[data-testid="export-selected-btn"]')

      const download = await downloadPromise
      expect(download.suggestedFilename()).toContain('products')
    })
  })

  test.describe('Error Handling', () => {
    test('should handle API errors', async ({ page }) => {
      await page.route('**/api/products**', async (route) => {
        await route.fulfill({
          status: 500,
          contentType: 'application/json',
          body: JSON.stringify({ error: 'Internal Server Error' })
        })
      })

      await page.goto('/products')

      await expect(page.locator('[data-testid="error-message"]'))
        .toContainText('Failed to load products')
    })

    test('should handle network offline', async ({ page }) => {
      await page.context().setOffline(true)

      await page.goto('/products')

      await expect(page.locator('[data-testid="offline-message"]')).toBeVisible()
    })

    test('should show error for failed image upload', async ({ page }) => {
      await page.route('**/api/upload**', async (route) => {
        await route.fulfill({
          status: 500,
          contentType: 'application/json',
          body: JSON.stringify({ error: 'Upload failed' })
        })
      })

      await page.goto('/products/create')

      await page.evaluate(() => {
        const fs = require('fs')
        fs.writeFileSync('test-image.png', 'fake-image-data')
      })

      await page.setInputFiles('[data-testid="image-upload"]', 'test-image.png')

      await expect(page.locator('[data-testid="error-image"]'))
        .toContainText('Upload failed')
    })
  })

  test.describe('Accessibility', () => {
    test('should support keyboard navigation', async ({ page }) => {
      await page.goto('/products')

      // Tab through products
      await page.keyboard.press('Tab')
      await page.keyboard.press('Tab')

      const focused = await page.evaluate(() => document.activeElement?.getAttribute('data-testid'))
      expect(focused).toBeDefined()
    })

    test('should have proper ARIA labels', async ({ page }) => {
      await page.goto('/products')

      await expect(page.locator('[data-testid="search-input"]'))
        .toHaveAttribute('aria-label', 'Search products')

      await expect(page.locator('[data-testid="create-product-btn"]'))
        .toHaveAttribute('aria-label', 'Create new product')
    })

    test('should announce product count to screen readers', async ({ page }) => {
      await page.goto('/products')

      const count = await page.evaluate(() => {
        return document.querySelector('[aria-live]')?.textContent || ''
      })

      expect(count).toContain('products')
    })
  })
})
