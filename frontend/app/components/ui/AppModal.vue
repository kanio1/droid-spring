<template>
  <Teleport to="body">
    <Transition name="modal" appear>
      <div
        v-if="modelValue"
        class="modal-backdrop"
        @click="handleBackdropClick"
      >
        <div class="modal-container" @click.stop>
          <Transition name="modal-content" appear>
            <div
              v-if="modelValue"
              :style="modalStyle"
              class="modal"
              role="dialog"
              :aria-labelledby="titleId"
              :aria-describedby="descriptionId"
            >
              <!-- Header -->
              <div v-if="title || closable" class="modal__header">
                <h2 :id="titleId" class="modal__title">
                  {{ title }}
                </h2>
                <button
                  v-if="closable"
                  class="modal__close"
                  @click="handleClose"
                  aria-label="Close modal"
                >
                  ✕
                </button>
              </div>

              <!-- Description -->
              <p v-if="description" :id="descriptionId" class="modal__description">
                {{ description }}
              </p>

              <!-- Body -->
              <div class="modal__body">
                <slot />
              </div>

              <!-- Footer -->
              <div v-if="$slots.footer" class="modal__footer">
                <slot name="footer" />
              </div>
            </div>
          </Transition>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'

interface Props {
  modelValue: boolean
  title?: string
  description?: string
  size?: 'small' | 'normal' | 'large' | 'xl'
  closable?: boolean
  closeOnBackdrop?: boolean
  persistent?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  size: 'normal',
  closable: true,
  closeOnBackdrop: true,
  persistent: false
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  close: []
}>()

const titleId = ref(`modal-title-${Math.random().toString(36).substr(2, 9)}`)
const descriptionId = ref(`modal-desc-${Math.random().toString(36).substr(2, 9)}`)

const modalStyle = computed(() => {
  const sizes = {
    small: 'max-width: 400px',
    normal: 'max-width: 600px',
    large: 'max-width: 800px',
    xl: 'max-width: 1200px'
  }
  return sizes[props.size]
})

const handleBackdropClick = () => {
  if (props.closeOnBackdrop && !props.persistent) {
    handleClose()
  }
}

const handleClose = () => {
  emit('update:modelValue', false)
  emit('close')
}

// Handle escape key
const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape' && props.modelValue && props.closable) {
    handleClose()
  }
}

watch(
  () => props.modelValue,
  (isOpen) => {
    if (isOpen) {
      document.addEventListener('keydown', handleKeydown)
      document.body.style.overflow = 'hidden'
    } else {
      document.removeEventListener('keydown', handleKeydown)
      document.body.style.overflow = ''
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: var(--z-modal-backdrop);
  padding: var(--space-4);
}

.modal-container {
  width: 100%;
  max-height: 100vh;
  overflow-y: auto;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xl);
  display: flex;
  flex-direction: column;
  max-height: 90vh;
  width: 100%;
  margin: var(--space-4);
}

.modal__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-6);
  border-bottom: 1px solid var(--color-border);
}

.modal__title {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0;
}

.modal__close {
  padding: var(--space-2);
  background: none;
  border: none;
  font-size: var(--font-size-xl);
  color: var(--color-text-secondary);
  cursor: pointer;
  border-radius: var(--radius-base);
  transition: all var(--transition-fast) var(--transition-timing);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
}

.modal__close:hover {
  background: var(--color-surface-alt);
  color: var(--color-text-primary);
}

.modal__description {
  padding: 0 var(--space-6) var(--space-4);
  color: var(--color-text-secondary);
  margin: 0;
  font-size: var(--font-size-sm);
}

.modal__body {
  padding: var(--space-6);
  overflow-y: auto;
  flex: 1;
}

.modal__footer {
  padding: var(--space-6);
  border-top: 1px solid var(--color-border);
  display: flex;
  gap: var(--space-3);
  justify-content: flex-end;
  align-items: center;
}

/* Transitions */
.modal-enter-active,
.modal-leave-active {
  transition: opacity var(--transition-base) var(--transition-timing);
}

.modal-enter-active .modal-container,
.modal-leave-active .modal-container {
  transition: all var(--transition-base) var(--transition-timing);
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .modal-container,
.modal-leave-to .modal-container {
  opacity: 0;
  transform: scale(0.95);
}

.modal-content-enter-active,
.modal-content-leave-active {
  transition: all var(--transition-base) var(--transition-timing);
}

.modal-content-enter-from,
.modal-content-leave-to {
  opacity: 0;
  transform: translateY(-20px) scale(0.95);
}

/* Responsive */
@media (max-width: 640px) {
  .modal-backdrop {
    padding: var(--space-2);
  }

  .modal__header,
  .modal__body,
  .modal__footer {
    padding: var(--space-4);
  }

  .modal__title {
    font-size: var(--font-size-lg);
  }
}
</style>
