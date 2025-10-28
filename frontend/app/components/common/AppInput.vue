<template>
  <div class="app-input" :class="inputContainerClasses">
    <label v-if="label" :for="inputId" class="app-input__label">
      {{ label }}
      <span v-if="required" class="app-input__required" aria-label="required">*</span>
    </label>
    
    <div class="app-input__field-wrapper">
      <span v-if="icon" class="app-input__icon" :class="{ 'app-input__icon--left': iconPosition === 'left' }">
        {{ icon }}
      </span>
      
      <input
        :id="inputId"
        ref="inputRef"
        v-model="inputValue"
        :type="type"
        :placeholder="placeholder"
        :disabled="disabled"
        :readonly="readonly"
        :required="required"
        :min="min"
        :max="max"
        :step="step"
        :autocomplete="autocomplete"
        :class="inputClasses"
        @focus="handleFocus"
        @blur="handleBlur"
        @input="handleInput"
        @change="handleChange"
        @keydown="handleKeydown"
      />
      
      <button
        v-if="clearable && inputValue && !disabled"
        type="button"
        class="app-input__clear"
        @click="clearInput"
        aria-label="Clear input"
      >
        ×
      </button>
    </div>
    
    <p v-if="errorMessage" class="app-input__error" role="alert">
      {{ errorMessage }}
    </p>
    
    <p v-else-if="hint" class="app-input__hint">
      {{ hint }}
    </p>
  </div>
</template>

<script setup lang="ts">
interface Props {
  modelValue?: string | number
  type?: 'text' | 'email' | 'password' | 'number' | 'tel' | 'url' | 'search'
  label?: string
  placeholder?: string
  hint?: string
  errorMessage?: string
  disabled?: boolean
  readonly?: boolean
  required?: boolean
  clearable?: boolean
  size?: 'sm' | 'md' | 'lg'
  icon?: string
  iconPosition?: 'left' | 'right'
  min?: number
  max?: number
  step?: number
  autocomplete?: string
}

const props = withDefaults(defineProps<Props>(), {
  type: 'text',
  disabled: false,
  readonly: false,
  required: false,
  clearable: false,
  size: 'md',
  iconPosition: 'left'
})

const emit = defineEmits<{
  'update:modelValue': [value: string | number]
  focus: [event: FocusEvent]
  blur: [event: FocusEvent]
  change: [value: string | number]
  input: [value: string | number]
  keydown: [event: KeyboardEvent]
}>()

// Generate unique ID for accessibility
const inputId = computed(() => `app-input-${Math.random().toString(36).substr(2, 9)}`)

// Reactive value
const inputValue = computed({
  get: () => props.modelValue ?? '',
  set: (value: string | number) => emit('update:modelValue', value)
})

// Template refs
const inputRef = ref<HTMLInputElement>()

// Classes
const inputContainerClasses = computed(() => [
  'app-input__container',
  {
    'app-input__container--error': props.errorMessage,
    'app-input__container--disabled': props.disabled,
    'app-input__container--readonly': props.readonly,
    'app-input__container--with-icon-left': props.icon && props.iconPosition === 'left',
    'app-input__container--with-icon-right': props.icon && props.iconPosition === 'right'
  }
])

const inputClasses = computed(() => [
  'app-input__input',
  `app-input__input--${props.size}`,
  {
    'app-input__input--error': props.errorMessage,
    'app-input__input--with-icon-left': props.icon && props.iconPosition === 'left',
    'app-input__input--with-icon-right': props.icon && props.iconPosition === 'right'
  }
])

// Event handlers
const handleFocus = (event: FocusEvent) => {
  emit('focus', event)
}

const handleBlur = (event: FocusEvent) => {
  emit('blur', event)
}

const handleInput = (event: Event) => {
  const target = event.target as HTMLInputElement
  const value = props.type === 'number' ? Number(target.value) : target.value
  emit('update:modelValue', value)
  emit('input', value)
}

const handleChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  const value = props.type === 'number' ? Number(target.value) : target.value
  emit('change', value)
}

const handleKeydown = (event: KeyboardEvent) => {
  emit('keydown', event)
}

const clearInput = () => {
  inputValue.value = ''
  emit('update:modelValue', '')
  inputRef.value?.focus()
}

// Expose methods
defineExpose({
  focus: () => inputRef.value?.focus(),
  blur: () => inputRef.value?.blur(),
  select: () => inputRef.value?.select()
})
</script>

<style scoped>
.app-input {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.app-input__container {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.app-input__container--error .app-input__input {
  border-color: var(--color-danger);
  box-shadow: 0 0 0 3px rgba(245, 198, 203, 0.1);
}

.app-input__container--disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.app-input__label {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
  display: flex;
  align-items: center;
  gap: var(--space-1);
}

.app-input__required {
  color: var(--color-danger);
  font-weight: var(--font-weight-bold);
}

.app-input__field-wrapper {
  position: relative;
  display: flex;
  align-items: stretch;
}

.app-input__icon {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  color: var(--color-text-muted);
  font-size: var(--font-size-base);
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
}

.app-input__icon--left {
  left: var(--space-3);
}

.app-input__icon:not(.app-input__icon--left) {
  right: var(--space-3);
}

.app-input__clear {
  position: absolute;
  top: 50%;
  right: var(--space-3);
  transform: translateY(-50%);
  background: none;
  border: none;
  color: var(--color-text-muted);
  cursor: pointer;
  padding: var(--space-1);
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast) var(--transition-timing);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  font-size: var(--font-size-lg);
  line-height: 1;
}

.app-input__clear:hover {
  color: var(--color-text-secondary);
  background: var(--color-surface-alt);
}

.app-input__input {
  width: 100%;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text-primary);
  font-family: var(--font-family-base);
  font-size: var(--font-size-base);
  transition: all var(--transition-fast) var(--transition-timing);
  outline: none;
}

.app-input__input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(168, 218, 220, 0.1);
  transform: translateY(-1px);
}

.app-input__input:disabled {
  background: var(--color-surface-alt);
  cursor: not-allowed;
  color: var(--color-text-muted);
}

.app-input__input:read-only {
  background: var(--color-surface-alt);
}

/* Sizes */
.app-input__input--sm {
  height: var(--space-8);
  padding: 0 var(--space-3);
  font-size: var(--font-size-sm);
}

.app-input__input--sm.app-input__input--with-icon-left {
  padding-left: calc(var(--space-3) + 20px + var(--space-2));
}

.app-input__input--sm.app-input__input--with-icon-right {
  padding-right: calc(var(--space-3) + 20px + var(--space-2));
}

.app-input__input--md {
  height: var(--input-height);
  padding: 0 var(--space-4);
}

.app-input__input--md.app-input__input--with-icon-left {
  padding-left: calc(var(--space-4) + 20px + var(--space-2));
}

.app-input__input--md.app-input__input--with-icon-right {
  padding-right: calc(var(--space-4) + 20px + var(--space-2));
}

.app-input__input--lg {
  height: var(--space-12);
  padding: 0 var(--space-5);
  font-size: var(--font-size-lg);
}

.app-input__input--lg.app-input__input--with-icon-left {
  padding-left: calc(var(--space-5) + 20px + var(--space-2));
}

.app-input__input--lg.app-input__input--with-icon-right {
  padding-right: calc(var(--space-5) + 20px + var(--space-2));
}

/* Error and hint styles */
.app-input__error {
  font-size: var(--font-size-xs);
  color: var(--color-danger);
  margin: 0;
}

.app-input__hint {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
  margin: 0;
}

/* Mobile adjustments */
@media (max-width: 640px) {
  .app-input__input--sm {
    height: var(--space-9);
  }
  
  .app-input__input--md {
    height: var(--space-10);
  }
  
  .app-input__input--lg {
    height: var(--space-12);
  }
}
</style>
