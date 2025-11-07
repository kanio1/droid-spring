import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import AppCard from '~/components/ui/AppCard.vue'

describe('AppCard', () => {
  it('renders with default props', () => {
    const wrapper = mount(AppCard, {
      props: { title: 'Default Card' }
    })

    expect(wrapper.text()).toContain('Default Card')
    expect(wrapper.classes()).toContain('card--default')
    expect(wrapper.classes()).toContain('card--normal')
  })

  it('applies variant classes correctly', () => {
    const variants = ['default', 'bordered', 'elevated', 'flat'] as const

    variants.forEach(variant => {
      const wrapper = mount(AppCard, {
        props: { title: `Card ${variant}`, variant }
      })

      expect(wrapper.classes()).toContain(`card--${variant}`)
    })
  })

  it('applies padding classes correctly', () => {
    const paddings = ['none', 'small', 'normal', 'large'] as const

    paddings.forEach(padding => {
      const wrapper = mount(AppCard, {
        props: { title: `Card ${padding}`, padding }
      })

      expect(wrapper.classes()).toContain(`card--${padding}`)
    })
  })

  it('applies hoverable class when hoverable prop is true', () => {
    const wrapper = mount(AppCard, {
      props: { title: 'Hoverable', hoverable: true }
    })

    expect(wrapper.classes()).toContain('card--hoverable')
  })

  it('applies clickable class when clickable prop is true', () => {
    const wrapper = mount(AppCard, {
      props: { title: 'Clickable', clickable: true }
    })

    expect(wrapper.classes()).toContain('card--clickable')
  })

  it('emits click event when clicked and clickable', async () => {
    const wrapper = mount(AppCard, {
      props: { title: 'Clickable', clickable: true }
    })

    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toHaveLength(1)
  })

  it('does not emit click when not clickable', async () => {
    const wrapper = mount(AppCard, {
      props: { title: 'Not Clickable' }
    })

    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toBeUndefined()
  })

  it('emits click event when clicked and hoverable', async () => {
    const wrapper = mount(AppCard, {
      props: { title: 'Hoverable', hoverable: true }
    })

    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toHaveLength(1)
  })

  it('handles click with custom handler', async () => {
    const clickHandler = vi.fn()
    const wrapper = mount(AppCard, {
      props: { title: 'Clickable', clickable: true, onClick: clickHandler }
    })

    await wrapper.trigger('click')
    expect(clickHandler).toHaveBeenCalledTimes(1)
  })

  it('renders title in header slot', () => {
    const wrapper = mount(AppCard, {
      props: { title: 'Card Title' }
    })

    const header = wrapper.find('.card__header')
    expect(header.exists()).toBe(true)
    expect(header.text()).toContain('Card Title')
  })

  it('renders custom title via slot', () => {
    const wrapper = mount(AppCard, {
      slots: {
        title: '<h2>Custom Title</h2>'
      }
    })

    const header = wrapper.find('.card__header')
    expect(header.html()).toContain('Custom Title')
  })

  it('renders header slot content', () => {
    const wrapper = mount(AppCard, {
      slots: {
        header: '<div class="custom-header">Header Content</div>'
      }
    })

    const headerSlot = wrapper.find('.custom-header')
    expect(headerSlot.exists()).toBe(true)
    expect(headerSlot.text()).toContain('Header Content')
  })

  it('renders default slot content in body', () => {
    const wrapper = mount(AppCard, {
      slots: {
        default: '<p>Card body content</p>'
      }
    })

    const body = wrapper.find('.card__body')
    expect(body.html()).toContain('Card body content')
  })

  it('renders footer slot content', () => {
    const wrapper = mount(AppCard, {
      slots: {
        footer: '<button>Action</button>'
      }
    })

    const footer = wrapper.find('.card__footer')
    expect(footer.exists()).toBe(true)
    expect(footer.html()).toContain('Action')
  })

  it('does not render header when no title and no header slot', () => {
    const wrapper = mount(AppCard, {
      slots: {
        default: '<p>Content</p>'
      }
    })

    const header = wrapper.find('.card__header')
    expect(header.exists()).toBe(false)
  })

  it('does not render footer when no footer slot', () => {
    const wrapper = mount(AppCard, {
      slots: {
        default: '<p>Content</p>'
      }
    })

    const footer = wrapper.find('.card__footer')
    expect(footer.exists()).toBe(false)
  })

  it('applies custom class via class prop', () => {
    const wrapper = mount(AppCard, {
      props: { title: 'Custom', class: 'custom-card' }
    })

    expect(wrapper.classes()).toContain('custom-card')
  })

  it('applies id when provided', () => {
    const wrapper = mount(AppCard, {
      props: { title: 'ID', id: 'card-id' }
    })

    const card = wrapper.find('.card')
    expect(card.attributes('id')).toBe('card-id')
  })

  it('generates id when not provided', () => {
    const wrapper = mount(AppCard, {
      props: { title: 'Auto ID' }
    })

    const card = wrapper.find('.card')
    expect(card.attributes('id')).toBeDefined()
  })

  it('applies data-testid attribute', () => {
    const wrapper = mount(AppCard, {
      props: { title: 'Test', testId: 'custom-card' }
    })

    const card = wrapper.find('.card')
    expect(card.attributes('data-testid')).toBe('custom-card')
  })

  it('handles loading state', () => {
    const wrapper = mount(AppCard, {
      props: { title: 'Loading', loading: true }
    })

    expect(wrapper.classes()).toContain('card--loading')
    const spinner = wrapper.find('.card__spinner')
    expect(spinner.exists()).toBe(true)
  })

  it('hides content when loading', () => {
    const wrapper = mount(AppCard, {
      props: { title: 'Loading', loading: true },
      slots: {
        default: '<p>Hidden content</p>'
      }
    })

    const body = wrapper.find('.card__body')
    expect(body.classes()).toContain('card__body--loading')
  })

  it('shows loading overlay', () => {
    const wrapper = mount(AppCard, {
      props: { title: 'Loading', loading: true }
    })

    const overlay = wrapper.find('.card__loading')
    expect(overlay.exists()).toBe(true)
  })

  it('applies disabled class when disabled', () => {
    const wrapper = mount(AppCard, {
      props: { title: 'Disabled', disabled: true }
    })

    expect(wrapper.classes()).toContain('card--disabled')
  })

  it('does not emit click when disabled', async () => {
    const wrapper = mount(AppCard, {
      props: { title: 'Disabled', disabled: true, clickable: true }
    })

    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toBeUndefined()
  })

  it('applies gradient background when gradient prop is true', () => {
    const wrapper = mount(AppCard, {
      props: { title: 'Gradient', gradient: true }
    })

    expect(wrapper.classes()).toContain('card--gradient')
  })

  it('applies no-border class when noBorder prop is true', () => {
    const wrapper = mount(AppCard, {
      props: { title: 'No Border', noBorder: true }
    })

    expect(wrapper.classes()).toContain('card--no-border')
  })

  it('applies no-padding class when noPadding prop is true', () => {
    const wrapper = mount(AppCard, {
      props: { title: 'No Padding', noPadding: true }
    })

    expect(wrapper.classes()).toContain('card--no-padding')
  })

  it('combines multiple modifiers', () => {
    const wrapper = mount(AppCard, {
      props: {
        title: 'Multi',
        variant: 'elevated',
        padding: 'large',
        hoverable: true
      }
    })

    expect(wrapper.classes()).toContain('card--elevated')
    expect(wrapper.classes()).toContain('card--large')
    expect(wrapper.classes()).toContain('card--hoverable')
  })

  it('renders with icon in title', () => {
    const wrapper = mount(AppCard, {
      props: { title: 'Icon Card', icon: 'lucide:star' }
    })

    const icon = wrapper.find('svg')
    expect(icon.exists()).toBe(true)
  })

  it('renders action slot in header', () => {
    const wrapper = mount(AppCard, {
      slots: {
        action: '<button>Action</button>'
      }
    })

    const action = wrapper.find('.card__action')
    expect(action.exists()).toBe(true)
    expect(action.html()).toContain('Action')
  })

  it('applies full height when fullHeight prop is true', () => {
    const wrapper = mount(AppCard, {
      props: { title: 'Full Height', fullHeight: true }
    })

    expect(wrapper.classes()).toContain('card--full-height')
  })

  it('applies full width when fullWidth prop is true', () => {
    const wrapper = mount(AppCard, {
      props: { title: 'Full Width', fullWidth: true }
    })

    expect(wrapper.classes()).toContain('card--full-width')
  })

  it('renders correctly in dark mode', () => {
    document.documentElement.setAttribute('data-theme', 'dark')

    const wrapper = mount(AppCard, {
      props: { title: 'Dark Mode', variant: 'default' }
    })

    expect(wrapper.text()).toContain('Dark Mode')
    expect(wrapper.classes()).toContain('card--default')

    document.documentElement.removeAttribute('data-theme')
  })

  it('applies elevation styles correctly', () => {
    const wrapper = mount(AppCard, {
      props: { title: 'Elevated', variant: 'elevated' }
    })

    const card = wrapper.find('.card')
    expect(card.classes()).toContain('card--elevated')
    // Shadow styles would be tested with visual tests
  })

  it('handles empty state', () => {
    const wrapper = mount(AppCard)

    // Should not crash with minimal props
    expect(wrapper.find('.card').exists()).toBe(true)
  })

  it('renders subtitle in header', () => {
    const wrapper = mount(AppCard, {
      props: { title: 'Title', subtitle: 'Subtitle' }
    })

    const subtitle = wrapper.find('.card__subtitle')
    expect(subtitle.exists()).toBe(true)
    expect(subtitle.text()).toBe('Subtitle')
  })

  it('applies header padding variant', () => {
    const wrapper = mount(AppCard, {
      props: { title: 'Title', headerPadding: 'small' }
    })

    const header = wrapper.find('.card__header')
    expect(header.classes()).toContain('card__header--small')
  })

  it('applies body padding variant', () => {
    const wrapper = mount(AppCard, {
      props: { title: 'Title', bodyPadding: 'large' }
    })

    const body = wrapper.find('.card__body')
    expect(body.classes()).toContain('card__body--large')
  })

  it('snapshots in different states', () => {
    const states = [
      { title: 'Default', variant: 'default' },
      { title: 'Bordered', variant: 'bordered' },
      { title: 'Elevated', variant: 'elevated' },
      { title: 'Flat', variant: 'flat' },
      { title: 'Hoverable', hoverable: true },
      { title: 'Clickable', clickable: true },
      { title: 'Loading', loading: true },
      { title: 'Disabled', disabled: true },
      { title: 'Gradient', gradient: true }
    ]

    states.forEach(state => {
      const wrapper = mount(AppCard, { props: state })
      expect(wrapper.element).toMatchSnapshot()
    })
  })
})
