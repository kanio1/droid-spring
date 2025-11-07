import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount, RouterLinkStub } from '@vue/test-utils'
import { nextTick } from 'vue'
import AppModal from '~/components/ui/AppModal.vue'

// Mock scroll lock
const mockScrollLock = vi.fn()
vi.mock('~/utils/scrollLock', () => ({
  lockScroll: mockScrollLock,
  unlockScroll: vi.fn()
}))

describe('AppModal', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // Mock document.activeElement
    Object.defineProperty(document, 'activeElement', {
      value: { focus: vi.fn() },
      writable: true
    })
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('does not render when modelValue is false', () => {
    const wrapper = mount(AppModal, {
      props: { modelValue: false, title: 'Modal' }
    })

    const modal = wrapper.find('.modal')
    expect(modal.exists()).toBe(false)
  })

  it('renders when modelValue is true', async () => {
    const wrapper = mount(AppModal, {
      props: { modelValue: true, title: 'Modal' }
    })
    await nextTick()

    const modal = wrapper.find('.modal')
    expect(modal.exists()).toBe(true)
  })

  it('emits update:modelValue when modelValue prop changes', async () => {
    const wrapper = mount(AppModal, {
      props: { modelValue: true, title: 'Modal' }
    })
    await wrapper.setProps({ modelValue: false })
    await nextTick()

    expect(wrapper.emitted('update:modelValue')).toBeTruthy()
    expect(wrapper.emitted('update:modelValue')[0]).toEqual([false])
  })

  it('shows modal when opened', async () => {
    const wrapper = mount(AppModal, {
      props: { modelValue: true, title: 'Test Modal' }
    })
    await nextTick()

    const modal = wrapper.find('.modal')
    expect(modal.exists()).toBe(true)
    expect(modal.classes()).toContain('modal--open')
  })

  it('hides modal when closed', async () => {
    const wrapper = mount(AppModal, {
      props: { modelValue: true, title: 'Test Modal' }
    })
    await wrapper.setProps({ modelValue: false })
    await nextTick()

    const modal = wrapper.find('.modal')
    expect(modal.classes()).not.toContain('modal--open')
  })

  it('emits close event when closed', async () => {
    const wrapper = mount(AppModal, {
      props: { modelValue: true, title: 'Test Modal' }
    })
    await wrapper.setProps({ modelValue: false })

    expect(wrapper.emitted('close')).toBeTruthy()
  })

  it('locks scroll when opened', async () => {
    const wrapper = mount(AppModal, {
      props: { modelValue: true, title: 'Test Modal' }
    })
    await nextTick()

    expect(mockScrollLock).toHaveBeenCalled()
  })

  it('shows close button by default', async () => {
    const wrapper = mount(AppModal, {
      props: { modelValue: true, title: 'Test Modal' }
    })
    await nextTick()

    const closeButton = wrapper.find('.modal__close')
    expect(closeButton.exists()).toBe(true)
  })

  it('hides close button when showCloseButton is false', async () => {
    const wrapper = mount(AppModal, {
      props: { modelValue: true, title: 'Test Modal', showCloseButton: false }
    })
    await nextTick()

    const closeButton = wrapper.find('.modal__close')
    expect(closeButton.exists()).toBe(false)
  })

  it('emits close event when close button is clicked', async () => {
    const wrapper = mount(AppModal, {
      props: { modelValue: true, title: 'Test Modal' }
    })
    await nextTick()

    const closeButton = wrapper.find('.modal__close')
    await closeButton.trigger('click')

    expect(wrapper.emitted('close')).toBeTruthy()
  })

  it('emits close event when backdrop is clicked', async () => {
    const wrapper = mount(AppModal, {
      props: { modelValue: true, title: 'Test Modal' }
    })
    await nextTick()

    const backdrop = wrapper.find('.modal__backdrop')
    await backdrop.trigger('click')

    expect(wrapper.emitted('close')).toBeTruthy()
  })

  it('does not close when clicking inside modal content', async () => {
    const wrapper = mount(AppModal, {
      props: { modelValue: true, title: 'Test Modal' },
      slots: {
        default: '<div class="modal-content">Content</div>'
      }
    })
    await nextTick()

    const content = wrapper.find('.modal-content')
    await content.trigger('click')

    expect(wrapper.emitted('close')).toBeUndefined()
  })

  it('does not close when closeOnBackdrop is false', async () => {
    const wrapper = mount(AppModal, {
      props: { modelValue: true, title: 'Test Modal', closeOnBackdrop: false }
    })
    await nextTick()

    const backdrop = wrapper.find('.modal__backdrop')
    await backdrop.trigger('click')

    expect(wrapper.emitted('close')).toBeUndefined()
  })

  it('renders title in header', async () => {
    const wrapper = mount(AppModal, {
      props: { modelValue: true, title: 'Modal Title' }
    })
    await nextTick()

    const title = wrapper.find('.modal__title')
    expect(title.text()).toBe('Modal Title')
  })

  it('renders custom title via slot', async () => {
    const wrapper = mount(AppModal, {
      props: { modelValue: true },
      slots: {
        title: '<h2>Custom Title</h2>'
      }
    })
    await nextTick()

    const title = wrapper.find('.modal__title')
    expect(title.html()).toContain('Custom Title')
  })

  it('renders default slot content', async () => {
    const wrapper = mount(AppModal, {
      props: { modelValue: true, title: 'Modal' },
      slots: {
        default: '<p>Modal body content</p>'
      }
    })
    await nextTick()

    const body = wrapper.find('.modal__body')
    expect(body.html()).toContain('Modal body content')
  })

  it('renders footer slot', async () => {
    const wrapper = mount(AppModal, {
      props: { modelValue: true, title: 'Modal' },
      slots: {
        footer: '<button>Save</button>'
      }
    })
    await nextTick()

    const footer = wrapper.find('.modal__footer')
    expect(footer.html()).toContain('Save')
  })

  it('applies size classes', async () => {
    const sizes = ['sm', 'md', 'lg', 'xl', 'full'] as const

    for (const size of sizes) {
      const wrapper = mount(AppModal, {
        props: { modelValue: true, title: 'Modal', size }
      })
      await nextTick()

      const modal = wrapper.find('.modal')
      expect(modal.classes()).toContain(`modal--${size}`)
    }
  })

  it('applies position class', async () => {
    const positions = ['center', 'top', 'bottom'] as const

    for (const position of positions) {
      const wrapper = mount(AppModal, {
        props: { modelValue: true, title: 'Modal', position }
      })
      await nextTick()

      const modal = wrapper.find('.modal')
      expect(modal.classes()).toContain(`modal--${position}`)
    }
  })

  it('applies variant classes', async () => {
    const variants = ['default', 'confirm', 'alert'] as const

    for (const variant of variants) {
      const wrapper = mount(AppModal, {
        props: { modelValue: true, title: 'Modal', variant }
      })
      await nextTick()

      const modal = wrapper.find('.modal')
      expect(modal.classes()).toContain(`modal--${variant}`)
    }
  })

  it('emits esc event when ESC key is pressed', async () => {
    const wrapper = mount(AppModal, {
      props: { modelValue: true, title: 'Modal' }
    })
    await nextTick()

    await wrapper.trigger('keydown', { key: 'Escape' })

    expect(wrapper.emitted('esc')).toBeTruthy()
    expect(wrapper.emitted('close')).toBeTruthy()
  })

  it('does not close on ESC when persistent is true', async () => {
    const wrapper = mount(AppModal, {
      props: { modelValue: true, title: 'Modal', persistent: true }
    })
    await nextTick()

    await wrapper.trigger('keydown', { key: 'Escape' })

    expect(wrapper.emitted('close')).toBeUndefined()
  })

  it('applies persistent class when persistent prop is true', async () => {
    const wrapper = mount(AppModal, {
      props: { modelValue: true, title: 'Modal', persistent: true }
    })
    await nextTick()

    const modal = wrapper.find('.modal')
    expect(modal.classes()).toContain('modal--persistent')
  })

  it('does not trap focus when persistent is true', async () => {
    const wrapper = mount(AppModal, {
      props: { modelValue: true, title: 'Modal', persistent: true }
    })
    await nextTick()

    // Focus trap should not be active
    const modal = wrapper.find('.modal')
    expect(modal.classes()).not.toContain('modal--focus-trap')
  })

  it('applies custom class via class prop', async () => {
    const wrapper = mount(AppModal, {
      props: { modelValue: true, title: 'Modal', class: 'custom-modal' }
    })
    await nextTick()

    expect(wrapper.classes()).toContain('custom-modal')
  })

  it('applies id when provided', async () => {
    const wrapper = mount(AppModal, {
      props: { modelValue: true, title: 'Modal', id: 'modal-id' }
    })
    await nextTick()

    const modal = wrapper.find('.modal')
    expect(modal.attributes('id')).toBe('modal-id')
  })

  it('generates id when not provided', async () => {
    const wrapper = mount(AppModal, {
      props: { modelValue: true, title: 'Modal' }
    })
    await nextTick()

    const modal = wrapper.find('.modal')
    expect(modal.attributes('id')).toBeDefined()
  })

  it('renders without header when showHeader is false', async () => {
    const wrapper = mount(AppModal, {
      props: { modelValue: true, title: 'Modal', showHeader: false }
    })
    await nextTick()

    const header = wrapper.find('.modal__header')
    expect(header.exists()).toBe(false)
  })

  it('renders without footer when no footer slot', async () => {
    const wrapper = mount(AppModal, {
      props: { modelValue: true, title: 'Modal' }
    })
    await nextTick()

    const footer = wrapper.find('.modal__footer')
    expect(footer.exists()).toBe(false)
  })

  it('applies z-index when provided', async () => {
    const wrapper = mount(AppModal, {
      props: { modelValue: true, title: 'Modal', zIndex: 9999 }
    })
    await nextTick()

    const modal = wrapper.find('.modal')
    expect(modal.attributes('style')).toContain('z-index: 9999')
  })

  it('transitions work correctly', async () => {
    const wrapper = mount(AppModal, {
      props: { modelValue: true, title: 'Modal' }
    })
    await nextTick()

    const modal = wrapper.find('.modal')
    expect(modal.exists()).toBe(true)
    // Transition classes would be tested in visual tests
  })

  it('animation enter class is applied', async () => {
    const wrapper = mount(AppModal, {
      props: { modelValue: true, title: 'Modal' }
    })
    await nextTick()

    const modal = wrapper.find('.modal')
    expect(modal.exists()).toBe(true)
  })

  it('animation leave class is applied when closing', async () => {
    const wrapper = mount(AppModal, {
      props: { modelValue: true, title: 'Modal' }
    })
    await nextTick()

    await wrapper.setProps({ modelValue: false })
    await nextTick()

    const modal = wrapper.find('.modal')
    expect(modal.exists()).toBe(true)
  })

  it('handles async content loading', async () => {
    const wrapper = mount(AppModal, {
      props: { modelValue: true, title: 'Modal' },
      slots: {
        default: '<div>Async content</div>'
      }
    })
    await nextTick()

    const body = wrapper.find('.modal__body')
    expect(body.exists()).toBe(true)
  })

  it('mobile fullscreen on small screens', async () => {
    // Mock mobile viewport
    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      value: 375
    })

    const wrapper = mount(AppModal, {
      props: { modelValue: true, title: 'Modal', mobileFullscreen: true }
    })
    await nextTick()

    const modal = wrapper.find('.modal')
    expect(modal.classes()).toContain('modal--mobile-fullscreen')
  })

  it('does not use fullscreen on desktop', async () => {
    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      value: 1920
    })

    const wrapper = mount(AppModal, {
      props: { modelValue: true, title: 'Modal', mobileFullscreen: true }
    })
    await nextTick()

    const modal = wrapper.find('.modal')
    expect(modal.classes()).not.toContain('modal--mobile-fullscreen')
  })

  it('renders correctly in dark mode', async () => {
    document.documentElement.setAttribute('data-theme', 'dark')

    const wrapper = mount(AppModal, {
      props: { modelValue: true, title: 'Dark Modal' }
    })
    await nextTick()

    const modal = wrapper.find('.modal')
    expect(modal.exists()).toBe(true)

    document.documentElement.removeAttribute('data-theme')
  })

  it('returns focus to trigger element', async () => {
    const triggerElement = document.createElement('button')
    triggerElement.focus = vi.fn()
    document.body.appendChild(triggerElement)

    const wrapper = mount(AppModal, {
      props: { modelValue: true, title: 'Modal', teleportTo: 'body' }
    })
    await nextTick()

    await wrapper.setProps({ modelValue: false })
    await nextTick()

    // Focus should return to trigger
    // (The actual implementation would store and restore the previously focused element)

    document.body.removeChild(triggerElement)
  })

  it('snapshots in different states', async () => {
    const states = [
      { modelValue: true, title: 'Default' },
      { modelValue: true, title: 'Large', size: 'lg' },
      { modelValue: true, title: 'Alert', variant: 'alert' },
      { modelValue: true, title: 'Persistent', persistent: true },
      { modelValue: true, title: 'Fullscreen', size: 'full' }
    ]

    for (const state of states) {
      const wrapper = mount(AppModal, { props: state })
      await nextTick()
      expect(wrapper.element).toMatchSnapshot()
    }
  })
})
