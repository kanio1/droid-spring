/**
 * AI/ML Features E2E Tests
 *
 * Comprehensive tests for AI-powered features:
 * - Customer insights
 * - Sentiment analysis
 * - Prediction validation
 * - OCR capabilities
 * - Model accuracy testing
 *
 * Uses AITester utility for testing
 */

import { test, expect } from '@playwright/test'
import { AITester, OCRTextExtractor, SentimentAnalyzer } from '../framework/utils/ai-tester'
import { registerCustomMatchers } from '../framework/matchers/playwright-matchers'

// Register custom matchers
registerCustomMatchers()

test.describe('AI-Powered Customer Insights', () => {
  let page: any
  let aiTester: AITester

  test.beforeEach(async ({ page: p }) => {
    page = p
    aiTester = new AITester(page)
  })

  test('should display customer insights with AI-generated recommendations', async () => {
    test.info().annotations.push({
      type: 'testgroup',
      description: 'AI Customer Insights Testing'
    })

    // Navigate to customer page
    await page.goto('/customers/cust-123/insights')

    // Wait for insights to load
    await expect(page.locator('[data-testid="customer-insights"]')).toBeVisible()

    // Verify AI insights are displayed
    const insightsCount = await page.locator('[data-testid^="insight-"]').count()
    expect(insightsCount).toBeGreaterThan(0)

    // Verify high-confidence insights
    for (let i = 0; i < insightsCount; i++) {
      const insight = page.locator('[data-testid^="insight-"]').nth(i)
      await expect(insight).toContainText(/confidence/i)
    }

    // Test insight interaction
    const firstInsight = page.locator('[data-testid^="insight-"]').first()
    await firstInsight.locator('[data-testid="btn-view-insight"]').click()

    // Verify insight marked as viewed
    await expect(firstInsight.locator('.status-viewed')).toBeVisible()

    console.log('✓ Customer insights displayed correctly')
  })

  test('should generate churn risk insight using AI model', async () => {
    // Test churn risk generation
    const churnRiskInsight = await aiTester.getPrediction(
      {
        customerId: 'cust-123',
        features: {
          daysSinceLastPurchase: 45,
          supportTickets: 3,
          emailOpenRate: 0.15,
          loginFrequency: 0.3
        }
      },
      'ChurnRiskModel'
    )

    expect(churnRiskInsight.prediction).toBeDefined()
    expect(churnRiskInsight.confidence).toBeGreaterThan(0.7)
    expect(churnRiskInsight.modelVersion).toBeDefined()

    console.log(`✓ Churn risk prediction: ${churnRiskInsight.prediction} (confidence: ${(churnRiskInsight.confidence * 100).toFixed(1)}%)`)
  })

  test('should generate LTV prediction', async () => {
    const ltvPrediction = await aiTester.getPrediction(
      {
        customerId: 'cust-123',
        features: {
          totalSpent: 5000,
          orderCount: 25,
          avgOrderValue: 200,
          retentionRate: 0.85
        }
      },
      'LTVPredictionModel'
    )

    expect(ltvPrediction.prediction).toBeGreaterThan(0)
    expect(ltvPrediction.confidence).toBeGreaterThan(0.7)

    console.log(`✓ LTV prediction: $${ltvPrediction.prediction} (confidence: ${(ltvPrediction.confidence * 100).toFixed(1)}%)`)
  })
})

test.describe('Sentiment Analysis Testing', () => {
  let page: any
  let aiTester: AITester
  let sentimentAnalyzer: SentimentAnalyzer

  test.beforeEach(async ({ page: p }) => {
    page = p
    aiTester = new AITester(page)
    sentimentAnalyzer = new SentimentAnalyzer(page)
  })

  test('should analyze customer feedback sentiment', async () => {
    const feedback = 'I am extremely satisfied with the product quality and customer service. Highly recommend!'

    const sentiment = await aiTester.analyzeSentiment(feedback, {
      includeEmotions: true,
      includeKeywords: true
    })

    expect(sentiment.sentiment).toBe('positive')
    expect(sentiment.confidence).toBeGreaterThan(0.7)
    expect(sentiment.score).toBeGreaterThan(0)
    expect(sentiment.emotions).toBeDefined()
    expect(sentiment.emotions?.joy).toBeGreaterThan(0.5)
    expect(sentiment.keywords).toBeDefined()
    expect(sentiment.keywords?.length).toBeGreaterThan(0)

    console.log(`✓ Sentiment: ${sentiment.sentiment} (${(sentiment.confidence * 100).toFixed(1)}% confidence)`)
    console.log(`  Emotions: ${JSON.stringify(sentiment.emotions)}`)
  })

  test('should detect negative sentiment', async () => {
    const negativeFeedback = 'Terrible experience. Product broke after 2 days. Very disappointed.'

    const sentiment = await aiTester.analyzeSentiment(negativeFeedback)

    expect(sentiment.sentiment).toBe('negative')
    expect(sentiment.score).toBeLessThan(0)
    expect(sentiment.emotions?.anger).toBeGreaterThan(0.5)

    console.log(`✓ Negative sentiment detected: ${sentiment.sentiment} (${(sentiment.confidence * 100).toFixed(1)}% confidence)`)
  })

  test('should batch analyze sentiment', async () => {
    const texts = [
      'Great product! Love it!',
      'Okay product, nothing special',
      'Bad product. Hate it.'
    ]

    const results = await sentimentAnalyzer.batchAnalyze(texts)

    expect(results).toHaveLength(3)
    expect(results[0].sentiment).toBe('positive')
    expect(results[1].sentiment).toBe('neutral')
    expect(results[2].sentiment).toBe('negative')

    console.log(`✓ Batch analysis: ${results.length} texts processed`)
    results.forEach((r, i) => {
      console.log(`  Text ${i + 1}: ${r.sentiment} (${(r.confidence * 100).toFixed(1)}%)`)
    })
  })

  test('should compare sentiment across languages', async () => {
    const texts = {
      en: 'Great product!',
      es: '¡Excelente producto!',
      fr: 'Excellent produit!',
      de: 'Großartiges Produkt!',
      zh: '很棒的产品!',
      ja: '素晴らしい製品です！'
    }

    const results = await sentimentAnalyzer.compareSentimentAcrossLanguages(texts)

    expect(Object.keys(results)).toHaveLength(6)
    Object.entries(results).forEach(([lang, result]) => {
      expect(result.sentiment).toBe('positive')
      console.log(`✓ ${lang}: ${result.sentiment} (${(result.confidence * 100).toFixed(1)}%)`)
    })
  })
})

test.describe('Prediction Validation Testing', () => {
  let aiTester: AITester

  test.beforeEach(async ({ page }) => {
    aiTester = new AITester(page)
  })

  test('should validate ML model predictions', async () => {
    // Create test data
    const testData = [
      { input: { score: 85 }, expected: 'high' },
      { input: { score: 45 }, expected: 'medium' },
      { input: { score: 20 }, expected: 'low' },
      { input: { score: 90 }, expected: 'high' },
      { input: { score: 30 }, expected: 'low' }
    ]

    // Test model accuracy
    const accuracy = await aiTester.testModelAccuracy(testData, 'SegmentModel')

    expect(accuracy.total).toBe(5)
    expect(accuracy.accuracy).toBeGreaterThan(0.6) // At least 60% accuracy
    expect(accuracy.precision).toBeGreaterThan(0.6)
    expect(accuracy.recall).toBeGreaterThan(0.6)
    expect(accuracy.f1Score).toBeGreaterThan(0.6)

    console.log(`✓ Model accuracy: ${(accuracy.accuracy * 100).toFixed(1)}%`)
    console.log(`  Precision: ${(accuracy.precision * 100).toFixed(1)}%`)
    console.log(`  Recall: ${(accuracy.recall * 100).toFixed(1)}%`)
    console.log(`  F1 Score: ${(accuracy.f1Score * 100).toFixed(1)}%`)
  })

  test('should validate individual prediction', async () => {
    const result = await aiTester.validatePrediction(
      { feature1: 100, feature2: 50 },
      85,
      { tolerance: 10, modelName: 'TestModel' }
    )

    expect(result.prediction).toBeDefined()
    expect(result.confidence).toBeDefined()
    expect(result.isCorrect !== undefined).toBe(true)
    expect(result.processingTime).toBeGreaterThan(0)

    console.log(`✓ Prediction validated: ${result.isCorrect ? 'correct' : 'incorrect'}`)
    console.log(`  Confidence: ${(result.confidence * 100).toFixed(1)}%`)
  })
})

test.describe('OCR Testing', () => {
  let page: any
  let aiTester: AITester
  let ocrExtractor: OCRTextExtractor

  test.beforeEach(async ({ page: p }) => {
    page = p
    aiTester = new AITester(page)
    ocrExtractor = new OCRTextExtractor(page)
  })

  test('should extract text from image using OCR', async () => {
    // Test OCR extraction
    const ocrResult = await aiTester.extractTextFromImage(
      '/tests/fixtures/sample-invoice.png',
      { language: 'en', includeConfidence: true }
    )

    expect(ocrResult.text).toBeDefined()
    expect(ocrResult.confidence).toBeGreaterThan(0.7)
    expect(ocrResult.boundingBoxes).toBeDefined()
    expect(ocrResult.boundingBoxes.length).toBeGreaterThan(0)
    expect(ocrResult.processingTime).toBeGreaterThan(0)

    console.log(`✓ OCR extraction successful`)
    console.log(`  Text: "${ocrResult.text}"`)
    console.log(`  Confidence: ${(ocrResult.confidence * 100).toFixed(1)}%`)
  })

  test('should validate extracted text', async () => {
    const ocrResult = await aiTester.extractTextFromImage('/tests/fixtures/sample-document.png')

    // Validate against expected text
    const validation = await aiTester.validateExtractedText(
      ocrResult,
      'Expected text from document',
      { minConfidence: 0.7, allowFuzzyMatch: true }
    )

    expect(validation.isValid).toBeDefined()
    expect(validation.match).toBeGreaterThan(0)
    expect(validation.confidence).toBeGreaterThan(0)

    console.log(`✓ Text validation: ${validation.isValid ? 'valid' : 'invalid'}`)
    console.log(`  Match score: ${(validation.match * 100).toFixed(1)}%`)
  })

  test('should batch extract text from multiple images', async () => {
    const imagePaths = [
      '/tests/fixtures/doc1.png',
      '/tests/fixtures/doc2.png',
      '/tests/fixtures/doc3.png'
    ]

    const results = await ocrExtractor.batchExtract(imagePaths, { parallel: true })

    expect(results).toHaveLength(3)
    results.forEach((result, i) => {
      expect(result.text).toBeDefined()
      expect(result.confidence).toBeGreaterThan(0.7)
      console.log(`✓ Document ${i + 1}: ${result.text.substring(0, 50)}...`)
    })
  })

  test('should extract text from page element', async () => {
    // Navigate to page with image
    await page.goto('/invoice/123')

    // Extract from image element
    const imageElement = page.locator('img[data-testid="invoice-image"]')
    const ocrResult = await aiTester.extractTextFromImageElement(imageElement)

    expect(ocrResult.text).toBeDefined()
    expect(ocrResult.confidence).toBeGreaterThan(0.7)

    console.log(`✓ Element OCR: "${ocrResult.text.substring(0, 50)}..."`)
  })
})

test.describe('Image Comparison & Processing', () => {
  let aiTester: AITester

  test.beforeEach(async ({ page }) => {
    aiTester = new AITester(page)
  })

  test('should compare two images for similarity', async () => {
    const comparison = await aiTester.compareImages(
      '/tests/fixtures/before.png',
      '/tests/fixtures/after.png'
    )

    expect(comparison.similarity).toBeGreaterThanOrEqual(0)
    expect(comparison.similarity).toBeLessThanOrEqual(1)
    expect(comparison.differences).toBeDefined()
    expect(comparison.processingTime).toBeGreaterThan(0)

    console.log(`✓ Image similarity: ${(comparison.similarity * 100).toFixed(1)}%`)
    console.log(`  Differences found: ${comparison.differences.length}`)
  })

  test('should validate image processing result', async () => {
    const validation = await aiTester.validateImageProcessing(
      '/tests/fixtures/original.png',
      '/tests/fixtures/processed.png',
      { brightness: 10, filter: 'enhance' }
    )

    expect(validation.isValid).toBeDefined()
    expect(validation.changesDetected).toBeDefined()
    expect(validation.processingTime).toBeGreaterThan(0)

    console.log(`✓ Image processing validation: ${validation.isValid ? 'valid' : 'invalid'}`)
    console.log(`  Changes: ${JSON.stringify(validation.changesDetected)}`)
  })
})

test.describe('Anomaly Detection & Clustering', () => {
  let aiTester: AITester

  test.beforeEach(async ({ page }) => {
    aiTester = new AITester(page)
  })

  test('should detect anomalies in data', async () => {
    // Create sample data with anomalies
    const data = Array.from({ length: 100 }, (_, i) => ({
      id: i,
      value: i < 95 ? 50 + Math.random() * 10 : 200 // Last 5 are anomalies
    }))

    const anomalies = await aiTester.detectAnomalies(data)

    expect(anomalies.anomalies).toBeDefined()
    expect(anomalies.count).toBeGreaterThan(0)
    expect(anomalies.threshold).toBeGreaterThan(0)
    expect(anomalies.processingTime).toBeGreaterThan(0)

    console.log(`✓ Anomalies detected: ${anomalies.count}`)
    console.log(`  Threshold: ${anomalies.threshold}`)
  })

  test('should cluster data points', async () => {
    const data = Array.from({ length: 50 }, () => ({
      x: Math.random() * 100,
      y: Math.random() * 100
    }))

    const clustering = await aiTester.clusterData(data, {
      algorithm: 'k-means',
      clusters: 3
    })

    expect(clustering.clusters).toHaveLength(3)
    expect(clustering.centroids).toBeDefined()
    expect(clustering.centroids?.length).toBe(3)
    expect(clustering.algorithm).toBe('k-means')
    expect(clustering.processingTime).toBeGreaterThan(0)

    console.log(`✓ Data clustered into ${clustering.clusters.length} groups`)
    clustering.clusters.forEach((cluster, i) => {
      console.log(`  Cluster ${i + 1}: ${cluster.length} points`)
    })
  })
})

test.describe('Model Information & Performance', () => {
  let aiTester: AITester

  test.beforeEach(async ({ page }) => {
    aiTester = new AITester(page)
  })

  test('should retrieve model information', async () => {
    const modelInfo = await aiTester.getModelInfo('ChurnRiskModel-v2.1')

    expect(modelInfo.name).toBe('ChurnRiskModel-v2.1')
    expect(modelInfo.version).toBeDefined()
    expect(modelInfo.accuracy).toBeGreaterThan(0)
    expect(modelInfo.precision).toBeGreaterThan(0)
    expect(modelInfo.recall).toBeGreaterThan(0)
    expect(modelInfo.f1Score).toBeGreaterThan(0)
    expect(modelInfo.lastUpdated).toBeDefined()
    expect(modelInfo.trainingData).toBeDefined()

    console.log(`✓ Model info retrieved`)
    console.log(`  Name: ${modelInfo.name}`)
    console.log(`  Version: ${modelInfo.version}`)
    console.log(`  Accuracy: ${(modelInfo.accuracy * 100).toFixed(1)}%`)
  })
})

test.describe('AI Features - Multi-tenancy', () => {
  test('should isolate AI insights by tenant', async () => {
    test.skip(true, 'Multi-tenant AI testing - requires actual implementation')
    // This would test that AI insights are properly isolated between tenants
    // Would use tenant-specific data and verify no cross-tenant leakage
  })

  test('should validate tenant-specific model performance', async () => {
    test.skip(true, 'Multi-tenant model testing - requires actual implementation')
    // This would test model performance per tenant
  })
})

// Global test configuration
test.afterEach(async () => {
  // Cleanup after each test
  console.log('✓ Test completed\n')
})
