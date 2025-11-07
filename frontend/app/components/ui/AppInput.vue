<template>
  <div class="app-input">
    <label v-if="label" :for="inputId" class="app-input__label">
      {{ label }}
      <span v-if="required" class="app-input__required">*</span>
    </label>

    <div class="app-input__field-wrapper">
      <span v-if="prefixIcon" class="app-input__icon app-input__icon--prefix">
        <component :is="prefixIcon" />
      </span>

      <input
        :id="inputId"
        v-model="inputValue"
        :type="type"
        :placeholder="placeholder"
        :disabled="disabled"
        :readonly="readonly"
        :autocomplete="autocomplete"
        :class="inputClass"
        @blur="onBlur"
        @focus="onFocus"
        @input="onInput"
        @change="onChange"
      />

      <span v-if="suffixIcon" class="app-input__icon app-input__icon--suffix">
        <component :is="suffixIcon" />
      </span>
    </div>

    <p v-if="error" class="app-input__error">
      {{ error }}
    </p>
    <p v-else-if="hint" class="app-input__hint">
      {{ hint }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

interface Props {
  label?: string
  modelValue?: string | number
  type?: string
  placeholder?: string
  disabled?: boolean
  readonly?: boolean
  error?: string
  hint?: string
  required?: boolean
  autocomplete?: string
  prefixIcon?: any
  suffixIcon?: any
  size?: 'small' | 'normal' | 'large'
}

const props = withDefaults(defineProps<Props>(), {
  type: 'text',
  size: 'normal',
  disabled: false,
  readonly: false
})

const emit = defineEmits<{
  'update:modelValue': [value: string | number]
  blur: [event: FocusEvent]
  focus: [event: FocusEvent]
  input: [event: Event]
  change: [event: Event]
}>()

const inputId = ref(`input-${Math.random().toString(36).substr(2, 9)}`)

const inputValue = computed({
  get: () => props.modelValue ?? '',
  set: (value) => emit('update:modelValue', value)
})

const inputClass = computed(() => ({
  'app-input__field': true,
  'app-input__field--error': props.error,
  'app-input__field--disabled': props.disabled,
  'app-input__field--readonly': props.readonly,
  'app-input__field--small': props.size === 'small',
  'app-input__field--large': props.size === 'large',
  'app-input__field--with-prefix': props.prefixIcon,
  'app-input__field--with-suffix': props.suffixIcon
}))

const onBlur = (event: FocusEvent) => {
  emit('blur', event)
}

const onFocus = (event: FocusEvent) => {
  emit('focus', event)
}

const onInput = (event: Event) => {
  emit('input', event)
}

const onChange = (event: Event) => {
  emit('change', event)
}
</script>

<style scoped>
.app-input {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  width: 100%;
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
}

.app-input__field-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.app-input__icon {
  position: absolute;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-secondary);
  width: 20px;
  height: 20px;
}

.app-input__icon--prefix {
  left: var(--space-3);
  z-index: 1;
}

.app-input__icon--suffix {
  right: var(--space-3);
  z-index: 1;
}

.app-input__field {
  width: 100%;
  height: var(--input-height);
  padding: 0 var(--space-4);
  font-size: var(--font-size-base);
  font-family: var(--font-family-base);
  color: var(--color-text-primary);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast) var(--transition-timing);
  outline: none;
}

.app-input__field--small {
  height: 2rem;
  font-size: var(--font-size-sm);
  padding: 0 var(--space-3);
}

.app-input__field--large {
  height: 3rem;
  font-size: var(--font-size-lg);
  padding: 0 var(--space-5);
}

.app-input__field--with-prefix {
  padding-left: calc(var(--space-4) + var(--space-5));
}

.app-input__field--with-suffix {
  padding-right: calc(var(--space-4) + var(--space-5));
}

.app-input__field:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-light);
}

.app-input__field--error {
  border-color: var(--color-danger);
}

.app-input__field--error:focus {
  border-color: var(--color-danger);
  box-shadow: 0 0 0 3px var(--color-danger-light);
}

.app-input__field:disabled {
  background: var(--color-surface-alt);
  cursor: not-allowed;
  opacity: 0.6;
}

.app-input__field:read-only {
  background: var(--color-surface-alt);
  cursor: default;
}

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
</style>
