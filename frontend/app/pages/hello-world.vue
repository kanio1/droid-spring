<script setup lang="ts">
import { computed } from 'vue'

const config = useRuntimeConfig()
const auth = useAuth()

await auth.ensureReady()

const { data: hello, pending, error, refresh, status: fetchStatus } = await useFetch('/api/hello', {
  baseURL: config.public.apiBaseUrl,
  credentials: 'include',
  key: 'hello-world',
  immediate: true,
  onRequest: ({ options }) => {
    const headers = new Headers(options.headers as HeadersInit | undefined)
    if (auth.authHeader.value) {
      headers.set('Authorization', auth.authHeader.value)
    }
    options.headers = headers
  }
})

const displayName = computed(() => {
  const profile = auth.profile.value
  if (profile?.firstName || profile?.lastName) {
    return `${profile.firstName ?? ''} ${profile.lastName ?? ''}`.trim()
  }
  return profile?.username ?? profile?.email ?? 'Authenticated user'
})

const greeting = computed(() => hello.value?.message ?? `Hello, ${displayName.value}`)
const subject = computed(() => hello.value?.subject ?? auth.profile.value?.id ?? '—')
const roles = computed(() => hello.value?.roles ?? [])

const hasError = computed(() => Boolean(error.value))
const errorMessage = computed(() => error.value?.data?.detail ?? error.value?.message ?? 'Unable to fetch greeting')

const retry = async () => {
  if (pending.value) {
    return
  }
  const refreshed = await auth.refreshToken(10)
  if (refreshed) {
    await refresh()
  } else {
    await refresh()
  }
}
</script>

<template>
  <section class="greeting" data-testid="hello-card">
    <div class="greeting__card">
      <header class="greeting__header">
        <span class="greeting__badge">Hello world</span>
        <p class="greeting__subtitle">
          Secure endpoint response from backend BFF
        </p>
      </header>

      <div
        v-if="pending && fetchStatus === 'pending'"
        class="greeting__loader"
        data-testid="hello-loader"
      >
        <span class="greeting__spinner" aria-hidden="true" />
        <p>Loading greeting…</p>
      </div>

      <div v-else>
        <h2 class="greeting__title" data-testid="hello-greeting">
          {{ greeting }}
        </h2>
        <dl class="greeting__details">
          <div class="greeting__row">
            <dt>Subject</dt>
            <dd data-testid="hello-subject">
              {{ subject }}
            </dd>
          </div>
          <div class="greeting__row">
            <dt>Display name</dt>
            <dd data-testid="hello-display-name">
              {{ displayName }}
            </dd>
          </div>
          <div class="greeting__row">
            <dt>Roles</dt>
            <dd>
              <span
                v-if="roles.length === 0"
                class="greeting__chip greeting__chip--empty"
                data-testid="hello-roles-empty"
              >
                No roles
              </span>
              <span
                v-for="role in roles"
                v-else
                :key="role"
                class="greeting__chip"
                data-testid="hello-role"
              >
                {{ role }}
              </span>
            </dd>
          </div>
        </dl>
      </div>

      <transition name="fade">
        <p
          v-if="hasError"
          class="greeting__error"
          role="alert"
          data-testid="hello-error"
        >
          {{ errorMessage }}
        </p>
      </transition>

      <footer class="greeting__footer">
        <button
          type="button"
          class="greeting__button"
          :disabled="pending"
          data-testid="hello-refresh"
          @click="retry"
        >
          <span v-if="pending" class="greeting__spinner greeting__spinner--button" aria-hidden="true" />
          <span>{{ pending ? 'Refreshing…' : 'Refresh greeting' }}</span>
        </button>
      </footer>
    </div>
  </section>
</template>

<style scoped>
.greeting {
  width: 100%;
  max-width: 720px;
}

.greeting__card {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  background: rgba(15, 23, 42, 0.78);
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 20px;
  padding: 2.5rem;
  box-shadow: 0 28px 70px -30px rgba(15, 23, 42, 0.9);
  backdrop-filter: blur(18px);
}

.greeting__header {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.greeting__badge {
  align-self: flex-start;
  padding: 0.3rem 0.85rem;
  border-radius: 999px;
  background: rgba(74, 222, 128, 0.18);
  color: #4ade80;
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.greeting__subtitle {
  margin: 0;
  color: rgba(148, 163, 184, 0.8);
  font-size: 0.95rem;
}

.greeting__loader {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  color: rgba(226, 232, 240, 0.9);
}

.greeting__spinner {
  width: 1.5rem;
  height: 1.5rem;
  border-radius: 50%;
  border: 3px solid rgba(148, 163, 184, 0.4);
  border-top-color: #38bdf8;
  animation: spin 0.9s linear infinite;
}

.greeting__spinner--button {
  width: 1.1rem;
  height: 1.1rem;
  border-width: 2px;
}

.greeting__title {
  margin: 0;
  font-size: 2.4rem;
  font-weight: 600;
  color: #f8fafc;
}

.greeting__details {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin: 1rem 0 0;
}

.greeting__row {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid rgba(148, 163, 184, 0.15);
}

.greeting__row:last-child {
  border-bottom: none;
}

.greeting__row dt {
  margin: 0;
  font-size: 0.95rem;
  color: rgba(148, 163, 184, 0.85);
}

.greeting__row dd {
  margin: 0;
  font-size: 1rem;
  font-weight: 500;
  color: #e2e8f0;
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  justify-content: flex-end;
}

.greeting__chip {
  padding: 0.25rem 0.75rem;
  border-radius: 999px;
  background: rgba(14, 165, 233, 0.15);
  color: #38bdf8;
  font-size: 0.85rem;
}

.greeting__chip--empty {
  background: rgba(148, 163, 184, 0.2);
  color: rgba(226, 232, 240, 0.8);
}

.greeting__error {
  padding: 0.85rem 1.1rem;
  border-radius: 12px;
  background: rgba(248, 113, 113, 0.18);
  color: #fecaca;
  font-size: 0.9rem;
  margin: 0;
}

.greeting__footer {
  display: flex;
  justify-content: flex-end;
}

.greeting__button {
  display: inline-flex;
  align-items: center;
  gap: 0.65rem;
  padding: 0.75rem 1.5rem;
  border-radius: 12px;
  border: 1px solid rgba(248, 250, 252, 0.25);
  background: rgba(30, 41, 59, 0.85);
  color: #f8fafc;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.greeting__button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.greeting__button:not(:disabled):hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 22px -15px rgba(248, 250, 252, 0.65);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 640px) {
  .greeting__card {
    padding: 1.75rem;
  }

  .greeting__title {
    font-size: 1.8rem;
  }

  .greeting__row {
    flex-direction: column;
    align-items: flex-start;
  }

  .greeting__row dd {
    justify-content: flex-start;
  }
}
</style>
