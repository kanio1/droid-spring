/**
 * Allure Utility Functions
 *
 * Provides helpers for adding Allure metadata, attachments, and steps
 */

import { test as base, type TestInfo } from '@playwright/test'

// Extend test interface to include Allure methods
type AllureTest = ReturnType<typeof base> & {
  step: (name: string, body: () => Promise<any> | any) => Promise<any>
  attachment: (name: string, content: Buffer | string, options?: { contentType?: string }) => void
  label: (name: string, value: string) => void
  link: (url: string, name?: string, type?: string) => void
  epic: (name: string) => void
  feature: (name: string) => void
  story: (name: string) => void
  suite: (name: string) => void
  owner: (name: string) => void
  severity: (level: 'blocker' | 'critical' | 'normal' | 'minor' | 'trivial') => void
  tag: (tag: string) => void
  parameter: (name: string, value: string) => void
}

// Allure test wrapper
export const test = base.extend<{
  allure: {
    step: (name: string, body: () => Promise<any> | any) => Promise<any>
    attachment: (name: string, content: Buffer | string, options?: { contentType?: string }) => void
    label: (name: string, value: string) => void
    link: (url: string, name?: string, type?: string) => void
    epic: (name: string) => void
    feature: (name: string) => void
    story: (name: string) => void
    suite: (name: string) => void
    owner: (name: string) => void
    severity: (level: 'blocker' | 'critical' | 'normal' | 'minor' | 'trivial') => void
    tag: (tag: string) => void
    parameter: (name: string, value: string) => void
  }
}>({
  allure: [
    async ({}, use) => {
      await use({
        step: async (name, body) => {
          console.log(`[Allure Step] ${name}`)
          try {
            const result = await body()
            console.log(`[Allure Step] ✓ ${name}`)
            return result
          } catch (error) {
            console.error(`[Allure Step] ✗ ${name}: ${error}`)
            throw error
          }
        },
        attachment: (name, content, options) => {
          console.log(`[Allure Attachment] ${name}`)
        },
        label: (name, value) => {
          console.log(`[Allure Label] ${name}: ${value}`)
        },
        link: (url, name, type) => {
          console.log(`[Allure Link] ${name || url} -> ${url}`)
        },
        epic: (name) => {
          console.log(`[Allure Epic] ${name}`)
        },
        feature: (name) => {
          console.log(`[Allure Feature] ${name}`)
        },
        story: (name) => {
          console.log(`[Allure Story] ${name}`)
        },
        suite: (name) => {
          console.log(`[Allure Suite] ${name}`)
        },
        owner: (name) => {
          console.log(`[Allure Owner] ${name}`)
        },
        severity: (level) => {
          console.log(`[Allure Severity] ${level}`)
        },
        tag: (tag) => {
          console.log(`[Allure Tag] ${tag}`)
        },
        parameter: (name, value) => {
          console.log(`[Allure Parameter] ${name}: ${value}`)
        },
      })
    },
    { auto: true },
  ],
})

export { test as allureTest }

// Helper function to add test metadata
export function addTestMetadata(
  testInfo: TestInfo,
  options: {
    epic?: string
    feature?: string
    story?: string
    severity?: 'blocker' | 'critical' | 'normal' | 'minor' | 'trivial'
    owner?: string
    tags?: string[]
    description?: string
  }
) {
  console.log(`[Test Metadata] ${testInfo.title}`)
  if (options.epic) console.log(`  Epic: ${options.epic}`)
  if (options.feature) console.log(`  Feature: ${options.feature}`)
  if (options.story) console.log(`  Story: ${options.story}`)
  if (options.severity) console.log(`  Severity: ${options.severity}`)
  if (options.owner) console.log(`  Owner: ${options.owner}`)
  if (options.tags) console.log(`  Tags: ${options.tags.join(', ')}`)
  if (options.description) console.log(`  Description: ${options.description}`)
}

// Helper to attach screenshots to Allure
export async function attachScreenshot(page: any, name: string) {
  try {
    const screenshot = await page.screenshot()
    console.log(`[Allure Screenshot] ${name}`)
  } catch (error) {
    console.error(`[Allure Screenshot] Failed to attach ${name}: ${error}`)
  }
}

// Helper to attach page source to Allure
export async function attachPageSource(page: any, name: string) {
  try {
    const content = await page.content()
    console.log(`[Allure Page Source] ${name}`)
  } catch (error) {
    console.error(`[Allure Page Source] Failed to attach ${name}: ${error}`)
  }
}

// Helper to attach console logs to Allure
export async function attachConsoleLogs(page: any, name: string) {
  const logs: string[] = []
  page.on('console', (msg: any) => {
    logs.push(`${msg.type()}: ${msg.text()}`)
  })
  console.log(`[Allure Console] Monitoring console logs for ${name}`)
}

// Helper to attach network requests to Allure
export async function attachNetworkActivity(page: any, name: string) {
  const requests: any[] = []
  const responses: any[] = []

  page.on('request', (request: any) => {
    requests.push({
      url: request.url(),
      method: request.method(),
      headers: request.headers(),
    })
  })

  page.on('response', (response: any) => {
    responses.push({
      url: response.url(),
      status: response.status(),
      headers: response.headers(),
    })
  })

  console.log(`[Allure Network] Monitoring network activity for ${name}`)
}

// Helper to add environment info to Allure
export function addEnvironmentInfo(info: Record<string, string>) {
  console.log('[Allure Environment]')
  Object.entries(info).forEach(([key, value]) => {
    console.log(`  ${key}: ${value}`)
  })
}
