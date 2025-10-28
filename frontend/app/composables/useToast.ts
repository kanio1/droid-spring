// Toast composable - manage toast notifications

interface ToastOptions {
  type?: 'success' | 'error' | 'warning' | 'info'
  title?: string
  message: string
  duration?: number
  persistent?: boolean
  actions?: Array<{
    label: string
    variant?: 'primary' | 'secondary' | 'danger'
    action: () => void
  }>
  onClose?: () => void
}

interface ToastState extends ToastOptions {
  id: string
  visible: boolean
  timestamp: number
}

const TOAST_CONTAINER_ID = 'toast-container'

export const useToast = () => {
  const toasts = ref<ToastState[]>([])

  // Generate unique ID
  const generateId = (): string => {
    return `toast-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
  }

  // Create toast container if it doesn't exist
  const ensureContainer = () => {
    let container = document.getElementById(TOAST_CONTAINER_ID)
    if (!container) {
      container = document.createElement('div')
      container.id = TOAST_CONTAINER_ID
      container.className = 'toast-container'
      document.body.appendChild(container)
    }
    return container
  }

  // Show toast notification
  const showToast = (options: ToastOptions) => {
    const {
      type = 'info',
      title,
      message,
      duration = 4000,
      persistent = false,
      actions = [],
      onClose
    } = options

    const id = generateId()
    const timestamp = Date.now()

    const toast: ToastState = {
      id,
      type,
      title,
      message,
      duration,
      persistent,
      actions,
      onClose,
      visible: true,
      timestamp
    }

    // Add to toasts array
    toasts.value.push(toast)

    // Auto-remove if not persistent
    if (!persistent && duration > 0) {
      setTimeout(() => {
        removeToast(id)
      }, duration)
    }

    return id
  }

  // Remove toast by ID
  const removeToast = (id: string) => {
    const index = toasts.value.findIndex(toast => toast.id === id)
    if (index > -1) {
      const toast = toasts.value[index]
      
      // Trigger onClose callback
      if (toast.onClose) {
        toast.onClose()
      }

      // Mark as invisible for animation
      toast.visible = false

      // Remove after animation
      setTimeout(() => {
        toasts.value.splice(index, 1)
      }, 300) // Match CSS transition duration
    }
  }

  // Clear all toasts
  const clearAll = () => {
    toasts.value.forEach(toast => {
      if (toast.onClose) {
        toast.onClose()
      }
    })
    toasts.value = []
  }

  // Convenience methods
  const success = (message: string, options: Omit<ToastOptions, 'type' | 'message'> = {}) => {
    return showToast({
      ...options,
      type: 'success',
      message
    })
  }

  const error = (message: string, options: Omit<ToastOptions, 'type' | 'message'> = {}) => {
    return showToast({
      ...options,
      type: 'error',
      message,
      duration: options.duration || 6000 // Longer duration for errors
    })
  }

  const warning = (message: string, options: Omit<ToastOptions, 'type' | 'message'> = {}) => {
    return showToast({
      ...options,
      type: 'warning',
      message
    })
  }

  const info = (message: string, options: Omit<ToastOptions, 'type' | 'message'> = {}) => {
    return showToast({
      ...options,
      type: 'info',
      message
    })
  }

  // Get toast count
  const toastCount = computed(() => toasts.value.length)

  // Get visible toasts
  const visibleToasts = computed(() => toasts.value.filter(toast => toast.visible))

  return {
    // State
    toasts: readonly(toasts),
    visibleToasts: readonly(visibleToasts),
    toastCount: readonly(toastCount),
    
    // Methods
    showToast,
    removeToast,
    clearAll,
    
    // Convenience methods
    success,
    error,
    warning,
    info,
    
    // Utilities
    generateId
  }
}

// Toast component for rendering in the DOM
export const ToastContainer = defineComponent({
  name: 'ToastContainer',
  setup() {
    const { visibleToasts } = useToast()

    return () => (
      h('div', {
        id: TOAST_CONTAINER_ID,
        class: 'toast-container'
      }, 
      visibleToasts.value.map(toast => 
        h(ToastItem, { 
          key: toast.id,
          toast 
        })
      ))
    )
  }
})

const ToastItem = defineComponent({
  name: 'ToastItem',
  props: {
    toast: {
      type: Object as PropType<ToastState>,
      required: true
    }
  },
  emits: ['remove'],
  setup(props, { emit }) {
    const { removeToast } = useToast()

    const handleRemove = () => {
      removeToast(props.toast.id)
    }

    const handleAction = (action: any) => {
      action.action()
      removeToast(props.toast.id)
    }

    return () => (
      h('div', {
        class: [
          'toast',
          `toast--${props.toast.type}`,
          { 'toast--visible': props.toast.visible }
        ]
      }, [
        // Icon
        h('div', { class: 'toast__icon' }, getToastIcon(props.toast.type)),
        
        // Content
        h('div', { class: 'toast__content' }, [
          props.toast.title && h('div', { class: 'toast__title' }, props.toast.title),
          h('div', { class: 'toast__message' }, props.toast.message)
        ]),
        
        // Actions
        props.toast.actions && props.toast.actions.length > 0 && 
          h('div', { class: 'toast__actions' }, 
            props.toast.actions.map(action => 
              h('button', {
                class: ['toast__action', `toast__action--${action.variant || 'secondary'}`],
                onClick: () => handleAction(action)
              }, action.label)
            )
          ),
        
        // Close button
        h('button', {
          class: 'toast__close',
          onClick: handleRemove,
          'aria-label': 'Close notification'
        }, '×')
      ])
    )
  }
})

const getToastIcon = (type: string): string => {
  const icons = {
    success: '✅',
    error: '❌',
    warning: '⚠️',
    info: 'ℹ️'
  }
  return icons[type as keyof typeof icons] || icons.info
}
