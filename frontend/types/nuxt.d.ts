import type Keycloak from 'keycloak-js'

declare module '#app' {
  interface NuxtApp {
    $keycloak: Keycloak | null
  }
}

declare module 'vue' {
  interface ComponentCustomProperties {
    $keycloak: Keycloak | null
  }
}

export {}
