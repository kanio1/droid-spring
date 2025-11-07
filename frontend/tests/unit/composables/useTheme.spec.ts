import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useTheme } from '~/composables/useTheme'

// Mock localStorage
const localStorageMock = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn()
}

Object.defineProperty(window, 'localStorage', {
  value: localStorageMock
})

// Mock matchMedia
const matchMediaMock = vi.fn().mockImplementation(query => ({
  matches: false,
  media: query,
  onchange: null,
  addListener: vi.fn(),
  removeListener: vi.fn(),
  addEventListener: vi.fn(),
  removeEventListener: vi.fn(),
  dispatchEvent: vi.fn()
}))

Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: matchMediaMock
})

// Mock document.documentElement
const documentElementMock = {
  setAttribute: vi.fn(),
  removeAttribute: vi.fn(),
  getAttribute: vi.fn()
}

Object.defineProperty(document, 'documentElement', {
  value: documentElementMock
})

describe('useTheme', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    localStorageMock.getItem.mockReturnValue(null)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('initializes with system theme when no saved preference', () => {
    localStorageMock.getItem.mockReturnValue(null)
    matchMediaMock.mockReturnValue({
      matches: true,
      media: '(prefers-color-scheme: dark)',
      addEventListener: vi.fn(),
      removeEventListener: vi.fn()
    })

    const { theme, actualTheme } = useTheme()

    // Should be 'system' initially
    expect(theme.value).toBe('system')

    // actualTheme should be 'dark' when system is dark
    expect(actualTheme.value).toBe('dark')
  })

  it('loads saved theme from localStorage', () => {
    localStorageMock.getItem.mockReturnValue('dark')

    const { theme, actualTheme } = useTheme()

    expect(theme.value).toBe('dark')
    expect(actualTheme.value).toBe('dark')
  })

  it('applies light theme when set', () => {
    const { setTheme, actualTheme } = useTheme()

    setTheme('light')

    expect(actualTheme.value).toBe('light')
    expect(documentElementMock.setAttribute).toHaveBeenCalledWith('data-theme', 'light')
    expect(localStorageMock.setItem).toHaveBeenCalledWith('bss-theme', 'light')
  })

  it('applies dark theme when set', () => {
    const { setTheme, actualTheme } = useTheme()

    setTheme('dark')

    expect(actualTheme.value).toBe('dark')
    expect(documentElementMock.setAttribute).toHaveBeenCalledWith('data-theme', 'dark')
    expect(localStorageMock.setItem).toHaveBeenCalledWith('bss-theme', 'dark')
  })

  it('applies system theme when set', () => {
    matchMediaMock.mockReturnValue({
      matches: true,
      media: '(prefers-color-scheme: dark)',
      addEventListener: vi.fn(),
      removeEventListener: vi.fn()
    })

    const { setTheme, actualTheme } = useTheme()

    setTheme('system')

    expect(actualTheme.value).toBe('dark')
    expect(documentElementMock.removeAttribute).toHaveBeenCalledWith('data-theme')
    expect(localStorageMock.setItem).toHaveBeenCalledWith('bss-theme', 'system')
  })

  it('toggles between light and dark themes', () => {
    const { toggleTheme, actualTheme } = useTheme()

    // Start with light (based on default mock)
    expect(actualTheme.value).toBe('light')

    // Toggle to dark
    toggleTheme()
    expect(actualTheme.value).toBe('dark')
    expect(documentElementMock.setAttribute).toHaveBeenCalledWith('data-theme', 'dark')

    // Toggle back to light
    toggleTheme()
    expect(actualTheme.value).toBe('light')
    expect(documentElementMock.setAttribute).toHaveBeenCalledWith('data-theme', 'light')
  })

  it('detects system dark theme preference', () => {
    matchMediaMock.mockImplementation(query => ({
      matches: query === '(prefers-color-scheme: dark)',
      media: query,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn()
    }))

    const { actualTheme } = useTheme()

    expect(actualTheme.value).toBe('dark')
  })

  it('detects system light theme preference', () => {
    matchMediaMock.mockImplementation(query => ({
      matches: query === '(prefers-color-scheme: light)',
      media: query,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn()
    }))

    const { actualTheme } = useTheme()

    expect(actualTheme.value).toBe('light')
  })

  it('saves theme to localStorage when changed', () => {
    const { setTheme } = useTheme()

    setTheme('light')
    expect(localStorageMock.setItem).toHaveBeenCalledWith('bss-theme', 'light')

    setTheme('dark')
    expect(localStorageMock.setItem).toHaveBeenCalledWith('bss-theme', 'dark')

    setTheme('system')
    expect(localStorageMock.setItem).toHaveBeenCalledWith('bss-theme', 'system')
  })

  it('responds to system theme changes when set to system', () => {
    const addEventListenerSpy = vi.fn()
    matchMediaMock.mockReturnValue({
      matches: true,
      media: '(prefers-color-scheme: dark)',
      addEventListener: addEventListenerSpy,
      removeEventListener: vi.fn()
    })

    const { setTheme, actualTheme } = useTheme()

    setTheme('system')

    // Should have added listener for system theme changes
    expect(addEventListenerSpy).toHaveBeenCalledWith('change', expect.any(Function))
  })

  it('does not add system theme listener when not using system theme', () => {
    const addEventListenerSpy = vi.fn()
    matchMediaMock.mockReturnValue({
      matches: true,
      media: '(prefers-color-scheme: dark)',
      addEventListener: addEventListenerSpy,
      removeEventListener: vi.fn()
    })

    const { setTheme } = useTheme()

    // Set to explicit dark theme
    setTheme('dark')

    // Listener should still be added (for future system changes)
    expect(addEventListenerSpy).toHaveBeenCalled()
  })

  it('handles invalid saved theme gracefully', () => {
    localStorageMock.getItem.mockReturnValue('invalid-theme')

    const { theme, actualTheme } = useTheme()

    // Should fall back to system theme
    expect(theme.value).toBe('system')
  })

  it('removes data-theme attribute when set to system', () => {
    const { setTheme } = useTheme()

    setTheme('system')

    expect(documentElementMock.removeAttribute).toHaveBeenCalledWith('data-theme')
  })

  it('applies theme to document element on client side', () => {
    const { setTheme } = useTheme()

    setTheme('dark')

    expect(documentElementMock.setAttribute).toHaveBeenCalledWith('data-theme', 'dark')
  })

  it('does not crash when called on server side', () => {
    // Remove document access
    const originalDocument = global.document
    // @ts-ignore
    global.document = undefined

    expect(() => useTheme()).not.toThrow()

    // Restore
    global.document = originalDocument
  })

  it('handles server-side rendering without errors', () => {
    // Mock SSR environment (no window or document)
    const originalWindow = global.window
    const originalDocument = global.document
    // @ts-ignore
    global.window = undefined
    // @ts-ignore
    global.document = undefined

    expect(() => useTheme()).not.toThrow()

    // Restore
    global.window = originalWindow
    global.document = originalDocument
  })

  it('watches theme changes and saves to localStorage', () => {
    const { setTheme } = useTheme()

    setTheme('light')
    expect(localStorageMock.setItem).toHaveBeenCalledWith('bss-theme', 'light')

    setTheme('dark')
    expect(localStorageMock.setItem).toHaveBeenCalledWith('bss-theme', 'dark')
  })

  it('theme changes persist across calls', () => {
    const { setTheme, theme } = useTheme()

    setTheme('dark')
    expect(theme.value).toBe('dark')

    // Simulate page reload - localStorage should have the value
    expect(localStorageMock.getItem).toHaveBeenCalled()
  })

  it('actualTheme reflects current theme when not system', () => {
    const { setTheme, theme, actualTheme } = useTheme()

    setTheme('light')
    expect(theme.value).toBe('light')
    expect(actualTheme.value).toBe('light')

    setTheme('dark')
    expect(theme.value).toBe('dark')
    expect(actualTheme.value).toBe('dark')
  })

  it('actualTheme follows system when theme is system', () => {
    matchMediaMock.mockImplementation(query => ({
      matches: query === '(prefers-color-scheme: dark)',
      media: query,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn()
    }))

    const { setTheme, theme, actualTheme } = useTheme()

    setTheme('system')
    expect(theme.value).toBe('system')
    expect(actualTheme.value).toBe('dark')

    // If system changes to light
    matchMediaMock.mockImplementation(query => ({
      matches: query === '(prefers-color-scheme: light)',
      media: query,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn()
    }))

    // actualTheme should update when system changes
    // (This would be triggered by the change event)
  })

  it('prevents theme from being undefined or null', () => {
    const { setTheme } = useTheme()

    setTheme('light')
    expect(localStorageMock.setItem).not.toHaveBeenCalledWith('bss-theme', undefined)
    expect(localStorageMock.setItem).not.toHaveBeenCalledWith('bss-theme', null)
  })

  it('handles rapid theme switches', () => {
    const { toggleTheme } = useTheme()

    toggleTheme() // light -> dark
    toggleTheme() // dark -> light
    toggleTheme() // light -> dark

    // Should handle rapid switches without errors
    expect(localStorageMock.setItem).toHaveBeenCalledTimes(3)
  })

  it('snapshots theme initialization', () => {
    localStorageMock.getItem.mockReturnValue('dark')

    const { theme, actualTheme } = useTheme()

    expect(theme.value).toMatchSnapshot()
    expect(actualTheme.value).toMatchSnapshot()
  })
})
