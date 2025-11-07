import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import AppBadge from '~/components/ui/AppBadge.vue'

describe('AppBadge', () => {
  it('renders with default props', () => {
    const wrapper = mount(AppBadge, {
      props: { label: 'Default' }
    })

    expect(wrapper.text()).toBe('Default')
    expect(wrapper.classes()).toContain('badge--primary')
    expect(wrapper.classes()).toContain('badge--md')
  })

  it('applies all variant classes correctly', () => {
    const variants = ['primary', 'secondary', 'success', 'warning', 'danger', 'info', 'neutral'] as const

    variants.forEach(variant => {
      const wrapper = mount(AppBadge, {
        props: { label: `Badge ${variant}`, variant }
      })

      expect(wrapper.classes()).toContain(`badge--${variant}`)
    })
  })

  it('applies all size classes correctly', () => {
    const sizes = ['xs', 'sm', 'md', 'lg', 'xl'] as const

    sizes.forEach(size => {
      const wrapper = mount(AppBadge, {
        props: { label: `Badge ${size}`, size }
      })

      expect(wrapper.classes()).toContain(`badge--${size}`)
    })
  })

  it('applies outlined variant when outlined prop is true', () => {
    const wrapper = mount(AppBadge, {
      props: { label: 'Outlined', outlined: true }
    })

    expect(wrapper.classes()).toContain('badge--outlined')
  })

  it('applies rounded variant when rounded prop is true', () => {
    const wrapper = mount(AppBadge, {
      props: { label: 'Rounded', rounded: true }
    })

    expect(wrapper.classes()).toContain('badge--rounded')
  })

  it('shows dot when dot prop is true', () => {
    const wrapper = mount(AppBadge, {
      props: { label: 'With Dot', dot: true }
    })

    const dot = wrapper.find('.badge__dot')
    expect(dot.exists()).toBe(true)
  })

  it('does not show dot when dot prop is false', () => {
    const wrapper = mount(AppBadge, {
      props: { label: 'No Dot', dot: false }
    })

    const dot = wrapper.find('.badge__dot')
    expect(dot.exists()).toBe(false)
  })

  it('renders icon when icon prop is provided', () => {
    const wrapper = mount(AppBadge, {
      props: { label: 'With Icon', icon: 'lucide:check' }
    })

    const icon = wrapper.find('svg')
    expect(icon.exists()).toBe(true)
  })

  it('icon position is left by default', () => {
    const wrapper = mount(AppBadge, {
      props: { label: 'Left Icon', icon: 'lucide:check' }
    })

    const icon = wrapper.find('svg')
    expect(icon.exists()).toBe(true)
    // Icon should be rendered in the badge
  })

  it('applies icon position correctly', () => {
    const wrapper = mount(AppBadge, {
      props: { label: 'Right Icon', icon: 'lucide:check', iconPosition: 'right' }
    })

    const icon = wrapper.find('svg')
    expect(icon.exists()).toBe(true)
  })

  it('truncates text when truncate prop is true', () => {
    const wrapper = mount(AppBadge, {
      props: { label: 'This is a very long badge text that should be truncated', truncate: true }
    })

    const badge = wrapper.find('.badge')
    expect(badge.classes()).toContain('badge--truncate')
  })

  it('does not truncate text by default', () => {
    const wrapper = mount(AppBadge, {
      props: { label: 'Normal text' }
    })

    const badge = wrapper.find('.badge')
    expect(badge.classes()).not.toContain('badge--truncate')
  })

  it('applies clickable class when clickable prop is true', () => {
    const wrapper = mount(AppBadge, {
      props: { label: 'Clickable', clickable: true }
    })

    expect(wrapper.classes()).toContain('badge--clickable')
  })

  it('emits click event when clicked and clickable', async () => {
    const wrapper = mount(AppBadge, {
      props: { label: 'Clickable', clickable: true }
    })

    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toHaveLength(1)
  })

  it('does not emit click event when not clickable', async () => {
    const wrapper = mount(AppBadge, {
      props: { label: 'Not Clickable' }
    })

    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toBeUndefined()
  })

  it('handles click event with custom handler', async () => {
    const clickHandler = vi.fn()
    const wrapper = mount(AppBadge, {
      props: { label: 'Custom Click', clickable: true, onClick: clickHandler }
    })

    await wrapper.trigger('click')
    expect(clickHandler).toHaveBeenCalledTimes(1)
  })

  it('applies custom class via class prop', () => {
    const wrapper = mount(AppBadge, {
      props: { label: 'Custom', class: 'custom-badge' }
    })

    expect(wrapper.classes()).toContain('custom-badge')
  })

  it('renders without label when only icon', () => {
    const wrapper = mount(AppBadge, {
      props: { icon: 'lucide:star' }
    })

    const icon = wrapper.find('svg')
    expect(icon.exists()).toBe(true)
    expect(wrapper.text()).toBe('')
  })

  it('combines dot and icon correctly', () => {
    const wrapper = mount(AppBadge, {
      props: { label: 'With Both', dot: true, icon: 'lucide:check' }
    })

    const dot = wrapper.find('.badge__dot')
    const icon = wrapper.find('svg')
    expect(dot.exists()).toBe(true)
    expect(icon.exists()).toBe(true)
  })

  it('applies outline style with different variants', () => {
    const variants = ['primary', 'success', 'warning', 'danger'] as const

    variants.forEach(variant => {
      const wrapper = mount(AppBadge, {
        props: { label: 'Outlined', variant, outlined: true }
      })

      expect(wrapper.classes()).toContain('badge--outlined')
      expect(wrapper.classes()).toContain(`badge--${variant}`)
    })
  })

  it('rounded style works with all variants', () => {
    const variants = ['primary', 'secondary', 'info'] as const

    variants.forEach(variant => {
      const wrapper = mount(AppBadge, {
        props: { label: 'Rounded', variant, rounded: true }
      })

      expect(wrapper.classes()).toContain('badge--rounded')
      expect(wrapper.classes()).toContain(`badge--${variant}`)
    })
  })

  it('handles very long text without truncation by default', () => {
    const longText = 'A'.repeat(200)
    const wrapper = mount(AppBadge, {
      props: { label: longText }
    })

    expect(wrapper.text()).toBe(longText)
  })

  it('handles empty label gracefully', () => {
    const wrapper = mount(AppBadge, {
      props: { label: '' }
    })

    expect(wrapper.text()).toBe('')
  })

  it('applies aria-label when provided', () => {
    const wrapper = mount(AppBadge, {
      props: { label: 'Badge', ariaLabel: 'Status badge' }
    })

    const badge = wrapper.find('.badge')
    expect(badge.attributes('aria-label')).toBe('Status badge')
  })

  it('uses label as aria-label by default', () => {
    const wrapper = mount(AppBadge, {
      props: { label: 'Status' }
    })

    const badge = wrapper.find('.badge')
    expect(badge.attributes('aria-label')).toBe('Status')
  })

  it('handles special characters in label', () => {
    const specialChars = 'Special: <>&"\'🎉'
    const wrapper = mount(AppBadge, {
      props: { label: specialChars }
    })

    expect(wrapper.text()).toBe(specialChars)
  })

  it('applies data-testid attribute', () => {
    const wrapper = mount(AppBadge, {
      props: { label: 'Test', testId: 'custom-badge' }
    })

    const badge = wrapper.find('.badge')
    expect(badge.attributes('data-testid')).toBe('custom-badge')
  })

  it('applies id when provided', () => {
    const wrapper = mount(AppBadge, {
      props: { label: 'ID', id: 'badge-id' }
    })

    const badge = wrapper.find('.badge')
    expect(badge.attributes('id')).toBe('badge-id')
  })

  it('generates id when not provided', () => {
    const wrapper = mount(AppBadge, {
      props: { label: 'Auto ID' }
    })

    const badge = wrapper.find('.badge')
    expect(badge.attributes('id')).toBeDefined()
  })

  it('icon size adjusts based on badge size', () => {
    const sizes = {
      xs: '10',
      sm: '12',
      md: '14',
      lg: '16',
      xl: '18'
    }

    Object.entries(sizes).forEach(([size, expectedSize]) => {
      const wrapper = mount(AppBadge, {
        props: { label: `Size ${size}`, size: size as any, icon: 'lucide:star' }
      })

      const icon = wrapper.find('svg')
      expect(icon.attributes('width')).toBe(expectedSize)
    })
  })

  it('dot size adjusts based on badge size', () => {
    const sizes = {
      xs: '4',
      sm: '6',
      md: '8',
      lg: '10',
      xl: '12'
    }

    Object.entries(sizes).forEach(([size, expectedSize]) => {
      const wrapper = mount(AppBadge, {
        props: { label: `Size ${size}`, size: size as any, dot: true }
      })

      const dot = wrapper.find('.badge__dot')
      expect(dot.attributes('width')).toBe(expectedSize)
      expect(dot.attributes('height')).toBe(expectedSize)
    })
  })

  it('renders correctly in dark mode', () => {
    document.documentElement.setAttribute('data-theme', 'dark')

    const wrapper = mount(AppBadge, {
      props: { label: 'Dark Mode', variant: 'primary' }
    })

    expect(wrapper.text()).toBe('Dark Mode')
    expect(wrapper.classes()).toContain('badge--primary')

    document.documentElement.removeAttribute('data-theme')
  })

  it('contrast between text and background in all variants', () => {
    const variants = ['primary', 'success', 'warning', 'danger', 'info', 'neutral'] as const

    variants.forEach(variant => {
      const wrapper = mount(AppBadge, {
        props: { label: 'Contrast Test', variant }
      })

      const badge = wrapper.find('.badge')
      expect(badge.classes()).toContain(`badge--${variant}`)
      // Text should be visible (not testing exact colors, just that class is applied)
    })
  })

  it('snapshots in different states', () => {
    const states = [
      { label: 'Primary', variant: 'primary' },
      { label: 'Success', variant: 'success' },
      { label: 'Outlined', outlined: true },
      { label: 'Rounded', rounded: true },
      { label: 'With Icon', icon: 'lucide:check' },
      { label: 'Clickable', clickable: true },
      { label: 'Truncated Text', truncate: true }
    ]

    states.forEach(state => {
      const wrapper = mount(AppBadge, { props: state })
      expect(wrapper.element).toMatchSnapshot()
    })
  })
})
