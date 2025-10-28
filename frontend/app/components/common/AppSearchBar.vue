<template>
  <div class="app-search-bar" :class="searchBarClasses">
    <div class="search-bar__input-wrapper">
      <span class="search-bar__icon">
        <slot name="icon">🔍</slot>
      </span>
      
      <input
        ref="inputRef"
        v-model="searchValue"
        :type="inputType"
        :placeholder="placeholder"
        :disabled="disabled"
        :readonly="readonly"
        :maxlength="maxLength"
        :class="inputClasses"
        @focus="handleFocus"
        @blur="handleBlur"
        @input="handleInput"
        @keydown="handleKeydown"
        @paste="handlePaste"
      />
      
      <!-- Clear button -->
      <button
        v-if="showClearButton && searchValue"
        type="button"
        class="search-bar__clear"
        @click="clearSearch"
        aria-label="Clear search"
      >
        ×
      </button>
      
      <!-- Search button -->
      <button
        v-if="showSearchButton"
        type="button"
        class="search-bar__search"
        :disabled="!canSearch"
        @click="handleSearchClick"
        aria-label="Search"
      >
        🔍
      </button>
    </div>
    
    <!-- Search suggestions/dropdown -->
    <div v-if="showSuggestions && suggestions.length > 0" class="search-bar__suggestions">
      <div
        v-for="(suggestion, index) in suggestions"
        :key="getSuggestionKey(suggestion)"
        class="search-bar__suggestion"
        :class="{ 'search-bar__suggestion--active': index === activeSuggestionIndex }"
        @click="selectSuggestion(suggestion)"
        @mouseenter="activeSuggestionIndex = index"
      >
        <slot name="suggestion" :suggestion="suggestion">
          {{ formatSuggestion(suggestion) }}
        </slot>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  modelValue?: string
  placeholder?: string
  type?: 'text' | 'search' | 'email'
  disabled?: boolean
  readonly?: boolean
  clearable?: boolean
  searchable?: boolean
  showSuggestions?: boolean
  suggestions?: any[]
  maxLength?: number
  debounceMs?: number
  size?: 'sm' | 'md' | 'lg'
  variant?: 'default' | 'minimal'
  showSearchButton?: boolean
  showClearButton?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  type: 'search',
  disabled: false,
  readonly: false,
  clearable: true,
  searchable: true,
  showSuggestions: false,
  suggestions: () => [],
  maxLength: 255,
  debounceMs: 300,
  size: 'md',
  variant: 'default',
  showSearchButton: false,
  showClearButton: true
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  search: [value: string]
  clear: []
  select: [value: string, suggestion?: any]
  focus: [event: FocusEvent]
  blur: [event: FocusEvent]
}>()

// Reactive state
const searchValue = computed({
  get: () => props.modelValue ?? '',
  set: (value: string) => emit('update:modelValue', value)
})

const inputRef = ref<HTMLInputElement>()
const isFocused = ref(false)
const activeSuggestionIndex = ref(-1)

// Debounced search
const debouncedSearch = useDebounceFn((value: string) => {
  if (props.searchable) {
    emit('search', value)
  }
}, props.debounceMs)

// Computed classes
const searchBarClasses = computed(() => [
  'app-search-bar',
  `app-search-bar--${props.variant}`,
  {
    'app-search-bar--disabled': props.disabled,
    'app-search-bar--focused': isFocused.value,
    'app-search-bar--with-suggestions': props.showSuggestions && props.suggestions.length > 0
  }
])

const inputClasses = computed(() => [
  'search-bar__input',
  `search-bar__input--${props.size}`
])

const canSearch = computed(() => {
  return searchValue.value.trim().length > 0
})

const showClearButton = computed(() => {
  return props.clearable && props.showClearButton && searchValue.value.length > 0
})

// Event handlers
const handleInput = (event: Event) => {
  const target = event.target as HTMLInputElement
  emit('update:modelValue', target.value)
  
  // Reset active suggestion
  activeSuggestionIndex.value = -1
  
  // Trigger debounced search
  debouncedSearch(target.value)
}

const handleSearchClick = () => {
  if (canSearch.value) {
    emit('search', searchValue.value.trim())
  }
}

const handleKeydown = (event: KeyboardEvent) => {
  // Handle special keys
  switch (event.key) {
    case 'Enter':
      event.preventDefault()
      if (activeSuggestionIndex.value >= 0 && props.suggestions[activeSuggestionIndex.value]) {
        selectSuggestion(props.suggestions[activeSuggestionIndex.value])
      } else {
        handleSearchClick()
      }
      break
      
    case 'Escape':
      if (searchValue.value) {
        clearSearch()
        event.preventDefault()
      } else {
        inputRef.value?.blur()
      }
      break
      
    case 'ArrowDown':
      if (props.showSuggestions && props.suggestions.length > 0) {
        event.preventDefault()
        activeSuggestionIndex.value = Math.min(
          activeSuggestionIndex.value + 1,
          props.suggestions.length - 1
        )
      }
      break
      
    case 'ArrowUp':
      if (props.showSuggestions && props.suggestions.length > 0) {
        event.preventDefault()
        activeSuggestionIndex.value = Math.max(activeSuggestionIndex.value - 1, -1)
      }
      break
  }
}

const handleFocus = (event: FocusEvent) => {
  isFocused.value = true
  emit('focus', event)
}

const handleBlur = (event: FocusEvent) => {
  isFocused.value = false
  // Delay to allow click on suggestions
  setTimeout(() => {
    activeSuggestionIndex.value = -1
  }, 150)
  emit('blur', event)
}

const handlePaste = (event: ClipboardEvent) => {
  // Handle paste events if needed
  // This can be extended for advanced paste handling
}

const clearSearch = () => {
  searchValue.value = ''
  emit('clear')
  emit('search', '')
  inputRef.value?.focus()
}

const selectSuggestion = (suggestion: any) => {
  const value = formatSuggestion(suggestion)
  searchValue.value = value
  emit('select', value, suggestion)
  activeSuggestionIndex.value = -1
  inputRef.value?.blur()
}

const formatSuggestion = (suggestion: any): string => {
  if (typeof suggestion === 'string') return suggestion
  if (typeof suggestion === 'object' && suggestion !== null) {
    return suggestion.label || suggestion.name || suggestion.value || String(suggestion)
  }
  return String(suggestion)
}

const getSuggestionKey = (suggestion: any, index: number): string => {
  if (typeof suggestion === 'object' && suggestion !== null) {
    return suggestion.id || suggestion.key || `${index}-${formatSuggestion(suggestion)}`
  }
  return `${index}-${suggestion}`
}

// Expose methods
defineExpose({
  focus: () => inputRef.value?.focus(),
  blur: () => inputRef.value?.blur(),
  clear: clearSearch,
  selectSuggestion
})
</script>

<style scoped>
.app-search-bar {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.app-search-bar--minimal {
  gap: 0;
}

/* Input Wrapper */
.search-bar__input-wrapper {
  position: relative;
  display: flex;
  align-items: stretch;
}

.search-bar__icon {
  position: absolute;
  left: var(--space-3);
  top: 50%;
  transform: translateY(-50%);
  color: var(--color-text-muted);
  font-size: var(--font-size-base);
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}

/* Input */
.search-bar__input {
  width: 100%;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text-primary);
  font-family: var(--font-family-base);
  transition: all var(--transition-fast) var(--transition-timing);
  outline: none;
}

.search-bar__input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(168, 218, 220, 0.1);
}

.search-bar__input:disabled {
  background: var(--color-surface-alt);
  cursor: not-allowed;
  color: var(--color-text-muted);
}

.search-bar__input:read-only {
  background: var(--color-surface-alt);
}

/* Sizes */
.search-bar__input--sm {
  height: var(--space-8);
  padding: 0 var(--space-3) 0 calc(var(--space-3) + 20px + var(--space-2));
  font-size: var(--font-size-sm);
}

.search-bar__input--sm + .search-bar__clear,
.search-bar__input--sm + .search-bar__search {
  width: var(--space-8);
  height: var(--space-8);
}

.search-bar__input--md {
  height: var(--input-height);
  padding: 0 var(--space-4) 0 calc(var(--space-4) + 20px + var(--space-2));
  font-size: var(--font-size-base);
}

.search-bar__input--md + .search-bar__clear,
.search-bar__input--md + .search-bar__search {
  width: var(--input-height);
  height: var(--input-height);
}

.search-bar__input--lg {
  height: var(--space-12);
  padding: 0 var(--space-5) 0 calc(var(--space-5) + 20px + var(--space-2));
  font-size: var(--font-size-lg);
}

.search-bar__input--lg + .search-bar__clear,
.search-bar__input--lg + .search-bar__search {
  width: var(--space-12);
  height: var(--space-12);
}

/* Clear and Search Buttons */
.search-bar__clear,
.search-bar__search {
  position: absolute;
  top: 0;
  border: none;
  background: none;
  color: var(--color-text-muted);
  cursor: pointer;
  padding: var(--space-2);
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast) var(--transition-timing);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-lg);
  line-height: 1;
}

.search-bar__clear {
  right: var(--space-2);
}

.search-bar__search {
  right: var(--space-8);
}

.search-bar__clear:hover,
.search-bar__search:hover {
  color: var(--color-text-secondary);
  background: var(--color-surface-alt);
}

.search-bar__search:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Suggestions */
.search-bar__suggestions {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  z-index: var(--z-dropdown);
  max-height: 200px;
  overflow-y: auto;
  margin-top: var(--space-1);
}

.search-bar__suggestion {
  padding: var(--space-2) var(--space-3);
  cursor: pointer;
  transition: background-color var(--transition-fast) var(--transition-timing);
  border-bottom: 1px solid var(--color-border-light);
}

.search-bar__suggestion:last-child {
  border-bottom: none;
}

.search-bar__suggestion:hover,
.search-bar__suggestion--active {
  background: var(--color-surface-alt);
}

/* Variant: Minimal */
.app-search-bar--minimal .search-bar__input {
  border: none;
  border-bottom: 1px solid var(--color-border);
  border-radius: 0;
  background: transparent;
}

.app-search-bar--minimal .search-bar__input:focus {
  border-color: var(--color-primary);
  box-shadow: none;
}

.app-search-bar--minimal .search-bar__icon {
  left: var(--space-2);
}

/* Focused state */
.app-search-bar--focused .search-bar__input {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(168, 218, 220, 0.1);
}

/* Disabled state */
.app-search-bar--disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Mobile Responsive */
@media (max-width: 640px) {
  .search-bar__input--sm {
    height: var(--space-9);
  }
  
  .search-bar__input--md {
    height: var(--space-10);
  }
  
  .search-bar__input--lg {
    height: var(--space-12);
  }
}

/* Accessibility */
@media (prefers-reduced-motion: reduce) {
  .search-bar__input,
  .search-bar__clear,
  .search-bar__search,
  .search-bar__suggestion {
    transition: none;
  }
}

/* High contrast mode */
@media (prefers-contrast: high) {
  .search-bar__input {
    border-width: 2px;
  }
  
  .search-bar__suggestions {
    border-width: 2px;
  }
}
</style>
