/**
 * useEventSource - Server-Sent Events (SSE) composable
 * For real-time updates from backend CloudEvents via Kafka
 */

export interface CloudEvent {
  id: string
  source: string
  type: string
  specversion: string
  datacontenttype: string
  time: string
  data: any
}

export interface EventSourceConfig {
  url: string
  eventTypes?: string[]
  withCredentials?: boolean
  autoReconnect?: boolean
  maxReconnectAttempts?: number
}

export function useEventSource(config: EventSourceConfig) {
  const eventSource = ref<EventSource | null>(null)
  const isConnected = ref(false)
  const isConnecting = ref(false)
  const error = ref<Error | null>(null)
  const reconnectAttempts = ref(0)
  const maxAttempts = config.maxReconnectAttempts ?? 5

  const listeners = new Map<string, Set<(event: MessageEvent) => void>>()

  const connect = () => {
    if (eventSource.value && eventSource.value.readyState === EventSource.OPEN) {
      return
    }

    isConnecting.value = true
    error.value = null

    try {
      eventSource.value = new EventSource(config.url, {
        withCredentials: config.withCredentials ?? true
      })

      eventSource.value.onopen = () => {
        isConnected.value = true
        isConnecting.value = false
        reconnectAttempts.value = 0
        console.log('[SSE] Connected to', config.url)
      }

      eventSource.value.onerror = (err) => {
        isConnected.value = false
        isConnecting.value = false
        error.value = err as any

        console.error('[SSE] Connection error:', err)

        if (config.autoReconnect && reconnectAttempts.value < maxAttempts) {
          reconnectAttempts.value++
          const delay = Math.min(1000 * Math.pow(2, reconnectAttempts.value), 30000)
          console.log(`[SSE] Reconnecting in ${delay}ms (attempt ${reconnectAttempts.value})`)
          setTimeout(connect, delay)
        }
      }

      // Generic message handler
      eventSource.value.onmessage = (event) => {
        try {
          const cloudEvent: CloudEvent = JSON.parse(event.data)
          handleEvent(cloudEvent)
        } catch (err) {
          console.error('[SSE] Failed to parse event:', err)
        }
      }

      // Listen for specific event types
      if (config.eventTypes && config.eventTypes.length > 0) {
        config.eventTypes.forEach(eventType => {
          eventSource.value!.addEventListener(eventType, (event) => {
            handleEvent(JSON.parse((event as MessageEvent).data))
          })
        })
      }

    } catch (err) {
      isConnecting.value = false
      error.value = err as Error
      console.error('[SSE] Failed to create connection:', err)
    }
  }

  const disconnect = () => {
    if (eventSource.value) {
      eventSource.value.close()
      eventSource.value = null
      isConnected.value = false
      isConnecting.value = false
      console.log('[SSE] Disconnected')
    }
  }

  const handleEvent = (cloudEvent: CloudEvent) => {
    console.log('[SSE] Received event:', cloudEvent.type, cloudEvent)

    // Emit to registered listeners
    const typeListeners = listeners.get(cloudEvent.type)
    if (typeListeners) {
      typeListeners.forEach(callback => {
        try {
          callback({
            data: JSON.stringify(cloudEvent),
            type: cloudEvent.type,
            id: cloudEvent.id,
            lastEventId: cloudEvent.id
          } as MessageEvent)
        } catch (err) {
          console.error('[SSE] Listener error:', err)
        }
      })
    }

    // Also emit to wildcard listeners
    const wildcardListeners = listeners.get('*')
    if (wildcardListeners) {
      wildcardListeners.forEach(callback => {
        try {
          callback({
            data: JSON.stringify(cloudEvent),
            type: cloudEvent.type,
            id: cloudEvent.id,
            lastEventId: cloudEvent.id
          } as MessageEvent)
        } catch (err) {
          console.error('[SSE] Wildcard listener error:', err)
        }
      })
    }
  }

  const addEventListener = (eventType: string, callback: (event: MessageEvent) => void) => {
    if (!listeners.has(eventType)) {
      listeners.set(eventType, new Set())
    }
    listeners.get(eventType)!.add(callback)

    return () => {
      const typeListeners = listeners.get(eventType)
      if (typeListeners) {
        typeListeners.delete(callback)
        if (typeListeners.size === 0) {
          listeners.delete(eventType)
        }
      }
    }
  }

  const removeEventListener = (eventType: string, callback: (event: MessageEvent) => void) => {
    const typeListeners = listeners.get(eventType)
    if (typeListeners) {
      typeListeners.delete(callback)
      if (typeListeners.size === 0) {
        listeners.delete(eventType)
      }
    }
  }

  // Auto-connect on mount
  onMounted(() => {
    connect()
  })

  // Cleanup on unmount
  onBeforeUnmount(() => {
    disconnect()
  })

  return {
    eventSource: readonly(eventSource),
    isConnected: readonly(isConnected),
    isConnecting: readonly(isConnecting),
    error: readonly(error),
    reconnectAttempts: readonly(reconnectAttempts),
    connect,
    disconnect,
    addEventListener,
    removeEventListener
  }
}
