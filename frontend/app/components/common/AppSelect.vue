<template>
  <div class="app-select" :class="selectContainerClasses">
    <label v-if="label" :for="selectId" class="app-select__label">
      {{ label }}
      <span v-if="required" class="app-select__required" aria-label="required">*</span>
    </label>
    
    <div class="app-select__wrapper">
      <select
        :id="selectId"
        ref="selectRef"
        v-model="selectedValue"
        :disabled="disabled"
        :required="required"
        :multiple="multiple"
        :size="size"
        :class="selectClasses"
        @focus="handleFocus"
        @blur="handleBlur"
        @change="handleChange"
      >
        <option
          v-if="placeholder && !multiple"
          value=""
          disabled
          selected
        >
          {{ placeholder }}
        </option>
        
        <option
          v-for="option in options"
          :key="getOptionValue(option)"
          :value="getOptionValue(option)"
          :disabled="getOptionDisabled(option)"
        >
          {{ getOptionLabel(option) }}
        </option>
      </select>
      
      <span class="app-select__arrow">▼</span>
    </div>
    
    <p v-if="errorMessage" class="app-select__error" role="alert">
      {{ errorMessage }}
    </p>
    
    <p v-else-if="hint" class="app-select__hint">
      {{ hint }}
    </p>
  </div>
</template>

<script setup lang="ts">
interface SelectOption {
  value: string | number
  label: string
  disabled?: boolean
}

interface Props {
  modelValue?: string | number | Array<string | number>
  options?: SelectOption[]
  label?: string
  placeholder?: string
  hint?: string
  errorMessage?: string
  disabled?: boolean
  required?: boolean
  multiple?: boolean
  size?: 'sm' | 'md' | 'lg'
}

const props = withDefaults(defineProps<Props>(), {
  options: () => [],
  disabled: false,
  required: false,
  multiple: false,
  size: 'md'
})

const emit = defineEmits<{
  'update:modelValue': [value: string | number | Array<string | number>]
  focus: [event: FocusEvent]
  blur: [event: FocusEvent]
  change: [value: string | number | Array<string | number>]
}>()

// Generate unique ID for accessibility
const selectId = computed(() => `app-select-${Math.random().toString(36).substr(2, 9)}`)

// Reactive value
const selectedValue = computed({
  get: () => props.modelValue,
  set: (value: string | number | Array<string | number>) => emit('update:modelValue', value)
})

// Template refs
const selectRef = ref<HTMLSelectElement>()

// Classes
const selectContainerClasses = computed(() => [
  'app-select__container',
  {
    'app-select__container--error': props.errorMessage,
    'app-select__container--disabled': props.disabled,
    'app-select__container--multiple': props.multiple
  }
])

const selectClasses = computed(() => [
  'app-select__input',
  `app-select__input--${props.size}`,
  {
    'app-select__input--error': props.errorMessage,
    'app-select__input--multiple': props.multiple
  }
])

// Option helpers
const getOptionValue = (option: SelectOption): string | number => option.value
const getOptionLabel = (option: SelectOption): string => option.label
const getOptionDisabled = (option: SelectOption): boolean => option.disabled ?? false

// Event handlers
const handleFocus = (event: FocusEvent) => {
  emit('focus', event)
}

const handleBlur = (event: FocusEvent) => {
  emit('blur', event)
}

const handleChange = (event: Event) => {
  const target = event.target as HTMLSelectElement
  let value: string | number | Array<string | number>
  
  if (props.multiple) {
    const selectedOptions = Array.from(target.selectedOptions).map(option => option.value)
    value = props.options.find(opt => opt.value === 'number') 
      ? selectedOptions.map(v => Number(v))
      : selectedOptions
  } else {
    value = target.value
    // Convert to number if needed
    if (props.options.length > 0) {
      const firstOption = props.options[0]
      if (typeof firstOption.value === 'number') {
        value = Number(target.value)
      }
    }
  }
  
  emit('update:modelValue', value)
  emit('change', value)
}

// Expose methods
defineExpose({
  focus: () => selectRef.value?.focus(),
  blur: () => selectRef.value?.blur(),
  select: (value: string | number) => {
    selectedValue.value = value
  }
})
</script>

<style scoped>
.app-select {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.app-select__container {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.app-select__container--error .app-select__input {
  border-color: var(--color-danger);
  box-shadow: 0 0 0 3px rgba(245, 198, 203, 0.1);
}

.app-select__container--disabled {
  opacity: 0.6;
  cursor: not-allowed;
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
  font-weight: var(--font-weight-bold);
}

.app-select__wrapper {
  position: relative;
}

.app-select__input {
  width: 100%;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text-primary);
  font-family: var(--font-family-base);
  font-size: var(--font-size-base);
  transition: all var(--transition-fast) var(--transition-timing);
  outline: none;
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;
  cursor: pointer;
}

.app-select__input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(168, 218, 220, 0.1);
  transform: translateY(-1px);
}

.app-select__input:disabled {
  background: var(--color-surface-alt);
  cursor: not-allowed;
  color: var(--color-text-muted);
}

.app-select__input:disabled + .app-select__arrow {
  color: var(--color-text-muted);
}

.app-select__arrow {
  position: absolute;
  top: 50%;
  right: var(--space-3);
  transform: translateY(-50%);
  color: var(--color-text-secondary);
  pointer-events: none;
  font-size: var(--font-size-sm);
  transition: color var(--transition-fast) var(--transition-timing);
}

.app-select__input:focus + .app-select__arrow {
  color: var(--color-primary);
}

/* Sizes */
.app-select__input--sm {
  height: var(--space-8);
  padding: 0 var(--space-8) var(--space-2) var(--space-3);
  font-size: var(--font-size-sm);
}

.app-select__input--md {
  height: var(--input-height);
  padding: 0 var(--space-10) var(--space-2) var(--space-4);
}

.app-select__input--lg {
  height: var(--space-12);
  padding: 0 var(--space-12) var(--space-2) var(--space-5);
  font-size: var(--font-size-lg);
}

/* Multiple select */
.app-select__input--multiple {
  height: auto;
  min-height: var(--input-height);
  padding: var(--space-2) var(--space-4);
  overflow-y: auto;
}

.app-select__input--multiple option {
  padding: var(--space-2) var(--space-3);
}

.app-select__container--multiple .app-select__arrow {
  display: none;
}

/* Error and hint styles */
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

/* Mobile adjustments */
@media (max-width: 640px) {
  .app-select__input--sm {
    height: var(--space-9);
  }
  
  .app-select__input--md {
    height: var(--space-10);
  }
  
  .app-select__input--lg {
    height: var(--space-12);
  }
}
</style>
