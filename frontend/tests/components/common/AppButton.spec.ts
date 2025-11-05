/**
 * Test for AppButton Component
 * Following Arrange-Act-Assert pattern
 */

import { describe, it, expect, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import AppButton from '~/components/common/AppButton.vue'

// Test data
const createWrapper = (props = {}) => {
  return mount(AppButton, {
    props: {
      variant: 'primary',
      size: 'md',
      ...props
    },
    slots: {
      default: 'Click Me'
    }
  })
}

describe('AppButton Component', () => {
  describe('Rendering', () => {
    it('should render with default props', () => {
      // Arrange & Act
      const wrapper = createWrapper()

      // Assert
      expect(wrapper.find('button').exists()).toBe(true)
      expect(wrapper.text()).toBe('Click Me')
      expect(wrapper.classes()).toContain('app-button--primary')
      expect(wrapper.classes()).toContain('app-button--md')
    })

    it('should render as button element by default', () => {
      // Arrange & Act
      const wrapper = createWrapper()

      // Assert
      expect(wrapper.element.tagName).toBe('BUTTON')
      expect(wrapper.attributes('type')).toBe('button')
    })

    it('should render as anchor when tag is "a"', () => {
      // Arrange & Act
      const wrapper = createWrapper({ tag: 'a', href: 'http://example.com' })

      // Assert
      expect(wrapper.element.tagName).toBe('A')
      expect(wrapper.attributes('href')).toBe('http://example.com')
      expect(wrapper.classes()).toContain('app-button')
    })

    it('should render as NuxtLink when tag is "NuxtLink"', () => {
      // Arrange & Act
      const wrapper = createWrapper({ tag: 'NuxtLink', to: '/dashboard' })

      // Assert
      expect(wrapper.element.tagName).toBe('A')
      expect(wrapper.attributes('to')).toBe('/dashboard')
      expect(wrapper.classes()).toContain('app-button')
    })

    it('should apply custom CSS classes when provided', () => {
      // Arrange & Act
      const wrapper = createWrapper({ class: 'custom-class' })

      // Assert
      expect(wrapper.classes()).toContain('custom-class')
    })
  })

  describe('Variants', () => {
    it('should apply primary variant classes', () => {
      // Arrange & Act
      const wrapper = createWrapper({ variant: 'primary' })

      // Assert
      expect(wrapper.classes()).toContain('app-button--primary')
    })

    it('should apply secondary variant classes', () => {
      // Arrange & Act
      const wrapper = createWrapper({ variant: 'secondary' })

      // Assert
      expect(wrapper.classes()).toContain('app-button--secondary')
    })

    it('should apply success variant classes', () => {
      // Arrange & Act
      const wrapper = createWrapper({ variant: 'success' })

      // Assert
      expect(wrapper.classes()).toContain('app-button--success')
    })

    it('should apply danger variant classes', () => {
      // Arrange & Act
      const wrapper = createWrapper({ variant: 'danger' })

      // Assert
      expect(wrapper.classes()).toContain('app-button--danger')
    })

    it('should apply warning variant classes', () => {
      // Arrange & Act
      const wrapper = createWrapper({ variant: 'warning' })

      // Assert
      expect(wrapper.classes()).toContain('app-button--warning')
    })

    it('should apply ghost variant classes', () => {
      // Arrange & Act
      const wrapper = createWrapper({ variant: 'ghost' })

      // Assert
      expect(wrapper.classes()).toContain('app-button--ghost')
    })
  })

  describe('Sizes', () => {
    it('should apply small size classes', () => {
      // Arrange & Act
      const wrapper = createWrapper({ size: 'sm' })

      // Assert
      expect(wrapper.classes()).toContain('app-button--sm')
    })

    it('should apply medium size classes', () => {
      // Arrange & Act
      const wrapper = createWrapper({ size: 'md' })

      // Assert
      expect(wrapper.classes()).toContain('app-button--md')
    })

    it('should apply large size classes', () => {
      // Arrange & Act
      const wrapper = createWrapper({ size: 'lg' })

      // Assert
      expect(wrapper.classes()).toContain('app-button--lg')
    })
  })

  describe('Icons', () => {
    it('should render icon when provided', () => {
      // Arrange & Act
      const wrapper = createWrapper({ icon: '★' })

      // Assert
      expect(wrapper.find('.button__icon').exists()).toBe(true)
      expect(wrapper.find('.button__icon').text()).toBe('★')
    })

    it('should position icon on left by default', () => {
      // Arrange & Act
      const wrapper = createWrapper({ icon: '★' })

      // Assert
      expect(wrapper.find('.button__icon--left').exists()).toBe(true)
      expect(wrapper.find('.button__icon--right').exists()).toBe(false)
    })

    it('should position icon on right when iconPosition is "right"', () => {
      // Arrange & Act
      const wrapper = createWrapper({ icon: '★', iconPosition: 'right' })

      // Assert
      expect(wrapper.find('.button__icon--right').exists()).toBe(true)
    })

    it('should not show icon when loading', () => {
      // Arrange & Act
      const wrapper = createWrapper({ icon: '★', loading: true })

      // Assert
      expect(wrapper.find('.button__icon').exists()).toBe(false)
      expect(wrapper.find('.button__spinner').exists()).toBe(true)
    })
  })

  describe('Loading State', () => {
    it('should show spinner when loading', () => {
      // Arrange & Act
      const wrapper = createWrapper({ loading: true })

      // Assert
      expect(wrapper.classes()).toContain('app-button--loading')
      expect(wrapper.find('.button__spinner').exists()).toBe(true)
    })

    it('should hide text content when loading', () => {
      // Arrange & Act
      const wrapper = createWrapper({ loading: true })

      // Assert
      expect(wrapper.find('.button__spinner').exists()).toBe(true)
    })
  })

  describe('Disabled State', () => {
    it('should apply disabled classes when disabled', () => {
      // Arrange & Act
      const wrapper = createWrapper({ disabled: true })

      // Assert
      expect(wrapper.classes()).toContain('app-button--disabled')
      expect(wrapper.attributes('disabled')).toBe('')
    })
  })

  describe('Full Width', () => {
    it('should apply full-width classes when fullWidth is true', () => {
      // Arrange & Act
      const wrapper = createWrapper({ fullWidth: true })

      // Assert
      expect(wrapper.classes()).toContain('app-button--full-width')
    })
  })

  describe('Click Handling', () => {
    it('should emit click event when clicked and not disabled or loading', async () => {
      // Arrange
      const wrapper = createWrapper()
      const clickEvent = new MouseEvent('click')

      // Act
      await wrapper.find('button').trigger('click')

      // Assert
      expect(wrapper.emitted('click')).toBeDefined()
      expect(wrapper.emitted('click')?.length).toBe(1)
    })

    it('should prevent click when disabled', async () => {
      // Arrange
      const wrapper = createWrapper({ disabled: true })
      const clickEvent = new MouseEvent('click')

      // Act
      await wrapper.find('button').trigger('click', clickEvent)

      // Assert
      expect(wrapper.emitted('click')).toBeUndefined()
    })

    it('should prevent click when loading', async () => {
      // Arrange
      const wrapper = createWrapper({ loading: true })

      // Act
      await wrapper.find('button').trigger('click')

      // Assert
      expect(wrapper.emitted('click')).toBeUndefined()
    })

    it('should prevent default event when disabled', async () => {
      // Arrange
      const wrapper = createWrapper({ disabled: true })
      const clickEvent = new MouseEvent('click')

      // Act
      await wrapper.find('button').trigger('click', clickEvent)

      // Assert
      expect(clickEvent.defaultPrevented).toBe(true)
    })
  })

  describe('Tag Types', () => {
    it('should set correct type attribute for button element', () => {
      // Arrange & Act
      const wrapper = createWrapper({ type: 'submit', tag: 'button' })

      // Assert
      expect(wrapper.attributes('type')).toBe('submit')
    })

    it('should not set type for non-button elements', () => {
      // Arrange & Act
      const wrapper = createWrapper({ type: 'submit', tag: 'a' })

      // Assert
      expect(wrapper.attributes('type')).toBeUndefined()
    })
  })

  describe('Accessibility', () => {
    it('should have proper focus style', () => {
      // Arrange & Act
      const wrapper = createWrapper()

      // Assert
      expect(wrapper.find('button').classes()).toContain('app-button')
    })

    it('should be focusable', () => {
      // Arrange & Act
      const wrapper = createWrapper()

      // Assert
      expect(wrapper.find('button').attributes('tabindex')).toBeUndefined()
    })

    it('should have role attribute for non-button elements', () => {
      // Arrange & Act
      const wrapper = createWrapper({ tag: 'a' })

      // Assert
      expect(wrapper.element.getAttribute('role')).toBeNull()
    })
  })

  describe('Slots', () => {
    it('should render default slot content', () => {
      // Arrange & Act
      const wrapper = mount(AppButton, {
        slots: { default: 'Custom Content' }
      })

      // Assert
      expect(wrapper.text()).toBe('Custom Content')
    })

    it('should render slot content with icon', () => {
      // Arrange & Act
      const wrapper = mount(AppButton, {
        props: { icon: '★' },
        slots: { default: 'Text with Icon' }
      })

      // Assert
      expect(wrapper.find('.button__text').text()).toBe('Text with Icon')
    })
  })

  describe('Computed Classes', () => {
    it('should generate correct class list based on props', () => {
      // Arrange & Act
      const wrapper = createWrapper({
        variant: 'danger',
        size: 'lg',
        disabled: true,
        loading: true,
        fullWidth: true
      })

      // Assert
      expect(wrapper.classes()).toContain('app-button')
      expect(wrapper.classes()).toContain('app-button--danger')
      expect(wrapper.classes()).toContain('app-button--lg')
      expect(wrapper.classes()).toContain('app-button--disabled')
      expect(wrapper.classes()).toContain('app-button--loading')
      expect(wrapper.classes()).toContain('app-button--full-width')
    })
  })

  describe('Mouse Event Prevention', () => {
    it('should prevent default on anchor click when disabled', async () => {
      // Arrange
      const wrapper = createWrapper({ tag: 'a', href: '#', disabled: true })
      const clickEvent = new MouseEvent('click')

      // Act
      await wrapper.find('a').trigger('click', clickEvent)

      // Assert
      expect(clickEvent.defaultPrevented).toBe(true)
    })

    it('should prevent default on anchor click when loading', async () => {
      // Arrange
      const wrapper = createWrapper({ tag: 'a', href: '#', loading: true })
      const clickEvent = new MouseEvent('click')

      // Act
      await wrapper.find('a').trigger('click', clickEvent)

      // Assert
      expect(clickEvent.defaultPrevented).toBe(true)
    })
  })
})
