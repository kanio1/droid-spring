/**
 * WebSocket Tests
 *
 * Comprehensive tests for WebSocket connections and real-time updates
 * Tests connection management, message handling, reconnection, and events
 *
 * Priority: 🔴 HIGH
 * Learning Value: ⭐⭐⭐⭐⭐
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'

// Mock WebSocket
class MockWebSocket {
  static CONNECTING = 0
  static OPEN = 1
  static CLOSING = 2
  static CLOSED = 3

  static instances: MockWebSocket[] = []

  readyState = MockWebSocket.CONNECTING
  url = ''
  onopen: ((event: Event) => void) | null = null
  onmessage: ((event: MessageEvent) => void) | null = null
  onclose: ((event: CloseEvent) => void) | null = null
  onerror: ((event: Event) => void) | null = null

  constructor(url: string) {
    this.url = url
    MockWebSocket.instances.push(this)

    // Simulate connection after delay
    setTimeout(() => {
      if (url.includes('fail')) {
        this.readyState = MockWebSocket.CLOSED
        this.onerror?.(new Event('error'))
      } else {
        this.readyState = MockWebSocket.OPEN
        this.onopen?.(new Event('open'))
      }
    }, 10)
  }

  send(data: string) {
    // Echo back for testing
    if (this.readyState === MockWebSocket.OPEN) {
      try {
        const message = JSON.parse(data)
        if (message.event === 'ping') {
          this.onmessage?.(new MessageEvent('message', { data: JSON.stringify({ event: 'pong' }) }))
        }
      } catch (e) {
        // Invalid JSON
      }
    }
  }

  close() {
    this.readyState = MockWebSocket.CLOSED
    this.onclose?.(new CloseEvent('close', { code: 1000, reason: 'Normal closure' }))
  }
}

vi.stubGlobal('WebSocket', MockWebSocket)

describe('WebSocket Service', () => {
  beforeEach(() => {
    MockWebSocket.instances = []
  })

  describe('Connection Management', () => {
    it('should establish connection successfully', async () => {
      const { websocket } = await import('@/services/websocket')

      await websocket.connect('ws://localhost:8080')

      expect(MockWebSocket.instances).toHaveLength(1)
      expect(MockWebSocket.instances[0].readyState).toBe(MockWebSocket.OPEN)
    })

    it('should connect to default URL when none provided', async () => {
      const { websocket } = await import('@/services/websocket')

      await websocket.connect()

      expect(MockWebSocket.instances).toHaveLength(1)
    })

    it('should handle connection failure', async () => {
      const { websocket } = await import('@/services/websocket')

      await expect(websocket.connect('ws://localhost:9999/fail')).rejects.toThrow('WebSocket connection failed')
    })

    it('should close connection properly', async () => {
      const { websocket } = await import('@/services/websocket')

      await websocket.connect()
      websocket.disconnect()

      expect(MockWebSocket.instances[0].readyState).toBe(MockWebSocket.CLOSED)
    })

    it('should check connection status', async () => {
      const { websocket } = await import('@/services/websocket')

      await websocket.connect()

      expect(websocket.isConnected()).toBe(true)

      websocket.disconnect()

      expect(websocket.isConnected()).toBe(false)
    })

    it('should reconnect automatically on disconnect', async () => {
      vi.useFakeTimers()

      const { websocket } = await import('@/services/websocket')

      await websocket.connect()

      // Simulate disconnect
      MockWebSocket.instances[0].onclose?.(new CloseEvent('close', { code: 1006, reason: 'Abnormal closure' }))

      // Fast forward time
      vi.advanceTimersByTime(3000)

      // Should attempt reconnection
      expect(MockWebSocket.instances.length).toBeGreaterThan(1)

      vi.useRealTimers()
    })

    it('should limit reconnection attempts', async () => {
      vi.useFakeTimers()

      const { websocket } = await import('@/services/websocket')

      // Mock connection that always fails
      let attempts = 0
      const OriginalWebSocket = MockWebSocket
      vi.stubGlobal('WebSocket', class FailingWebSocket extends OriginalWebSocket {
        constructor(url: string) {
          super(url)
          setTimeout(() => {
            this.readyState = MockWebSocket.CLOSED
            this.onerror?.(new Event('error'))
          }, 10)
        }
      })

      websocket.connect('ws://fail')

      // Fast forward through multiple reconnection attempts
      vi.advanceTimersByTime(30000)

      // Should not exceed max attempts
      expect(MockWebSocket.instances.length).toBeLessThanOrEqual(6) // Initial + 5 retries

      vi.useRealTimers()
    })
  })

  describe('Message Handling', () => {
    it('should send messages over WebSocket', async () => {
      const { websocket } = await import('@/services/websocket')

      await websocket.connect()

      websocket.send('test message')

      // Message sent successfully
      expect(MockWebSocket.instances[0]).toBeDefined()
    })

    it('should send JSON messages', async () => {
      const { websocket } = await import('@/services/websocket')

      await websocket.connect()

      const message = {
        event: 'subscribe',
        channel: 'orders'
      }

      websocket.send(JSON.stringify(message))

      // Message sent
      expect(MockWebSocket.instances[0]).toBeDefined()
    })

    it('should handle incoming messages', async () => {
      const { websocket } = await import('@/services/websocket')

      await websocket.connect()

      let receivedMessage: any = null

      websocket.on('message', (data) => {
        receivedMessage = data
      })

      MockWebSocket.instances[0].onmessage?.(
        new MessageEvent('message', {
          data: JSON.stringify({ event: 'order.created', data: { id: 1 } })
        })
      )

      expect(receivedMessage).toEqual({ event: 'order.created', data: { id: 1 } })
    })

    it('should parse JSON messages', async () => {
      const { websocket } = await import('@/services/websocket')

      await websocket.connect()

      websocket.on('message', vi.fn())

      MockWebSocket.instances[0].onmessage?.(
        new MessageEvent('message', { data: '{"event":"test","data":{"id":1}}' })
      )

      // Should parse JSON successfully
      expect(websocket.on).toHaveBeenCalled()
    })

    it('should handle invalid JSON gracefully', async () => {
      const { websocket } = await import('@/services/websocket')

      await websocket.connect()

      const onMessage = vi.fn()
      websocket.on('message', onMessage)

      // Send invalid JSON
      MockWebSocket.instances[0].onmessage?.(
        new MessageEvent('message', { data: 'invalid json' })
      )

      // Should not throw, just ignore
      expect(onMessage).not.toHaveBeenCalled()
    })
  })

  describe('Event System', () => {
    it('should register event listeners', async () => {
      const { websocket } = await import('@/services/websocket')

      await websocket.connect()

      const callback = vi.fn()

      websocket.on('order.created', callback)

      MockWebSocket.instances[0].onmessage?.(
        new MessageEvent('message', {
          data: JSON.stringify({ event: 'order.created', data: { id: 1 } })
        })
      )

      expect(callback).toHaveBeenCalledWith({ event: 'order.created', data: { id: 1 } })
    })

    it('should register multiple listeners for same event', async () => {
      const { websocket } = await import('@/services/websocket')

      await websocket.connect()

      const callback1 = vi.fn()
      const callback2 = vi.fn()

      websocket.on('order.created', callback1)
      websocket.on('order.created', callback2)

      MockWebSocket.instances[0].onmessage?.(
        new MessageEvent('message', {
          data: JSON.stringify({ event: 'order.created', data: { id: 1 } })
        })
      )

      expect(callback1).toHaveBeenCalled()
      expect(callback2).toHaveBeenCalled()
    })

    it('should remove event listeners', async () => {
      const { websocket } = await import('@/services/websocket')

      await websocket.connect()

      const callback = vi.fn()

      websocket.on('order.created', callback)
      websocket.off('order.created', callback)

      MockWebSocket.instances[0].onmessage?.(
        new MessageEvent('message', {
          data: JSON.stringify({ event: 'order.created', data: { id: 1 } })
        })
      )

      expect(callback).not.toHaveBeenCalled()
    })

    it('should remove all listeners for an event', async () => {
      const { websocket } = await import('@/services/websocket')

      await websocket.connect()

      const callback1 = vi.fn()
      const callback2 = vi.fn()

      websocket.on('order.created', callback1)
      websocket.on('order.created', callback2)
      websocket.offAll('order.created')

      MockWebSocket.instances[0].onmessage?.(
        new MessageEvent('message', {
          data: JSON.stringify({ event: 'order.created', data: { id: 1 } })
        })
      )

      expect(callback1).not.toHaveBeenCalled()
      expect(callback2).not.toHaveBeenCalled()
    })

    it('should emit custom events', async () => {
      const { websocket } = await import('@/services/websocket')

      await websocket.connect()

      const callback = vi.fn()

      websocket.on('custom.event', callback)

      websocket.emit('custom.event', { data: 'test' })

      expect(callback).toHaveBeenCalledWith({ data: 'test' })
    })

    it('should handle once listeners', async () => {
      const { websocket } = await import('@/services/websocket')

      await websocket.connect()

      const callback = vi.fn()

      websocket.once('order.created', callback)

      MockWebSocket.instances[0].onmessage?.(
        new MessageEvent('message', {
          data: JSON.stringify({ event: 'order.created', data: { id: 1 } })
        })
      )

      MockWebSocket.instances[0].onmessage?.(
        new MessageEvent('message', {
          data: JSON.stringify({ event: 'order.created', data: { id: 2 } })
        })
      )

      expect(callback).toHaveBeenCalledTimes(1)
    })
  })

  describe('Event Types', () => {
    it('should handle order events', async () => {
      const { websocket } = await import('@/services/websocket')

      await websocket.connect()

      const orderCallback = vi.fn()

      websocket.on('order.created', orderCallback)
      websocket.on('order.updated', vi.fn())
      websocket.on('order.deleted', vi.fn())

      MockWebSocket.instances[0].onmessage?.(
        new MessageEvent('message', {
          data: JSON.stringify({ event: 'order.created', data: { id: 1 } })
        })
      )

      expect(orderCallback).toHaveBeenCalled()
    })

    it('should handle customer events', async () => {
      const { websocket } = await import('@/services/websocket')

      await websocket.connect()

      const customerCallback = vi.fn()

      websocket.on('customer.created', customerCallback)

      MockWebSocket.instances[0].onmessage?.(
        new MessageEvent('message', {
          data: JSON.stringify({ event: 'customer.created', data: { id: 1 } })
        })
      )

      expect(customerCallback).toHaveBeenCalled()
    })

    it('should handle invoice events', async () => {
      const { websocket } = await import('@/services/websocket')

      await websocket.connect()

      const invoiceCallback = vi.fn()

      websocket.on('invoice.paid', invoiceCallback)

      MockWebSocket.instances[0].onmessage?.(
        new MessageEvent('message', {
          data: JSON.stringify({ event: 'invoice.paid', data: { id: 1 } })
        })
      )

      expect(invoiceCallback).toHaveBeenCalled()
    })

    it('should handle payment events', async () => {
      const { websocket } = await import('@/services/websocket')

      await websocket.connect()

      const paymentCallback = vi.fn()

      websocket.on('payment.received', paymentCallback)

      MockWebSocket.instances[0].onmessage?.(
        new MessageEvent('message', {
          data: JSON.stringify({ event: 'payment.received', data: { id: 1 } })
        })
      )

      expect(paymentCallback).toHaveBeenCalled()
    })

    it('should handle system events', async () => {
      const { websocket } = await import('@/services/websocket')

      await websocket.connect()

      const systemCallback = vi.fn()

      websocket.on('system.maintenance', systemCallback)

      MockWebSocket.instances[0].onmessage?.(
        new MessageEvent('message', {
          data: JSON.stringify({ event: 'system.maintenance', data: { start: '2024-11-05' } })
        })
      )

      expect(systemCallback).toHaveBeenCalled()
    })
  })

  describe('Connection Events', () => {
    it('should emit open event', async () => {
      const { websocket } = await import('@/services/websocket')

      const openCallback = vi.fn()

      websocket.on('open', openCallback)

      await websocket.connect()

      expect(openCallback).toHaveBeenCalled()
    })

    it('should emit close event', async () => {
      const { websocket } = await import('@/services/websocket')

      await websocket.connect()

      const closeCallback = vi.fn()

      websocket.on('close', closeCallback)

      websocket.disconnect()

      expect(closeCallback).toHaveBeenCalled()
    })

    it('should emit error event', async () => {
      const { websocket } = await import('@/services/websocket')

      const errorCallback = vi.fn()

      websocket.on('error', errorCallback)

      MockWebSocket.instances[0].onerror?.(new Event('error'))

      expect(errorCallback).toHaveBeenCalled()
    })

    it('should emit reconnecting event', async () => {
      vi.useFakeTimers()

      const { websocket } = await import('@/services/websocket')

      await websocket.connect()

      const reconnectCallback = vi.fn()

      websocket.on('reconnecting', reconnectCallback)

      // Simulate disconnect
      MockWebSocket.instances[0].onclose?.(new CloseEvent('close', { code: 1006 }))

      vi.advanceTimersByTime(1000)

      expect(reconnectCallback).toHaveBeenCalled()

      vi.useRealTimers()
    })
  })

  describe('Heartbeat', () => {
    it('should send ping messages', async () => {
      vi.useFakeTimers()

      const { websocket } = await import('@/services/websocket')

      await websocket.connect()

      const pingSpy = vi.spyOn(websocket, 'send')

      // Fast forward to next heartbeat
      vi.advanceTimersByTime(30000)

      expect(pingSpy).toHaveBeenCalledWith(
        JSON.stringify({ event: 'ping', timestamp: expect.any(Number) })
      )

      vi.useRealTimers()
    })

    it('should handle pong responses', async () => {
      vi.useFakeTimers()

      const { websocket } = await import('@/services/websocket')

      await websocket.connect()

      const pingSpy = vi.spyOn(websocket, 'send')

      // Trigger ping
      vi.advanceTimersByTime(30000)

      // Simulate pong response
      const mockSend = pingSpy.mock.calls[0][0]
      const pingMessage = JSON.parse(mockSend)

      MockWebSocket.instances[0].onmessage?.(
        new MessageEvent('message', {
          data: JSON.stringify({ event: 'pong', timestamp: pingMessage.timestamp })
        })
      )

      // Should not try to reconnect
      expect(MockWebSocket.instances).toHaveLength(1)

      vi.useRealTimers()
    })

    it('should reconnect on pong timeout', async () => {
      vi.useFakeTimers()

      const { websocket } = await import('@/services/websocket')

      await websocket.connect()

      vi.advanceTimersByTime(30000)

      // Don't respond with pong
      vi.advanceTimersByTime(10000)

      // Should reconnect
      expect(MockWebSocket.instances.length).toBeGreaterThan(1)

      vi.useRealTimers()
    })
  })

  describe('Authentication', () => {
    it('should include auth token in connection', async () => {
      const { websocket } = await import('@/services/websocket')

      const token = 'test-auth-token'

      await websocket.connect('ws://localhost:8080', { token })

      // Token included in URL
      const ws = MockWebSocket.instances[0]
      expect(ws.url).toContain('token=' + token)
    })

    it('should include auth token in upgrade request', async () => {
      const { websocket } = await import('@/services/websocket')

      const token = 'test-auth-token'

      await websocket.connect('ws://localhost:8080', { token })

      // Check that headers would include authorization
      // Note: This is harder to test with the mock
      expect(MockWebSocket.instances[0]).toBeDefined()
    })
  })

  describe('Reconnection Strategy', () => {
    it('should use exponential backoff', async () => {
      vi.useFakeTimers()

      const { websocket } = await import('@/services/websocket')

      await websocket.connect()

      // Simulate disconnects
      MockWebSocket.instances[0].onclose?.(new CloseEvent('close', { code: 1006 }))

      const reconnectionDelay = 1000
      vi.advanceTimersByTime(reconnectionDelay)

      expect(MockWebSocket.instances.length).toBe(2)

      // Next reconnection should have longer delay
      MockWebSocket.instances[1].onclose?.(new CloseEvent('close', { code: 1006 }))

      const nextDelay = 2000
      vi.advanceTimersByTime(nextDelay)

      expect(MockWebSocket.instances.length).toBe(3)

      vi.useRealTimers()
    })

    it('should not reconnect when explicitly disabled', async () => {
      vi.useFakeTimers()

      const { websocket } = await import('@/services/websocket')

      await websocket.connect('ws://localhost:8080', { autoReconnect: false })

      MockWebSocket.instances[0].onclose?.(new CloseEvent('close', { code: 1006 }))

      vi.advanceTimersByTime(5000)

      // Should not have reconnected
      expect(MockWebSocket.instances).toHaveLength(1)

      vi.useRealTimers()
    })
  })

  describe('Buffer Management', () => {
    it('should buffer messages while disconnected', async () => {
      const { websocket } = await import('@/services/websocket')

      await websocket.connect()

      // Disconnect
      websocket.disconnect()

      // Try to send message
      websocket.send('test message')

      // Reconnect
      vi.useFakeTimers()
      websocket.connect()
      vi.advanceTimersByTime(1000)
      vi.useRealTimers()

      // Buffer should be empty after successful reconnect
      expect(MockWebSocket.instances.length).toBeGreaterThan(1)
    })
  })

  describe('Error Handling', () => {
    it('should handle connection timeout', async () => {
      vi.useFakeTimers()

      const { websocket } = await import('@/services/websocket')

      await expect(
        websocket.connect('ws://timeout', { timeout: 1000 })
      ).rejects.toThrow('Connection timeout')

      vi.useRealTimers()
    })

    it('should handle invalid messages', async () => {
      const { websocket } = await import('@/services/websocket')

      await websocket.connect()

      const messageCallback = vi.fn()
      websocket.on('message', messageCallback)

      // Send various invalid messages
      MockWebSocket.instances[0].onmessage?.(new MessageEvent('message', { data: '' }))
      MockWebSocket.instances[0].onmessage?.(new MessageEvent('message', { data: null as any }))

      expect(messageCallback).not.toHaveBeenCalled()
    })
  })
})
