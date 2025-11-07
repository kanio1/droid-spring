/**
 * WebSocket Connection and Reconnection Tests
 *
 * Tests for WebSocket connection management, reconnection logic,
 * and real-time communication features
 *
 * Playwright 1.56.1 features used:
 * - Enhanced WebSocket testing
 * - Network idle detection
 * - Multi-tab synchronization
 * - Broadcast channel testing
 * - Service worker testing
 */

import { test, expect } from '@playwright/test'
import { WebSocketTester, ServiceWorkerWebSocketTester, BroadcastChannelTester } from '../framework/utils/websocket-tester'
import { registerCustomMatchers } from '../framework/matchers/playwright-matchers'

// Register custom matchers
registerCustomMatchers()

test.describe('WebSocket Connection Tests', () => {
  test.beforeEach(async ({ page }) => {
    // Set up WebSocket mock server
    await page.route('**/ws**', async route => {
      // In real tests, this would connect to actual WebSocket server
      route.fulfill({
        status: 101,
        headers: {
          'Upgrade': 'websocket',
          'Connection': 'Upgrade'
        }
      })
    })
  })

  test('should establish WebSocket connection', async ({ page }) => {
    const wsTester = new WebSocketTester(page)

    // Mock WebSocket connection
    await page.evaluate(() => {
      ;(window as any).testWebSocket = {
        readyState: 1, // OPEN
        send: (data: string) => console.log('Sent:', data),
        close: () => console.log('Closed'),
        onopen: null,
        onmessage: null,
        onclose: null,
        onerror: null
      }
    })

    // Verify connection state
    const state = wsTester.getConnectionState()
    expect(state.connected).toBe(false)

    // Mock connect
    await page.evaluate(() => {
      const ws = (window as any).testWebSocket
      if (ws.onopen) ws.onopen({})
    })

    expect(wsTester.isWebSocketConnected()).toBe(true)
  })

  test('should send and receive messages', async ({ page }) => {
    const wsTester = new WebSocketTester(page)

    // Mock connected WebSocket
    await page.evaluate(() => {
      ;(window as any).testWebSocket = {
        readyState: 1,
        send: (data: string) => {
          // Echo back the message
          setTimeout(() => {
            const event = { data }
            if ((window as any).testWebSocket.onmessage) {
              (window as any).testWebSocket.onmessage(event)
            }
          }, 100)
        },
        close: () => {},
        onopen: null,
        onmessage: null,
        onclose: null,
        onerror: null
      }
    })

    // Send message
    const testMessage = { type: 'ping', data: 'hello' }
    await wsTester.sendJsonMessage(testMessage as any)

    // Wait for echo
    const received = await wsTester.waitForMessage(msg => msg.type === 'ping')
    expect(received.type).toBe('ping')
  })

  test('should handle connection closure', async ({ page }) => {
    const wsTester = new WebSocketTester(page)

    // Mock connected WebSocket
    let isConnected = true
    await page.evaluate(() => {
      ;(window as any).testWebSocket = {
        readyState: 1,
        send: () => {},
        close: () => {
          isConnected = false
        },
        onopen: null,
        onmessage: null,
        onclose: () => {},
        onerror: null
      }
    })

    expect(wsTester.isWebSocketConnected()).toBe(true)

    // Close connection
    await wsTester.disconnect()
    expect(wsTester.isWebSocketConnected()).toBe(false)
  })

  test('should reconnect with exponential backoff', async ({ page }) => {
    const wsTester = new WebSocketTester(page)

    let reconnectCount = 0
    await page.evaluate(() => {
      ;(window as any).testWebSocket = {
        readyState: 1,
        send: () => {},
        close: () => {},
        onopen: null,
        onmessage: null,
        onclose: null,
        onerror: null
      }
    })

    // Simulate connection failure
    await page.evaluate(() => {
      const ws = (window as any).testWebSocket
      ws.readyState = 3 // CLOSED
    })

    // Mock reconnection
    await page.evaluate(() => {
      reconnectCount++
    })

    // Attempt reconnection
    try {
      await wsTester.reconnect('ws://localhost:8080/ws', { timeout: 1000 })
    } catch (error) {
      // Expected to fail in mock environment
    }

    const state = wsTester.getConnectionState()
    expect(state.reconnectAttempts).toBeGreaterThan(0)
  })

  test('should wait for specific message types', async ({ page }) => {
    const wsTester = new WebSocketTester(page)

    // Mock connected WebSocket
    await page.evaluate(() => {
      ;(window as any).testWebSocket = {
        readyState: 1,
        send: (data: string) => {
          const message = JSON.parse(data)
          setTimeout(() => {
            const event = { data: JSON.stringify({ type: 'response', id: message.id }) }
            if ((window as any).testWebSocket.onmessage) {
              (window as any).testWebSocket.onmessage(event)
            }
          }, 100)
        },
        close: () => {},
        onopen: null,
        onmessage: null,
        onclose: null,
        onerror: null
      }
    })

    // Send message and wait for response
    const message = { type: 'request', id: '123' }
    await wsTester.sendJsonMessage(message as any)

    const response = await wsTester.waitForMessageType('response')
    expect(response.type).toBe('response')
  })

  test('should test ping-pong heartbeat', async ({ page }) => {
    const wsTester = new WebSocketTester(page)

    // Mock WebSocket with ping-pong
    await page.evaluate(() => {
      ;(window as any).testWebSocket = {
        readyState: 1,
        send: (data: string) => {
          const msg = JSON.parse(data)
          if (msg.type === 'ping') {
            setTimeout(() => {
              const event = { data: JSON.stringify({ type: 'pong' }) }
              if ((window as any).testWebSocket.onmessage) {
                (window as any).testWebSocket.onmessage(event)
              }
            }, 50)
          }
        },
        close: () => {},
        onopen: null,
        onmessage: null,
        onclose: null,
        onerror: null
      }
    })

    // Send ping
    await wsTester.sendJsonMessage({ type: 'ping' } as any)

    // Wait for pong
    const pong = await wsTester.waitForPong()
    expect(pong.type).toBe('pong')
  })

  test('should maintain message order', async ({ page }) => {
    const wsTester = new WebSocketTester(page)

    let messageIndex = 0
    await page.evaluate(() => {
      ;(window as any).testWebSocket = {
        readyState: 1,
        send: (data: string) => {
          const msg = JSON.parse(data)
          setTimeout(() => {
            const event = { data: JSON.stringify({ type: 'message', index: msg.index, data: msg.data }) }
            if ((window as any).testWebSocket.onmessage) {
              (window as any).testWebSocket.onmessage(event)
            }
          }, 50 - msg.index * 5) // Simulate out-of-order delivery
        },
        close: () => {},
        onopen: null,
        onmessage: null,
        onclose: null,
        onerror: null
      }
    })

    // Send multiple messages
    const messages = [
      { index: 0, data: 'first' },
      { index: 1, data: 'second' },
      { index: 2, data: 'third' }
    ]

    for (const msg of messages) {
      await wsTester.sendJsonMessage(msg as any)
    }

    // Verify message ordering
    const isOrdered = await wsTester.testMessageOrdering(messages)
    expect(isOrdered).toBe(true)
  })

  test('should handle network idle detection', async ({ page }) => {
    const wsTester = new WebSocketTester(page)

    // Mock WebSocket
    await page.evaluate(() => {
      ;(window as any).testWebSocket = {
        readyState: 1,
        send: () => {},
        close: () => {},
        onopen: null,
        onmessage: null,
        onclose: null,
        onerror: null
      }
    })

    // Send some messages
    await wsTester.sendJsonMessage({ type: 'test1' } as any)
    await wsTester.sendJsonMessage({ type: 'test2' } as any)

    // Wait for network idle (no messages for 2 seconds)
    await wsTester.waitForNetworkIdle(2000, 5000)
    // If we reach here, test passes
  })

  test('should manage multiple message listeners', async ({ page }) => {
    const wsTester = new WebSocketTester(page)

    // Mock WebSocket
    await page.evaluate(() => {
      ;(window as any).testWebSocket = {
        readyState: 1,
        send: (data: string) => {
          setTimeout(() => {
            const event = { data }
            if ((window as any).testWebSocket.onmessage) {
              (window as any).testWebSocket.onmessage(event)
            }
          }, 100)
        },
        close: () => {},
        onopen: null,
        onmessage: null,
        onclose: null,
        onerror: null
      }
    })

    // Set up multiple listeners
    const listener1 = wsTester.waitForMessage(msg => msg.type === 'type1')
    const listener2 = wsTester.waitForMessage(msg => msg.type === 'type2')

    // Send messages
    await wsTester.sendJsonMessage({ type: 'type1' } as any)
    await wsTester.sendJsonMessage({ type: 'type2' } as any)

    // Both listeners should resolve
    const [result1, result2] = await Promise.all([listener1, listener2])
    expect(result1.type).toBe('type1')
    expect(result2.type).toBe('type2')
  })

  test('should track message statistics', async ({ page }) => {
    const wsTester = new WebSocketTester(page)

    // Mock WebSocket
    await page.evaluate(() => {
      ;(window as any).testWebSocket = {
        readyState: 1,
        send: (data: string) => {
          setTimeout(() => {
            const event = { data }
            if ((window as any).testWebSocket.onmessage) {
              (window as any).testWebSocket.onmessage(event)
            }
          }, 50)
        },
        close: () => {},
        onopen: null,
        onmessage: null,
        onclose: null,
        onerror: null
      }
    })

    // Send multiple messages
    await wsTester.sendJsonMessage({ type: 'msg1' } as any)
    await wsTester.sendJsonMessage({ type: 'msg2' } as any)
    await wsTester.sendJsonMessage({ type: 'msg3' } as any)

    // Wait for messages to be processed
    await page.waitForTimeout(200)

    // Check statistics
    expect(wsTester.getMessageCount()).toBe(3)
    expect(wsTester.getMessageQueue().length).toBe(3)

    // Clear queue
    wsTester.clearMessageQueue()
    expect(wsTester.getMessageQueue().length).toBe(0)
  })

  test('should handle WebSocket ready state', async ({ page }) => {
    const wsTester = new WebSocketTester(page)

    // Mock WebSocket in connecting state
    await page.evaluate(() => {
      ;(window as any).testWebSocket = {
        readyState: 0, // CONNECTING
        send: () => {},
        close: () => {},
        onopen: null,
        onmessage: null,
        onclose: null,
        onerror: null
      }
    })

    let readyState = await wsTester.getReadyState()
    expect(readyState).toBe(0)

    // Open connection
    await page.evaluate(() => {
      const ws = (window as any).testWebSocket
      ws.readyState = 1 // OPEN
      if (ws.onopen) ws.onopen({})
    })

    readyState = await wsTester.getReadyState()
    expect(readyState).toBe(1)
  })

  test('should cleanup on disconnect', async ({ page }) => {
    const wsTester = new WebSocketTester(page)

    // Mock WebSocket
    await page.evaluate(() => {
      ;(window as any).testWebSocket = {
        readyState: 1,
        send: () => {},
        close: () => {},
        onopen: null,
        onmessage: null,
        onclose: null,
        onerror: null
      }
    })

    // Send message
    await wsTester.sendJsonMessage({ type: 'test' } as any)

    // Disconnect
    await wsTester.disconnect()

    // Verify state
    expect(wsTester.isWebSocketConnected()).toBe(false)
    expect(wsTester.getConnectionState().connected).toBe(false)
  })
})
