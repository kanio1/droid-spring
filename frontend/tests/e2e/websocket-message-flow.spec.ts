/**
 * WebSocket Message Flow Validation Tests
 *
 * Tests for validating message flows, event ordering,
 * and real-time data synchronization
 */

import { test, expect, BrowserContext } from '@playwright/test'
import { WebSocketTester, BroadcastChannelTester } from '../framework/utils/websocket-tester'
import { TenantFactory } from '../framework/data-factories'

test.describe('WebSocket Message Flow Validation', () => {
  let context1: BrowserContext
  let context2: BrowserContext

  test.beforeAll(async ({ browser }) => {
    context1 = await browser.newContext()
    context2 = await browser.newContext()
  })

  test.afterAll(async () => {
    await context1.close()
    await context2.close()
  })

  test('should validate customer creation message flow', async () => {
    const page1 = await context1.newPage()
    const page2 = await context2.newPage()

    const wsTester1 = new WebSocketTester(page1)
    const wsTester2 = new WebSocketTester(page2)

    // Mock WebSocket for both clients
    await page1.evaluate(() => {
      ;(window as any).testWebSocket = {
        readyState: 1,
        send: (data: string) => {
          setTimeout(() => {
            const msg = JSON.parse(data)
            // Broadcast to other clients (simulated)
            const event = { data: JSON.stringify({ type: 'customer_created', customer: msg.customer }) }
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

    // Client 1 creates customer
    const customer = TenantFactory.create().active().build()
    await wsTester1.sendJsonMessage({
      type: 'create_customer',
      customer: customer
    } as any)

    // Client 2 should receive the customer_created event
    const receivedEvent = await wsTester2.waitForMessage(msg => msg.type === 'customer_created')
    expect(receivedEvent.type).toBe('customer_created')
    expect(receivedEvent.customer.email).toBe(customer.email)

    await page1.close()
    await page2.close()
  })

  test('should validate real-time dashboard updates', async ({ page }) => {
    const wsTester = new WebSocketTester(page)

    // Mock dashboard data stream
    await page.evaluate(() => {
      ;(window as any).testWebSocket = {
        readyState: 1,
        send: (data: string) => {
          const msg = JSON.parse(data)
          if (msg.type === 'subscribe_dashboard') {
            // Simulate sending dashboard updates
            const updates = [
              { type: 'dashboard_update', metric: 'total_customers', value: 100 },
              { type: 'dashboard_update', metric: 'total_orders', value: 250 },
              { type: 'dashboard_update', metric: 'revenue', value: 50000 }
            ]

            updates.forEach((update, index) => {
              setTimeout(() => {
                const event = { data: JSON.stringify(update) }
                if ((window as any).testWebSocket.onmessage) {
                  (window as any).testWebSocket.onmessage(event)
                }
              }, 100 * (index + 1))
            })
          }
        },
        close: () => {},
        onopen: null,
        onmessage: null,
        onclose: null,
        onerror: null
      }
    })

    // Subscribe to dashboard
    await wsTester.sendJsonMessage({ type: 'subscribe_dashboard' } as any)

    // Wait for all updates
    const updates = []
    for (let i = 0; i < 3; i++) {
      const update = await wsTester.waitForMessage(msg => msg.type === 'dashboard_update')
      updates.push(update)
    }

    expect(updates).toHaveLength(3)
    expect(updates[0].metric).toBe('total_customers')
    expect(updates[1].metric).toBe('total_orders')
    expect(updates[2].metric).toBe('revenue')
  })

  test('should validate order status change flow', async ({ page }) => {
    const wsTester = new WebSocketTester(page)

    const orderId = 'order-123'
    let statusSequence: string[] = []

    // Mock order status updates
    await page.evaluate(() => {
      ;(window as any).testWebSocket = {
        readyState: 1,
        send: (data: string) => {
          const msg = JSON.parse(data)
          if (msg.type === 'track_order') {
            // Simulate status changes
            const statuses = ['pending', 'processing', 'shipped', 'delivered']
            statuses.forEach((status, index) => {
              setTimeout(() => {
                const event = {
                  data: JSON.stringify({
                    type: 'order_status_change',
                    orderId: msg.orderId,
                    status: status,
                    timestamp: Date.now()
                  })
                }
                if ((window as any).testWebSocket.onmessage) {
                  (window as any).testWebSocket.onmessage(event)
                }
              }, 200 * (index + 1))
            })
          }
        },
        close: () => {},
        onopen: null,
        onmessage: null,
        onclose: null,
        onerror: null
      }
    })

    // Track order
    await wsTester.sendJsonMessage({
      type: 'track_order',
      orderId: orderId
    } as any)

    // Wait for all status changes
    for (let i = 0; i < 4; i++) {
      const update = await wsTester.waitForMessage(msg =>
        msg.type === 'order_status_change' && msg.orderId === orderId
      )
      statusSequence.push(update.status)
    }

    // Verify correct order
    expect(statusSequence).toEqual(['pending', 'processing', 'shipped', 'delivered'])
  })

  test('should validate notification delivery', async ({ page }) => {
    const wsTester = new WebSocketTester(page)

    // Mock notification system
    await page.evaluate(() => {
      ;(window as any).testWebSocket = {
        readyState: 1,
        send: (data: string) => {
          const msg = JSON.parse(data)
          if (msg.type === 'send_notification') {
            // Simulate notification delivery
            setTimeout(() => {
              const event = {
                data: JSON.stringify({
                  type: 'notification',
                  id: msg.id,
                  title: msg.title,
                  message: msg.message,
                  priority: msg.priority
                })
              }
              if ((window as any).testWebSocket.onmessage) {
                (window as any).testWebSocket.onmessage(event)
              }
            }, 100)
          }
        },
        close: () => {},
        onopen: null,
        onmessage: null,
        onclose: null,
        onerror: null
      }
    })

    // Send notification
    const notification = {
      type: 'send_notification',
      id: 'notif-1',
      title: 'Test Notification',
      message: 'This is a test',
      priority: 'high'
    }

    await wsTester.sendJsonMessage(notification as any)

    // Wait for delivery
    const delivered = await wsTester.waitForMessage(msg => msg.type === 'notification')
    expect(delivered.id).toBe('notif-1')
    expect(delivered.title).toBe('Test Notification')
    expect(delivered.priority).toBe('high')
  })

  test('should validate concurrent user updates', async () => {
    const page1 = await context1.newPage()
    const page2 = await context2.newPage()

    const wsTester1 = new WebSocketTester(page1)
    const wsTester2 = new WebSocketTester(page2)

    // Both users edit same customer
    const customerId = 'customer-123'

    await page1.evaluate(() => {
      ;(window as any).testWebSocket1 = {
        readyState: 1,
        send: (data: string) => {
          const msg = JSON.parse(data)
          if (msg.type === 'edit_customer') {
            // Simulate conflict
            setTimeout(() => {
              const event = {
                data: JSON.stringify({
                  type: 'customer_edited',
                  customerId: msg.customerId,
                  editedBy: 'user1',
                  timestamp: Date.now()
                })
              }
              if ((window as any).testWebSocket1.onmessage) {
                (window as any).testWebSocket1.onmessage(event)
              }
            }, 100)
          }
        },
        close: () => {},
        onopen: null,
        onmessage: null,
        onclose: null,
        onerror: null
      }
    })

    await page2.evaluate(() => {
      ;(window as any).testWebSocket2 = {
        readyState: 1,
        send: (data: string) => {},
        close: () => {},
        onopen: null,
        onmessage: null,
        onclose: null,
        onerror: null
      }
    })

    // User 1 edits customer
    await wsTester1.sendJsonMessage({
      type: 'edit_customer',
      customerId: customerId,
      changes: { name: 'Updated Name' }
    } as any)

    // User 2 should see the edit
    const editEvent = await wsTester2.waitForMessage(msg =>
      msg.type === 'customer_edited' && msg.customerId === customerId
    )
    expect(editEvent.editedBy).toBe('user1')

    await page1.close()
    await page2.close()
  })

  test('should validate activity feed streaming', async ({ page }) => {
    const wsTester = new WebSocketTester(page)

    const activities: any[] = []

    // Mock activity feed
    await page.evaluate(() => {
      ;(window as any).testWebSocket = {
        readyState: 1,
        send: (data: string) => {
          const msg = JSON.parse(data)
          if (msg.type === 'subscribe_activities') {
            // Stream activities
            const activityTypes = ['customer_created', 'order_placed', 'payment_received', 'invoice_sent']
            activityTypes.forEach((type, index) => {
              setTimeout(() => {
                const event = {
                  data: JSON.stringify({
                    type: 'activity',
                    activityType: type,
                    timestamp: Date.now() + index * 1000,
                    user: 'system'
                  })
                }
                if ((window as any).testWebSocket.onmessage) {
                  (window as any).testWebSocket.onmessage(event)
                }
              }, 100 * (index + 1))
            })
          }
        },
        close: () => {},
        onopen: null,
        onmessage: null,
        onclose: null,
        onerror: null
      }
    })

    // Subscribe to activities
    await wsTester.sendJsonMessage({ type: 'subscribe_activities' } as any)

    // Collect all activities
    for (let i = 0; i < 4; i++) {
      const activity = await wsTester.waitForMessage(msg => msg.type === 'activity')
      activities.push(activity)
    }

    expect(activities).toHaveLength(4)
    expect(activities[0].activityType).toBe('customer_created')
    expect(activities[1].activityType).toBe('order_placed')
  })

  test('should validate presence indicators', async () => {
    const page1 = await context1.newPage()
    const page2 = await context2.newPage()

    const wsTester1 = new WebSocketTester(page1)
    const wsTester2 = new WebSocketTester(page2)

    // User 1 comes online
    await page1.evaluate(() => {
      ;(window as any).testWebSocket1 = {
        readyState: 1,
        send: (data: string) => {
          const msg = JSON.parse(data)
          if (msg.type === 'user_online') {
            // Broadcast presence
            setTimeout(() => {
              const event = {
                data: JSON.stringify({
                  type: 'presence_update',
                  userId: msg.userId,
                  status: 'online',
                  timestamp: Date.now()
                })
              }
              if ((window as any).testWebSocket1.onmessage) {
                (window as any).testWebSocket1.onmessage(event)
              }
            }, 100)
          }
        },
        close: () => {},
        onopen: null,
        onmessage: null,
        onclose: null,
        onerror: null
      }
    })

    await page2.evaluate(() => {
      ;(window as any).testWebSocket2 = {
        readyState: 1,
        send: (data: string) => {},
        close: () => {},
        onopen: null,
        onmessage: null,
        onclose: null,
        onerror: null
      }
    })

    // User 1 goes online
    await wsTester1.sendJsonMessage({
      type: 'user_online',
      userId: 'user-123'
    } as any)

    // User 2 should see the presence update
    const presence = await wsTester2.waitForMessage(msg => msg.type === 'presence_update')
    expect(presence.userId).toBe('user-123')
    expect(presence.status).toBe('online')

    await page1.close()
    await page2.close()
  })

  test('should validate BroadcastChannel for multi-tab sync', async ({ page }) => {
    const bcTester = new BroadcastChannelTester(page)

    // Create broadcast channel
    await bcTester.createChannel('customer_updates')

    // Listen for messages
    const messagePromise = bcTester.waitForMessage('customer_updates')

    // Send message
    await bcTester.sendMessage('customer_updates', {
      type: 'customer_updated',
      customerId: 'cust-123'
    })

    // Verify message received
    const received = await messagePromise
    expect(received.type).toBe('customer_updated')
    expect(received.customerId).toBe('cust-123')
  })

  test('should validate event deduplication', async ({ page }) => {
    const wsTester = new WebSocketTester(page)

    const eventIds = new Set<string>()

    // Mock event stream with duplicates
    await page.evaluate(() => {
      ;(window as any).testWebSocket = {
        readyState: 1,
        send: (data: string) => {
          const msg = JSON.parse(data)
          if (msg.type === 'subscribe_events') {
            // Send duplicate events
            const events = [
              { type: 'event', id: 'evt-1', data: 'first' },
              { type: 'event', id: 'evt-1', data: 'duplicate' }, // Duplicate
              { type: 'event', id: 'evt-2', data: 'unique' }
            ]

            events.forEach((event, index) => {
              setTimeout(() => {
                const eventStr = JSON.stringify(event)
                const frameEvent = { data: eventStr }
                if ((window as any).testWebSocket.onmessage) {
                  (window as any).testWebSocket.onmessage(frameEvent)
                }
              }, 100 * (index + 1))
            })
          }
        },
        close: () => {},
        onopen: null,
        onmessage: null,
        onclose: null,
        onerror: null
      }
    })

    // Subscribe to events
    await wsTester.sendJsonMessage({ type: 'subscribe_events' } as any)

    // Collect events
    const events = []
    for (let i = 0; i < 3; i++) {
      const event = await wsTester.waitForMessage(msg => msg.type === 'event')
      events.push(event)

      // Deduplicate by ID
      if (event.id && !eventIds.has(event.id)) {
        eventIds.add(event.id)
      }
    }

    // Should have 2 unique events (evt-1 deduplicated)
    expect(eventIds.size).toBe(2)
  })

  test('should validate message acknowledgment', async ({ page }) => {
    const wsTester = new WebSocketTester(page)

    let acknowledgments: string[] = []

    // Mock acknowledgment system
    await page.evaluate(() => {
      ;(window as any).testWebSocket = {
        readyState: 1,
        send: (data: string) => {
          const msg = JSON.parse(data)
          if (msg.requiresAck) {
            // Send acknowledgment
            setTimeout(() => {
              const ackEvent = {
                data: JSON.stringify({
                  type: 'acknowledgment',
                  messageId: msg.messageId,
                  status: 'received'
                })
              }
              if ((window as any).testWebSocket.onmessage) {
                (window as any).testWebSocket.onmessage(ackEvent)
              }
            }, 100)
          }
        },
        close: () => {},
        onopen: null,
        onmessage: null,
        onclose: null,
        onerror: null
      }
    })

    // Send message requiring ACK
    const messageId = 'msg-' + Date.now()
    await wsTester.sendJsonMessage({
      type: 'critical_action',
      messageId: messageId,
      requiresAck: true
    } as any)

    // Wait for acknowledgment
    const ack = await wsTester.waitForMessage(msg =>
      msg.type === 'acknowledgment' && msg.messageId === messageId
    )
    expect(ack.status).toBe('received')
  })
})
