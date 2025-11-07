# CloudEvents - Real-time Events in Frontend

## Overview

Frontend nasłuchuje CloudEvents publikowanych przez backend (Kafka) w real-time przy użyciu Server-Sent Events (SSE).

## Architecture

```
Backend (Kafka) → CloudEvents → SSE Endpoint → Frontend Composables → Pinia Stores
```

## Files Created

### Composables

1. **`app/composables/useEventSource.ts`**
   - Generic SSE client
   - Auto-reconnection
   - Event listeners management
   - Connection state tracking

2. **`app/composables/useCloudEvents.ts`**
   - Domain-specific event handlers
   - Customer, Payment, Invoice, Order events
   - Toast notifications integration
   - Type-safe event data

### Store Integration

3. **`app/stores/customer.events.ts`**
   - Customer event listeners
   - Auto-update store state on events

4. **`app/stores/payment.events.ts`**
   - Payment event listeners
   - Real-time payment status updates

### Plugin

5. **`app/plugins/events.client.ts`**
   - Initializes all event listeners
   - Runs only on client side

### Example Component

6. **`app/components/EventListenerDemo.vue`**
   - Demo of CloudEvents usage
   - Manual event listener example

## Backend Events

### Customer Events
- `com.droid.bss.customer.created.v1`
- `com.droid.bss.customer.updated.v1`
- `com.droid.bss.customer.statusChanged.v1`
- `com.droid.bss.customer.terminated.v1`

### Payment Events
- `com.droid.bss.payment.created.v1`
- `com.droid.bss.payment.processing.v1`
- `com.droid.bss.payment.completed.v1`
- `com.droid.bss.payment.failed.v1`
- `com.droid.bss.payment.refunded.v1`

### Invoice Events
- `com.droid.bss.invoice.created.v1`
- `com.droid.bss.invoice.paid.v1`
- `com.droid.bss.invoice.overdue.v1`

### Order Events
- `com.droid.bss.order.created.v1`
- `com.droid.bss.order.completed.v1`
- `com.droid.bss.order.failed.v1`

## Usage

### In Components

```typescript
const { isConnected, onPaymentCompleted } = useCloudEvents()

// Listen for payment completion
const cleanup = onPaymentCompleted((data) => {
  console.log('Payment completed!', data)
  // Update UI, refresh data, etc.
})

// Cleanup when done
onBeforeUnmount(() => cleanup())
```

### In Stores

```typescript
// In store file
export function setupPaymentEventListeners(paymentStore) {
  const { onPaymentCompleted } = useCloudEvents()

  onPaymentCompleted((data) => {
    // Update store state
    const index = paymentStore.payments.findIndex(p => p.id === data.paymentId)
    if (index !== -1) {
      paymentStore.payments[index].status = 'COMPLETED'
    }
  })
}
```

### In Plugin (Global Setup)

```typescript
// In app/plugins/events.client.ts
export default defineNuxtPlugin(() => {
  const paymentStore = usePaymentStore()
  setupPaymentEventListeners(paymentStore)
})
```

## Features

### ✅ Auto-reconnection
- Exponential backoff (max 30s delay)
- Up to 10 reconnection attempts
- Automatic reconnection on network restore

### ✅ Toast Notifications
- Payment completed: Success toast
- Payment failed: Error toast
- Invoice overdue: Warning toast
- Customer terminated: Info toast

### ✅ Type Safety
- TypeScript interfaces for all event data
- Type-safe callbacks
- Compile-time checking

### ✅ Performance
- Minimal re-renders
- Efficient event filtering
- Cleanup on component unmount

## Backend Requirements

Backend musi expose SSE endpoint (np. `/api/events/stream`):

```java
@GetMapping(value = "/events/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> streamEvents() {
    return cloudEventPublisher.getEventFlux()
        .map(this::toSSE)
        .onErrorContinue((error, object) -> {
            log.error("Error publishing event", error);
        });
}
```

## Configuration

```typescript
// In nuxt.config.ts
export default defineNuxtConfig({
  runtimeConfig: {
    public: {
      apiBase: process.env.API_BASE || 'http://localhost:8080/api'
    }
  }
})
```

## Testing

Event listeners można testować mock'ując CloudEvents:

```typescript
import { vi } from 'vitest'

// Mock useCloudEvents
vi.mock('~/composables/useCloudEvents', () => ({
  useCloudEvents: () => ({
    isConnected: ref(true),
    onPaymentCompleted: (callback: Function) => {
      // Simulate event after 100ms
      setTimeout(() => callback({
        paymentId: '123',
        status: 'COMPLETED',
        amount: 100
      }), 100)
      return vi.fn()
    }
  })
}))
```

## Troubleshooting

### Connection Issues
1. Check if SSE endpoint is accessible
2. Verify CORS configuration
3. Check authentication (SSE sends cookies)

### Events Not Received
1. Verify event types match (exact names)
2. Check if event is published in backend
3. Look for JavaScript errors in console

### Memory Leaks
1. Always cleanup listeners in `onBeforeUnmount`
2. Plugin cleanup is automatic
3. Use `vi.clearAllMocks()` in tests

## Performance Considerations

- Events are filtered by type in the composable
- Only active components receive events
- Automatic cleanup prevents memory leaks
- Minimal state updates (only when necessary)

## Security

- SSE connection includes credentials (cookies)
- Events are domain-specific (no sensitive data)
- Client validates event structure
- No arbitrary code execution from events

## Next Steps

1. Add event listeners for Invoice and Order stores
2. Implement event replay (for missed events)
3. Add event history UI
4. Add event filtering and search
5. Implement event analytics/metrics
