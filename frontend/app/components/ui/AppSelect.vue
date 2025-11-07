<template>
  <div class="app-select">
    <label v-if="label" :for="selectId" class="app-select__label">
      {{ label }}
      <span v-if="required" class="app-select__required">*</span>
    </label>

    <div class="app-select__wrapper">
      <div
        :class="selectClass"
        @click="toggleDropdown"
        @keydown="handleKeydown"
        tabindex="0"
        role="combobox"
        :aria-expanded="isOpen"
        :aria-haspopup="true"
        :aria-labelledby="`${selectId}-label`"
        :aria-controls="`${selectId}-listbox`"
      >
        <span class="app-select__value">
          {{ displayValue }}
        </span>
        <span class="app-select__arrow" :class="{ 'app-select__arrow--open': isOpen }">
          ▼
        </span>
      </div>

      <Transition name="dropdown">
        <div
          v-if="isOpen"
          :id="`${selectId}-listbox`"
          class="app-select__dropdown"
          role="listbox"
        >
          <div
            v-if="searchable"
            class="app-select__search"
          >
            <input
              v-model="searchQuery"
              type="text"
              placeholder="Search..."
              class="app-select__search-input"
              @click.stop
            />
          </div>

          <ul class="app-select__options">
            <li
              v-for="option in filteredOptions"
              :key="getOptionValue(option)"
              :class="optionClass(option)"
              @click="selectOption(option)"
              @mouseenter="hoveredIndex = getOptionIndex(option)"
              role="option"
              :aria-selected="isSelected(option)"
            >
              <span class="app-select__option-label">
                {{ getOptionLabel(option) }}
              </span>
              <span v-if="isSelected(option)" class="app-select__checkmark">
                ✓
              </span>
            </li>

            <li v-if="filteredOptions.length === 0" class="app-select__empty">
              No options found
            </li>
          </ul>
        </div>
      </Transition>
    </div>

    <p v-if="error" class="app-select__error">
      {{ error }}
    </p>
    <p v-else-if="hint" class="app-select__hint">
      {{ hint }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'

interface SelectOption {
  label: string
  value: any
  disabled?: boolean
}

interface Props {
  label?: string
  modelValue?: any
  options?: SelectOption[]
  placeholder?: string
  disabled?: boolean
  error?: string
  hint?: string
  required?: boolean
  multiple?: boolean
  searchable?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  options: () => [],
  placeholder: 'Select an option',
  disabled: false,
  multiple: false,
  searchable: false
})

const emit = defineEmits<{
  'update:modelValue': [value: any]
  change: [value: any]
}>()

const selectId = ref(`select-${Math.random().toString(36).substr(2, 9)}`)
const isOpen = ref(false)
const searchQuery = ref('')
const hoveredIndex = ref(-1)

const displayValue = computed(() => {
  if (!props.modelValue) return props.placeholder

  if (props.multiple && Array.isArray(props.modelValue)) {
    return props.modelValue
      .map(v => getOptionLabel(v))
      .join(', ')
  }

  const option = props.options.find(o => o.value === props.modelValue)
  return option ? option.label : props.placeholder
})

const filteredOptions = computed(() => {
  if (!props.searchable || !searchQuery.value) {
    return props.options
  }

  const query = searchQuery.value.toLowerCase()
  return props.options.filter(option =>
    option.label.toLowerCase().includes(query)
  )
})

const selectClass = computed(() => ({
  'app-select__field': true,
  'app-select__field--error': props.error,
  'app-select__field--disabled': props.disabled,
  'app-select__field--open': isOpen.value
}))

const toggleDropdown = () => {
  if (!props.disabled) {
    isOpen.value = !isOpen.value
    if (isOpen.value) {
      hoveredIndex.value = -1
    }
  }
}

const selectOption = (option: SelectOption) => {
  if (option.disabled) return

  if (props.multiple) {
    const currentValue = Array.isArray(props.modelValue) ? props.modelValue : []
    const newValue = isSelected(option)
      ? currentValue.filter(v => v !== option.value)
      : [...currentValue, option.value]
    emit('update:modelValue', newValue)
    emit('change', newValue)
  } else {
    emit('update:modelValue', option.value)
    emit('change', option.value)
    isOpen.value = false
  }
}

const isSelected = (option: SelectOption) => {
  if (props.multiple && Array.isArray(props.modelValue)) {
    return props.modelValue.includes(option.value)
  }
  return props.modelValue === option.value
}

const getOptionLabel = (option: any) => {
  if (typeof option === 'string' || typeof option === 'number') {
    return option.toString()
  }
  return option.label
}

const getOptionValue = (option: any) => {
  if (typeof option === 'string' || typeof option === 'number') {
    return option
  }
  return option.value
}

const getOptionIndex = (option: SelectOption) => {
  return filteredOptions.value.indexOf(option)
}

const optionClass = (option: SelectOption) => ({
  'app-select__option': true,
  'app-select__option--disabled': option.disabled,
  'app-select__option--selected': isSelected(option),
  'app-select__option--hovered': hoveredIndex.value === getOptionIndex(option)
})

const handleKeydown = (event: KeyboardEvent) => {
  if (props.disabled) return

  switch (event.key) {
    case 'Enter':
    case ' ':
      event.preventDefault()
      toggleDropdown()
      break
    case 'ArrowDown':
      event.preventDefault()
      if (!isOpen.value) {
        isOpen.value = true
      } else {
        hoveredIndex.value = Math.min(
          hoveredIndex.value + 1,
          filteredOptions.value.length - 1
        )
      }
      break
    case 'ArrowUp':
      event.preventDefault()
      if (isOpen.value) {
        hoveredIndex.value = Math.max(hoveredIndex.value - 1, 0)
      }
      break
    case 'Escape':
      isOpen.value = false
      break
  }
}

// Close dropdown when clicking outside
const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  if (!target.closest('.app-select')) {
    isOpen.value = false
  }
}

watch(
  () => isOpen.value,
  (isOpen) => {
    if (isOpen) {
      document.addEventListener('click', handleClickOutside)
    } else {
      document.removeEventListener('click', handleClickOutside)
    }
  },
  { immediate: true }
)

// Clear search when dropdown closes
watch(
  () => isOpen.value,
  (isOpen) => {
    if (!isOpen) {
      searchQuery.value = ''
    }
  }
)
</script>

<style scoped>
.app-select {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  width: 100%;
  position: relative;
}

.app-select__label {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
  display: flex;
  align-items: center;
  gap: var(--space-1);
}

.app-select__required {
  color: var(--color-danger);
}

.app-select__wrapper {
  position: relative;
  width: 100%;
}

.app-select__field {
  width: 100%;
  min-height: var(--input-height);
  padding: 0 var(--space-4);
  font-size: var(--font-size-base);
  font-family: var(--font-family-base);
  color: var(--color-text-primary);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  transition: all var(--transition-fast) var(--transition-timing);
  outline: none;
  user-select: none;
}

.app-select__field:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-light);
}

.app-select__field--error {
  border-color: var(--color-danger);
}

.app-select__field--error:focus {
  border-color: var(--color-danger);
  box-shadow: 0 0 0 3px var(--color-danger-light);
}

.app-select__field--disabled {
  background: var(--color-surface-alt);
  cursor: not-allowed;
  opacity: 0.6;
}

.app-select__value {
  flex: 1;
  text-align: left;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-select__arrow {
  margin-left: var(--space-3);
  transition: transform var(--transition-fast) var(--transition-timing);
  color: var(--color-text-secondary);
  font-size: 0.75rem;
}

.app-select__arrow--open {
  transform: rotate(180deg);
}

.app-select__dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  z-index: var(--z-dropdown);
  max-height: 300px;
  overflow-y: auto;
}

.app-select__search {
  padding: var(--space-2);
  border-bottom: 1px solid var(--color-border);
}

.app-select__search-input {
  width: 100%;
  height: 32px;
  padding: 0 var(--space-3);
  font-size: var(--font-size-sm);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  outline: none;
}

.app-select__search-input:focus {
  border-color: var(--color-primary);
}

.app-select__options {
  list-style: none;
  margin: 0;
  padding: var(--space-1) 0;
}

.app-select__option {
  padding: var(--space-3) var(--space-4);
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  transition: background-color var(--transition-fast) var(--transition-timing);
  user-select: none;
}

.app-select__option:hover {
  background: var(--color-surface-alt);
}

.app-select__option--selected {
  background: var(--color-primary-light);
  color: var(--color-text-primary);
  font-weight: var(--font-weight-medium);
}

.app-select__option--hovered {
  background: var(--color-surface-alt);
}

.app-select__option--disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.app-select__option-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-select__checkmark {
  color: var(--color-primary);
  font-weight: bold;
  margin-left: var(--space-2);
}

.app-select__empty {
  padding: var(--space-4);
  text-align: center;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.app-select__error {
  font-size: var(--font-size-xs);
  color: var(--color-danger);
  margin: 0;
}

.app-select__hint {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
  margin: 0;
}

/* Transitions */
.dropdown-enter-active,
.dropdown-leave-active {
  transition: all var(--transition-fast) var(--transition-timing);
  transform-origin: top;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: scaleY(0.95) translateY(-10px);
}
</style>
