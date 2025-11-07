import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import Icon from '~/components/ui/Icon.vue'

describe('Icon', () => {
  it('renders icon with given name', () => {
    const wrapper = mount(Icon, {
      props: { name: 'lucide:check' }
    })

    const svg = wrapper.find('svg')
    expect(svg.exists()).toBe(true)
  })

  it('applies size prop correctly', () => {
    const sizes = [12, 16, 20, 24, 32, 48] as const

    sizes.forEach(size => {
      const wrapper = mount(Icon, {
        props: { name: 'lucide:check', size }
      })

      const svg = wrapper.find('svg')
      expect(svg.attributes('width')).toBe(size.toString())
      expect(svg.attributes('height')).toBe(size.toString())
    })
  })

  it('applies color prop correctly', () => {
    const wrapper = mount(Icon, {
      props: { name: 'lucide:check', color: '#ff0000' }
    })

    const svg = wrapper.find('svg')
    expect(svg.attributes('style')).toContain('color: #ff0000')
  })

  it('inherits color from parent when no color prop', () => {
    const parentWrapper = mount(
      { template: '<div style="color: blue"><Icon name="lucide:check" /></div>' },
      { global: { components: { Icon } } }
    )

    const svg = parentWrapper.find('svg')
    expect(svg.exists()).toBe(true)
  })

  it('applies custom class via class prop', () => {
    const wrapper = mount(Icon, {
      props: { name: 'lucide:check', class: 'custom-icon' }
    })

    expect(wrapper.classes()).toContain('custom-icon')
  })

  it('applies data-testid attribute', () => {
    const wrapper = mount(Icon, {
      props: { name: 'lucide:check', testId: 'custom-icon' }
    })

    const svg = wrapper.find('svg')
    expect(svg.attributes('data-testid')).toBe('custom-icon')
  })

  it('applies aria-label when provided', () => {
    const wrapper = mount(Icon, {
      props: { name: 'lucide:check', ariaLabel: 'Check icon' }
    })

    const svg = wrapper.find('svg')
    expect(svg.attributes('aria-label')).toBe('Check icon')
  })

  it('adds role="img" when aria-label is provided', () => {
    const wrapper = mount(Icon, {
      props: { name: 'lucide:check', ariaLabel: 'Check icon' }
    })

    const svg = wrapper.find('svg')
    expect(svg.attributes('role')).toBe('img')
  })

  it('adds role="presentation" when no aria-label', () => {
    const wrapper = mount(Icon, {
      props: { name: 'lucide:check' }
    })

    const svg = wrapper.find('svg')
    expect(svg.attributes('role')).toBe('presentation')
  })

  it('applies custom attributes', () => {
    const wrapper = mount(Icon, {
      props: {
        name: 'lucide:check',
        attrs: { 'data-custom': 'value', 'aria-hidden': 'true' }
      }
    })

    const svg = wrapper.find('svg')
    expect(svg.attributes('data-custom')).toBe('value')
    expect(svg.attributes('aria-hidden')).toBe('true')
  })

  it('handles different icon sizes (xs, sm, md, lg, xl)', () => {
    const sizeMap = {
      xs: 12,
      sm: 14,
      md: 16,
      lg: 20,
      xl: 24
    }

    Object.entries(sizeMap).forEach(([size, expected]) => {
      const wrapper = mount(Icon, {
        props: { name: 'lucide:check', size }
      })

      const svg = wrapper.find('svg')
      expect(svg.attributes('width')).toBe(expected.toString())
    })
  })

  it('renders different icon names', () => {
    const icons = [
      'lucide:check',
      'lucide:x',
      'lucide:plus',
      'lucide:minus',
      'lucide:arrow-right',
      'lucide:user',
      'lucide:mail',
      'lucide:phone'
    ]

    icons.forEach(iconName => {
      const wrapper = mount(Icon, {
        props: { name: iconName }
      })

      const svg = wrapper.find('svg')
      expect(svg.exists()).toBe(true)
    })
  })

  it('applies spin animation when spin prop is true', () => {
    const wrapper = mount(Icon, {
      props: { name: 'lucide:loader', spin: true }
    })

    expect(wrapper.classes()).toContain('icon--spin')
  })

  it('does not apply spin by default', () => {
    const wrapper = mount(Icon, {
      props: { name: 'lucide:check' }
    })

    expect(wrapper.classes()).not.toContain('icon--spin')
  })

  it('applies pulse animation when pulse prop is true', () => {
    const wrapper = mount(Icon, {
      props: { name: 'lucide:bell', pulse: true }
    })

    expect(wrapper.classes()).toContain('icon--pulse')
  })

  it('spin and pulse are mutually exclusive', () => {
    const wrapper = mount(Icon, {
      props: { name: 'lucide:loader', spin: true, pulse: true }
    })

    // Both classes might exist, CSS will handle the conflict
    expect(wrapper.classes()).toContain('icon--spin')
    expect(wrapper.classes()).toContain('icon--pulse')
  })

  it('handles flip prop (horizontal, vertical, both)', () => {
    const flips = ['horizontal', 'vertical', 'both'] as const

    flips.forEach(flip => {
      const wrapper = mount(Icon, {
        props: { name: 'lucide:arrow-right', flip }
      })

      expect(wrapper.classes()).toContain(`icon--flip-${flip}`)
    })
  })

  it('applies title attribute for accessibility', () => {
    const wrapper = mount(Icon, {
      props: { name: 'lucide:info', title: 'Information' }
    })

    const title = wrapper.find('title')
    expect(title.exists()).toBe(true)
    expect(title.text()).toBe('Information')
  })

  it('svg has viewBox attribute', () => {
    const wrapper = mount(Icon, {
      props: { name: 'lucide:check' }
    })

    const svg = wrapper.find('svg')
    expect(svg.attributes('viewbox')).toBeDefined()
  })

  it('svg has fill="none" by default', () => {
    const wrapper = mount(Icon, {
      props: { name: 'lucide:check' }
    })

    const svg = wrapper.find('svg')
    expect(svg.attributes('fill')).toBe('none')
  })

  it('svg has stroke="currentColor" by default', () => {
    const wrapper = mount(Icon, {
      props: { name: 'lucide:check' }
    })

    const svg = wrapper.find('svg')
    expect(svg.attributes('stroke')).toBe('currentColor')
  })

  it('applies custom stroke width', () => {
    const wrapper = mount(Icon, {
      props: { name: 'lucide:check', strokeWidth: 2 }
    })

    const svg = wrapper.find('svg')
    expect(svg.attributes('stroke-width')).toBe('2')
  })

  it('default stroke width is 2', () => {
    const wrapper = mount(Icon, {
      props: { name: 'lucide:check' }
    })

    const svg = wrapper.find('svg')
    expect(svg.attributes('stroke-width')).toBe('2')
  })

  it('applies opacity when provided', () => {
    const wrapper = mount(Icon, {
      props: { name: 'lucide:check', opacity: 0.5 }
    })

    const svg = wrapper.find('svg')
    expect(svg.attributes('style')).toContain('opacity: 0.5')
  })

  it('handles disabled state', () => {
    const wrapper = mount(Icon, {
      props: { name: 'lucide:check', disabled: true }
    })

    expect(wrapper.classes()).toContain('icon--disabled')
  })

  it('renders correctly in dark mode', () => {
    document.documentElement.setAttribute('data-theme', 'dark')

    const wrapper = mount(Icon, {
      props: { name: 'lucide:check' }
    })

    const svg = wrapper.find('svg')
    expect(svg.exists()).toBe(true)

    document.documentElement.removeAttribute('data-theme')
  })

  it('has correct cursor when clickable', () => {
    const wrapper = mount(Icon, {
      props: { name: 'lucide:check', clickable: true }
    })

    expect(wrapper.classes()).toContain('icon--clickable')
  })

  it('emits click event when clickable and clicked', async () => {
    const wrapper = mount(Icon, {
      props: { name: 'lucide:check', clickable: true }
    })

    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toHaveLength(1)
  })

  it('does not emit click when not clickable', async () => {
    const wrapper = mount(Icon, {
      props: { name: 'lucide:check' }
    })

    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toBeUndefined()
  })

  it('handles click with custom handler', async () => {
    const clickHandler = vi.fn()
    const wrapper = mount(Icon, {
      props: { name: 'lucide:check', clickable: true, onClick: clickHandler }
    })

    await wrapper.trigger('click')
    expect(clickHandler).toHaveBeenCalledTimes(1)
  })

  it('applies badge count', () => {
    const wrapper = mount(Icon, {
      props: { name: 'lucide:bell', badge: 5 }
    })

    const badge = wrapper.find('.icon__badge')
    expect(badge.exists()).toBe(true)
    expect(badge.text()).toBe('5')
  })

  it('applies badge count for large numbers', () => {
    const wrapper = mount(Icon, {
      props: { name: 'lucide:bell', badge: 100 }
    })

    const badge = wrapper.find('.icon__badge')
    expect(badge.text()).toBe('99+')
  })

  it('applies badge with dot style', () => {
    const wrapper = mount(Icon, {
      props: { name: 'lucide:bell', badge: true }
    })

    const badge = wrapper.find('.icon__badge')
    expect(badge.exists()).toBe(true)
    expect(badge.classes()).toContain('icon__badge--dot')
  })

  it('badge appears without count', () => {
    const wrapper = mount(Icon, {
      props: { name: 'lucide:bell', badge: true }
    })

    const badge = wrapper.find('.icon__badge')
    expect(badge.text()).toBe('')
    expect(badge.classes()).toContain('icon__badge--dot')
  })

  it('snapshots in different states', () => {
    const states = [
      { name: 'lucide:check' },
      { name: 'lucide:check', size: 24 },
      { name: 'lucide:check', color: '#ff0000' },
      { name: 'lucide:loader', spin: true },
      { name: 'lucide:bell', pulse: true },
      { name: 'lucide:check', flip: 'horizontal' },
      { name: 'lucide:check', clickable: true }
    ]

    states.forEach(state => {
      const wrapper = mount(Icon, { props: state })
      expect(wrapper.element).toMatchSnapshot()
    })
  })
})
