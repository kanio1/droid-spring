/**
 * Allure Reports Configuration
 *
 * Configures Allure reporting for Playwright and Vitest
 * Provides comprehensive test reporting with attachments, steps, and categories
 *
 * Usage:
 * ```typescript
 * import { AllureReporter } from '@/tests/allure/allure.config'
 *
 * test('test with allure steps', async ({ page }) => {
 *   await AllureReporter.step('Navigate to page')
 *   await page.goto('/')
 *
 *   await AllureReporter.step('Fill form')
 *   await page.fill('[data-testid="email"]', 'test@example.com')
 *
 *   await AllureReporter.attach('Screenshot', async () => {
 *     await page.screenshot({ path: 'screenshot.png' })
 *     return 'screenshot.png'
 *   })
 * })
 * ```
 */

import { test as base, type TestInfo } from '@playwright/test'
import AllureReporter from '@wdio/allure-reporter'

// Allure environment info
export const allureEnvironmentInfo = {
  project: 'BSS System',
  version: '1.0.0',
  environment: process.env.NODE_ENV || 'development',
  browser: 'chromium',
  platform: process.platform,
  nodeVersion: process.version
}

/**
 * Start Allure test reporting
 */
export function startAllureReporting() {
  console.log('📊 Starting Allure reporting...')

  // Set environment info
  process.env.ALLURE_ENV_INFO = JSON.stringify(allureEnvironmentInfo)

  // Create results directory
  const fs = require('fs')
  const path = require('path')

  const resultsDir = path.join(process.cwd(), 'test-results', 'allure')
  if (!fs.existsSync(resultsDir)) {
    fs.mkdirSync(resultsDir, { recursive: true })
  }
}

/**
 * Add environment information to Allure report
 */
export function addAllureEnvironment() {
  const fs = require('fs')
  const path = require('path')

  const envFile = path.join(process.cwd(), 'test-results', 'allure', 'environment.properties')

  const envContent = Object.entries(allureEnvironmentInfo)
    .map(([key, value]) => `${key}=${value}`)
    .join('\n')

  fs.writeFileSync(envFile, envContent)
}

/**
 * Allure test wrapper with lifecycle management
 */
export class AllureTestWrapper {
  private static currentTest: TestInfo | null = null

  static setCurrentTest(testInfo: TestInfo) {
    this.currentTest = testInfo
  }

  static getCurrentTest(): TestInfo | null {
    return this.currentTest
  }

  /**
   * Start a test step
   */
  static step(name: string, callback: () => Promise<any>): Promise<any> {
    console.log(`  ⏱️  Allure Step: ${name}`)
    return AllureReporter.step(name, callback)
  }

  /**
   * Add attachment to report
   */
  static async attach(name: string, content: string | Buffer | (() => Promise<string | Buffer>)) {
    if (typeof content === 'function') {
      const result = await content()
      AllureReporter.addAttachment(name, result, 'image/png')
    } else {
      AllureReporter.addAttachment(name, content, 'image/png')
    }
  }

  /**
   * Add description to test
   */
  static description(description: string) {
    AllureReporter.description(description)
  }

  /**
   * Add severity
   */
  static severity(severity: 'critical' | 'high' | 'medium' | 'low' | 'trivial') {
    AllureReporter.severity(severity)
  }

  /**
   * Add epic/feature
   */
  static epic(epic: string) {
    AllureReporter.epic(epic)
  }

  /**
   * Add feature
   */
  static feature(feature: string) {
    AllureReporter.feature(feature)
  }

  /**
   * Add story
   */
  static story(story: string) {
    AllureReporter.story(story)
  }

  /**
   * Add owner
   */
  static owner(owner: string) {
    AllureReporter.owner(owner)
  }

  /**
   * Add tag
   */
  static tag(tag: string) {
    AllureReporter.tag(tag)
  }

  /**
   * Add link
   */
  static link(name: string, url: string, type?: string) {
    AllureReporter.addLink(name, url, type)
  }

  /**
   * Add issue link
   */
  static issue(name: string, url: string) {
    AllureReporter.issue(name, url)
  }

  /**
   * Add TMS link
   */
  static tms(name: string, url: string) {
    AllureReporter.tms(name, url)
  }
}

/**
 * Custom test wrapper with Allure integration
 */
export const test = base.extend({
  '@allure': async ({}, use) => {
    // Setup
    startAllureReporting()
    addAllureEnvironment()

    await use({
      step: AllureTestWrapper.step.bind(AllureTestWrapper),
      attach: AllureTestWrapper.attach.bind(AllureTestWrapper),
      description: AllureTestWrapper.description.bind(AllureTestWrapper),
      severity: AllureTestWrapper.severity.bind(AllureTestWrapper),
      epic: AllureTestWrapper.epic.bind(AllureTestWrapper),
      feature: AllureTestWrapper.feature.bind(AllureTestWrapper),
      story: AllureTestWrapper.story.bind(AllureTestWrapper),
      owner: AllureTestWrapper.owner.bind(AllureTestWrapper),
      tag: AllureTestWrapper.tag.bind(AllureTestWrapper),
      link: AllureTestWrapper.link.bind(AllureTestWrapper),
      issue: AllureTestWrapper.issue.bind(AllureTestWrapper),
      tms: AllureTestWrapper.tms.bind(AllureTestWrapper)
    })

    // Cleanup
  }
})

/**
 * Allure test result categories
 */
export const allureCategories = [
  {
    name: 'Passed tests',
    matchedStatuses: ['passed']
  },
  {
    name: 'Failed tests',
    matchedStatuses: ['failed']
  },
  {
    name: 'Skipped tests',
    matchedStatuses: ['skipped']
  },
  {
    name: 'Broken tests',
    matchedStatuses: ['broken']
  },
  {
    name: 'Product defects',
    matchedStatuses: ['failed'],
    messageRegex: 'AssertionError.*'
  },
  {
    name: 'Test defects',
    matchedStatuses: ['broken'],
    messageRegex: 'Error.*'
  },
  {
    name: 'Environment defects',
    matchedStatuses: ['broken'],
    messageRegex: 'TimeoutError.*'
  }
]

/**
 * Generate Allure report from results
 */
export async function generateAllureReport(options: {
  resultsDir?: string
  reportDir?: string
  port?: number
} = {}) {
  const { resultsDir = 'test-results/allure', reportDir = 'allure-report', port = 5050 } = options

  const { spawn } = require('child_process')

  return new Promise((resolve, reject) => {
    console.log('📊 Generating Allure report...')

    const allure = spawn('allure', ['generate', resultsDir, '-o', reportDir, '--clean'], {
      stdio: 'inherit'
    })

    allure.on('close', (code: number) => {
      if (code === 0) {
        console.log(`✅ Allure report generated at ${reportDir}`)

        // Optionally open in browser
        console.log(`🌐 Open report: http://localhost:${port}`)
        resolve(reportDir)
      } else {
        reject(new Error(`Allure generation failed with code ${code}`))
      }
    })
  })
}

/**
 * Serve Allure report
 */
export function serveAllureReport(reportDir: string = 'allure-report', port: number = 5050) {
  const { spawn } = require('child_process')

  console.log(`🌐 Serving Allure report on http://localhost:${port}`)

  const serve = spawn('allure', ['serve', reportDir, '-p', port.toString()], {
    stdio: 'inherit'
  })

  serve.on('error', (error: Error) => {
    console.error('❌ Failed to serve Allure report:', error)
  })

  return serve
}

/**
 * Clean Allure results
 */
export function cleanAllureResults(resultsDir: string = 'test-results/allure') {
  const fs = require('fs')
  const path = require('path')

  if (fs.existsSync(resultsDir)) {
    fs.rmSync(resultsDir, { recursive: true, force: true })
    console.log(`🧹 Cleaned Allure results: ${resultsDir}`)
  }
}

/**
 * Integration hooks for test lifecycle
 */
export class AllureLifecycle {
  static beforeTest(testInfo: TestInfo) {
    AllureTestWrapper.setCurrentTest(testInfo)

    // Set test metadata
    AllureReporter.feature(testInfo.titlePath[0])
    AllureReporter.story(testInfo.titlePath[1] || 'Default')
  }

  static afterTest(testInfo: TestInfo, error: Error | null) {
    if (error) {
      AllureReporter.addArgument('error', error.message)
      AllureReporter.addAttachment(
        'Error Stack',
        error.stack || 'No stack trace',
        'text/plain'
      )
    }
  }

  static afterStep(testInfo: TestInfo, error: Error | null) {
    if (error) {
      // Add screenshot on step failure
      // This would be implemented in the actual test
    }
  }
}
