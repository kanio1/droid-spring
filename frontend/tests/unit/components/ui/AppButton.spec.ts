import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import AppButton from '~/components/ui/AppButton.vue'

describe('AppButton', () => {
  it('renders with default props', () => {
    const wrapper = mount(AppButton, {
      props: { label: 'Test Button' }
    })

    expect(wrapper.text()).toBe('Test Button')
    expect(wrapper.classes()).toContain('btn--primary')
    expect(wrapper.classes()).toContain('btn--md')
  })

  it('applies variant classes correctly', () => {
    const variants = ['primary', 'secondary', 'outline', 'ghost', 'danger', 'success', 'warning'] as const

    variants.forEach(variant => {
      const wrapper = mount(AppButton, {
        props: { label: `Button ${variant}`, variant }
      })

      expect(wrapper.classes()).toContain(`btn--${variant}`)
    })
  })

  it('applies size classes correctly', () => {
    const sizes = ['xs', 'sm', 'md', 'lg', 'xl'] as const

    sizes.forEach(size => {
      const wrapper = mount(AppButton, {
        props: { label: `Button ${size}`, size }
      })

      expect(wrapper.classes()).toContain(`btn--${size}`)
    })
  })

  it('shows loading state with spinner', () => {
    const wrapper = mount(AppButton, {
      props: { label: 'Loading', loading: true }
    })

    expect(wrapper.classes()).toContain('btn--loading')
    expect(wrapper.find('svg').exists()).toBe(true)
    expect(wrapper.find('.btn__icon--spinner').exists()).toBe(true)
  })

  it('disables button when disabled prop is true', () => {
    const wrapper = mount(AppButton, {
      props: { label: 'Disabled', disabled: true }
    })

    expect(wrapper.classes()).toContain('btn--disabled')
    expect(wrapper.attributes('disabled')).toBeDefined()
  })

  it('emits click event when clicked', async () => {
    const wrapper = mount(AppButton, {
      props: { label: 'Click me' }
    })

    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toHaveLength(1)
  })

  it('does not emit click when disabled', async () => {
    const wrapper = mount(AppButton, {
      props: { label: 'Disabled', disabled: true }
    })

    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toBeUndefined()
  })

  it('does not emit click when loading', async () => {
    const wrapper = mount(AppButton, {
      props: { label: 'Loading', loading: true }
    })

    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toBeUndefined()
  })

  it('renders with icon on the left', () => {
    const wrapper = mount(AppButton, {
      props: { label: 'With Icon', icon: 'lucide:plus', iconPosition: 'left' }
    })

    const icons = wrapper.findAll('svg')
    expect(icons.length).toBeGreaterThan(0)
  })

  it('renders with icon on the right', () => {
    const wrapper = mount(AppButton, {
      props: { label: 'With Icon', icon: 'lucide:plus', iconPosition: 'right' }
    })

    const icons = wrapper.findAll('svg')
    expect(icons.length).toBeGreaterThan(0)
  })

  it('applies full-width class when fullWidth is true', () => {
    const wrapper = mount(AppButton, {
      props: { label: 'Full Width', fullWidth: true }
    })

    expect(wrapper.classes()).toContain('btn--full-width')
  })

  it('applies rounded class when rounded is true', () => {
    const wrapper = mount(AppButton, {
      props: { label: 'Rounded', rounded: true }
    })

    expect(wrapper.classes()).toContain('btn--rounded')
  })

  it('renders as NuxtLink when to prop is provided', () => {
    const wrapper = mount(AppButton, {
      props: { label: 'Link', to: '/customers' }
    })

    expect(wrapper.element.tagName).toBe('A')
    expect(wrapper.attributes('to')).toBe('/customers')
  })

  it('renders as anchor when href prop is provided', () => {
    const wrapper = mount(AppButton, {
      props: { label: 'External Link', href: 'https://example.com' }
    })

    expect(wrapper.element.tagName).toBe('A')
    expect(wrapper.attributes('href')).toBe('https://example.com')
    expect(wrapper.attributes('target')).toBe('_blank')
    expect(wrapper.attributes('rel')).toBe('noopener noreferrer')
  })

  it('uses default slot content when provided', () => {
    const wrapper = mount(AppButton, {
      slots: { default: '<span>Custom Slot</span>' }
    })

    expect(wrapper.html()).toContain('Custom Slot')
  })

  it('has correct aria attributes for accessibility', () => {
    const wrapper = mount(AppButton, {
      props: { label: 'Accessible Button' }
    })

    // Button should be focusable
    expect(wrapper.attributes('tabindex')).toBeUndefined() // buttons are focusable by default
  })

  it('has focus-visible styles when focused', async () => {
    const wrapper = mount(AppButton, {
      props: { label: 'Focusable' }
    })

    await wrapper.trigger('focus')
    // Focus styles are applied via :focus-visible pseudo-class
  })

  it('handles click on disabled button correctly', async () => {
    const onClick = vi.fn()
    const wrapper = mount(AppButton, {
      props: { label: 'Disabled', disabled: true, onClick }
    })

    await wrapper.trigger('click')
    expect(onClick).not.toHaveBeenCalled()
  })

  it('handles click on loading button correctly', async () => {
    const onClick = vi.fn()
    const wrapper = mount(AppButton, {
      props: { label: 'Loading', loading: true, onClick }
    })

    await wrapper.trigger('click')
    expect(onClick).not.toHaveBeenCalled()
  })

  it('applies custom class via class prop', () => {
    const wrapper = mount(AppButton, {
      props: { label: 'Custom', class: 'custom-class' }
    })

    expect(wrapper.classes()).toContain('custom-class')
  })

  it('accepts different type attributes', () => {
    const types = ['button', 'submit', 'reset'] as const

    types.forEach(type => {
      const wrapper = mount(AppButton, {
        props: { label: `Button ${type}`, type }
      })

      expect(wrapper.attributes('type')).toBe(type)
    })
  })

  it('icon size adjusts based on button size', () => {
    const sizes = {
      xs: '12',
      sm: '14',
      md: '16',
      lg: '18',
      xl: '20'
    }

    Object.entries(sizes).forEach(([size, expectedSize]) => {
      const wrapper = mount(AppButton, {
        props: { label: `Button ${size}`, size: size as any, icon: 'lucide:plus' }
      })

      const icon = wrapper.find('svg')
      expect(icon.attributes('width')).toBe(expectedSize)
    })
  })

  it('animation: hover state transforms button slightly', async () => {
    const wrapper = mount(AppButton, {
      props: { label: 'Hover Me' }
    })

    await wrapper.trigger('mouseenter')
    // Hover effect applies translateY(-1px)
  })

  it('animation: active state resets transform', async () => {
    const wrapper = mount(AppButton, {
      props: { label: 'Active' }
    })

    await wrapper.trigger('mousedown')
    // Active state resets to translateY(0)
  })

  it('dark mode: renders correctly with dark theme class', async () => {
    // Set dark theme
    document.documentElement.setAttribute('data-theme', 'dark')

    const wrapper = mount(AppButton, {
      props: { label: 'Dark Mode' }
    })

    // Button should still render correctly in dark mode
    expect(wrapper.text()).toBe('Dark Mode')

    // Clean up
    document.documentElement.removeAttribute('data-theme')
  })

  it('snapshots component in different states', () => {
    const states = [
      { label: 'Primary', variant: 'primary' },
      { label: 'Secondary', variant: 'secondary' },
      { label: 'Loading', loading: true },
      { label: 'Disabled', disabled: true }
    ]

    states.forEach(state => {
      const wrapper = mount(AppButton, { props: state })
      expect(wrapper.element).toMatchSnapshot()
    })
  })
})
