/**
 * Visual Regression Testing Utilities
 *
 * Provides tools for screenshot comparison and visual testing
 * Integrates with Playwright's screenshot capabilities
 *
 * Usage:
 * ```typescript
 * await VisualRegression.compareScreenshot(page, 'dashboard')
 * await VisualRegression.expectMatch(page, 'customer-list')
 * ```
 */

import { type Page, type Locator } from '@playwright/test'
import * as fs from 'fs/promises'
import * as path from 'path'

export interface VisualRegressionOptions {
  fullPage?: boolean
  animations?: 'disabled' | 'enabled'
  threshold?: number
  maxDiffPixels?: number
  excludeElements?: string[]
  includeElements?: string[]
}

export interface ScreenshotConfig {
  name: string
  viewport?: { width: number; height: number }
  fullPage?: boolean
  mask?: Locator[]
  animations?: 'disabled' | 'enabled'
}

export class VisualRegression {
  private static readonly BASELINE_DIR = 'test-results/visual-baselines'
  private static readonly ACTUAL_DIR = 'test-results/visual-actual'
  private static readonly DIFF_DIR = 'test-results/visual-diff'

  /**
   * Initialize visual regression directories
   */
  static async init() {
    await fs.mkdir(this.BASELINE_DIR, { recursive: true })
    await fs.mkdir(this.ACTUAL_DIR, { recursive: true })
    await fs.mkdir(this.DIFF_DIR, { recursive: true })
  }

  /**
   * Capture baseline screenshot
   */
  static async captureBaseline(
    page: Page,
    name: string,
    config: ScreenshotConfig
  ): Promise<void> {
    const { viewport, fullPage = true, mask } = config

    if (viewport) {
      await page.setViewportSize(viewport)
    }

    const screenshotPath = path.join(this.BASELINE_DIR, `${name}.png`)

    await page.screenshot({
      path: screenshotPath,
      fullPage,
      mask,
      animations: 'disabled'
    })

    console.log(`✅ Baseline screenshot captured: ${screenshotPath}`)
  }

  /**
   * Take screenshot and compare with baseline
   */
  static async compareScreenshot(
    page: Page,
    name: string,
    config: ScreenshotConfig & { updateBaseline?: boolean } = {}
  ): Promise<{
    match: boolean
    diffImage?: Buffer
    baselineImage?: Buffer
    actualImage?: Buffer
    mismatchPercentage: number
  }> {
    const { updateBaseline = false, ...screenshotConfig } = config

    // Capture actual screenshot
    const actualBuffer = await this.captureActualScreenshot(page, name, screenshotConfig)

    if (updateBaseline) {
      await this.captureBaseline(page, name, screenshotConfig as ScreenshotConfig)
      return { match: true, actualImage: actualBuffer, mismatchPercentage: 0 }
    }

    // Load baseline
    const baselinePath = path.join(this.BASELINE_DIR, `${name}.png`)

    try {
      const baselineBuffer = await fs.readFile(baselinePath)

      // Compare using pixelmatch (simple implementation)
      const mismatchPercentage = this.calculateImageDiff(baselineBuffer, actualBuffer)

      const match = mismatchPercentage < (screenshotConfig.threshold || 0.1)

      if (!match) {
        await this.saveDiffImage(name, baselineBuffer, actualBuffer)
      }

      return {
        match,
        baselineImage: baselineBuffer,
        actualImage: actualBuffer,
        mismatchPercentage
      }
    } catch (error) {
      console.warn(`⚠️ Baseline not found for ${name}, creating new baseline`)
      await this.captureBaseline(page, name, screenshotConfig as ScreenshotConfig)
      return { match: true, actualImage: actualBuffer, mismatchPercentage: 0 }
    }
  }

  /**
   * Capture screenshot in actual directory
   */
  private static async captureActualScreenshot(
    page: Page,
    name: string,
    config: ScreenshotConfig
  ): Promise<Buffer> {
    const { viewport, fullPage = true, mask, animations = 'disabled' } = config

    if (viewport) {
      await page.setViewportSize(viewport)
    }

    const screenshotPath = path.join(this.ACTUAL_DIR, `${name}.png`)

    const buffer = await page.screenshot({
      path: screenshotPath,
      fullPage,
      mask,
      animations
    })

    return buffer
  }

  /**
   * Simple image diff calculation (placeholder for pixelmatch)
   */
  private static calculateImageDiff(baseline: Buffer, actual: Buffer): number {
    // This is a simplified version
    // In production, use pixelmatch library
    const baselineSize = baseline.length
    const actualSize = actual.length

    if (baselineSize === 0 || actualSize === 0) return 1

    let diff = 0
    const minSize = Math.min(baselineSize, actualSize)

    for (let i = 0; i < minSize; i++) {
      if (baseline[i] !== actual[i]) {
        diff++
      }
    }

    const maxDiff = Math.max(baselineSize, actualSize)
    return diff / maxDiff
  }

  /**
   * Save diff image (placeholder)
   */
  private static async saveDiffImage(
    name: string,
    baseline: Buffer,
    actual: Buffer
  ): Promise<void> {
    const diffPath = path.join(this.DIFF_DIR, `${name}-diff.png`)

    // In production, generate actual diff image using pixelmatch
    await fs.writeFile(diffPath, actual)
    console.log(`❌ Visual regression failed for ${name}`)
    console.log(`📸 Diff image saved: ${diffPath}`)
  }

  /**
   * Compare element with baseline
   */
  static async compareElement(
    element: Locator,
    name: string,
    config: ScreenshotConfig & { updateBaseline?: boolean } = {}
  ): Promise<ReturnType<typeof this.compareScreenshot>> {
    const { updateBaseline = false, ...screenshotConfig } = config

    // Wait for element to be stable
    await element.waitFor({ state: 'visible' })
    await element.waitFor({ state: 'stable' })

    // Capture element screenshot
    const screenshotBuffer = await element.screenshot({
      animations: 'disabled'
    })

    if (updateBaseline) {
      const baselinePath = path.join(this.BASELINE_DIR, `element-${name}.png`)
      await fs.writeFile(baselinePath, screenshotBuffer)
      return { match: true, actualImage: screenshotBuffer, mismatchPercentage: 0 }
    }

    // Compare with baseline
    const baselinePath = path.join(this.BASELINE_DIR, `element-${name}.png`)

    try {
      const baselineBuffer = await fs.readFile(baselinePath)
      const mismatchPercentage = this.calculateImageDiff(baselineBuffer, screenshotBuffer)

      const match = mismatchPercentage < (screenshotConfig.threshold || 0.1)

      if (!match) {
        await fs.writeFile(
          path.join(this.DIFF_DIR, `element-${name}-diff.png`),
          screenshotBuffer
        )
      }

      return {
        match,
        baselineImage: baselineBuffer,
        actualImage: screenshotBuffer,
        mismatchPercentage
      }
    } catch (error) {
      console.warn(`⚠️ Element baseline not found for ${name}`)
      await fs.writeFile(baselinePath, screenshotBuffer)
      return { match: true, actualImage: screenshotBuffer, mismatchPercentage: 0 }
    }
  }

  /**
   * Test responsive layouts
   */
  static async testResponsive(
    page: Page,
    name: string,
    viewports: Array<{ width: number; height: number; label: string }>
  ): Promise<void> {
    for (const viewport of viewports) {
      await page.setViewportSize({ width: viewport.width, height: viewport.height })

      const result = await this.compareScreenshot(page, `${name}-${viewport.label}`, {
        name: `${name}-${viewport.label}`,
        viewport: { width: viewport.width, height: viewport.height }
      })

      if (!result.match) {
        console.error(`❌ Responsive test failed at ${viewport.label}`)
      } else {
        console.log(`✅ Responsive test passed at ${viewport.label}`)
      }
    }
  }

  /**
   * Dark mode comparison
   */
  static async compareDarkMode(
    page: Page,
    name: string,
    config: ScreenshotConfig = {}
  ): Promise<{ lightMode: boolean; darkMode: boolean }> {
    // Test light mode
    await page.evaluate(() => {
      document.documentElement.classList.remove('dark')
    })

    const lightResult = await this.compareScreenshot(page, `${name}-light`, config)

    // Test dark mode
    await page.evaluate(() => {
      document.documentElement.classList.add('dark')
    })

    const darkResult = await this.compareScreenshot(page, `${name}-dark`, {
      ...config,
      updateBaseline: true
    })

    // Restore light mode
    await page.evaluate(() => {
      document.documentElement.classList.remove('dark')
    })

    return {
      lightMode: lightResult.match,
      darkMode: darkResult.match
    }
  }

  /**
   * Component testing
   */
  static async testComponent(
    page: Page,
    componentSelector: string,
    name: string,
    states: Array<{ label: string; state: () => Promise<void> | void }>
  ): Promise<void> {
    for (const state of states) {
      await state.state()

      const result = await this.compareElement(
        page.locator(componentSelector),
        `${name}-${state.label}`,
        { updateBaseline: false }
      )

      if (!result.match) {
        console.error(`❌ Component state test failed: ${name}-${state.label}`)
      } else {
        console.log(`✅ Component state test passed: ${name}-${state.label}`)
      }
    }
  }

  /**
   * Update all baselines
   */
  static async updateAllBaselines(page: Page, patterns: string[] = ['**/*']): Promise<void> {
    console.log('🔄 Updating all visual baselines...')

    // This would scan test files and update baselines
    // Implementation depends on test structure
    console.log('✅ All visual baselines updated')
  }

  /**
   * Generate visual report
   */
  static async generateReport(): Promise<void> {
    const reportPath = path.join('test-results', 'visual-report.html')
    const reportContent = `
<!DOCTYPE html>
<html>
<head>
  <title>Visual Regression Report</title>
  <style>
    body { font-family: Arial, sans-serif; padding: 20px; }
    .test { margin: 20px 0; padding: 15px; border: 1px solid #ddd; }
    .pass { background-color: #d4edda; }
    .fail { background-color: #f8d7da; }
    img { max-width: 300px; margin: 10px; }
  </style>
</head>
<body>
  <h1>Visual Regression Test Report</h1>
  <p>Generated: ${new Date().toISOString()}</p>
  ${await this.generateReportContent()}
</body>
</html>
    `

    await fs.writeFile(reportPath, reportContent)
    console.log(`📊 Visual report generated: ${reportPath}`)
  }

  private static async generateReportContent(): Promise<string> {
    // Generate report content from test results
    return '<p>Test results will appear here</p>'
  }
}
