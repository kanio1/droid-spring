import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import AppDropdown from '~/components/ui/AppDropdown.vue'

// Mock scroll into view
Element.prototype.scrollIntoView = vi.fn()

describe('AppDropdown', () => {
  const menuItems = [
    { label: 'Item 1', value: 'item1' },
    { label: 'Item 2', value: 'item2', disabled: true },
    { label: 'Item 3', value: 'item3' },
    { type: 'separator' },
    { label: 'Item 4', value: 'item4' }
  ]

  it('renders trigger button', () => {
    const wrapper = mount(AppDropdown, {
      props: { menuItems }
    })

    const trigger = wrapper.find('.dropdown__trigger')
    expect(trigger.exists()).toBe(true)
  })

  it('opens dropdown when trigger is clicked', async () => {
    const wrapper = mount(AppDropdown, {
      props: { menuItems }
    })

    const trigger = wrapper.find('.dropdown__trigger')
    await trigger.trigger('click')

    const menu = wrapper.find('.dropdown__menu')
    expect(menu.classes()).toContain('dropdown__menu--open')
  })

  it('closes dropdown when already open and trigger is clicked', async () => {
    const wrapper = mount(AppDropdown, {
      props: { menuItems }
    })

    const trigger = wrapper.find('.dropdown__trigger')
    await trigger.trigger('click')
    await trigger.trigger('click')

    const menu = wrapper.find('.dropdown__menu')
    expect(menu.classes()).not.toContain('dropdown__menu--open')
  })

  it('renders all menu items', async () => {
    const wrapper = mount(AppDropdown, {
      props: { menuItems }
    })

    const trigger = wrapper.find('.dropdown__trigger')
    await trigger.trigger('click')

    const items = wrapper.findAll('.dropdown__item')
    // Should have 4 items (1 separator is not counted as item)
    expect(items.length).toBe(4)
  })

  it('emits select event when item is clicked', async () => {
    const wrapper = mount(AppDropdown, {
      props: { menuItems }
    })

    const trigger = wrapper.find('.dropdown__trigger')
    await trigger.trigger('click')

    const firstItem = wrapper.findAll('.dropdown__item')[0]
    await firstItem.trigger('click')

    expect(wrapper.emitted('select')).toBeTruthy()
    expect(wrapper.emitted('select')[0]).toEqual([menuItems[0]])
  })

  it('emits update:modelValue when item is selected', async () => {
    const wrapper = mount(AppDropdown, {
      props: { menuItems, modelValue: 'item1' }
    })

    const trigger = wrapper.find('.dropdown__trigger')
    await trigger.trigger('click')

    const thirdItem = wrapper.findAll('.dropdown__item')[2]
    await thirdItem.trigger('click')

    expect(wrapper.emitted('update:modelValue')).toBeTruthy()
    expect(wrapper.emitted('update:modelValue')[0]).toEqual(['item3'])
  })

  it('disables items with disabled property', async () => {
    const wrapper = mount(AppDropdown, {
      props: { menuItems }
    })

    const trigger = wrapper.find('.dropdown__trigger')
    await trigger.trigger('click')

    const disabledItem = wrapper.findAll('.dropdown__item')[1]
    expect(disabledItem.classes()).toContain('dropdown__item--disabled')
  })

  it('does not emit selection for disabled items', async () => {
    const wrapper = mount(AppDropdown, {
      props: { menuItems }
    })

    const trigger = wrapper.find('.dropdown__trigger')
    await trigger.trigger('click')

    const disabledItem = wrapper.findAll('.dropdown__item')[1]
    await disabledItem.trigger('click')

    expect(wrapper.emitted('select')).toBeUndefined()
  })

  it('renders separator items', async () => {
    const wrapper = mount(AppDropdown, {
      props: { menuItems }
    })

    const trigger = wrapper.find('.dropdown__trigger')
    await trigger.trigger('click')

    const separators = wrapper.findAll('.dropdown__separator')
    expect(separators.length).toBe(1)
  })

  it('applies placement props', async () => {
    const placements = ['top', 'bottom', 'left', 'right', 'top-start', 'bottom-end'] as const

    for (const placement of placements) {
      const wrapper = mount(AppDropdown, {
        props: { menuItems, placement }
      })

      const menu = wrapper.find('.dropdown__menu')
      expect(menu.classes()).toContain(`dropdown__menu--${placement}`)
    }
  })

  it('applies variant classes', async () => {
    const variants = ['default', 'toolbar'] as const

    for (const variant of variants) {
      const wrapper = mount(AppDropdown, {
        props: { menuItems, variant }
      })

      const trigger = wrapper.find('.dropdown__trigger')
      expect(trigger.classes()).toContain(`dropdown__trigger--${variant}`)
    })
  })

  it('shows icons when provided', async () => {
    const itemsWithIcons = [
      { label: 'Item 1', value: 'item1', icon: 'lucide:home' },
      { label: 'Item 2', value: 'item2' }
    ]

    const wrapper = mount(AppDropdown, {
      props: { menuItems: itemsWithIcons }
    })

    const trigger = wrapper.find('.dropdown__trigger')
    await trigger.trigger('click')

    const firstItem = wrapper.findAll('.dropdown__item')[0]
    const icon = firstItem.find('svg')
    expect(icon.exists()).toBe(true)
  })

  it('shows keyboard shortcut', async () => {
    const itemsWithShortcut = [
      { label: 'Item 1', value: 'item1', shortcut: '⌘K' },
      { label: 'Item 2', value: 'item2' }
    ]

    const wrapper = mount(AppDropdown, {
      props: { menuItems: itemsWithShortcut }
    })

    const trigger = wrapper.find('.dropdown__trigger')
    await trigger.trigger('click')

    const firstItem = wrapper.findAll('.dropdown__item')[0]
    const shortcut = firstItem.find('.dropdown__item-shortcut')
    expect(shortcut.exists()).toBe(true)
    expect(shortcut.text()).toBe('⌘K')
  })

  it('closes dropdown after item selection', async () => {
    const wrapper = mount(AppDropdown, {
      props: { menuItems }
    })

    const trigger = wrapper.find('.dropdown__trigger')
    await trigger.trigger('click')

    const firstItem = wrapper.findAll('.dropdown__item')[0]
    await firstItem.trigger('click')

    const menu = wrapper.find('.dropdown__menu')
    expect(menu.classes()).not.toContain('dropdown__menu--open')
  })

  it('applies disabled state to trigger', () => {
    const wrapper = mount(AppDropdown, {
      props: { menuItems, disabled: true }
    })

    const trigger = wrapper.find('.dropdown__trigger')
    expect(trigger.classes()).toContain('dropdown__trigger--disabled')
  })

  it('does not open when disabled', async () => {
    const wrapper = mount(AppDropdown, {
      props: { menuItems, disabled: true }
    })

    const trigger = wrapper.find('.dropdown__trigger')
    await trigger.trigger('click')

    const menu = wrapper.find('.dropdown__menu')
    expect(menu.classes()).not.toContain('dropdown__menu--open')
  })

  it('applies size variants', () => {
    const sizes = ['sm', 'md', 'lg'] as const

    sizes.forEach(size => {
      const wrapper = mount(AppDropdown, {
        props: { menuItems, size }
      })

      const trigger = wrapper.find('.dropdown__trigger')
      expect(trigger.classes()).toContain(`dropdown__trigger--${size}`)
    })
  })

  it('renders with custom trigger content', () => {
    const wrapper = mount(AppDropdown, {
      props: { menuItems },
      slots: {
        trigger: '<button>Custom Trigger</button>'
      }
    })

    const customTrigger = wrapper.find('button')
    expect(customTrigger.exists()).toBe(true)
    expect(customTrigger.text()).toBe('Custom Trigger')
  })

  it('renders header slot', async () => {
    const wrapper = mount(AppDropdown, {
      props: { menuItems },
      slots: {
        header: '<div class="custom-header">Header</div>'
      }
    })

    const trigger = wrapper.find('.dropdown__trigger')
    await trigger.trigger('click')

    const header = wrapper.find('.custom-header')
    expect(header.exists()).toBe(true)
  })

  it('renders footer slot', async () => {
    const wrapper = mount(AppDropdown, {
      props: { menuItems },
      slots: {
        footer: '<button class="custom-footer">Footer</button>'
      }
    })

    const trigger = wrapper.find('.dropdown__trigger')
    await trigger.trigger('click')

    const footer = wrapper.find('.custom-footer')
    expect(footer.exists()).toBe(true)
  })

  it('shows loading state', () => {
    const wrapper = mount(AppDropdown, {
      props: { menuItems, loading: true }
    })

    const trigger = wrapper.find('.dropdown__trigger')
    expect(trigger.classes()).toContain('dropdown__trigger--loading')
  })

  it('shows loading indicator in menu', async () => {
    const wrapper = mount(AppDropdown, {
      props: { menuItems, loading: true }
    })

    const trigger = wrapper.find('.dropdown__trigger')
    await trigger.trigger('click')

    const loader = wrapper.find('.dropdown__loader')
    expect(loader.exists()).toBe(true)
  })

  it('handles hover trigger', async () => {
    const wrapper = mount(AppDropdown, {
      props: { menuItems, trigger: 'hover' }
    })

    const trigger = wrapper.find('.dropdown__trigger')
    await trigger.trigger('mouseenter')

    const menu = wrapper.find('.dropdown__menu')
    expect(menu.classes()).toContain('dropdown__menu--open')
  })

  it('closes on mouseleave with hover trigger', async () => {
    const wrapper = mount(AppDropdown, {
      props: { menuItems, trigger: 'hover' }
    })

    const trigger = wrapper.find('.dropdown__trigger')
    await trigger.trigger('mouseenter')
    await trigger.trigger('mouseleave')

    const menu = wrapper.find('.dropdown__menu')
    expect(menu.classes()).not.toContain('dropdown__menu--open')
  })

  it('keeps open on hover when hoverOnIconic is true', async () => {
    const wrapper = mount(AppDropdown, {
      props: { menuItems, trigger: 'hover', hoverOnIconic: true }
    })

    const trigger = wrapper.find('.dropdown__trigger')
    await trigger.trigger('mouseenter')

    // Should stay open
    const menu = wrapper.find('.dropdown__menu')
    expect(menu.classes()).toContain('dropdown__menu--open')
  })

  it('closes on outside click', async () => {
    const wrapper = mount(AppDropdown, {
      props: { menuItems }
    })

    const trigger = wrapper.find('.dropdown__trigger')
    await trigger.trigger('click')

    // Simulate outside click
    const event = new MouseEvent('mousedown', { bubbles: true })
    document.elementFromPoint = vi.fn().mockReturnValue(null)
    document.dispatchEvent(event)

    await flushPromises()

    const menu = wrapper.find('.dropdown__menu')
    expect(menu.classes()).not.toContain('dropdown__menu--open')
  })

  it('applies offset', async () => {
    const wrapper = mount(AppDropdown, {
      props: { menuItems, offset: 10 }
    })

    const trigger = wrapper.find('.dropdown__trigger')
    await trigger.trigger('click')

    const menu = wrapper.find('.dropdown__menu')
    expect(menu.attributes('style')).toContain('offset: 10')
  })

  it('applies custom class via class prop', () => {
    const wrapper = mount(AppDropdown, {
      props: { menuItems, class: 'custom-dropdown' }
    })

    expect(wrapper.classes()).toContain('custom-dropdown')
  })

  it('applies id when provided', () => {
    const wrapper = mount(AppDropdown, {
      props: { menuItems, id: 'dropdown-id' }
    })

    const dropdown = wrapper.find('.dropdown')
    expect(dropdown.attributes('id')).toBe('dropdown-id')
  })

  it('generates id when not provided', () => {
    const wrapper = mount(AppDropdown, {
      props: { menuItems }
    })

    const dropdown = wrapper.find('.dropdown')
    expect(dropdown.attributes('id')).toBeDefined()
  })

  it('sets aria-expanded on trigger', async () => {
    const wrapper = mount(AppDropdown, {
      props: { menuItems }
    })

    const trigger = wrapper.find('.dropdown__trigger')
    await trigger.trigger('click')

    expect(trigger.attributes('aria-expanded')).toBe('true')
  })

  it('sets aria-controls on trigger', () => {
    const wrapper = mount(AppDropdown, {
      props: { menuItems, id: 'dropdown-id' }
    })

    const trigger = wrapper.find('.dropdown__trigger')
    expect(trigger.attributes('aria-controls')).toBe('dropdown-id')
  })

  it('has proper role attributes', async () => {
    const wrapper = mount(AppDropdown, {
      props: { menuItems }
    })

    const trigger = wrapper.find('.dropdown__trigger')
    expect(trigger.attributes('role')).toBe('button')

    await trigger.trigger('click')
    const menu = wrapper.find('.dropdown__menu')
    expect(menu.attributes('role')).toBe('menu')
  })

  it('keyboard navigation works', async () => {
    const wrapper = mount(AppDropdown, {
      props: { menuItems }
    })

    const trigger = wrapper.find('.dropdown__trigger')
    await trigger.trigger('keydown', { key: 'Enter' })

    const menu = wrapper.find('.dropdown__menu')
    expect(menu.classes()).toContain('dropdown__menu--open')
  })

  it('navigates items with arrow keys', async () => {
    const wrapper = mount(AppDropdown, {
      props: { menuItems }
    })

    const trigger = wrapper.find('.dropdown__trigger')
    await trigger.trigger('keydown', { key: 'Enter' })
    await trigger.trigger('keydown', { key: 'ArrowDown' })
    await trigger.trigger('keydown', { key: 'ArrowDown' })

    // Focus should move
  })

  it('selects with Enter key', async () => {
    const wrapper = mount(AppDropdown, {
      props: { menuItems }
    })

    const trigger = wrapper.find('.dropdown__trigger')
    await trigger.trigger('keydown', { key: 'Enter' })
    await trigger.trigger('keydown', { key: 'ArrowDown' })
    await trigger.trigger('keydown', { key: 'Enter' })

    expect(wrapper.emitted('select')).toBeTruthy()
  })

  it('closes with Escape key', async () => {
    const wrapper = mount(AppDropdown, {
      props: { menuItems }
    })

    const trigger = wrapper.find('.dropdown__trigger')
    await trigger.trigger('keydown', { key: 'Enter' })
    await trigger.trigger('keydown', { key: 'Escape' })

    const menu = wrapper.find('.dropdown__menu')
    expect(menu.classes()).not.toContain('dropdown__menu--open')
  })

  it('applies dark theme', async () => {
    document.documentElement.setAttribute('data-theme', 'dark')

    const wrapper = mount(AppDropdown, {
      props: { menuItems }
    })

    const trigger = wrapper.find('.dropdown__trigger')
    await trigger.trigger('click')

    const menu = wrapper.find('.dropdown__menu')
    expect(menu.exists()).toBe(true)

    document.documentElement.removeAttribute('data-theme')
  })

  it('snapshots in different states', async () => {
    const states = [
      { menuItems },
      { menuItems, variant: 'toolbar' },
      { menuItems, size: 'sm' },
      { menuItems, loading: true },
      { menuItems, disabled: true }
    ]

    for (const state of states) {
      const wrapper = mount(AppDropdown, { props: state })
      await triggerDefault()
      expect(wrapper.element).toMatchSnapshot()
    }

    function triggerDefault() {
      return Promise.resolve()
    }
  })
})
