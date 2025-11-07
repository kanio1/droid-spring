// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  devtools: { enabled: true },
  typescript: {
    typeCheck: true,
    strict: true
  },
  modules: [
    '@pinia/nuxt',
    '@nuxtjs/tailwindcss',
    '@nuxtjs/i18n'
  ],
  css: [
    '~/assets/styles/main.css',
    '~/assets/styles/tokens.css'
  ],
  runtimeConfig: {
    public: {
      apiBaseUrl: process.env.NUXT_PUBLIC_API_BASE_URL ?? 'https://localhost:8443/api/v1',
      keycloakUrl: process.env.NUXT_PUBLIC_KEYCLOAK_URL ?? 'https://localhost:8443/auth',
      keycloakRealm: process.env.NUXT_PUBLIC_KEYCLOAK_REALM ?? 'bss',
      keycloakClientId: process.env.NUXT_PUBLIC_KEYCLOAK_CLIENT_ID ?? 'bss-frontend'
    }
  },
  srcDir: 'app/',
  app: {
    head: {
      title: 'BSS - Business Support System',
      titleTemplate: '%s | BSS - Business Support System',
      meta: [
        { name: 'viewport', content: 'width=device-width, initial-scale=1' },
        { name: 'description', content: 'Comprehensive Business Support System for telecom operators - manage customers, orders, billing, and services with real-time analytics.' },
        { name: 'keywords', content: 'BSS, business support system, telecom, billing, customer management, orders, subscriptions, invoicing, analytics' },
        { name: 'author', content: 'BSS Team' },
        { name: 'robots', content: 'index, follow' },
        { name: 'googlebot', content: 'index, follow' },

        // Open Graph
        { property: 'og:site_name', content: 'BSS - Business Support System' },
        { property: 'og:type', content: 'website' },
        { property: 'og:title', content: 'BSS - Business Support System' },
        { property: 'og:description', content: 'Comprehensive Business Support System for telecom operators - manage customers, orders, billing, and services with real-time analytics.' },
        { property: 'og:image', content: '/images/og-image.png' },
        { property: 'og:image:width', content: '1200' },
        { property: 'og:image:height', content: '630' },
        { property: 'og:url', content: 'https://bss.example.com' },

        // Twitter Card
        { name: 'twitter:card', content: 'summary_large_image' },
        { name: 'twitter:title', content: 'BSS - Business Support System' },
        { name: 'twitter:description', content: 'Comprehensive Business Support System for telecom operators - manage customers, orders, billing, and services with real-time analytics.' },
        { name: 'twitter:image', content: '/images/twitter-image.png' },

        // Mobile
        { name: 'mobile-web-app-capable', content: 'yes' },
        { name: 'apple-mobile-web-app-capable', content: 'yes' },
        { name: 'apple-mobile-web-app-status-bar-style', content: 'black-translucent' },

        // Theme
        { name: 'theme-color', content: '#3b82f6' },
        { name: 'msapplication-TileColor', content: '#3b82f6' }
      ],
      link: [
        { rel: 'icon', type: 'image/x-icon', href: '/favicon.ico' },
        { rel: 'icon', type: 'image/png', sizes: '32x32', href: '/favicon-32x32.png' },
        { rel: 'icon', type: 'image/png', sizes: '16x16', href: '/favicon-16x16.png' },
        { rel: 'apple-touch-icon', sizes: '180x180', href: '/apple-touch-icon.png' },
        { rel: 'manifest', href: '/site.webmanifest' }
      ]
    }
  },
  routeRules: {
    // Prerender static pages for better performance
    '/': { prerender: true },
    '/hello-world': { prerender: true },
    '/settings': { prerender: false },

    // Cache static content
    '/assets/**': { headers: { 'cache-control': 's-maxage=31536000' } },

    // Optimize page transitions
    '/customers/**': { swr: 300 },
    '/products/**': { swr: 300 },
    '/orders/**': { swr: 300 },
    '/invoices/**': { swr: 300 },
    '/payments/**': { swr: 300 },
    '/subscriptions/**': { swr: 300 },
    '/billing/**': { swr: 300 },
    '/addresses/**': { swr: 300 },
    '/assets/**': { swr: 300 },
    '/services/**': { swr: 300 },
    '/coverage-nodes/**': { swr: 300 },
    '/monitoring/**': { swr: 60 },
    '/admin/**': { swr: 0 }
  },
  future: {
    typescriptBundlerResolution: true
  },
  pinia: {
    autoImports: [
      'defineStore',
      'acceptHMRUpdate'
    ]
  },
  vite: {
    optimizeDeps: {
      include: [
        'primevue',
        'keycloak-js',
        'chart.js',
        'date-fns'
      ]
    },
    build: {
      rollupOptions: {
        output: {
          manualChunks: {
            // Vendor chunks for better caching
            'vendor-vue': ['vue', 'vue-router', 'nuxt'],
            'vendor-ui': ['primevue', '@primevue/themes', 'primeicons'],
            'vendor-charts': ['chart.js', 'vue-chartjs'],
            'vendor-auth': ['keycloak-js'],
            'vendor-utils': ['date-fns', 'axios']
          }
        }
      }
    }
  },
  experimental: {
    asyncContext: true,
    payloadExtraction: true,
    renderJsonPayloads: true,
    viewTransition: true
  },
  components: [
    {
      path: '~/components/ui',
      pathPrefix: false
    }
  ],
  i18n: {
    locales: [
      { code: 'en', name: 'English', file: 'en.json' },
      { code: 'pl', name: 'Polski', file: 'pl.json' }
    ],
    defaultLocale: 'en',
    langDir: 'locales',
    strategy: 'prefix_except_default',
    detectBrowserLanguage: {
      useCookie: true,
      cookieKey: 'i18n_redirected',
      redirectOn: 'root'
    }
  }
})
