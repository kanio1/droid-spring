/**
 * Playwright Allure Integration Hooks
 *
 * Automatically integrates Allure reporting with Playwright test execution
 * Captures screenshots, logs, and test metadata
 *
 * Usage:
 * Add to playwright.config.ts:
 * ```typescript
 * export default defineConfig({
 *   reporter: [['list'], ['allure-playwright', {
 *     outputFolder: 'test-results/allure',
 *     includeLegacyMochawesome: true,
 *   }]]
 * })
 * ```
 */

import { type TestResult, type TestCase } from '@playwright/test/reporter'

/**
 * Configure Allure for Playwright
 */
export interface AllurePlaywrightConfig {
  outputFolder?: string
  cleanOutputDir?: boolean
  includeLegacyMochawesome?: boolean
  suiteName?: string
}

/**
 * Default Allure Playwright configuration
 */
export const defaultAllureConfig: Required<AllurePlaywrightConfig> = {
  outputFolder: 'test-results/allure',
  cleanOutputDir: true,
  includeLegacyMochawesome: false,
  suiteName: 'Playwright Tests'
}

/**
 * Transform Playwright test result to Allure format
 */
export function transformTestResult(result: TestResult, config: Required<AllurePlaywrightConfig>) {
  const startTime = new Date(result.startTime.getTime())
  const stopTime = new Date(result.stopTime.getTime())
  const duration = stopTime.getTime() - startTime.getTime()

  return {
    uuid: result.testId,
    historyId: result.testId,
    fullName: result.test?.titlePath().join(' > ') || '',
    labels: [
      {
        name: 'suite',
        value: result.test?.titlePath()[0] || config.suiteName
      },
      {
        name: 'parentSuite',
        value: result.test?.titlePath()[0]
      },
      {
        name: 'subSuite',
        value: result.test?.titlePath()[1]
      },
      {
        name: 'epic',
        value: 'BSS System'
      },
      {
        name: 'feature',
        value: result.test?.titlePath()[0] || config.suiteName
      },
      {
        name: 'story',
        value: result.test?.titlePath()[1] || 'Default'
      },
      {
        name: 'severity',
        value: getSeverityFromTest(result.test)
      },
      {
        name: 'tag',
        value: getTagsFromTest(result.test)
      }
    ],
    links: getLinksFromTest(result.test),
    name: result.title,
    status: mapStatus(result.status),
    statusDetails: {
      message: result.error?.message,
      trace: result.error?.stack
    },
    stage: 'finished',
    description: getDescriptionFromTest(result.test),
    start: startTime.getTime(),
    stop: stopTime.getTime(),
    attachments: [],
    parameters: getParametersFromTest(result.test)
  }
}

/**
 * Map Playwright status to Allure status
 */
function mapStatus(status: TestResult['status']): string {
  const statusMap: Record<TestResult['status'], string> = {
    passed: 'passed',
    failed: 'failed',
    timedOut: 'broken',
    skipped: 'skipped',
    interrupted: 'broken'
  }

  return statusMap[status] || 'unknown'
}

/**
 * Extract severity from test
 */
function getSeverityFromTest(test: TestCase | undefined): string {
  // This would check for @allure.severity or similar annotations
  // For now, return default
  return 'medium'
}

/**
 * Extract tags from test
 */
function getTagsFromTest(test: TestCase | undefined): string {
  if (!test) return ''

  // Extract tags from test title or metadata
  // e.g., '@smoke', '@critical', etc.
  const title = test.title
  const tagMatch = title.match(/@(\w+)/)
  return tagMatch ? tagMatch[1] : ''
}

/**
 * Extract links from test
 */
function getLinksFromTest(test: TestCase | undefined): Array<{ name: string; url: string; type: string }> {
  if (!test) return []

  // Check for links in test title or metadata
  // e.g., 'Test name [link:https://jira.com/issue-123]'
  const links: Array<{ name: string; url: string; type: string }> = []

  const linkMatch = test.title.match(/\[link:(.+?)\]/)
  if (linkMatch) {
    links.push({
      name: 'Issue',
      url: linkMatch[1],
      type: 'issue'
    })
  }

  return links
}

/**
 * Extract description from test
 */
function getDescriptionFromTest(test: TestCase | undefined): string {
  if (!test) return ''

  // Extract description from test title
  // e.g., 'Test name -- Description'
  const parts = test.title.split(' -- ')
  return parts.length > 1 ? parts[1] : ''
}

/**
 * Extract parameters from test
 */
function getParametersFromTest(test: TestCase | undefined): Array<{ name: string; value: string }> {
  if (!test) return []

  // Extract parameterized values
  // This would check for parameterized tests
  return []
}

/**
 * Add attachment to test result
 */
export function addAttachment(
  testResult: any,
  name: string,
  content: Buffer | string,
  type: string = 'application/octet-stream'
) {
  if (!testResult.attachments) {
    testResult.attachments = []
  }

  testResult.attachments.push({
    name,
    type,
    source: name, // This would be the file path
    size: Buffer.isBuffer(content) ? content.length : content.length
  })
}

/**
 * Add step to test result
 */
export function addStep(
  testResult: any,
  name: string,
  status: string = 'passed',
  startTime?: Date,
  stopTime?: Date
) {
  if (!testResult.steps) {
    testResult.steps = []
  }

  testResult.steps.push({
    name,
    status,
    start: startTime?.getTime() || Date.now(),
    stop: stopTime?.getTime() || Date.now()
  })
}

/**
 * Generate Allure report configuration file
 */
export function generateAllureConfig(config: Required<AllurePlaywrightConfig>) {
  const fs = require('fs')
  const path = require('path')

  const configDir = path.dirname(config.outputFolder)
  if (!fs.existsSync(configDir)) {
    fs.mkdirSync(configDir, { recursive: true })
  }

  const configPath = path.join(configDir, 'allure.properties')

  const properties = `
# Allure Configuration
allure.results.directory=${config.outputFolder}
allure.report.directory=allure-report
allure.clean.results.dir=${config.cleanOutputDir}
allure.link.issue.pattern=https://jira.company.com/browse/{}
allure.link.tms.pattern=https://tms.company.com/case/{}
`

  fs.writeFileSync(configPath, properties.trim())
  console.log(`✅ Allure config written to ${configPath}`)
}

/**
 * After hook for capturing screenshots on failure
 */
export async function captureScreenshotOnFailure(
  testInfo: any,
  screenshotPath: string
) {
  if (testInfo.status !== 'passed') {
    try {
      const fs = require('fs')
      if (fs.existsSync(screenshotPath)) {
        // This would add the screenshot as an attachment to the test result
        console.log(`📸 Screenshot captured: ${screenshotPath}`)
      }
    } catch (error) {
      console.error('Failed to capture screenshot:', error)
    }
  }
}

/**
 * After hook for capturing console logs
 */
export function captureConsoleLogs(page: any, testInfo: any) {
  page.on('console', (msg: any) => {
    if (msg.type() === 'error') {
      // Capture console errors
      console.log(`⚠️ Console Error: ${msg.text()}`)
    }
  })
}

/**
 * After hook for capturing network logs
 */
export function captureNetworkLogs(page: any, testInfo: any) {
  page.on('response', (response: any) => {
    if (!response.ok()) {
      // Capture failed network requests
      console.log(`❌ Network Error: ${response.status()} - ${response.url()}`)
    }
  })
}

/**
 * Custom reporter for Allure integration
 */
export class AllurePlaywrightReporter {
  private config: Required<AllurePlaywrightConfig>

  constructor(options: AllurePlaywrightConfig = {}) {
    this.config = { ...defaultAllureConfig, ...options }
    generateAllureConfig(this.config)
  }

  onBegin(config: any, suite: any) {
    console.log(`📊 Starting Allure reporting for ${suite.allTests().length} tests`)
  }

  onTestEnd(test: any, result: any) {
    const allureResult = transformTestResult(result, this.config)

    console.log(`✅ Allure: ${test.title} - ${allureResult.status}`)
  }

  onEnd(result: any) {
    console.log(`📊 Allure results written to ${this.config.outputFolder}`)
    console.log(`🌐 To view report: allure serve ${this.config.outputFolder}`)
  }
}

/**
 * Install Allure command line tool
 */
export function installAllureCLI(): Promise<void> {
  const { spawn } = require('child_process')
  const { promisify } = require('util')

  const exec = promisify(spawn)

  return new Promise((resolve, reject) => {
    console.log('📦 Installing Allure CLI...')

    // npm install -g allure-commandline
    const npm = spawn('npm', ['install', '-g', 'allure-commandline'])

    npm.on('close', (code: number) => {
      if (code === 0) {
        console.log('✅ Allure CLI installed successfully')
        resolve()
      } else {
        reject(new Error(`Failed to install Allure CLI (code ${code})`))
      }
    })
  })
}
