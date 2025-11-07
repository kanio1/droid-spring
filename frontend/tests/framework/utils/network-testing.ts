/**
 * Network Testing Utilities
 * Simulate various network conditions for testing resilience
 */

import { type Page, type Route, type HTTPResponse } from '@playwright/test'

export interface NetworkCondition {
  downloadThroughput?: number // bytes per second
  uploadThroughput?: number   // bytes per second
  latency?: number           // milliseconds
  connectionType?: 'slow-2g' | '2g' | '3g' | '4g' | 'wifi' | 'ethernet' | 'default'
}

export interface RequestPattern {
  url: string | RegExp
  method?: string
  status?: number
  delay?: number
  abort?: boolean
  response?: any
}

class NetworkSimulator {
  private routes: RequestPattern[] = []
  private originalRoutes: Map<string, any> = new Map()

  async simulateCondition(page: Page, condition: NetworkCondition) {
    await page.route('**/*', async (route) => {
      if (route.request().url().includes('data:') || route.request().url().includes('blob:')) {
        await route.continue()
        return
      }

      // Add latency
      if (condition.latency) {
        await new Promise(resolve => setTimeout(resolve, condition.latency))
      }

      // Apply throttling
      if (condition.downloadThroughput || condition.uploadThroughput) {
        // Note: Playwright doesn't support direct throttling at the route level
        // This would require using browser devtools protocol
        console.warn('Throughput throttling requires CDP - consider using browser.emulateNetworkConditions')
      }

      await route.continue()
    })
  }

  async simulateOffline(page: Page) {
    await page.context().setOffline(true)

    // Also route requests to fail
    await page.route('**/*', async (route) => {
      if (!route.request().url().startsWith('data:') && !route.request().url().startsWith('blob:')) {
        await route.abort('internetdisconnected')
      } else {
        await route.continue()
      }
    })
  }

  async simulateOnline(page: Page) {
    await page.context().setOffline(false)
    await page.unroute('**/*')
  }

  async mockRequests(page: Page, patterns: RequestPattern[]) {
    this.routes = patterns

    for (const pattern of patterns) {
      await page.route(pattern.url, async (route) => {
        const request = route.request()

        // Check method
        if (pattern.method && request.method() !== pattern.method) {
          await route.continue()
          return
        }

        // Check status if specified
        if (pattern.status) {
          if (pattern.abort) {
            await route.abort('failed')
            return
          }

          if (pattern.delay) {
            await new Promise(resolve => setTimeout(resolve, pattern.delay))
          }

          await route.fulfill({
            status: pattern.status,
            contentType: 'application/json',
            body: JSON.stringify(pattern.response || { error: 'Mocked response' })
          })
          return
        }

        await route.continue()
      })
    }
  }

  async clearMocks(page: Page) {
    await page.unroute('**/*')
    this.routes = []
  }

  async interceptAndModify(page: Page, config: {
    match: string | RegExp
    modifyRequest?: (request: any) => any
    modifyResponse?: (response: any) => any
  }) {
    await page.route(config.match, async (route) => {
      const request = route.request()

      // Modify request
      let modifiedRequest = request
      if (config.modifyRequest) {
        modifiedRequest = config.modifyRequest(request)
      }

      // Fetch original response
      const response = await route.fetch()

      // Modify response
      let modifiedResponse = response
      if (config.modifyResponse) {
        const body = await response.text()
        const modifiedBody = config.modifyResponse(body)
        await route.fulfill({
          response,
          body: modifiedBody
        })
        return
      }

      await route.continue()
    })
  }

  async captureNetworkTraffic(page: Page) {
    const requests: any[] = []
    const responses: any[] = []

    page.on('request', (request) => {
      requests.push({
        url: request.url(),
        method: request.method(),
        headers: request.headers(),
        timestamp: Date.now()
      })
    })

    page.on('response', async (response) => {
      const body = await response.text().catch(() => null)
      responses.push({
        url: response.url(),
        status: response.status(),
        headers: response.headers(),
        body: body?.substring(0, 1000), // Limit body size
        timestamp: Date.now()
      })
    })

    return {
      getRequests: () => requests,
      getResponses: () => responses,
      getSummary: () => ({
        totalRequests: requests.length,
        totalResponses: responses.length,
        statusCodes: responses.reduce((acc, r) => {
          acc[r.status] = (acc[r.status] || 0) + 1
          return acc
        }, {} as Record<number, number>),
        averageResponseTime: 0 // Would need timing data
      })
    }
  }
}

export const networkSimulator = new NetworkSimulator()

// Convenience functions
export async function slowNetwork(page: Page) {
  await networkSimulator.simulateCondition(page, {
    connectionType: 'slow-2g',
    latency: 500
  })
}

export async function fastNetwork(page: Page) {
  await networkSimulator.simulateCondition(page, {
    connectionType: '4g',
    latency: 50
  })
}

export async function unstableNetwork(page: Page, {
  latency = 100,
  jitter = 200
} = {}) {
  await page.route('**/*', async (route) => {
    const randomLatency = latency + Math.random() * jitter
    await new Promise(resolve => setTimeout(resolve, randomLatency))
    await route.continue()
  })
}

export async function blockRequests(page: Page, patterns: (string | RegExp)[]) {
  await page.route('**/*', async (route) => {
    const url = route.request().url()
    const shouldBlock = patterns.some(pattern => {
      if (typeof pattern === 'string') {
        return url.includes(pattern)
      }
      return pattern.test(url)
    })

    if (shouldBlock) {
      await route.abort('blocked')
    } else {
      await route.continue()
    }
  })
}

export async function cacheResponses(page: Page, config: {
  match: string | RegExp
  ttl: number // milliseconds
}) {
  const cache = new Map<string, { response: any; timestamp: number }>()

  await page.route(config.match, async (route) => {
    const url = route.request().url()
    const cached = cache.get(url)

    if (cached && (Date.now() - cached.timestamp) < config.ttl) {
      await route.fulfill(cached.response)
      return
    }

    const response = await route.fetch()
    const body = await response.text()

    cache.set(url, {
      response: {
        status: response.status(),
        headers: response.headers(),
        body
      },
      timestamp: Date.now()
    })

    await route.continue()
  })

  return cache
}

export async function simulateSlowAPI(page: Page, urlPattern: string | RegExp, delay: number) {
  await page.route(urlPattern, async (route) => {
    await new Promise(resolve => setTimeout(resolve, delay))
    await route.continue()
  })
}

export async function simulateAPIError(page: Page, urlPattern: string | RegExp, errorCode: number) {
  await page.route(urlPattern, async (route) => {
    await route.fulfill({
      status: errorCode,
      contentType: 'application/json',
      body: JSON.stringify({
        error: 'Simulated error',
        code: errorCode,
        message: 'This is a simulated error for testing'
      })
    })
  })
}

export async function replayNetwork(page: Page, replayFile: string) {
  const fs = require('fs')
  if (!fs.existsSync(replayFile)) {
    throw new Error(`Replay file not found: ${replayFile}`)
  }

  const replayData = JSON.parse(fs.readFileSync(replayFile, 'utf-8'))
  const playbackIndex = { current: 0 }

  await page.route('**/*', async (route) => {
    if (playbackIndex.current >= replayData.length) {
      await route.continue()
      return
    }

    const playback = replayData[playbackIndex.current]
    playbackIndex.current++

    if (playbookMatchesRequest(playbook, route.request())) {
      await route.fulfill(playbook.response)
    } else {
      await route.continue()
    }
  })
}

function playbookMatchesRequest(playback: any, request: any): boolean {
  return playback.url === request.url() && playback.method === request.method()
}

// Network debugging utilities
export async function logNetworkActivity(page: Page) {
  page.on('request', (request) => {
    console.log(`[Request] ${request.method()} ${request.url()}`)
  })

  page.on('response', async (response) => {
    const url = response.url()
    const status = response.status()
    const ok = response.ok()
    console.log(`[Response] ${status} ${url} ${ok ? '✓' : '✗'}`)
  })
}

export async function measureNetworkPerformance(page: Page) {
  const [response] = await Promise.all([
    page.waitForResponse(r => r.url().includes('/api/')),
    page.goto('/')
  ])

  const timing = response.timing()
  return {
    dns: timing.domainLookupEnd - timing.domainLookupStart,
    connect: timing.connectEnd - timing.connectStart,
    tls: timing.secureConnectionStart ? timing.connectEnd - timing.secureConnectionStart : 0,
    ttfb: timing.responseStart - timing.requestStart,
    download: timing.responseEnd - timing.responseStart,
    total: timing.responseEnd - timing.requestStart
  }
}
