/**
 * Composable for lazy loading heavy components
 * Improves initial page load performance by loading components on demand
 */

export const useLazyLoad = () => {
  /**
   * Lazy load a component with loading state
   */
  const lazyComponent = async <T>(
    loader: () => Promise<{ default: T }>,
    delay = 0
  ) => {
    // Optional delay to prevent excessive loading
    if (delay > 0) {
      await new Promise(resolve => setTimeout(resolve, delay))
    }

    return loader()
  }

  /**
   * Lazy load chart components
   */
  const lazyChart = async (chartType: 'line' | 'bar' | 'pie' | 'area') => {
    const components = {
      line: () => import('~/components/charts/LineChart.vue'),
      bar: () => import('~/components/charts/BarChart.vue'),
      pie: () => import('~/components/charts/PieChart.vue'),
      area: () => import('~/components/charts/AreaChart.vue')
    }

    return lazyComponent(components[chartType])
  }

  /**
   * Lazy load data table with heavy features
   */
  const lazyDataTable = async () => {
    return lazyComponent(() => import('~/components/common/DataTable.vue'))
  }

  /**
   * Lazy load form components
   */
  const lazyForm = async (formType: 'customer' | 'product' | 'order' | 'payment') => {
    const components = {
      customer: () => import('~/components/customer/CustomerForm.vue'),
      product: () => import('~/components/product/ProductForm.vue'),
      order: () => import('~/components/order/OrderForm.vue'),
      payment: () => import('~/components/payment/PaymentForm.vue')
    }

    return lazyComponent(components[formType])
  }

  /**
   * Lazy load monitoring dashboard
   */
  const lazyMonitoringDashboard = async () => {
    return lazyComponent(() => import('~/components/monitoring/MonitoringDashboard.vue'))
  }

  /**
   * Lazy load billing components
   */
  const lazyBillingDashboard = async () => {
    return lazyComponent(() => import('~/components/billing/BillingDashboard.vue'))
  }

  /**
   * Preload critical components after idle
   */
  const preloadOnIdle = (loader: () => void) => {
    if (typeof window !== 'undefined' && 'requestIdleCallback' in window) {
      // @ts-ignore
      window.requestIdleCallback(loader, { timeout: 2000 })
    } else {
      // Fallback for older browsers
      setTimeout(loader, 2000)
    }
  }

  /**
   * Preload frequently used components
   */
  const preloadComponents = () => {
    if (process.client) {
      // Preload common components in the background
      preloadOnIdle(() => {
        // These will be cached for faster access
        import('~/components/common/Navigation.vue').catch(() => {})
        import('~/components/common/Footer.vue').catch(() => {})
      })
    }
  }

  return {
    lazyComponent,
    lazyChart,
    lazyDataTable,
    lazyForm,
    lazyMonitoringDashboard,
    lazyBillingDashboard,
    preloadOnIdle,
    preloadComponents
  }
}
