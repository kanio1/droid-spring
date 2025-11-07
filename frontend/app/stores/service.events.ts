/**
 * CloudEvents integration for Service Store
 * Automatically updates store state when service events arrive from backend
 */

import type { Service } from '~/schemas/service'

export function setupServiceEventListeners(serviceStore: ReturnType<typeof useServiceStore>) {
  const { addEventListener } = useEventSource({
    url: `${useRuntimeConfig().public.apiBase}/events/stream`,
    eventTypes: [
      'com.droid.bss.service.*'
    ],
    withCredentials: true,
    autoReconnect: true
  })

  // Service activated - update status
  addEventListener('com.droid.bss.service.activated.v1', (event) => {
    const data = JSON.parse(event.data)
    console.log('Service activated via event:', data.data.serviceId)
    serviceStore.fetchServices()
  })

  // Service deactivated - update status
  addEventListener('com.droid.bss.service.deactivated.v1', (event) => {
    const data = JSON.parse(event.data)
    console.log('Service deactivated via event:', data.data.serviceId)
    serviceStore.fetchServices()
  })

  // Service provisioned - update status
  addEventListener('com.droid.bss.service.provisioned.v1', (event) => {
    const data = JSON.parse(event.data)
    console.log('Service provisioned via event:', data.data.serviceId)
    serviceStore.fetchServices()
  })

  // Service failed - update status
  addEventListener('com.droid.bss.service.failed.v1', (event) => {
    const data = JSON.parse(event.data)
    console.log('Service failed via event:', data.data.serviceId)
    serviceStore.fetchServices()
  })
}
