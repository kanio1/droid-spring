<script setup lang="ts">
import { computed } from 'vue'

const auth = useAuth()
const diagnostics = useLoginDiagnostics()

const displayName = computed(() => {
  const profile = auth.profile.value
  if (!profile) {
    return 'loading…'
  }
  return profile.firstName ?? profile.username ?? profile.email ?? profile.id ?? 'user'
})

const email = computed(() => auth.profile.value?.email ?? '—')
const status = computed(() => auth.status.value)
const errorMessage = computed(() => auth.lastError.value)
const steps = diagnostics.steps
const lastEvent = diagnostics.lastEvent
const primaryActionLabel = diagnostics.primaryActionLabel
const canRetryLogin = diagnostics.canRetryLogin
const hasBlockingError = diagnostics.hasBlockingError

const showPrimaryAction = computed(() => canRetryLogin.value || hasBlockingError.value || !auth.profile.value)
const isSessionLoading = computed(() => !auth.isReady.value || auth.status.value === 'loading')
const decoratedLastEvent = computed(() => {
  const event = lastEvent.value
  if (!event) {
    return null
  }

  return {
    ...event,
    displayTime: process.client ? new Date(event.at).toLocaleTimeString() : event.at
  }
})

const handleLogout = async () => {
  await auth.logout()
}

const handlePrimaryAction = async () => {
  await diagnostics.runPrimaryAction()
}
</script>

<template>
  <section class="session" data-testid="session-card">
    <div class="session__card">
      <div class="session__badge" data-testid="session-badge">
        Session
      </div>
      <h2 class="session__heading" data-testid="session-heading">
        Welcome back, {{ displayName }}
      </h2>

      <dl class="session__details">
        <div class="session__row">
          <dt>Status</dt>
          <dd>
            <span
              :class="['session__status', `session__status--${status}`]"
              :data-testid="`session-status-${status}`"
            >
              {{ status }}
            </span>
          </dd>
        </div>
        <div class="session__row">
          <dt>Email</dt>
          <dd data-testid="session-email">
            {{ email }}
          </dd>
        </div>
        <div class="session__row">
          <dt>Token</dt>
          <dd data-testid="session-token">
            {{ auth.token.value ? 'Active' : 'Missing' }}
          </dd>
        </div>
      </dl>

      <p v-if="errorMessage" class="session__error" role="alert" data-testid="session-error">
        {{ errorMessage }}
      </p>

      <section
        :class="['session__diagnostics', { 'session__diagnostics--error': hasBlockingError }]"
        aria-labelledby="diagnostics-title"
      >
        <div class="session__diagnostics-header">
          <h3 id="diagnostics-title">
            Login flow checklist
          </h3>
          <p v-if="decoratedLastEvent" class="session__diagnostics-event" data-testid="session-last-event">
            Last event:
            <span class="session__diagnostics-event-type">{{ decoratedLastEvent.type }}</span>
            ·
            <time :datetime="decoratedLastEvent.at">{{ decoratedLastEvent.displayTime }}</time>
            <span v-if="decoratedLastEvent.message" class="session__diagnostics-event-message"> – {{ decoratedLastEvent.message }}</span>
          </p>
        </div>
        <ul class="session__checklist" data-testid="session-checklist">
          <li
            v-for="step in steps"
            :key="step.id"
            :data-testid="`session-checklist-${step.id}`"
            :class="['session__checklist-item', `session__checklist-item--${step.status}`]"
          >
            <div class="session__checklist-dot" aria-hidden="true" />
            <div class="session__checklist-body">
              <p class="session__checklist-title">
                {{ step.label }}
              </p>
              <p class="session__checklist-description">
                {{ step.description }}
              </p>
            </div>
            <span class="session__checklist-status">{{ step.status }}</span>
          </li>
        </ul>
      </section>

      <div class="session__actions">
        <NuxtLink
          to="/hello-world"
          class="session__button session__button--secondary"
          data-testid="session-link-hello-world"
        >
          Go to hello-world
        </NuxtLink>
        <button
          v-if="showPrimaryAction"
          type="button"
          class="session__button session__button--primary"
          :disabled="isSessionLoading"
          data-testid="session-primary-action"
          @click="handlePrimaryAction"
        >
          {{ primaryActionLabel }}
        </button>
        <button
          type="button"
          class="session__button"
          :disabled="isSessionLoading"
          data-testid="session-logout"
          @click="handleLogout"
        >
          Sign out
        </button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.session {
  max-width: 640px;
  width: 100%;
}

.session__card {
  background: rgba(15, 23, 42, 0.75);
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 20px;
  padding: 2.25rem;
  box-shadow: 0 24px 60px -30px rgba(15, 23, 42, 0.9);
  backdrop-filter: blur(14px);
}

.session__badge {
  display: inline-flex;
  align-items: center;
  padding: 0.25rem 0.75rem;
  border-radius: 999px;
  background: rgba(56, 189, 248, 0.15);
  color: #38bdf8;
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.session__heading {
  margin: 1rem 0 1.5rem;
  font-size: 2.125rem;
  font-weight: 600;
  line-height: 1.3;
}

.session__details {
  margin: 0 0 1.5rem;
  padding: 0;
}

.session__diagnostics {
  margin: 1.5rem 0;
  padding: 1.25rem 1.5rem;
  border-radius: 16px;
  background: rgba(30, 41, 59, 0.55);
  border: 1px solid rgba(148, 163, 184, 0.16);
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.session__diagnostics--error {
  border-color: rgba(248, 113, 113, 0.45);
  background: rgba(248, 113, 113, 0.12);
}

.session__diagnostics-header {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.session__diagnostics h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  color: #f8fafc;
}

.session__diagnostics-event {
  margin: 0;
  font-size: 0.85rem;
  color: rgba(148, 163, 184, 0.85);
}

.session__diagnostics-event-type {
  font-weight: 600;
}

.session__diagnostics-event-message {
  color: rgba(248, 113, 113, 0.8);
}

.session__checklist {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.session__checklist-item {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 0.75rem;
  padding: 0.85rem 1rem;
  border-radius: 14px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  align-items: center;
  background: rgba(15, 23, 42, 0.45);
}

.session__checklist-item--success {
  border-color: rgba(74, 222, 128, 0.25);
  background: rgba(22, 163, 74, 0.18);
}

.session__checklist-item--error {
  border-color: rgba(248, 113, 113, 0.35);
  background: rgba(248, 113, 113, 0.15);
}

.session__checklist-dot {
  width: 0.6rem;
  height: 0.6rem;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.6);
}

.session__checklist-item--success .session__checklist-dot {
  background: #4ade80;
}

.session__checklist-item--error .session__checklist-dot {
  background: #f87171;
}

.session__checklist-body {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.session__checklist-title {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 600;
  color: #f8fafc;
}

.session__checklist-description {
  margin: 0;
  font-size: 0.85rem;
  color: rgba(148, 163, 184, 0.9);
}

.session__checklist-status {
  font-size: 0.8rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: rgba(148, 163, 184, 0.85);
}

.session__row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  padding: 0.5rem 0;
  border-bottom: 1px solid rgba(148, 163, 184, 0.12);
}

.session__row:last-child {
  border-bottom: 0;
}

.session__row dt {
  font-size: 0.9rem;
  color: rgba(148, 163, 184, 0.85);
}

.session__row dd {
  margin: 0;
  font-size: 1rem;
  font-weight: 500;
  color: #e2e8f0;
}

.session__status {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.2rem 0.65rem;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.15);
  font-size: 0.875rem;
  text-transform: capitalize;
}

.session__status--authenticated {
  background: rgba(74, 222, 128, 0.2);
  color: #4ade80;
}

.session__status--error {
  background: rgba(248, 113, 113, 0.2);
  color: #f87171;
}

.session__error {
  margin: 0 0 1rem;
  padding: 0.75rem 1rem;
  border-radius: 12px;
  background: rgba(248, 113, 113, 0.15);
  color: #fecaca;
  font-size: 0.9rem;
}

.session__actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.session__button {
  padding: 0.75rem 1.5rem;
  border-radius: 12px;
  border: 1px solid rgba(248, 250, 252, 0.25);
  background: rgba(30, 41, 59, 0.8);
  color: #f8fafc;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.session__button:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 20px -15px rgba(248, 250, 252, 0.6);
}

.session__button--primary {
  background: rgba(59, 130, 246, 0.8);
  border-color: rgba(59, 130, 246, 0.9);
}

.session__button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.session__button--secondary {
  background: rgba(15, 23, 42, 0.6);
  border-color: rgba(148, 163, 184, 0.35);
  color: rgba(226, 232, 240, 0.95);
}

@media (max-width: 640px) {
  .session__heading {
    font-size: 1.8rem;
  }

  .session__actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
