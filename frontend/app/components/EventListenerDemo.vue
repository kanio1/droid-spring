<template>
  <div class="event-listener-demo">
    <h3>Real-time Events Status</h3>

    <div class="connection-status" :class="{ connected: isConnected }">
      <i :class="isConnected ? 'pi pi-wifi' : 'pi pi-wifi-off'" />
      <span>{{ isConnected ? 'Connected' : 'Disconnected' }}</span>
    </div>

    <div class="events-feed">
      <h4>Recent Events</h4>
      <div v-for="event in recentEvents" :key="event.id" class="event-item">
        <span class="event-type">{{ event.type }}</span>
        <span class="event-time">{{ formatTime(event.time) }}</span>
        <span class="event-data">{{ event.data }}</span>
      </div>
    </div>

    <div class="manual-listeners">
      <h4>Manual Event Listeners</h4>

      <Button
        label="Listen for Payment Completed"
        icon="pi pi-bell"
        @click="listenForPaymentCompleted"
        :disabled="isListening"
      />

      <Button
        label="Stop Listening"
        icon="pi pi-times"
        severity="secondary"
        @click="stopListening"
        :disabled="!isListening"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
const { isConnected, onAnyEvent, onPaymentCompleted } = useCloudEvents()
const recentEvents = ref<any[]>([])
const isListening = ref(false)
let cleanup: (() => void) | null = null

// Listen for all events
const unsubscribeAll = onAnyEvent((event) => {
  recentEvents.value.unshift({
    id: event.id,
    type: event.type,
    time: event.time,
    data: JSON.stringify(event.data).substring(0, 100)
  })

  // Keep only last 10 events
  if (recentEvents.value.length > 10) {
    recentEvents.value = recentEvents.value.slice(0, 10)
  }
})

const listenForPaymentCompleted = () => {
  if (cleanup) {
    cleanup()
  }

  cleanup = onPaymentCompleted((data) => {
    console.log('Payment completed!', data)
    // You can trigger UI updates here
    // e.g., refresh payments list, show success message, etc.
  })

  isListening.value = true
}

const stopListening = () => {
  if (cleanup) {
    cleanup()
    cleanup = null
  }
  isListening.value = false
}

const formatTime = (time: string) => {
  return new Date(time).toLocaleTimeString()
}

onBeforeUnmount(() => {
  unsubscribeAll()
  if (cleanup) {
    cleanup()
  }
})
</script>

<style scoped>
.event-listener-demo {
  padding: 1rem;
}

.connection-status {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem;
  border-radius: 4px;
  margin-bottom: 1rem;
}

.connection-status.connected {
  background-color: #d4edda;
  color: #155724;
}

.connection-status:not(.connected) {
  background-color: #f8d7da;
  color: #721c24;
}

.events-feed {
  margin-top: 1rem;
}

.event-item {
  display: flex;
  gap: 1rem;
  padding: 0.5rem;
  border-bottom: 1px solid #eee;
  font-size: 0.9rem;
}

.event-type {
  font-weight: bold;
  min-width: 200px;
}

.event-time {
  color: #666;
  min-width: 100px;
}

.event-data {
  color: #888;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.manual-listeners {
  margin-top: 2rem;
  display: flex;
  gap: 1rem;
}
</style>
