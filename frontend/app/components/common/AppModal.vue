<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="modelValue"
        class="modal-overlay"
        :class="{ 'modal-overlay--closable': closable }"
        @click="handleOverlayClick"
        @keydown.esc="handleEscape"
        tabindex="-1"
      >
        <Transition name="modal-content">
          <div
            v-if="modelValue"
            ref="modalContent"
            class="modal-content"
            :class="modalClasses"
            role="dialog"
            :aria-labelledby="titleId"
            :aria-describedby="descriptionId"
            @click.stop
          >
            <!-- Modal Header -->
            <div v-if="title || $slots.header || closable" class="modal-header">
              <div class="modal-header__content">
                <h2 v-if="title" :id="titleId" class="modal-title">
                  {{ title }}
                </h2>
                <div v-if="$slots.header" class="modal-header__slot">
                  <slot name="header" />
                </div>
              </div>
              
              <button
                v-if="closable"
                type="button"
                class="modal-close"
                @click="closeModal"
                aria-label="Close modal"
              >
                ×
              </button>
            </div>

            <!-- Modal Body -->
            <div class="modal-body">
              <p v-if="description" :id="descriptionId" class="modal-description">
                {{ description }}
              </p>
              <div class="modal-content-area">
                <slot />
              </div>
            </div>

            <!-- Modal Footer -->
            <div v-if="$slots.footer" class="modal-footer">
              <slot name="footer" />
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
interface Props {
  modelValue: boolean
  title?: string
  description?: string
  size?: 'sm' | 'md' | 'lg' | 'xl' | 'full'
  closable?: boolean
  persistent?: boolean
  closeOnOverlay?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  size: 'md',
  closable: true,
  persistent: false,
  closeOnOverlay: true
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  close: []
  open: []
}>()

// Template refs
const modalContent = ref<HTMLElement>()

// Generate unique IDs for accessibility
const titleId = computed(() => `modal-title-${Math.random().toString(36).substr(2, 9)}`)
const descriptionId = computed(() => `modal-description-${Math.random().toString(36).substr(2, 9)}`)

// Classes
const modalClasses = computed(() => [
  'modal-content',
  `modal-content--${props.size}`,
  {
    'modal-content--with-header': props.title || props.closable,
    'modal-content--with-footer': false // This would be determined by slot presence
  }
])

// Methods
const closeModal = () => {
  if (props.persistent) return
  emit('update:modelValue', false)
  emit('close')
}

const handleOverlayClick = () => {
  if (props.closeOnOverlay && !props.persistent) {
    closeModal()
  }
}

const handleEscape = () => {
  if (props.closable && !props.persistent) {
    closeModal()
  }
}

// Handle focus management
const focusableElements = computed(() => {
  if (!modalContent.value) return []
  
  const focusable = modalContent.value.querySelectorAll(
    'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
  )
  return Array.from(focusable) as HTMLElement[]
})

const trapFocus = (event: KeyboardEvent) => {
  if (event.key !== 'Tab') return
  
  const elements = focusableElements.value
  if (elements.length === 0) return
  
  const firstElement = elements[0]
  const lastElement = elements[elements.length - 1]
  
  if (event.shiftKey && document.activeElement === firstElement) {
    event.preventDefault()
    lastElement.focus()
  } else if (!event.shiftKey && document.activeElement === lastElement) {
    event.preventDefault()
    firstElement.focus()
  }
}

// Watch for modal open/close
watch(
  () => props.modelValue,
  (isOpen) => {
    if (isOpen) {
      emit('open')
      // Focus the modal content when opened
      nextTick(() => {
        modalContent.value?.focus()
        document.addEventListener('keydown', trapFocus)
      })
    } else {
      document.removeEventListener('keydown', trapFocus)
    }
  },
  { immediate: false }
)

// Clean up on unmount
onBeforeUnmount(() => {
  document.removeEventListener('keydown', trapFocus)
})

// Expose methods
defineExpose({
  focus: () => modalContent.value?.focus(),
  close: closeModal
})
</script>

<style scoped>
/* Overlay */
.modal-overlay {
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
  backdrop-filter: blur(2px);
}

.modal-overlay--closable {
  cursor: pointer;
}

/* Modal Content */
.modal-content {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xl);
  max-height: calc(100vh - 2rem);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  outline: none;
  cursor: default;
  min-width: 320px;
}

.modal-content--sm {
  width: 100%;
  max-width: 400px;
}

.modal-content--md {
  width: 100%;
  max-width: 500px;
}

.modal-content--lg {
  width: 100%;
  max-width: 700px;
}

.modal-content--xl {
  width: 100%;
  max-width: 900px;
}

.modal-content--full {
  width: calc(100vw - 2rem);
  height: calc(100vh - 2rem);
  max-width: none;
  max-height: none;
}

/* Header */
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-6);
  border-bottom: 1px solid var(--color-border);
  background: var(--color-surface-alt);
  flex-shrink: 0;
}

.modal-header__content {
  flex: 1;
}

.modal-header__content h2 {
  margin: 0;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.modal-title {
  margin: 0;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.modal-header__slot {
  margin-top: var(--space-2);
}

.modal-close {
  background: none;
  border: none;
  font-size: var(--font-size-2xl);
  color: var(--color-text-muted);
  cursor: pointer;
  padding: var(--space-1);
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast) var(--transition-timing);
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-left: var(--space-4);
}

.modal-close:hover {
  color: var(--color-text-secondary);
  background: var(--color-surface);
}

/* Body */
.modal-body {
  padding: var(--space-6);
  flex: 1;
  overflow-y: auto;
}

.modal-description {
  margin: 0 0 var(--space-4) 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-base);
  line-height: var(--line-height-relaxed);
}

.modal-content-area {
  /* Content area for slot */
}

/* Footer */
.modal-footer {
  padding: var(--space-6);
  border-top: 1px solid var(--color-border);
  background: var(--color-surface-alt);
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-3);
  flex-shrink: 0;
}

/* Transitions */
.modal-enter-active,
.modal-leave-active {
  transition: opacity var(--transition-base) var(--transition-timing);
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-content-enter-active,
.modal-content-leave-active {
  transition: opacity var(--transition-base) var(--transition-timing),
              transform var(--transition-base) var(--transition-timing);
}

.modal-content-enter-from,
.modal-content-leave-to {
  opacity: 0;
  transform: scale(0.95) translateY(-10px);
}

/* Mobile Responsive */
@media (max-width: 640px) {
  .modal-overlay {
    padding: var(--space-2);
  }
  
  .modal-content {
    max-width: none;
    width: 100%;
    max-height: calc(100vh - 1rem);
  }
  
  .modal-header {
    padding: var(--space-4);
  }
  
  .modal-body {
    padding: var(--space-4);
  }
  
  .modal-footer {
    padding: var(--space-4);
    flex-direction: column;
    align-items: stretch;
  }
  
  .modal-title {
    font-size: var(--font-size-lg);
  }
}

/* Tablet responsive */
@media (min-width: 641px) and (max-width: 1024px) {
  .modal-content--sm,
  .modal-content--md {
    max-width: 90vw;
  }
  
  .modal-content--lg {
    max-width: 80vw;
  }
}

/* High contrast mode support */
@media (prefers-contrast: high) {
  .modal-overlay {
    background: rgba(0, 0, 0, 0.8);
  }
  
  .modal-content {
    border: 2px solid var(--color-border);
  }
}

/* Reduced motion support */
@media (prefers-reduced-motion: reduce) {
  .modal-enter-active,
  .modal-leave-active,
  .modal-content-enter-active,
  .modal-content-leave-active {
    transition: none;
  }
}
</style>
