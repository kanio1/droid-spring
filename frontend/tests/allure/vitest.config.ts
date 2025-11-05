/**
 * Vitest Allure Reporter Configuration
 *
 * Configures Allure reporting for Vitest unit tests
 * Integrates with Vitest's reporter API
 *
 * Usage:
 * ```typescript
 * // vitest.config.ts
 * import { defineConfig } from 'vitest/config'
 * import { allureReporter } from '@/tests/allure/vitest.config'
 *
 * export default defineConfig({
 *   test: {
 *     reporters: [
 *       'default',
 *       allureReporter({
 *         outputDir: 'test-results/allure-vitest'
 *       })
 *     ]
 *   }
 * })
 * ```
 */

import { type VitestReporter } from 'vitest'

export interface AllureVitestConfig {
  outputDir?: string
  reportFlatten?: boolean
  includeConsoleOutput?: boolean
  includeTestOutput?: boolean
}

export interface AllureTestResult {
  uid: string
  title: string
  fullTitle: string
  duration: number
  status: 'passed' | 'failed' | 'skipped' | 'pending' | 'todo'
  failure?: {
    message: string
    stack?: string
  }
  testCase?: {
    file?: string
    line?: number
    column?: number
  }
  tags?: string[]
}

export class AllureVitestReporter implements VitestReporter {
  private config: Required<AllureVitestConfig>
  private results: AllureTestResult[] = []

  constructor(options: AllureVitestConfig = {}) {
    this.config = {
      outputDir: 'test-results/allure-vitest',
      reportFlatten: false,
      includeConsoleOutput: true,
      includeTestOutput: true,
      ...options
    }
  }

  onInit(config: any) {
    console.log('📊 Allure Vitest Reporter initialized')
    this.createConfigFile()
  }

  onTestAdded(test: any) {
    // Called when a test is added
  }

  onTestStarted(test: any) {
    // Called when a test starts
  }

  onTestFinished(test: any) {
    // Called when a test finishes
  }

  onTestFailed(test: any, errors: any[]) {
    const failure = errors.find((err: any) => err.stack)
    this.results.push({
      uid: test.id,
      title: test.name,
      fullTitle: test.suite?.name ? `${test.suite.name} > ${test.name}` : test.name,
      duration: test.result?.duration || 0,
      status: 'failed',
      failure: failure ? {
        message: failure.message,
        stack: failure.stack
      } : undefined,
      testCase: {
        file: test.file?.name,
        line: test?.result?.startTime
      },
      tags: test.tags
    })
  }

  onTestPassed(test: any) {
    this.results.push({
      uid: test.id,
      title: test.name,
      fullTitle: test.suite?.name ? `${test.suite.name} > ${test.name}` : test.name,
      duration: test.result?.duration || 0,
      status: 'passed',
      testCase: {
        file: test.file?.name
      },
      tags: test.tags
    })
  }

  onTestSkipped(test: any) {
    this.results.push({
      uid: test.id,
      title: test.name,
      fullTitle: test.suite?.name ? `${test.suite.name} > ${test.name}` : test.name,
      duration: 0,
      status: 'skipped',
      testCase: {
        file: test.file?.name
      },
      tags: test.tags
    })
  }

  onStdout(chunk: any, type: 'stdout' | 'stderr') {
    if (this.config.includeConsoleOutput) {
      console.log(`Console ${type}:`, chunk.toString())
    }
  }

  onStdError(chunk: any, type: 'stdout' | 'stderr') {
    if (this.config.includeConsoleOutput) {
      console.error(`Console ${type}:`, chunk.toString())
    }
  }

  onFinished(files: any, errors: any[]) {
    this.generateAllureResults()

    console.log(`📊 Allure results written to ${this.config.outputDir}`)
    console.log(`🌐 To view report: allure serve ${this.config.outputDir}`)
  }

  private createConfigFile() {
    const fs = require('fs')
    const path = require('path')

    if (!fs.existsSync(this.config.outputDir)) {
      fs.mkdirSync(this.config.outputDir, { recursive: true })
    }

    const configPath = path.join(this.config.outputDir, 'environment.properties')

    const envContent = `
project=BSS System
version=1.0.0
framework=Vitest
reporter=Allure
`

    fs.writeFileSync(configPath, envContent.trim())
  }

  private generateAllureResults() {
    const fs = require('fs')
    const path = require('path')
    const crypto = require('crypto')

    const resultsDir = path.join(this.config.outputDir, 'results')
    if (!fs.existsSync(resultsDir)) {
      fs.mkdirSync(resultsDir, { recursive: true })
    }

    // Generate environment file
    const envFile = path.join(this.config.outputDir, 'environment.properties')
    const envContent = `
project=BSS System
version=1.0.0
framework=Vitest
node=${process.version}
platform=${process.platform}
timestamp=${new Date().toISOString()}
`
    fs.writeFileSync(envFile, envContent.trim())

    // Generate result files
    this.results.forEach((result, index) => {
      const resultFile = path.join(resultsDir, `${index + 1}-${result.uid}.json`)

      const allureResult = {
        uuid: result.uid,
        historyId: crypto.createHash('md5').update(result.fullTitle).digest('hex'),
        fullName: result.fullTitle,
        labels: [
          { name: 'suite', value: this.extractSuite(result.fullTitle) },
          { name: 'parentSuite', value: this.extractSuite(result.fullTitle) },
          { name: 'subSuite', value: this.extractSubSuite(result.fullTitle) },
          { name: 'epic', value: 'BSS Unit Tests' },
          { name: 'feature', value: this.extractFeature(result.fullTitle) },
          { name: 'story', value: this.extractStory(result.fullTitle) }
        ].filter((label: any) => label.value),
        links: [],
        name: result.title,
        status: this.mapStatus(result.status),
        statusDetails: result.failure ? {
          message: result.failure.message,
          trace: result.failure.stack
        } : undefined,
        stage: 'finished',
        description: '',
        start: Date.now(),
        stop: Date.now() + result.duration,
        attachments: [],
        steps: [],
        parameters: []
      }

      fs.writeFileSync(resultFile, JSON.stringify(allureResult, null, 2))
    })

    console.log(`✅ Generated ${this.results.length} Allure test results`)
  }

  private extractSuite(fullTitle: string): string {
    const parts = fullTitle.split(' > ')
    return parts[0] || 'Default Suite'
  }

  private extractSubSuite(fullTitle: string): string | undefined {
    const parts = fullTitle.split(' > ')
    return parts[1] || undefined
  }

  private extractFeature(fullTitle: string): string {
    const parts = fullTitle.split(' > ')
    return parts[0] || 'Feature'
  }

  private extractStory(fullTitle: string): string {
    const parts = fullTitle.split(' > ')
    return parts[1] || parts[0] || 'Story'
  }

  private mapStatus(status: string): string {
    const statusMap: Record<string, string> = {
      passed: 'passed',
      failed: 'failed',
      skipped: 'skipped',
      pending: 'pending',
      todo: 'pending'
    }

    return statusMap[status] || 'unknown'
  }
}

export function allureReporter(options: AllureVitestConfig = {}) {
  return new AllureVitestReporter(options)
}

export { AllureVitestReporter }
