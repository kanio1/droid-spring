import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import AppSelect from '~/components/ui/AppSelect.vue'

// Mock scroll into view
Element.prototype.scrollIntoView = vi.fn()

describe('AppSelect', () => {
  const options = [
    { label: 'Option 1', value: 'option1' },
    { label: 'Option 2', value: 'option2' },
    { label: 'Option 3', value: 'option3' }
  ]

  it('renders select trigger button', () => {
    const wrapper = mount(AppSelect, {
      props: { modelValue: '', options }
    })

    const trigger = wrapper.find('.select__trigger')
    expect(trigger.exists()).toBe(true)
  })

  it('shows placeholder when no value selected', () => {
    const wrapper = mount(AppSelect, {
      props: { modelValue: '', options, placeholder: 'Select an option' }
    })

    const trigger = wrapper.find('.select__trigger')
    expect(trigger.text()).toContain('Select an option')
  })

  it('shows selected option label', () => {
    const wrapper = mount(AppSelect, {
      props: { modelValue: 'option1', options }
    })

    const trigger = wrapper.find('.select__trigger')
    expect(trigger.text()).toBe('Option 1')
  })

  it('opens dropdown when trigger is clicked', async () => {
    const wrapper = mount(AppSelect, {
      props: { modelValue: '', options }
    })

    const trigger = wrapper.find('.select__trigger')
    await trigger.trigger('click')

    const dropdown = wrapper.find('.select__dropdown')
    expect(dropdown.classes()).toContain('select__dropdown--open')
  })

  it('closes dropdown when already open and trigger is clicked', async () => {
    const wrapper = mount(AppSelect, {
      props: { modelValue: '', options }
    })

    const trigger = wrapper.find('.select__trigger')
    await trigger.trigger('click')
    await trigger.trigger('click')

    const dropdown = wrapper.find('.select__dropdown')
    expect(dropdown.classes()).not.toContain('select__dropdown--open')
  })

  it('renders all options in dropdown', async () => {
    const wrapper = mount(AppSelect, {
      props: { modelValue: '', options }
    })

    const trigger = wrapper.find('.select__trigger')
    await trigger.trigger('click')

    const optionElements = wrapper.findAll('.select__option')
    expect(optionElements.length).toBe(options.length)
  })

  it('emits update:modelValue when option is selected', async () => {
    const wrapper = mount(AppSelect, {
      props: { modelValue: '', options }
    })

    const trigger = wrapper.find('.select__trigger')
    await trigger.trigger('click')

    const secondOption = wrapper.findAll('.select__option')[1]
    await secondOption.trigger('click')

    expect(wrapper.emitted('update:modelValue')).toBeTruthy()
    expect(wrapper.emitted('update:modelValue')[0]).toEqual(['option2'])
  })

  it('emits change event when selection changes', async () => {
    const wrapper = mount(AppSelect, {
      props: { modelValue: 'option1', options }
    })

    const trigger = wrapper.find('.select__trigger')
    await trigger.trigger('click')

    const thirdOption = wrapper.findAll('.select__option')[2]
    await thirdOption.trigger('click')

    expect(wrapper.emitted('change')).toBeTruthy()
    expect(wrapper.emitted('change')[0]).toEqual(['option3'])
  })

  it('closes dropdown after selection', async () => {
    const wrapper = mount(AppSelect, {
      props: { modelValue: '', options }
    })

    const trigger = wrapper.find('.select__trigger')
    await trigger.trigger('click')

    const firstOption = wrapper.findAll('.select__option')[0]
    await firstOption.trigger('click')

    const dropdown = wrapper.find('.select__dropdown')
    expect(dropdown.classes()).not.toContain('select__dropdown--open')
  })

  it('highlights selected option', async () => {
    const wrapper = mount(AppSelect, {
      props: { modelValue: 'option2', options }
    })

    const trigger = wrapper.find('.select__trigger')
    await trigger.trigger('click')

    const secondOption = wrapper.findAll('.select__option')[1]
    expect(secondOption.classes()).toContain('select__option--selected')
  })

  it('marks disabled options correctly', () => {
    const optionsWithDisabled = [
      { label: 'Option 1', value: 'option1' },
      { label: 'Option 2', value: 'option2', disabled: true },
      { label: 'Option 3', value: 'option3' }
    ]

    const wrapper = mount(AppSelect, {
      props: { modelValue: '', options: optionsWithDisabled }
    })

    const trigger = wrapper.find('.select__trigger')
    trigger.trigger('click')

    const options = wrapper.findAll('.select__option')
    expect(options[1].classes()).toContain('select__option--disabled')
  })

  it('does not emit selection when disabled option is clicked', async () => {
    const optionsWithDisabled = [
      { label: 'Option 1', value: 'option1' },
      { label: 'Option 2', value: 'option2', disabled: true }
    ]

    const wrapper = mount(AppSelect, {
      props: { modelValue: '', options: optionsWithDisabled }
    })

    const trigger = wrapper.find('.select__trigger')
    await trigger.trigger('click')

    const disabledOption = wrapper.findAll('.select__option')[1]
    await disabledOption.trigger('click')

    expect(wrapper.emitted('update:modelValue')).toBeUndefined()
  })

  it('applies searchable variant', async () => {
    const wrapper = mount(AppSelect, {
      props: { modelValue: '', options, searchable: true }
    })

    const trigger = wrapper.find('.select__trigger')
    await trigger.trigger('click')

    const searchInput = wrapper.find('.select__search')
    expect(searchInput.exists()).toBe(true)
  })

  it('filters options when searching', async () => {
    const wrapper = mount(AppSelect, {
      props: { modelValue: '', options, searchable: true }
    })

    const trigger = wrapper.find('.select__trigger')
    await trigger.trigger('click')

    const searchInput = wrapper.find('.select__search')
    await searchInput.setValue('Option 1')

    await flushPromises()

    const optionElements = wrapper.findAll('.select__option')
    expect(optionElements.length).toBe(1)
  })

  it('shows no results message when search finds nothing', async () => {
    const wrapper = mount(AppSelect, {
      props: { modelValue: '', options, searchable: true }
    })

    const trigger = wrapper.find('.select__trigger')
    await trigger.trigger('click')

    const searchInput = wrapper.find('.select__search')
    await searchInput.setValue('NoMatch')

    await flushPromises()

    const noResults = wrapper.find('.select__no-results')
    expect(noResults.exists()).toBe(true)
  })

  it('supports multiple selection', () => {
    const wrapper = mount(AppSelect, {
      props: { modelValue: [], options, multiple: true }
    })

    const trigger = wrapper.find('.select__trigger')
    expect(trigger.classes()).toContain('select__trigger--multiple')
  })

  it('shows multiple selected values', () => {
    const wrapper = mount(AppSelect, {
      props: { modelValue: ['option1', 'option3'], options, multiple: true }
    })

    const trigger = wrapper.find('.select__trigger')
    expect(trigger.text()).toContain('Option 1')
    expect(trigger.text()).toContain('Option 3')
  })

  it('renders tags for multiple selection', () => {
    const wrapper = mount(AppSelect, {
      props: { modelValue: ['option1', 'option2'], options, multiple: true }
    })

    const tags = wrapper.findAll('.select__tag')
    expect(tags.length).toBe(2)
  })

  it('emits array value for multiple selection', async () => {
    const wrapper = mount(AppSelect, {
      props: { modelValue: [], options, multiple: true }
    })

    const trigger = wrapper.find('.select__trigger')
    await trigger.trigger('click')

    const firstOption = wrapper.findAll('.select__option')[0]
    await firstOption.trigger('click')

    expect(wrapper.emitted('update:modelValue')[0][0]).toEqual(['option1'])
  })

  it('toggles selection in multiple mode', async () => {
    const wrapper = mount(AppSelect, {
      props: { modelValue: ['option1'], options, multiple: true }
    })

    const trigger = wrapper.find('.select__trigger')
    await trigger.trigger('click')

    const firstOption = wrapper.findAll('.select__option')[0]
    await firstOption.trigger('click')

    expect(wrapper.emitted('update:modelValue')[0][0]).toEqual([])
  })

  it('has clear button when clearable and has value', async () => {
    const wrapper = mount(AppSelect, {
      props: { modelValue: 'option1', options, clearable: true }
    })

    const clearButton = wrapper.find('.select__clear')
    expect(clearButton.exists()).toBe(true)
  })

  it('clears value when clear button is clicked', async () => {
    const wrapper = mount(AppSelect, {
      props: { modelValue: 'option1', options, clearable: true }
    })

    const clearButton = wrapper.find('.select__clear')
    await clearButton.trigger('click')

    expect(wrapper.emitted('update:modelValue')).toBeTruthy()
    expect(wrapper.emitted('update:modelValue')[0]).toEqual([''])
  })

  it('does not show clear button when value is empty', () => {
    const wrapper = mount(AppSelect, {
      props: { modelValue: '', options, clearable: true }
    })

    const clearButton = wrapper.find('.select__clear')
    expect(clearButton.exists()).toBe(false)
  })

  it('disables select when disabled prop is true', () => {
    const wrapper = mount(AppSelect, {
      props: { modelValue: 'option1', options, disabled: true }
    })

    const trigger = wrapper.find('.select__trigger')
    expect(trigger.classes()).toContain('select__trigger--disabled')
  })

  it('does not open when disabled', async () => {
    const wrapper = mount(AppSelect, {
      props: { modelValue: 'option1', options, disabled: true }
    })

    const trigger = wrapper.find('.select__trigger')
    await trigger.trigger('click')

    const dropdown = wrapper.find('.select__dropdown')
    expect(dropdown.classes()).not.toContain('select__dropdown--open')
  })

  it('supports grouped options', () => {
    const groupedOptions = [
      {
        label: 'Group 1',
        options: [
          { label: 'Option 1', value: 'opt1' },
          { label: 'Option 2', value: 'opt2' }
        ]
      },
      {
        label: 'Group 2',
        options: [
          { label: 'Option 3', value: 'opt3' }
        ]
      }
    ]

    const wrapper = mount(AppSelect, {
      props: { modelValue: '', options: groupedOptions }
    })

    const trigger = wrapper.find('.select__trigger')
    trigger.trigger('click')

    const groups = wrapper.findAll('.select__group')
    expect(groups.length).toBe(2)
  })

  it('shows group labels', () => {
    const groupedOptions = [
      {
        label: 'Group 1',
        options: [{ label: 'Option 1', value: 'opt1' }]
      }
    ]

    const wrapper = mount(AppSelect, {
      props: { modelValue: '', options: groupedOptions }
    })

    const trigger = wrapper.find('.select__trigger')
    trigger.trigger('click')

    const groupLabel = wrapper.find('.select__group-label')
    expect(groupLabel.text()).toBe('Group 1')
  })

  it('applies loading state', () => {
    const wrapper = mount(AppSelect, {
      props: { modelValue: '', options, loading: true }
    })

    const trigger = wrapper.find('.select__trigger')
    expect(trigger.classes()).toContain('select__trigger--loading')
  })

  it('shows loading spinner when loading', () => {
    const wrapper = mount(AppSelect, {
      props: { modelValue: '', options, loading: true }
    })

    const trigger = wrapper.find('.select__trigger')
    trigger.trigger('click')

    const spinner = wrapper.find('.select__spinner')
    expect(spinner.exists()).toBe(true)
  })

  it('applies error state', () => {
    const wrapper = mount(AppSelect, {
      props: { modelValue: 'option1', options, error: 'Error message' }
    })

    const trigger = wrapper.find('.select__trigger')
    expect(trigger.classes()).toContain('select__trigger--error')
  })

  it('shows error message', () => {
    const wrapper = mount(AppSelect, {
      props: { modelValue: 'option1', options, error: 'Error message' }
    })

    const errorMessage = wrapper.find('.select__error')
    expect(errorMessage.text()).toBe('Error message')
  })

  it('applies size variants', () => {
    const sizes = ['sm', 'md', 'lg'] as const

    sizes.forEach(size => {
      const wrapper = mount(AppSelect, {
        props: { modelValue: '', options, size }
      })

      const trigger = wrapper.find('.select__trigger')
      expect(trigger.classes()).toContain(`select__trigger--${size}`)
    })
  })

  it('closes dropdown on outside click', async () => {
    const wrapper = mount(AppSelect, {
      props: { modelValue: '', options }
    })

    const trigger = wrapper.find('.select__trigger')
    await trigger.trigger('click')

    // Simulate outside click
    const event = new MouseEvent('mousedown', { bubbles: true })
    document.elementFromPoint = vi.fn().mockReturnValue(null)
    document.dispatchEvent(event)

    await flushPromises()

    const dropdown = wrapper.find('.select__dropdown')
    expect(dropdown.classes()).not.toContain('select__dropdown--open')
  })

  it('handles keyboard navigation', async () => {
    const wrapper = mount(AppSelect, {
      props: { modelValue: '', options }
    })

    const trigger = wrapper.find('.select__trigger')
    await trigger.trigger('keydown', { key: 'Enter' })

    const dropdown = wrapper.find('.select__dropdown')
    expect(dropdown.classes()).toContain('select__dropdown--open')
  })

  it('navigates options with arrow keys', async () => {
    const wrapper = mount(AppSelect, {
      props: { modelValue: '', options }
    })

    const trigger = wrapper.find('.select__trigger')
    await trigger.trigger('keydown', { key: 'Enter' })

    await trigger.trigger('keydown', { key: 'ArrowDown' })
    await trigger.trigger('keydown', { key: 'ArrowDown' })

    // Focus should move
  })

  it('selects option with Enter key', async () => {
    const wrapper = mount(AppSelect, {
      props: { modelValue: '', options }
    })

    const trigger = wrapper.find('.select__trigger')
    await trigger.trigger('keydown', { key: 'Enter' })
    await trigger.trigger('keydown', { key: 'ArrowDown' })
    await trigger.trigger('keydown', { key: 'Enter' })

    expect(wrapper.emitted('update:modelValue')).toBeTruthy()
  })

  it('closes with Escape key', async () => {
    const wrapper = mount(AppSelect, {
      props: { modelValue: '', options }
    })

    const trigger = wrapper.find('.select__trigger')
    await trigger.trigger('keydown', { key: 'Enter' })
    await trigger.trigger('keydown', { key: 'Escape' })

    const dropdown = wrapper.find('.select__dropdown')
    expect(dropdown.classes()).not.toContain('select__dropdown--open')
  })

  it('custom option formatter', () => {
    const wrapper = mount(AppSelect, {
      props: {
        modelValue: '',
        options,
        formatOption: (option: any) => `Custom: ${option.label}`
      }
    })

    const trigger = wrapper.find('.select__trigger')
    trigger.trigger('click')

    const firstOption = wrapper.findAll('.select__option')[0]
    expect(firstOption.text()).toBe('Custom: Option 1')
  })

  it('renders with icon', () => {
    const wrapper = mount(AppSelect, {
      props: { modelValue: 'option1', options, icon: 'lucide:chevron-down' }
    })

    const icon = wrapper.find('svg')
    expect(icon.exists()).toBe(true)
  })

  it('applies label prop', () => {
    const wrapper = mount(AppSelect, {
      props: { modelValue: '', options, label: 'Select Label' }
    })

    const label = wrapper.find('label')
    expect(label.text()).toBe('Select Label')
  })

  it('shows required indicator', () => {
    const wrapper = mount(AppSelect, {
      props: { modelValue: '', options, label: 'Required', required: true }
    })

    const required = wrapper.find('.select__required')
    expect(required.exists()).toBe(true)
  })

  it('snapshots in different states', () => {
    const states = [
      { modelValue: '', options },
      { modelValue: 'option1', options },
      { modelValue: [], options, multiple: true },
      { modelValue: '', options, searchable: true },
      { modelValue: 'option1', options, error: 'Error' },
      { modelValue: 'option1', options, disabled: true }
    ]

    states.forEach(state => {
      const wrapper = mount(AppSelect, { props: state })
      expect(wrapper.element).toMatchSnapshot()
    })
  })
})
