/**
 * OpenTelemetry Plugin for Nuxt.js
 * Provides distributed tracing, metrics, and error tracking
 */

import { defineNuxtPlugin, useRuntimeConfig } from '#app'
import { trace, metrics, context } from '@opentelemetry/api'
import { WebTracerProvider } from '@opentelemetry/sdk-trace-web'
import { SimpleSpanProcessor } from '@opentelemetry/sdk-trace-base'
import { OTLPTraceExporter } from '@opentelemetry/exporter-otlp-http'
import { Resource } from '@opentelemetry/resources'
import { SemanticResourceAttributes } from '@opentelemetry/semantic-conventions'
import { registerErrorInstrumentation } from '@opentelemetry/instrumentation'
import { FetchInstrumentation } from '@opentelemetry/instrumentation-fetch'
import { XMLHttpRequestInstrumentation } from '@opentelemetry/instrumentation-xml-http-request'
import { UserInteractionInstrumentation } from '@opentelemetry/instrumentation-user-interaction'

export default defineNuxtPlugin(() => {
  const config = useRuntimeConfig()

  // Initialize tracer provider
  const provider = new WebTracerProvider({
    resource: new Resource({
      [SemanticResourceAttributes.SERVICE_NAME]: 'bss-frontend',
      [SemanticResourceAttributes.SERVICE_VERSION]: '1.0.0',
      [SemanticResourceAttributes.DEPLOYMENT_ENVIRONMENT]: process.env.NODE_ENV || 'development'
    })
  })

  // Configure trace exporter
  const exporter = new OTLPTraceExporter({
    url: '/v1/traces', // Tempo endpoint - proxied through backend
    headers: {
      'Content-Type': 'application/json'
    }
  })

  // Add span processor
  provider.addSpanProcessor(new SimpleSpanProcessor(exporter))

  // Register provider
  provider.register()

  // Create tracer
  const tracer = provider.getTracer('bss-frontend-tracer')

  // Initialize metrics
  const meter = metrics.getMeter('bss-frontend-meter')

  // Custom metrics
  const pageLoadCounter = meter.createCounter('bss.frontend.page_loads', {
    description: 'Number of page loads'
  })

  const apiCallCounter = meter.createCounter('bss.frontend.api_calls', {
    description: 'Number of API calls'
  })

  const apiErrorCounter = meter.createCounter('bss.frontend.api_errors', {
    description: 'Number of API errors'
  })

  const userInteractionCounter = meter.createCounter('bss.frontend.user_interactions', {
    description: 'Number of user interactions'
  })

  const pageLoadTimer = meter.createHistogram('bss.frontend.page_load_duration', {
    description: 'Time taken to load pages',
    unit: 'ms'
  })

  // HTTP fetch instrumentation
  const fetchInstrumentation = new FetchInstrumentation({
    clearTimingResources: true,
    propagateTraceHeaderCorsUrls: [/.*/],
    spanName: 'HTTP GET'
  })

  // XMLHttpRequest instrumentation
  const xhrInstrumentation = new XMLHttpRequestInstrumentation({
    propagateTraceHeaderCorsUrls: [/.*/],
    spanName: 'XHR Request'
  })

  // User interaction instrumentation
  const userInteractionInstrumentation = new UserInteractionInstrumentation()

  // Register instrumentations
  registerErrorInstrumentation([fetchInstrumentation, xhrInstrumentation, userInteractionInstrumentation])

  // Track page load performance
  if (process.client) {
    window.addEventListener('load', () => {
      const perfData = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming
      if (perfData) {
        pageLoadCounter.add(1, {
          'page': window.location.pathname
        })
        pageLoadTimer.record(perfData.loadEventEnd - perfData.fetchStart, {
          'page': window.location.pathname
        })
      }
    })
  }

  // API call wrapper for useApi composable
  const trackApiCall = (url: string, method: string, success: boolean) => {
    apiCallCounter.add(1, {
      'endpoint': url,
      'method': method
    })

    if (!success) {
      apiErrorCounter.add(1, {
        'endpoint': url,
        'method': method
      })
    }
  }

  // Track user interactions
  const trackUserInteraction = (element: string, action: string) => {
    userInteractionCounter.add(1, {
      'element': element,
      'action': action
    })
  }

  // Expose instrumentation to the app
  return {
    provide: {
      otel: {
        tracer,
        meter,
        trackApiCall,
        trackUserInteraction
      }
    }
  }
})
