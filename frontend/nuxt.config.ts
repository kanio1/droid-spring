// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  devtools: { enabled: true },
  typescript: {
    typeCheck: false,
    strict: true
  },
  runtimeConfig: {
    public: {
      apiBaseUrl: process.env.NUXT_PUBLIC_API_BASE_URL ?? 'https://localhost:8443/api',
      keycloakUrl: process.env.NUXT_PUBLIC_KEYCLOAK_URL ?? 'https://localhost:8443/auth',
      keycloakRealm: process.env.NUXT_PUBLIC_KEYCLOAK_REALM ?? 'bss',
      keycloakClientId: process.env.NUXT_PUBLIC_KEYCLOAK_CLIENT_ID ?? 'bss-frontend'
    }
  },
  srcDir: 'app/',
  app: {
    head: {
      title: 'BSS Frontend',
      meta: [
        { name: 'viewport', content: 'width=device-width, initial-scale=1' }
      ]
    }
  },
  future: {
    typescriptBundlerResolution: true
  }
})
