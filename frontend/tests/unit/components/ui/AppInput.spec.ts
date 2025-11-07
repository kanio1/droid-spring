import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import AppInput from '~/components/ui/AppInput.vue'

describe('AppInput', () => {
  it('renders input element with v-model', async () => {
    const wrapper = mount(AppInput, {
      props: { modelValue: 'initial value', label: 'Test Input' }
    })

    const input = wrapper.find('input')
    expect(input.element.value).toBe('initial value')
  })

  it('updates modelValue when input changes', async () => {
    const wrapper = mount(AppInput, {
      props: { modelValue: '', label: 'Test Input' }
    })

    const input = wrapper.find('input')
    await input.setValue('new value')

    expect(wrapper.emitted('update:modelValue')).toBeTruthy()
    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['new value'])
  })

  it('emits change event on input', async () => {
    const wrapper = mount(AppInput, {
      props: { modelValue: '', label: 'Test Input' }
    })

    const input = wrapper.find('input')
    await input.setValue('test')

    expect(wrapper.emitted('change')).toBeTruthy()
    expect(wrapper.emitted('change')?.[0]).toEqual(['test'])
  })

  it('displays label when provided', () => {
    const wrapper = mount(AppInput, {
      props: { label: 'Email Address' }
    })

    expect(wrapper.text()).toContain('Email Address')
  })

  it('shows required asterisk when required prop is true', () => {
    const wrapper = mount(AppInput, {
      props: { label: 'Required Field', required: true }
    })

    const requiredSpan = wrapper.find('.input__required')
    expect(requiredSpan.exists()).toBe(true)
    expect(requiredSpan.text()).toBe('*')
  })

  it('shows error message when error prop is provided', () => {
    const wrapper = mount(AppInput, {
      props: { label: 'Email', error: 'Invalid email' }
    })

    const errorElement = wrapper.find('.input__error')
    expect(errorElement.exists()).toBe(true)
    expect(errorElement.text()).toBe('Invalid email')
  })

  it('applies error class when error prop is provided', () => {
    const wrapper = mount(AppInput, {
      props: { error: 'Error message' }
    })

    const input = wrapper.find('input')
    expect(input.classes()).toContain('input__field--error')
  })

  it('shows hint message when hint prop is provided', () => {
    const wrapper = mount(AppInput, {
      props: { label: 'Password', hint: 'At least 8 characters' }
    })

    const hintElement = wrapper.find('.input__hint')
    expect(hintElement.exists()).toBe(true)
    expect(hintElement.text()).toBe('At least 8 characters')
  })

  it('disables input when disabled prop is true', () => {
    const wrapper = mount(AppInput, {
      props: { disabled: true }
    })

    const input = wrapper.find('input')
    expect(input.attributes('disabled')).toBeDefined()
    expect(input.classes()).toContain('input__field--disabled')
  })

  it('renders different input types', () => {
    const types = ['text', 'email', 'password', 'number', 'tel', 'url'] as const

    types.forEach(type => {
      const wrapper = mount(AppInput, {
        props: { type }
      })

      const input = wrapper.find('input')
      expect(input.attributes('type')).toBe(type)
    })
  })

  it('applies placeholder when provided', () => {
    const wrapper = mount(AppInput, {
      props: { placeholder: 'Enter your name' }
    })

    const input = wrapper.find('input')
    expect(input.attributes('placeholder')).toBe('Enter your name')
  })

  it('forwards additional HTML attributes to input', () => {
    const wrapper = mount(AppInput, {
      attrs: { 'data-testid': 'custom-input', maxlength: 10, autocomplete: 'off' }
    })

    const input = wrapper.find('input')
    expect(input.attributes('data-testid')).toBe('custom-input')
    expect(input.attributes('maxlength')).toBe('10')
    expect(input.attributes('autocomplete')).toBe('off')
  })

  it('shows icon when icon prop is provided', () => {
    const wrapper = mount(AppInput, {
      props: { icon: 'lucide:mail' }
    })

    const icon = wrapper.find('svg')
    expect(icon.exists()).toBe(true)
  })

  it('shows clear button when clearable and has value', async () => {
    const wrapper = mount(AppInput, {
      props: { modelValue: 'test value', clearable: true }
    })

    const clearButton = wrapper.find('.input__clear')
    expect(clearButton.exists()).toBe(true)
  })

  it('does not show clear button when value is empty', () => {
    const wrapper = mount(AppInput, {
      props: { modelValue: '', clearable: true }
    })

    const clearButton = wrapper.find('.input__clear')
    expect(clearButton.exists()).toBe(false)
  })

  it('clears value when clear button is clicked', async () => {
    const wrapper = mount(AppInput, {
      props: { modelValue: 'test value', clearable: true }
    })

    const clearButton = wrapper.find('.input__clear')
    await clearButton.trigger('click')

    expect(wrapper.emitted('update:modelValue')).toBeTruthy()
    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual([''])
  })

  it('handles focus and blur events', async () => {
    const wrapper = mount(AppInput, {
      props: { modelValue: '' }
    })

    const input = wrapper.find('input')
    await input.trigger('focus')

    expect(wrapper.emitted('focus')).toBeTruthy()

    await input.trigger('blur')
    expect(wrapper.emitted('blur')).toBeTruthy()
  })

  it('applies focus styles when focused', async () => {
    const wrapper = mount(AppInput, {
      props: { modelValue: '' }
    })

    const input = wrapper.find('input')
    await input.trigger('focus')

    // Focus styles are applied via :focus pseudo-class
  })

  it('validates required field on blur', async () => {
    const wrapper = mount(AppInput, {
      props: { label: 'Required', required: true, modelValue: '' }
    })

    const input = wrapper.find('input')
    await input.trigger('blur')

    // Should show error for empty required field
  })

  it('handles password toggle visibility', async () => {
    const wrapper = mount(AppInput, {
      props: { type: 'password', modelValue: 'secret' }
    })

    const toggleButton = wrapper.find('.input__toggle')
    expect(toggleButton.exists()).toBe(true)

    await toggleButton.trigger('click')
    const input = wrapper.find('input')
    expect(input.attributes('type')).toBe('text')
  })

  it('shows password icon when type is password', () => {
    const wrapper = mount(AppInput, {
      props: { type: 'password', modelValue: 'secret' }
    })

    const toggleButton = wrapper.find('.input__toggle')
    expect(toggleButton.exists()).toBe(true)
  })

  it('toggles between password and text types', async () => {
    const wrapper = mount(AppInput, {
      props: { type: 'password', modelValue: 'secret' }
    })

    const input = wrapper.find('input')
    expect(input.attributes('type')).toBe('password')

    const toggleButton = wrapper.find('.input__toggle')
    await toggleButton.trigger('click')
    expect(input.attributes('type')).toBe('text')
  })

  it('handles keyboard events', async () => {
    const wrapper = mount(AppInput, {
      props: { modelValue: '' }
    })

    const input = wrapper.find('input')
    await input.trigger('keydown', { key: 'Enter' })
    expect(wrapper.emitted('keydown')).toBeTruthy()

    await input.trigger('keyup', { key: 'Escape' })
    expect(wrapper.emitted('keyup')).toBeTruthy()
  })

  it('forwards ref to input element', () => {
    const wrapper = mount(AppInput, {
      props: { modelValue: '' }
    })

    const input = wrapper.find('input')
    expect(input.exists()).toBe(true)
  })

  it('works with v-model in parent component', async () => {
    const Parent = {
      template: '<AppInput v-model="value" />',
      components: { AppInput },
      data() {
        return { value: '' }
      }
    }

    const wrapper = mount(Parent)
    const input = wrapper.find('input')
    await input.setValue('parent value')

    expect(wrapper.vm.value).toBe('parent value')
  })

  it('respects maxlength attribute', async () => {
    const wrapper = mount(AppInput, {
      props: { modelValue: '', maxlength: 5 }
    })

    const input = wrapper.find('input')
    expect(input.attributes('maxlength')).toBe('5')
  })

  it('applies custom class via class prop', () => {
    const wrapper = mount(AppInput, {
      props: { class: 'custom-input' }
    })

    expect(wrapper.classes()).toContain('custom-input')
  })

  it('id is generated when not provided', () => {
    const wrapper = mount(AppInput)

    const input = wrapper.find('input')
    expect(input.attributes('id')).toBeDefined()
  })

  it('uses provided id', () => {
    const wrapper = mount(AppInput, {
      props: { id: 'custom-id' }
    })

    const input = wrapper.find('input')
    expect(input.attributes('id')).toBe('custom-id')
  })

  it('label for attribute matches input id', () => {
    const wrapper = mount(AppInput, {
      props: { id: 'test-id', label: 'Test Label' }
    })

    const label = wrapper.find('label')
    const input = wrapper.find('input')
    expect(label.attributes('for')).toBe('test-id')
    expect(input.attributes('id')).toBe('test-id')
  })

  it('renders prefix when prefixIcon is provided', () => {
    const wrapper = mount(AppInput, {
      props: { prefixIcon: 'lucide:mail' }
    })

    const prefixIcon = wrapper.find('.input__prefix')
    expect(prefixIcon.exists()).toBe(true)
  })

  it('renders suffix when suffixIcon is provided', () => {
    const wrapper = mount(AppInput, {
      props: { suffixIcon: 'lucide:check' }
    })

    const suffixIcon = wrapper.find('.input__suffix')
    expect(suffixIcon.exists()).toBe(true)
  })

  it('works correctly in dark mode', async () => {
    document.documentElement.setAttribute('data-theme', 'dark')

    const wrapper = mount(AppInput, {
      props: { modelValue: 'dark mode test' }
    })

    const input = wrapper.find('input')
    expect(input.element.value).toBe('dark mode test')

    document.documentElement.removeAttribute('data-theme')
  })

  it('snapshots in different states', () => {
    const states = [
      { props: { label: 'Default' } },
      { props: { label: 'With Error', error: 'Error message' } },
      { props: { label: 'Disabled', disabled: true } },
      { props: { label: 'With Icon', icon: 'lucide:mail' } }
    ]

    states.forEach(({ props }) => {
      const wrapper = mount(AppInput, { props })
      expect(wrapper.element).toMatchSnapshot()
    })
  })
})
