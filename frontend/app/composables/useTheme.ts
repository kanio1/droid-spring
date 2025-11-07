/**
 * Theme Management Composable
 * Manages light/dark mode theme switching
 */

export type Theme = 'light' | 'dark' | 'system'

export const useTheme = () => {
  const theme = ref<Theme>('system')

  // Get system theme preference
  const getSystemTheme = (): 'light' | 'dark' => {
    if (typeof window === 'undefined') return 'light'
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
  }

  // Get actual theme (resolves 'system' to actual theme)
  const actualTheme = computed(() => {
    return theme.value === 'system' ? getSystemTheme() : theme.value
  })

  // Apply theme to document
  const applyTheme = (newTheme: Theme) => {
    theme.value = newTheme

    if (typeof document === 'undefined') return

    const html = document.documentElement

    if (newTheme === 'system') {
      html.removeAttribute('data-theme')
      return
    }

    html.setAttribute('data-theme', newTheme)
  }

  // Toggle between light and dark
  const toggleTheme = () => {
    const current = actualTheme.value
    const newTheme: Theme = current === 'light' ? 'dark' : 'light'
    applyTheme(newTheme)
  }

  // Set specific theme
  const setTheme = (newTheme: Theme) => {
    applyTheme(newTheme)
  }

  // Initialize theme
  const initTheme = () => {
    // Try to get theme from localStorage
    if (typeof window !== 'undefined') {
      const saved = localStorage.getItem('bss-theme') as Theme | null
      if (saved && (saved === 'light' || saved === 'dark' || saved === 'system')) {
        applyTheme(saved)
        return
      }
    }

    // Default to system theme
    applyTheme('system')
  }

  // Save theme to localStorage
  const saveTheme = (newTheme: Theme) => {
    if (typeof window !== 'undefined') {
      localStorage.setItem('bss-theme', newTheme)
    }
  }

  // Watch theme changes and save to localStorage
  watch(theme, (newTheme) => {
    saveTheme(newTheme)
  })

  // Listen for system theme changes
  if (typeof window !== 'undefined') {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    mediaQuery.addEventListener('change', () => {
      if (theme.value === 'system') {
        applyTheme('system')
      }
    })
  }

  // Initialize on mount
  onMounted(() => {
    initTheme()
  })

  return {
    theme: readonly(theme),
    actualTheme,
    toggleTheme,
    setTheme,
    initTheme
  }
}
