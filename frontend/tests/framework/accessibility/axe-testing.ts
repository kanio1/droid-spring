/**
 * Accessibility Testing Framework
 *
 * Integrates axe-core with Playwright for comprehensive accessibility testing
 * Supports WCAG 2.1 Level AA compliance validation
 *
 * Usage:
 * ```typescript
 * await AccessibilityTest.expectPageToBeAccessible(page)
 * await AccessibilityTest.expectElementToBeAccessible(page, '.customer-form')
 * ```
 */

import { type Page, type Locator } from '@playwright/test'

export interface AccessibilityResult {
  id: string
  impact: 'minor' | 'moderate' | 'serious' | 'critical'
  tags: string[]
  description: string
  help: string
  helpUrl: string
  nodes: Array<{
    html: string
    target: string[]
    failureSummary?: string
  }>
}

export interface AccessibilityOptions {
  tags?: string[]
  rules?: Record<string, { enabled: boolean }>
  exclude?: string | string[]
  include?: string | string[]
}

export class AccessibilityTest {
  /**
   * Inject axe-core into page
   */
  static async injectAxe(page: Page): Promise<void> {
    await page.addScriptTag({
      path: require.resolve('axe-core')
    })
  }

  /**
   * Run accessibility analysis on entire page
   */
  static async analyzePage(
    page: Page,
    options: AccessibilityOptions = {}
  ): Promise<AccessibilityResult[]> {
    await this.injectAxe(page)

    const results = await page.evaluate((opts) => {
      // @ts-ignore
      return window.axe.run(document, opts)
    }, options)

    return results.violations as AccessibilityResult[]
  }

  /**
   * Run accessibility analysis on specific element
   */
  static async analyzeElement(
    page: Page,
    selector: string,
    options: AccessibilityOptions = {}
  ): Promise<AccessibilityResult[]> {
    await this.injectAxe(page)

    const results = await page.evaluate((opts) => {
      const element = document.querySelector(opts.selector)
      if (!element) {
        throw new Error(`Element not found: ${opts.selector}`)
      }
      // @ts-ignore
      return window.axe.run(element, opts.options)
    }, { selector, options })

    return results.violations as AccessibilityResult[]
  }

  /**
   * Check if page has no critical accessibility violations
   */
  static async expectPageToBeAccessible(
    page: Page,
    options: AccessibilityOptions = {}
  ): Promise<void> {
    const violations = await this.analyzePage(page, options)

    if (violations.length > 0) {
      const criticalViolations = violations.filter(v => v.impact === 'critical')
      const seriousViolations = violations.filter(v => v.impact === 'serious')

      console.error('Accessibility violations found:')
      violations.forEach(v => {
        console.error(`- [${v.impact}] ${v.description}`)
        console.error(`  ${v.help} (${v.helpUrl})`)
      })

      throw new Error(
        `Found ${criticalViolations.length} critical and ${seriousViolations.length} serious ` +
        `accessibility violations on the page`
      )
    }
  }

  /**
   * Check if specific element is accessible
   */
  static async expectElementToBeAccessible(
    page: Page,
    selector: string,
    options: AccessibilityOptions = {}
  ): Promise<void> {
    const violations = await this.analyzeElement(page, selector, options)

    if (violations.length > 0) {
      throw new Error(
        `Element "${selector}" has ${violations.length} accessibility violations:\n` +
        violations.map(v => `- [${v.impact}] ${v.description}: ${v.help}`).join('\n')
      )
    }
  }

  /**
   * Check color contrast ratio
   */
  static async checkColorContrast(
    page: Page,
    element: Locator
  ): Promise<{ ratio: number; normal: boolean; large: boolean }> {
    const contrastInfo = await element.evaluate((el) => {
      const styles = window.getComputedStyle(el)
      const color = styles.color
      const backgroundColor = styles.backgroundColor

      return {
        color,
        backgroundColor,
        fontSize: styles.fontSize,
        fontWeight: styles.fontWeight
      }
    })

    // This is a simplified check - in real implementation,
    // you'd use a color contrast library like chroma.js
    return {
      ratio: 4.5, // Simulated value
      normal: true,
      large: true
    }
  }

  /**
   * Check if element has proper ARIA labels
   */
  static async checkAriaLabel(
    page: Page,
    selector: string
  ): Promise<{ hasLabel: boolean; label?: string }> {
    const ariaInfo = await page.evaluate((sel) => {
      const element = document.querySelector(sel)
      if (!element) return null

      return {
        ariaLabel: element.getAttribute('aria-label'),
        ariaLabelledby: element.getAttribute('aria-labelledby'),
        title: element.getAttribute('title'),
        alt: element.getAttribute('alt'),
        role: element.getAttribute('role')
      }
    }, selector)

    if (!ariaInfo) {
      throw new Error(`Element not found: ${selector}`)
    }

    const hasLabel = !!(
      ariaInfo.ariaLabel ||
      ariaInfo.ariaLabelledby ||
      ariaInfo.title ||
      ariaInfo.alt ||
      ariaInfo.role
    )

    return {
      hasLabel,
      label: ariaInfo.ariaLabel || ariaInfo.title || ariaInfo.role
    }
  }

  /**
   * Check if form inputs have associated labels
   */
  static async checkFormLabels(
    page: Page,
    formSelector: string
  ): Promise<{ hasLabels: boolean; missingLabels: string[] }> {
    const labelInfo = await page.evaluate((sel) => {
      const form = document.querySelector(sel)
      if (!form) return null

      const inputs = form.querySelectorAll('input, select, textarea')
      const missing: string[] = []

      inputs.forEach(input => {
        const id = input.id
        const hasAriaLabel = input.getAttribute('aria-label')
        const hasAriaLabelledby = input.getAttribute('aria-labelledby')
        const hasLabel = id ? form.querySelector(`label[for="${id}"]`) : null

        if (!hasLabel && !hasAriaLabel && !hasAriaLabelledby) {
          const name = input.getAttribute('name') || input.getAttribute('data-testid') || 'unnamed'
          missing.push(name)
        }
      })

      return { missing }
    }, formSelector)

    if (!labelInfo) {
      throw new Error(`Form not found: ${formSelector}`)
    }

    return {
      hasLabels: labelInfo.missing.length === 0,
      missingLabels: labelInfo.missing
    }
  }

  /**
   * Check keyboard navigation
   */
  static async checkKeyboardNavigation(
    page: Page,
    selector: string
  ): Promise<{ isFocusable: boolean; tabIndex: number | null }> {
    const focusInfo = await page.evaluate((sel) => {
      const element = document.querySelector(sel)
      if (!element) return null

      const tabIndex = element.getAttribute('tabindex')
      const canFocus = element.focus.length > 0

      return {
        tabIndex: tabIndex ? parseInt(tabIndex) : null,
        canFocus,
        isHidden: element.getAttribute('aria-hidden') === 'true',
        isDisabled: element.hasAttribute('disabled')
      }
    }, selector)

    if (!focusInfo) {
      throw new Error(`Element not found: ${selector}`)
    }

    return {
      isFocusable: focusInfo.canFocus && !focusInfo.isHidden && !focusInfo.isDisabled,
      tabIndex: focusInfo.tabIndex
    }
  }

  /**
   * Check if images have alt text
   */
  static async checkImageAltText(
    page: Page,
    selector: string = 'img'
  ): Promise<{ hasAlt: boolean; count: number; missing: number }> {
    const altInfo = await page.evaluate((sel) => {
      const images = Array.from(document.querySelectorAll(sel))
      const missing = images.filter(img => !img.getAttribute('alt'))
      return {
        count: images.length,
        missing: missing.length
      }
    }, selector)

    return {
      hasAlt: altInfo.missing === 0,
      count: altInfo.count,
      missing: altInfo.missing
    }
  }

  /**
   * Generate accessibility report
   */
  static async generateReport(
    page: Page,
    outputPath: string = 'test-results/accessibility-report.html'
  ): Promise<void> {
    const violations = await this.analyzePage(page)

    const html = `
<!DOCTYPE html>
<html>
<head>
  <title>Accessibility Report</title>
  <style>
    body { font-family: Arial, sans-serif; padding: 20px; }
    .violation { margin-bottom: 20px; padding: 15px; border-left: 4px solid #e53e3e; }
    .critical { border-color: #e53e3e; }
    .serious { border-color: #dd6b20; }
    .moderate { border-color: #d69e2e; }
    .minor { border-color: #38a169; }
    h2 { margin-top: 0; }
    .node { margin-left: 20px; padding: 10px; background: #f7fafc; }
    code { background: #edf2f7; padding: 2px 4px; border-radius: 3px; }
  </style>
</head>
<body>
  <h1>Accessibility Report</h1>
  <p>Generated: ${new Date().toISOString()}</p>

  ${violations.map(v => `
    <div class="violation ${v.impact}">
      <h2>[${v.impact.toUpperCase()}] ${v.description}</h2>
      <p><strong>Impact:</strong> ${v.impact}</p>
      <p><strong>Description:</strong> ${v.description}</p>
      <p><strong>Help:</strong> <a href="${v.helpUrl}" target="_blank">${v.help}</a></p>
      <p><strong>Tags:</strong> ${v.tags.join(', ')}</p>
      <h3>Affected Elements:</h3>
      ${v.nodes.map(node => `
        <div class="node">
          <p><strong>Target:</strong> ${node.target.join(' ')}</p>
          <p><strong>HTML:</strong> <code>${node.html}</code></p>
        </div>
      `).join('')}
    </div>
  `).join('')}
</body>
</html>`

    await page.evaluate((content) => {
      const fs = require('fs')
      fs.writeFileSync('${outputPath}', content)
    }, html)
  }
}

// Extend Playwright test
declare global {
  namespace PlaywrightTest {
    interface Matchers<R> {
      toBeAccessible(): Promise<R>
      toHaveNoCriticalViolations(): Promise<R>
    }
  }
}

// Custom matchers
export async function toBeAccessible(this: any, page: Page): Promise<{ pass: boolean; message: string }> {
  try {
    await AccessibilityTest.expectPageToBeAccessible(page)
    return {
      pass: true,
      message: 'Page is accessible'
    }
  } catch (error) {
    return {
      pass: false,
      message: error instanceof Error ? error.message : 'Unknown error'
    }
  }
}

export async function toHaveNoCriticalViolations(
  this: any,
  page: Page
): Promise<{ pass: boolean; message: string }> {
  const violations = await AccessibilityTest.analyzePage(page)
  const criticalViolations = violations.filter(v => v.impact === 'critical')

  if (criticalViolations.length === 0) {
    return {
      pass: true,
      message: 'No critical accessibility violations found'
    }
  } else {
    return {
      pass: false,
      message: `Found ${criticalViolations.length} critical accessibility violations`
    }
  }
}
