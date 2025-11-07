/**
 * AI & Machine Learning Testing
 * Demonstrates OCR, Search, Recommendations, NLP, Computer Vision
 */

import { test, expect } from '@playwright/test'
import { DataFactory } from '../framework/data-factories'

test.describe('AI/ML Features Testing', () => {
  test.describe('OCR (Optical Character Recognition)', () => {
    test('should extract text from invoice images', async ({ page }) => {
      // Create test invoice image
      const { createCanvas } = require('canvas')
      const canvas = createCanvas(400, 600)
      const ctx = canvas.getContext('2d')

      // Simulate invoice layout
      ctx.fillStyle = '#ffffff'
      ctx.fillRect(0, 0, 400, 600)

      ctx.fillStyle = '#000000'
      ctx.font = '24px Arial'
      ctx.fillText('INVOICE', 150, 50)

      ctx.font = '16px Arial'
      ctx.fillText('Invoice #: INV-2024-001', 50, 100)
      ctx.fillText('Date: 2024-01-15', 50, 130)
      ctx.fillText('Amount: $1,500.00', 50, 160)
      ctx.fillText('Customer: John Doe', 50, 190)

      const fs = require('fs')
      const filePath = 'test-results/test-invoice.png'
      const buffer = canvas.toBuffer('image/png')
      fs.writeFileSync(filePath, buffer)

      await page.goto('/ocr')
      await page.setInputFiles('input[type="file"]', filePath)

      // Wait for OCR processing
      await expect(page.locator('[data-testid="ocr-status"]')).toHaveText('Processing...')
      await expect(page.locator('[data-testid="ocr-result"]')).toBeVisible({ timeout: 30000 })

      const extractedText = await page.locator('[data-testid="ocr-result"]').textContent()

      // Verify extracted text
      expect(extractedText).toContain('INVOICE')
      expect(extractedText).toMatch(/INV-\d{4}-\d{3}/)
      expect(extractedText).toMatch(/\d{4}-\d{2}-\d{2}/)
      expect(extractedText).toContain('$1,500.00')
      expect(extractedText).toContain('John Doe')

      // Test confidence score
      const confidence = await page.locator('[data-testid="ocr-confidence"]').textContent()
      expect(parseFloat(confidence)).toBeGreaterThan(0.8)

      fs.unlinkSync(filePath)
    })

    test('should handle multi-language OCR', async ({ page }) => {
      // Test with Spanish invoice
      const spanishText = `
        FACTURA
        Número: FACT-2024-001
        Fecha: 15/01/2024
        Cantidad: €1.200,50
        Cliente: María García
      `

      const { createCanvas } = require('canvas')
      const canvas = createCanvas(400, 600)
      const ctx = canvas.getContext('2d')

      ctx.fillStyle = '#ffffff'
      ctx.fillRect(0, 0, 400, 600)

      ctx.fillStyle = '#000000'
      ctx.font = '20px Arial'
      ctx.fillText(spanishText.trim(), 50, 100, 300)

      const fs = require('fs')
      const filePath = 'test-results/spanish-invoice.png'
      const buffer = canvas.toBuffer('image/png')
      fs.writeFileSync(filePath, buffer)

      await page.goto('/ocr')
      await page.selectOption('select[name="language"]', 'es')
      await page.setInputFiles('input[type="file"]', filePath)

      await expect(page.locator('[data-testid="ocr-result"]')).toBeVisible({ timeout: 30000 })

      const extractedText = await page.locator('[data-testid="ocr-result"]').textContent()
      expect(extractedText).toContain('FACTURA')
      expect(extractedText).toContain('FACT-2024-001')

      fs.unlinkSync(filePath)
    })
  })

  test.describe('AI-Powered Search', () => {
    test('should perform semantic search', async ({ page }) => {
      await page.goto('/search')

      // Natural language query
      await page.fill('input[name="query"]', 'Find orders from last month that are pending')
      await page.click('button[data-testid="search"]')

      // Should understand intent
      await expect(page.locator('[data-testid="search-results"]')).toBeVisible()

      const results = await page.locator('[data-testid="result-item"]').count()
      expect(results).toBeGreaterThan(0)

      // Verify results are relevant
      const firstResult = await page.locator('[data-testid="result-item"]').first().textContent()
      expect(firstResult).toMatch(/order|pending/i)
    })

    test('should handle fuzzy matching', async ({ page }) => {
      await page.goto('/search')

      // Typos in search
      const typos = ['custmer', 'invoces', 'paymnts']

      for (const typo of typos) {
        await page.fill('input[name="query"]', typo)
        await page.click('button[data-testid="search"]')

        await expect(page.locator('[data-testid="search-results"]')).toBeVisible()

        // Should still find relevant results despite typos
        const results = await page.locator('[data-testid="result-item"]').count()
        expect(results).toBeGreaterThan(0)
      }
    })

    test('should suggest similar queries', async ({ page }) => {
      await page.goto('/search')

      await page.fill('input[name="query"]', 'invoice')
      await page.click('button[data-testid="search"]')

      // Check for search suggestions
      const suggestions = page.locator('[data-testid="search-suggestion"]')
      await expect(suggestions.first()).toBeVisible()

      const suggestionText = await suggestions.first().textContent()
      expect(suggestionText).toMatch(/invoice/i)
      expect(suggestionText).not.toBe('invoice') // Should be different
    })
  })

  test.describe('Recommendation Engine', () => {
    test('should generate product recommendations', async ({ page }) => {
      const customer = DataFactory.createCustomer()
      const customerId = 'cust-recommendation-test'

      // Simulate customer browsing history
      await page.goto('/login')
      await page.evaluate((id) => {
        localStorage.setItem('customer_id', id)
      }, customerId)

      await page.goto('/products')
      await page.click('[data-testid="product-1"]')
      await page.click('[data-testid="product-2"]')
      await page.goto('/dashboard')

      // Check recommendations
      const recommendations = page.locator('[data-testid="recommendation-item"]')
      await expect(recommendations.first()).toBeVisible()

      const count = await recommendations.count()
      expect(count).toBeGreaterThan(0)
      expect(count).toBeLessThanOrEqual(10)

      // Verify recommendations are different from viewed products
      for (let i = 0; i < count; i++) {
        const recText = await recommendations.nth(i).textContent()
        expect(recText).not.toBe('Product 1')
        expect(recText).not.toBe('Product 2')
      }
    })

    test('should personalize recommendations based on behavior', async ({ page }) => {
      // User interested in electronics
      await page.goto('/products')
      await page.click('[data-testid="category-electronics"]')
      await page.click('[data-testid="product-laptop"]')
      await page.click('[data-testid="product-phone"]')

      await page.goto('/dashboard')

      // Should recommend more electronics
      const recItems = await page.locator('[data-testid="recommendation-item"]').allTextContents()
      const electronicsCount = recItems.filter(item =>
        item.toLowerCase().includes('laptop') ||
        item.toLowerCase().includes('phone') ||
        item.toLowerCase().includes('electronics')
      ).length

      expect(electronicsCount).toBeGreaterThan(0)
    })

    test('should use collaborative filtering', async ({ page }) => {
      // Users who bought X also bought Y
      await page.goto('/product/laptop-123')

      // Check "Customers who bought this also bought" section
      const relatedProducts = page.locator('[data-testid="related-product"]')
      await expect(relatedProducts.first()).toBeVisible()

      const relatedCount = await relatedProducts.count()
      expect(relatedCount).toBeGreaterThan(0)

      // Verify related products are different
      const currentProduct = await page.locator('[data-testid="current-product"]').textContent()
      const firstRelated = await relatedProducts.first().textContent()
      expect(firstRelated).not.toBe(currentProduct)
    })
  })

  test.describe('Sentiment Analysis', () => {
    test('should analyze customer feedback sentiment', async ({ page }) => {
      await page.goto('/feedback')

      const feedbacks = [
        { text: 'Great service! Love the product!', expected: 'positive' },
        { text: 'Terrible experience, very disappointed', expected: 'negative' },
        { text: 'The product is okay, nothing special', expected: 'neutral' },
        { text: 'Excellent quality and fast delivery!', expected: 'positive' },
        { text: 'Worst purchase ever, waste of money', expected: 'negative' }
      ]

      for (const feedback of feedbacks) {
        await page.fill('textarea[name="feedback"]', feedback.text)
        await page.click('button[data-testid="analyze"]')

        const sentiment = await page.locator('[data-testid="sentiment-result"]').textContent()
        expect(sentiment).toBe(feedback.expected)

        // Check confidence score
        const confidence = await page.locator('[data-testid="sentiment-confidence"]').textContent()
        expect(parseFloat(confidence)).toBeGreaterThan(0.7)
      }
    })

    test('should aggregate sentiment over time', async ({ page }) => {
      // Submit multiple feedbacks
      const feedbacks = [
        { text: 'Good product', sentiment: 'positive' },
        { text: 'Average service', sentiment: 'neutral' },
        { text: 'Excellent!', sentiment: 'positive' }
      ]

      for (const feedback of sentiments) {
        await page.fill('textarea[name="feedback"]', feedback.text)
        await page.click('button[data-testid="submit"]')
        await page.waitForTimeout(500)
      }

      // Check sentiment trend
      await page.goto('/analytics/sentiment')

      const chart = page.locator('[data-testid="sentiment-chart"]')
      await expect(chart).toBeVisible()

      // Verify trend is calculated
      const trend = await page.locator('[data-testid="sentiment-trend"]').textContent()
      expect(['improving', 'declining', 'stable']).toContain(trend)
    })
  })

  test.describe('Image Recognition', () => {
    test('should classify product images', async ({ page }) => {
      // Create test product image
      const { createCanvas } = require('canvas')
      const canvas = createCanvas(300, 300)
      const ctx = canvas.getContext('2d')

      // Draw a simple "laptop" representation
      ctx.fillStyle = '#808080'
      ctx.fillRect(50, 100, 200, 150) // Base
      ctx.fillStyle = '#000000'
      ctx.fillRect(50, 50, 200, 50)   // Screen

      const fs = require('fs')
      const filePath = 'test-results/laptop-image.png'
      const buffer = canvas.toBuffer('image/png')
      fs.writeFileSync(filePath, buffer)

      await page.goto('/image-classifier')
      await page.setInputFiles('input[type="file"]', filePath)

      // Wait for classification
      await expect(page.locator('[data-testid="classification-result"]')).toBeVisible({ timeout: 30000 })

      const result = await page.locator('[data-testid="classification-result"]').textContent()
      expect(result).toMatch(/laptop|computer|electronics/i)

      // Check confidence
      const confidence = await page.locator('[data-testid="classification-confidence"]').textContent()
      expect(parseFloat(confidence)).toBeGreaterThan(0.6)

      fs.unlinkSync(filePath)
    })

    test('should detect objects in image', async ({ page }) => {
      // Create image with multiple objects
      const { createCanvas } = require('canvas')
      const canvas = createCanvas(400, 400)
      const ctx = canvas.getContext('2d')

      // Background
      ctx.fillStyle = '#ffffff'
      ctx.fillRect(0, 0, 400, 400)

      // Draw "phone"
      ctx.fillStyle = '#000000'
      ctx.fillRect(50, 50, 100, 200)

      // Draw "wallet"
      ctx.fillStyle = '#8B4513'
      ctx.fillRect(200, 100, 120, 80)

      const fs = require('fs')
      const filePath = 'test-results/objects-image.png'
      const buffer = canvas.toBuffer('image/png')
      fs.writeFileSync(filePath, buffer)

      await page.goto('/object-detection')
      await page.setInputFiles('input[type="file"]', filePath)

      await expect(page.locator('[data-testid="detected-object"]')).toBeVisible({ timeout: 30000 })

      // Should detect multiple objects
      const objects = await page.locator('[data-testid="detected-object"]').allTextContents()
      expect(objects.length).toBeGreaterThan(1)

      // Verify bounding boxes
      const boxes = await page.locator('[data-testid="bounding-box"]').count()
      expect(boxes).toBe(objects.length)

      fs.unlinkSync(filePath)
    })
  })

  test.describe('Natural Language Processing', () => {
    test('should extract entities from text', async ({ page }) => {
      const text = `Contact John Doe at john.doe@example.com or call +1-555-0123.
      Meeting on January 15, 2024 at 3:00 PM.`

      await page.goto('/nlp')
      await page.fill('textarea[name="text"]', text)
      await page.click('button[data-testid="extract-entities"]')

      // Check for extracted entities
      await expect(page.locator('[data-testid="entity-person"]')).toContainText('John Doe')
      await expect(page.locator('[data-testid="entity-email"]')).toContainText('john.doe@example.com')
      await expect(page.locator('[data-testid="entity-phone"]')).toContainText('+1-555-0123')
      await expect(page.locator('[data-testid="entity-date"]')).toContainText('January 15, 2024')
      await expect(page.locator('[data-testid="entity-time"]')).toContainText('3:00 PM')
    })

    test('should perform text summarization', async ({ page }) => {
      const longText = `
        Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor
        incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud
        exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute
        irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla
        pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia
        deserunt mollit anim id est laborum.
      `

      await page.goto('/nlp')
      await page.fill('textarea[name="text"]', longText)
      await page.click('button[data-testid="summarize"]')

      await expect(page.locator('[data-testid="summary"]')).toBeVisible({ timeout: 30000 })

      const summary = await page.locator('[data-testid="summary"]').textContent()
      const originalLength = longText.length
      const summaryLength = summary?.length || 0

      // Summary should be significantly shorter
      expect(summaryLength).toBeLessThan(originalLength * 0.5)
      expect(summary).toBeTruthy()
    })

    test('should detect language automatically', async ({ page }) => {
      const texts = [
        { text: 'Hello, how are you?', expected: 'en' },
        { text: 'Bonjour, comment allez-vous?', expected: 'fr' },
        { text: 'Hola, ¿cómo estás?', expected: 'es' },
        { text: 'Hallo, wie geht es dir?', expected: 'de' }
      ]

      for (const { text, expected } of texts) {
        await page.goto('/nlp')
        await page.fill('textarea[name="text"]', text)
        await page.click('button[data-testid="detect-language"]')

        const detected = await page.locator('[data-testid="detected-language"]').textContent()
        expect(detected).toBe(expected)
      }
    })
  })

  test.describe('Predictive Analytics', () => {
    test('should predict customer churn', async ({ page }) => {
      await page.goto('/analytics/customer-churn')

      // Customer has decreased activity
      await page.fill('input[name="login_frequency"]', '1')
      await page.fill('input[name="last_purchase_days"]', '45')
      await page.fill('input[name="support_tickets"]', '3')
      await page.click('button[data-testid="predict"]')

      await expect(page.locator('[data-testid="churn-probability"]')).toBeVisible()

      const probability = await page.locator('[data-testid="churn-probability"]').textContent()
      const probValue = parseFloat(probability || '0')

      // Should predict high churn risk for inactive customer
      expect(probValue).toBeGreaterThan(0.5)
    })

    test('should forecast sales', async ({ page }) => {
      await page.goto('/analytics/sales-forecast')

      // Historical data points
      const monthlyData = [
        { month: '2023-07', sales: 10000 },
        { month: '2023-08', sales: 12000 },
        { month: '2023-09', sales: 11500 }
      ]

      for (const data of monthlyData) {
        await page.click('button[data-testid="add-data-point"]')
        await page.fill('input[name="month"]', data.month)
        await page.fill('input[name="sales"]', data.sales.toString())
        await page.click('button[data-testid="save-point"]')
      }

      await page.click('button[data-testid="generate-forecast"]')

      await expect(page.locator('[data-testid="forecast-chart"]')).toBeVisible()

      // Should show future predictions
      const futurePredictions = await page.locator('[data-testid="forecast-value"]').count()
      expect(futurePredictions).toBeGreaterThan(0)
    })
  })
})
