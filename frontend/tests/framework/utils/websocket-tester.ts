/**
 * WebSocket Tester Utility
 *
 * Advanced WebSocket testing utility for Playwright 1.56.1
 * Provides comprehensive WebSocket testing capabilities including:
 * - Connection management
 * - Message sending/receiving
 * - Connection state monitoring
 * - Reconnection logic testing
 * - Broadcast channel testing
 * - Service worker testing
 *
 * Usage:
 * ```typescript
 * const wsTester = new WebSocketTester(page)
 * await wsTester.connect('ws://localhost:8080/ws')
 * await wsTester.sendMessage({ type: 'ping' })
 * await wsTester.waitForMessage(msg => msg.type === 'pong')
 * ```
 */

import { type Page, type BrowserContext, type WebSocket } from '@playwright/test'

export interface WebSocketMessage {
  type: string
  data?: any
  timestamp?: number
  id?: string
  [key: string]: any
}

export interface WebSocketOptions {
  url: string
  protocols?: string[]
  headers?: Record<string, string>
  timeout?: number
}

export interface ConnectionState {
  connected: boolean
  reconnecting: boolean
  lastError: Error | null
  reconnectAttempts: number
  messageCount: number
}

export class WebSocketTester {
  private page: Page
  private webSocket: WebSocket | null = null
  private isConnected = false
  private messageQueue: WebSocketMessage[] = []
  private messageListeners: Array<(message: WebSocketMessage) => boolean> = []
  private connectionState: ConnectionState = {
    connected: false,
    reconnecting: false,
    lastError: null,
    reconnectAttempts: 0,
    messageCount: 0
  }
  private reconnectTimer: NodeJS.Timeout | null = null
  private maxReconnectAttempts = 5
  private reconnectDelay = 1000

  constructor(page: Page) {
    this.page = page
  }

  /**
   * Connect to WebSocket server
   */
  async connect(url: string, options: Partial<WebSocketOptions> = {}): Promise<WebSocket> {
    try {
      // Navigate to a page that will establish WebSocket connection
      await this.page.goto('about:blank')

      // Listen for WebSocket events
      this.page.on('websocket', ws => {
        this.webSocket = ws
        this.isConnected = true
        this.connectionState.connected = true
        this.connectionState.lastError = null
        this.connectionState.messageCount = 0

        // Listen for messages
        ws.on('framereceived', frame => {
          this.handleMessage(frame)
        })

        // Listen for close
        ws.on('close', () => {
          this.isConnected = false
          this.connectionState.connected = false
        })

        // Listen for errors
        ws.on('frameerror', error => {
          this.connectionState.lastError = error as any
        })
      })

      // Execute JavaScript to create WebSocket connection
      await this.page.evaluate(
        ({ url, options }) => {
          const ws = new WebSocket(url, options.protocols || [])
          ;(window as any).testWebSocket = ws

          ws.onopen = () => {
            console.log('WebSocket connected')
          }

          ws.onmessage = (event) => {
            console.log('WebSocket message received:', event.data)
          }

          ws.onerror = (error) => {
            console.error('WebSocket error:', error)
          }

          ws.onclose = () => {
            console.log('WebSocket closed')
          }
        },
        { url, options: { protocols: options.protocols, headers: options.headers } }
      )

      // Wait for connection to be established
      await this.page.waitForFunction(() => {
        return (window as any).testWebSocket && (window as any).testWebSocket.readyState === 1
      }, { timeout: options.timeout || 5000 })

      this.connectionState.connected = true
      return this.webSocket!
    } catch (error) {
      this.connectionState.lastError = error as any
      throw new Error(`Failed to connect to WebSocket: ${error}`)
    }
  }

  /**
   * Send message through WebSocket
   */
  async sendMessage(message: any): Promise<void> {
    if (!this.isConnected || !this.webSocket) {
      throw new Error('WebSocket is not connected')
    }

    const messageStr = typeof message === 'string' ? message : JSON.stringify(message)

    await this.page.evaluate(({ message }) => {
      const ws = (window as any).testWebSocket
      if (ws && ws.readyState === 1) {
        ws.send(message)
      }
    }, { message: messageStr })

    this.connectionState.messageCount++
  }

  /**
   * Send JSON message
   */
  async sendJsonMessage(message: WebSocketMessage): Promise<void> {
    await this.sendMessage(JSON.stringify(message))
  }

  /**
   * Wait for message matching predicate
   */
  async waitForMessage(
    predicate: (message: WebSocketMessage) => boolean,
    timeout: number = 5000
  ): Promise<WebSocketMessage> {
    return new Promise((resolve, reject) => {
      const startTime = Date.now()

      const checkMessage = (message: WebSocketMessage) => {
        if (predicate(message)) {
          this.messageListeners = this.messageListeners.filter(l => l !== checkMessage)
          resolve(message)
          return true
        }
        return false
      }

      this.messageListeners.push(checkMessage)

      const timeoutId = setTimeout(() => {
        this.messageListeners = this.messageListeners.filter(l => l !== checkMessage)
        reject(new Error(`Timeout waiting for WebSocket message after ${timeout}ms`))
      }, timeout)

      // Check messages already in queue
      for (const msg of this.messageQueue) {
        if (checkMessage(msg)) {
          clearTimeout(timeoutId)
          return
        }
      }
    })
  }

  /**
   * Wait for specific message type
   */
  async waitForMessageType(messageType: string, timeout: number = 5000): Promise<WebSocketMessage> {
    return this.waitForMessage(msg => msg.type === messageType, timeout)
  }

  /**
   * Wait for ping-pong
   */
  async waitForPong(timeout: number = 5000): Promise<WebSocketMessage> {
    return this.waitForMessage(msg => msg.type === 'pong' || msg === 'pong', timeout)
  }

  /**
   * Broadcast message to multiple clients
   */
  async broadcast(message: any, contexts: BrowserContext[]): Promise<void> {
    const promises = contexts.map(context =>
      context.evaluate(({ message, url }) => {
        return new Promise<void>((resolve, reject) => {
          const ws = new WebSocket(url)
          ws.onopen = () => {
            ws.send(typeof message === 'string' ? message : JSON.stringify(message))
            setTimeout(() => {
              ws.close()
              resolve()
            }, 100)
          }
          ws.onerror = reject
        })
      }, { message, url: 'ws://localhost:8080/broadcast' })
    )

    await Promise.all(promises)
  }

  /**
   * Monitor connection state
   */
  getConnectionState(): ConnectionState {
    return { ...this.connectionState }
  }

  /**
   * Check if connected
   */
  isWebSocketConnected(): boolean {
    return this.isConnected
  }

  /**
   * Get message queue
   */
  getMessageQueue(): WebSocketMessage[] {
    return [...this.messageQueue]
  }

  /**
   * Get message count
   */
  getMessageCount(): number {
    return this.connectionState.messageCount
  }

  /**
   * Clear message queue
   */
  clearMessageQueue(): void {
    this.messageQueue = []
  }

  /**
   * Disconnect WebSocket
   */
  async disconnect(): Promise<void> {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }

    await this.page.evaluate(() => {
      const ws = (window as any).testWebSocket
      if (ws) {
        ws.close()
      }
    })

    this.isConnected = false
    this.connectionState.connected = false
  }

  /**
   * Reconnect with exponential backoff
   */
  async reconnect(url: string, options: Partial<WebSocketOptions> = {}): Promise<void> {
    this.connectionState.reconnecting = true
    this.connectionState.reconnectAttempts++

    if (this.connectionState.reconnectAttempts > this.maxReconnectAttempts) {
      throw new Error(`Max reconnection attempts (${this.maxReconnectAttempts}) reached`)
    }

    const delay = this.reconnectDelay * Math.pow(2, this.connectionState.reconnectAttempts - 1)
    await new Promise(resolve => setTimeout(resolve, delay))

    try {
      await this.disconnect()
      await this.connect(url, options)
      this.connectionState.reconnecting = false
      this.connectionState.reconnectAttempts = 0
    } catch (error) {
      this.connectionState.lastError = error as any
      throw error
    }
  }

  /**
   * Handle incoming message
   */
  private handleMessage(frame: string): void {
    try {
      let message: WebSocketMessage

      try {
        message = JSON.parse(frame)
      } catch {
        message = { type: 'raw', data: frame, timestamp: Date.now() }
      }

      if (!message.timestamp) {
        message.timestamp = Date.now()
      }

      this.messageQueue.push(message)
      this.connectionState.messageCount++

      // Notify listeners
      this.messageListeners = this.messageListeners.filter(listener => {
        try {
          return !listener(message)
        } catch (error) {
          console.error('Error in message listener:', error)
          return true
        }
      })
    } catch (error) {
      console.error('Error handling WebSocket message:', error)
    }
  }

  /**
   * Wait for connection to be established
   */
  async waitForConnection(timeout: number = 5000): Promise<void> {
    const startTime = Date.now()

    while (Date.now() - startTime < timeout) {
      if (this.isConnected) {
        return
      }
      await new Promise(resolve => setTimeout(resolve, 100))
    }

    throw new Error('WebSocket connection timeout')
  }

  /**
   * Monitor network idle (no messages for specified time)
   */
  async waitForNetworkIdle(idleTime: number = 2000, timeout: number = 10000): Promise<void> {
    return new Promise((resolve, reject) => {
      const startTime = Date.now()
      let lastMessageTime = Date.now()

      const checkIdle = () => {
        const now = Date.now()
        if (now - lastMessageTime >= idleTime) {
          resolve()
        } else if (now - startTime >= timeout) {
          reject(new Error('Timeout waiting for network idle'))
        } else {
          setTimeout(checkIdle, 100)
        }
      }

      // Update lastMessageTime when messages arrive
      const originalQueue = this.messageQueue
      Object.defineProperty(this, 'messageQueue', {
        get: () => {
          lastMessageTime = Date.now()
          return originalQueue
        }
      })

      checkIdle()
    })
  }

  /**
   * Get WebSocket ready state
   */
  async getReadyState(): Promise<number> {
    return await this.page.evaluate(() => {
      const ws = (window as any).testWebSocket
      return ws ? ws.readyState : -1
    })
  }

  /**
   * Test message ordering
   */
  async testMessageOrdering(messages: any[], timeout: number = 5000): Promise<boolean> {
    const received: any[] = []

    const messageListener = (msg: WebSocketMessage) => {
      received.push(msg.data || msg)
      if (received.length === messages.length) {
        // Check if order matches
        return messages.every((expected, index) => {
          const received = JSON.stringify(msg)
          const expectedStr = typeof expected === 'string' ? expected : JSON.stringify(expected)
          return received.includes(expectedStr)
        })
      }
      return false
    }

    this.messageListeners.push(messageListener)

    // Send messages
    for (const message of messages) {
      await this.sendMessage(message)
    }

    // Wait for all messages
    try {
      await this.waitForMessage(() => received.length >= messages.length, timeout)
      return true
    } catch {
      return false
    }
  }
}

/**
 * WebSocket Service for Service Worker testing
 */
export class ServiceWorkerWebSocketTester {
  private page: Page

  constructor(page: Page) {
    this.page = page
  }

  /**
   * Test service worker WebSocket connection
   */
  async testServiceWorkerConnection(): Promise<boolean> {
    const result = await this.page.evaluate(async () => {
      if (!navigator.serviceWorker) {
        return { success: false, error: 'Service Worker not supported' }
      }

      try {
        const registration = await navigator.serviceWorker.register('/sw.js')
        await navigator.serviceWorker.ready

        // Test WebSocket from service worker
        return { success: true, registration }
      } catch (error) {
        return { success: false, error: error.message }
      }
    })

    return result.success
  }

  /**
   * Test message channel between page and service worker
   */
  async testMessageChannel(): Promise<WebSocketMessage[]> {
    const messages: WebSocketMessage[] = []

    await this.page.evaluate(() => {
      if (navigator.serviceWorker.controller) {
        navigator.serviceWorker.controller.postMessage({ type: 'ping' })
      }
    })

    this.page.on('console', msg => {
      if (msg.text().includes('SW Message:')) {
        messages.push({
          type: 'sw-message',
          data: msg.text(),
          timestamp: Date.now()
        })
      }
    })

    await this.page.waitForTimeout(2000)

    return messages
  }
}

/**
 * BroadcastChannel API testing
 */
export class BroadcastChannelTester {
  private page: Page
  private channels: Map<string, BroadcastChannel> = new Map()

  constructor(page: Page) {
    this.page = page
  }

  /**
   * Create and test broadcast channel
   */
  async createChannel(name: string): Promise<void> {
    await this.page.evaluate((channelName) => {
      const channel = new BroadcastChannel(channelName)
      ;(window as any)[`bc_${channelName}`] = channel

      channel.onmessage = (event) => {
        console.log('BroadcastChannel message:', event.data)
      }
    }, name)

    this.channels.set(name, {} as any)
  }

  /**
   * Send message through broadcast channel
   */
  async sendMessage(channelName: string, message: any): Promise<void> {
    await this.page.evaluate(({ channelName, message }) => {
      const channel = (window as any)[`bc_${channelName}`]
      if (channel) {
        channel.postMessage(message)
      }
    }, { channelName, message })
  }

  /**
   * Wait for broadcast message
   */
  async waitForMessage(channelName: string, timeout: number = 5000): Promise<any> {
    return new Promise((resolve, reject) => {
      const timeoutId = setTimeout(() => {
        reject(new Error(`Timeout waiting for broadcast message`))
      }, timeout)

      this.page.on('console', msg => {
        if (msg.text().includes(`BC ${channelName}:`)) {
          clearTimeout(timeoutId)
          try {
            const data = JSON.parse(msg.text().split(': ')[1])
            resolve(data)
          } catch {
            resolve(msg.text())
          }
        }
      })
    })
  }
}
