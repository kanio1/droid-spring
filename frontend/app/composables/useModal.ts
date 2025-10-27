// Modal composable - manage modal state and interactions

interface ModalState {
  isOpen: boolean
  data?: any
  component?: any
  props?: Record<string, any>
}

interface ModalOptions {
  persistent?: boolean
  closable?: boolean
  size?: 'sm' | 'md' | 'lg' | 'xl' | 'full'
  title?: string
  description?: string
  onClose?: () => void
  onConfirm?: (data?: any) => void
}

interface ConfirmOptions extends ModalOptions {
  confirmText?: string
  cancelText?: string
  variant?: 'danger' | 'warning' | 'info'
}

export const useModal = () => {
  const modalState = ref<ModalState>({
    isOpen: false,
    data: null,
    component: null,
    props: {}
  })

  const activeModals = ref<ModalState[]>([])

  // Open modal with component
  const open = (
    component?: any,
    data?: any,
    options: ModalOptions = {}
  ) => {
    const modal: ModalState = {
      isOpen: true,
      data,
      component,
      props: {
        persistent: options.persistent ?? false,
        closable: options.closable ?? true,
        size: options.size ?? 'md',
        title: options.title,
        description: options.description,
        ...options
      }
    }

    // Add to active modals stack
    activeModals.value.push(modal)
    modalState.value = modal

    // Prevent body scroll
    document.body.style.overflow = 'hidden'

    return modal
  }

  // Close current modal
  const close = (data?: any) => {
    const modal = activeModals.value.pop()
    if (modal) {
      modal.isOpen = false
      if (modal.props?.onClose) {
        modal.props.onClose()
      }
    }

    // Update current modal state
    if (activeModals.value.length > 0) {
      modalState.value = activeModals.value[activeModals.value.length - 1]
    } else {
      modalState.value = {
        isOpen: false,
        data: null,
        component: null,
        props: {}
      }
      // Restore body scroll
      document.body.style.overflow = ''
    }

    return data
  }

  // Close all modals
  const closeAll = () => {
    activeModals.value = []
    modalState.value = {
      isOpen: false,
      data: null,
      component: null,
      props: {}
    }
    // Restore body scroll
    document.body.style.overflow = ''
  }

  // Check if any modal is open
  const isOpen = computed(() => modalState.value.isOpen)

  // Get current modal
  const currentModal = computed(() => modalState.value)

  // Get modal stack size
  const modalCount = computed(() => activeModals.value.length)

  // Confirm dialog
  const confirm = (message: string, options: ConfirmOptions = {}) => {
    const {
      persistent = false,
      closable = true,
      confirmText = 'Confirm',
      cancelText = 'Cancel',
      variant = 'info',
      onConfirm,
      onClose,
      ...modalOptions
    } = options

    return new Promise<boolean>((resolve) => {
      // Create confirm component or use default
      const ConfirmComponent = defineComponent({
        props: {
          isOpen: Boolean,
          title: String,
          message: String,
          confirmText: String,
          cancelText: String,
          variant: String
        },
        emits: ['confirm', 'cancel', 'close'],
        setup(props, { emit }) {
          const handleConfirm = () => {
            emit('confirm')
          }

          const handleCancel = () => {
            emit('cancel')
          }

          const handleClose = () => {
            emit('close')
          }

          return {
            handleConfirm,
            handleCancel,
            handleClose
          }
        },
        template: `
          <div>
            <h3 class="modal-title">{{ title || 'Confirm Action' }}</h3>
            <p class="modal-message">{{ message }}</p>
            <div class="modal-footer">
              <AppButton 
                variant="secondary" 
                @click="handleCancel"
              >
                {{ cancelText }}
              </AppButton>
              <AppButton 
                :variant="variant" 
                @click="handleConfirm"
              >
                {{ confirmText }}
              </AppButton>
            </div>
          </div>
        `
      })

      // Create a wrapper component that handles the promise resolution
      const WrapperComponent = defineComponent({
        props: ['isOpen'],
        emits: ['update:isOpen'],
        setup(props, { emit }) {
          const resolvePromise = (result: boolean) => {
            // This will be handled by the parent scope
            ;(emit as any).resolve(result)
            emit('update:isOpen', false)
          }

          return {
            handleConfirm: () => resolvePromise(true),
            handleCancel: () => resolvePromise(false),
            handleClose: () => resolvePromise(false)
          }
        },
        template: `
          <AppModal 
            :model-value="isOpen"
            @update:model-value="$emit('update:isOpen', $event)"
            v-bind="$attrs"
          >
            <ConfirmComponent 
              :title="title"
              :message="message"
              :confirm-text="confirmText"
              :cancel-text="cancelText"
              :variant="variant"
              @confirm="handleConfirm"
              @cancel="handleCancel"
              @close="handleClose"
            />
          </AppModal>
        `
      })

      // Open the modal and return promise
      const promise = new Promise<boolean>((resolve) => {
        const modal = open(WrapperComponent, null, {
          ...modalOptions,
          persistent,
          closable
        })

        // Store resolve function for later
        ;(modal as any).resolve = resolve
      })

      // Configure the wrapper component
      const modal = activeModals.value[activeModals.value.length - 1]
      modal.props = {
        ...modal.props,
        title: modalOptions.title || 'Confirm Action',
        message,
        confirmText,
        cancelText,
        variant,
        onConfirm: () => {
          if (onConfirm) onConfirm()
        },
        onClose: () => {
          if (onClose) onClose()
        }
      }

      return promise
    })
  }

  // Alert dialog
  const alert = (message: string, options: ModalOptions = {}) => {
    const {
      title = 'Alert',
      confirmText = 'OK',
      ...modalOptions
    } = options

    return confirm(message, {
      ...modalOptions,
      title,
      confirmText,
      cancelText: null, // Hide cancel button
      closable: false,
      persistent: true
    })
  }

  // Loading modal
  const showLoading = (message = 'Loading...', options: ModalOptions = {}) => {
    const LoadingComponent = defineComponent({
      props: ['isOpen', 'message'],
      template: `
        <div class="loading-modal">
          <div class="loading-spinner"></div>
          <p class="loading-message">{{ message }}</p>
        </div>
      `,
      style: `
        .loading-modal {
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 16px;
          padding: 40px;
        }
        
        .loading-spinner {
          width: 40px;
          height: 40px;
          border: 4px solid var(--color-border);
          border-top: 4px solid var(--color-primary);
          border-radius: 50%;
          animation: spin 1s linear infinite;
        }
        
        .loading-message {
          margin: 0;
          color: var(--color-text-secondary);
        }
        
        @keyframes spin {
          to { transform: rotate(360deg); }
        }
      `
    })

    return open(LoadingComponent, null, {
      ...options,
      message,
      closable: false,
      persistent: true,
      size: 'sm'
    })
  }

  // Hide loading modal
  const hideLoading = () => {
    close()
  }

  return {
    // State
    isOpen: readonly(isOpen),
    currentModal: readonly(currentModal),
    modalCount: readonly(modalCount),
    
    // Methods
    open,
    close,
    closeAll,
    
    // Convenience methods
    confirm,
    alert,
    showLoading,
    hideLoading
  }
}
