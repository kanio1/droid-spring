# TOP 5 IMMEDIATE ACTIONS - TODO LIST
**Projekt:** BSS (Business Support System)
**Deadline:** 1 tydzień
**Priorytet:** KRYTYCZNY
**Status:** DO WYKONANIA

---

## 📋 CHECKLIST PROGRESSU

- [ ] 1. Tailwind CSS Integration
- [ ] 2. Replace Emoji Icons
- [ ] 3. Implement Dark Mode
- [ ] 4. Expand UI Components
- [ ] 5. Document Architecture

---

## 🎯 1. TAILWIND CSS INTEGRATION
**Czas wykonania:** 4 godziny
**Priorytet:** P0 - KRYTYCZNY
**Impact:** +50% productivity

### Kroki implementacji:

#### 1.1. Instalacja Tailwind
```bash
# 1. Zmień katalog na frontend
cd frontend

# 2. Zainstaluj Tailwind CSS
pnpm add -D @nuxtjs/tailwindcss

# 3. Zainstaluj typy TypeScript
pnpm add -D @types/node

# 4. Zainicjalizuj Tailwind
npx tailwindcss init -p
```

**Status:** ⏳

#### 1.2. Konfiguracja tailwind.config.js
**Plik:** `/frontend/tailwind.config.js`

**Zamień zawartość na:**
```javascript
/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./app/**/*.{js,ts,vue,html}",
    "./components/**/*.{js,ts,vue,html}",
    "./layouts/**/*.{js,ts,vue,html}",
    "./pages/**/*.{js,ts,vue,html}",
    "./plugins/**/*.{js,ts}",
    "./nuxt.config.{js,ts}"
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#f0f9ff',
          100: '#e0f2fe',
          200: '#bae6fd',
          300: '#7dd3fc',
          400: '#38bdf8',
          500: '#0ea5e9',
          600: '#0284c7',
          700: '#0369a1',
          800: '#075985',
          900: '#0c4a6e',
          DEFAULT: '#0ea5e9',
        },
        // Dodaj inne kolory z tokens.css
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
      spacing: {
        '18': '4.5rem',
        '88': '22rem',
      }
    },
  },
  plugins: [
    require('@tailwindcss/forms'),
    require('@tailwindcss/typography'),
  ],
}
```

**Status:** ⏳

#### 1.3. Aktualizacja assets/styles/main.css
**Plik:** `/frontend/assets/styles/main.css`

**Dodaj na początek:**
```css
@tailwind base;
@tailwind components;
@tailwind utilities;

@layer base {
  html {
    font-family: 'Inter', system-ui, sans-serif;
  }
}

@layer components {
  /* Custom component layer */
  .btn-primary {
    @apply bg-primary-600 text-white px-4 py-2 rounded-lg hover:bg-primary-700 transition-colors;
  }
}
```

**Status:** ⏳

#### 1.4. Aktualizacja nuxt.config.ts
**Plik:** `/frontend/nuxt.config.ts`

**Dodaj do modules:**
```typescript
export default defineNuxtConfig({
  modules: [
    '@nuxtjs/tailwindcss',
    '@pinia/nuxt',
  ],
  // ...
})
```

**Status:** ⏳

#### 1.5. Migracja pierwszych komponentów
**Pliki do refaktoryzacji:**
- `components/ui/AppButton.vue` - użyj Tailwind classes
- `layouts/default.vue` - zastąp CSS utility classes
- `pages/index.vue` - przykład migracji

**Status:** ⏳

#### 1.6. Weryfikacja
```bash
cd frontend
pnpm run dev
# Sprawdź czy build przechodzi
pnpm run build
```

**Status:** ⏳

---

## 🎯 2. REPLACE EMOJI ICONS
**Czas wykonania:** 2 godziny
**Priorytet:** P0 - KRYTYCZNY
**Impact:** Professional appearance

### Kroki implementacji:

#### 2.1. Instalacja Icon Library
```bash
cd frontend

# Opcja 1: Lucide Icons (zalecane - light, modern)
pnpm add @iconify-icon/lu

# Opcja 2: Heroicons
pnpm add @heroicons/vue

# Opcja 3: Phosphor Icons
pnpm add @iconify/vue
```

**Status:** ⏳

#### 2.2. Stwórz Icon komponent
**Plik:** `/frontend/components/ui/AppIcon.vue`

**Stwórz plik:**
```vue
<template>
  <component
    :is="iconComponent"
    :class="iconClass"
    v-bind="$attrs"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  name: string
  size?: 'sm' | 'md' | 'lg' | 'xl'
  class?: string
}

const props = withDefaults(defineProps<Props>(), {
  size: 'md',
  class: ''
})

const iconClass = computed(() => {
  const sizeMap = {
    sm: 'w-4 h-4',
    md: 'w-5 h-5',
    lg: 'w-6 h-6',
    xl: 'w-8 h-8'
  }
  return `${sizeMap[props.size]} ${props.class}`
})

// Dynamic import ikony
const iconComponent = computed(() => {
  // Przykład z Lucide
  const iconMap: Record<string, any> = {
    users: () => import('@iconify-icon/lu:users'),
    dashboard: () => import('@iconify-icon/lu:home'),
    settings: () => import('@iconify-icon/lu:settings'),
    // Dodaj więcej wg potrzeb
  }
  return iconMap[props.name] || iconMap['dashboard']
})
</script>
```

**Status:** ⏳

#### 2.3. Znajdź i zastąp emoji
```bash
# Znajdź pliki z emoji
cd frontend
grep -r "👥\|📊\|🏢\|📋\|⚙️\|🔍\|➕\|✏️\|🗑️" --include="*.vue" --include="*.ts" .

# Lista plików do aktualizacji:
# - layouts/default.vue
# - pages/customers/index.vue
# - components/*/*.vue
```

**Status:** ⏳

#### 2.4. Mapa zamian
**Plik:** `/frontend/ICON_MIGRATION_MAP.md`

```markdown
# Emoji → Icon mapping

👥 → users
📊 → bar-chart
🏢 → building
📋 → clipboard
⚙️ → settings
🔍 → search
➕ → plus
✏️ → edit
🗑️ → trash
✅ → check
❌ → x
```

**Status:** ⏳

#### 2.5. Aktualizacja layout/default.vue
**Plik:** `/frontend/layouts/default.vue`

**Znajdź linie 60-90:**
```vue
<!-- PRZED -->
<span>👥</span>

<!-- PO -->
<AppIcon name="users" size="md" class="text-gray-600" />
```

**Status:** ⏳

#### 2.6. Aktualizacja komponentów
**Pliki:**
- `pages/customers/index.vue` - zastąp emoji w przyciskach
- `components/customer/*` - ikony w listach
- `components/common/*` - ikony nawigacji

**Status:** ⏳

---

## 🎯 3. IMPLEMENT DARK MODE
**Czas wykonania:** 3 godziny
**Priorytet:** P0 - KRYTYCZNY
**Impact:** Modern UX

### Kroki implementacji:

#### 3.1. Rozszerz tokens.css
**Plik:** `/frontend/assets/styles/tokens.css`

**Dodaj na końcu:**
```css
/* Dark Mode Color Tokens */
:root[data-theme="dark"] {
  --color-background: #1a1a1a;
  --color-surface: #2d2d2d;
  --color-text-primary: #ffffff;
  --color-text-secondary: #a3a3a3;
  --color-border: #404040;

  --color-primary: #60a5fa;
  --color-secondary: #f87171;
  --color-accent: #34d399;
  --color-warning: #fbbf24;

  /* Adjust light variants for dark mode */
  --color-primary-light: #93c5fd;
  --color-secondary-light: #fca5a5;
  --color-accent-light: #6ee7b7;
}

/* High Contrast Dark Mode */
:root[data-theme="dark-contrast"] {
  --color-background: #000000;
  --color-surface: #1a1a1a;
  --color-text-primary: #ffffff;
  --color-text-secondary: #ffffff;
  --color-border: #ffffff;
}
```

**Status:** ⏳

#### 3.2. Aktualizuj base.css
**Plik:** `/frontend/assets/styles/base.css`

**Dodaj na początek:**
```css
/* Dark mode support */
@media (prefers-color-scheme: dark) {
  :root {
    --color-background: #1a1a1a;
    --color-surface: #2d2d2d;
    --color-text-primary: #ffffff;
    --color-text-secondary: #a3a3a3;
  }
}
```

**Status:** ⏳

#### 3.3. Stwórz composable useDarkMode
**Plik:** `/frontend/composables/useDarkMode.ts`

**Stwórz plik:**
```typescript
export const useDarkMode = () => {
  const isDark = useState<boolean>('dark-mode', () => false)

  // Initialize from localStorage
  onMounted(() => {
    const saved = localStorage.getItem('dark-mode')
    if (saved !== null) {
      isDark.value = saved === 'true'
    } else {
      // Use system preference
      isDark.value = window.matchMedia('(prefers-color-scheme: dark)').matches
    }
    applyTheme()
  })

  const applyTheme = () => {
    if (process.client) {
      const root = document.documentElement
      if (isDark.value) {
        root.setAttribute('data-theme', 'dark')
        localStorage.setItem('dark-mode', 'true')
      } else {
        root.setAttribute('data-theme', 'light')
        localStorage.setItem('dark-mode', 'false')
      }
    }
  }

  const toggle = () => {
    isDark.value = !isDark.value
    applyTheme()
  }

  const setTheme = (theme: 'light' | 'dark') => {
    isDark.value = theme === 'dark'
    applyTheme()
  }

  return {
    isDark: readonly(isDark),
    toggle,
    setTheme
  }
}
```

**Status:** ⏳

#### 3.4. Aktualizuj Tailwind config
**Plik:** `/frontend/tailwind.config.js`

**Dodaj darkMode:**
```javascript
export default {
  darkMode: 'class', // or 'media'
  // ...
}
```

**Status:** ⏳

#### 3.5. Stwórz ThemeToggle komponent
**Plik:** `/frontend/components/ui/ThemeToggle.vue`

**Stwórz plik:**
```vue
<template>
  <button
    @click="toggle"
    class="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
    :aria-label="isDark ? 'Switch to light mode' : 'Switch to dark mode'"
  >
    <AppIcon
      :name="isDark ? 'sun' : 'moon'"
      size="md"
      class="text-gray-700 dark:text-gray-300"
    />
  </button>
</template>

<script setup lang="ts">
const { isDark, toggle } = useDarkMode()
</script>
```

**Status:** ⏳

#### 3.6. Integruj w layout
**Plik:** `/frontend/layouts/default.vue`

**Znajdź header section (~linia 30) i dodaj:**
```vue
<!-- W sekcji z użytkownikiem -->
<div class="flex items-center gap-2">
  <ThemeToggle />
  <!-- reszta header -->
</div>
```

**Status:** ⏳

#### 3.7. Aktualizuj style komponentów
**Przykład AppButton.vue:**
```vue
<template>
  <button
    :class="[
      'px-4 py-2 rounded-lg font-medium transition-colors',
      variant === 'primary'
        ? 'bg-primary-600 text-white hover:bg-primary-700 dark:bg-primary-500 dark:hover:bg-primary-600'
        : 'bg-gray-200 text-gray-900 hover:bg-gray-300 dark:bg-gray-700 dark:text-gray-100 dark:hover:bg-gray-600',
      size === 'sm' ? 'px-3 py-1.5 text-sm' : '',
      size === 'lg' ? 'px-6 py-3 text-lg' : '',
      disabled ? 'opacity-50 cursor-not-allowed' : '',
      className
    ]"
    :disabled="disabled"
    v-bind="$attrs"
  >
    <slot />
  </button>
</template>
```

**Status:** ⏳

#### 3.8. Weryfikacja
```bash
# Sprawdź czy dark mode działa
pnpm run dev
# 1. Kliknij toggle w header
# 2. Sprawdź czy strona się ciemnieje
# 3. Odśwież stronę - czy ustawienia się zapisują?
# 4. Sprawdź w dev tools czy data-theme jest ustawione
```

**Status:** ⏳

---

## 🎯 4. EXPAND UI COMPONENTS
**Czas wykonania:** 16 godzin (2 dni)
**Priorytet:** P0 - KRYTYCZNY
**Impact:** Development speed

### Lista komponentów do stworzenia:

#### 4.1. Form Components (4h)
**Priorytet:** P0

##### 4.1.1. AppInput.vue
**Plik:** `/frontend/components/ui/AppInput.vue`
**Czas:** 30 min

```vue
<template>
  <div class="w-full">
    <label
      v-if="label"
      :for="inputId"
      class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1"
    >
      {{ label }}
      <span v-if="required" class="text-red-500">*</span>
    </label>
    <input
      :id="inputId"
      v-model="modelValue"
      :type="type"
      :placeholder="placeholder"
      :disabled="disabled"
      :readonly="readonly"
      :class="[
        'w-full px-3 py-2 rounded-lg border transition-colors',
        'focus:outline-none focus:ring-2 focus:ring-primary-500',
        'dark:bg-gray-800 dark:border-gray-700 dark:text-white',
        error
          ? 'border-red-500 focus:ring-red-500'
          : 'border-gray-300 dark:border-gray-600',
        disabled ? 'opacity-50 cursor-not-allowed' : '',
        className
      ]"
      v-bind="$attrs"
    />
    <p
      v-if="error"
      class="mt-1 text-sm text-red-600 dark:text-red-400"
    >
      {{ error }}
    </p>
    <p
      v-else-if="hint"
      class="mt-1 text-sm text-gray-500 dark:text-gray-400"
    >
      {{ hint }}
    </p>
  </div>
</template>

<script setup lang="ts">
interface Props {
  modelValue?: string | number
  label?: string
  type?: string
  placeholder?: string
  hint?: string
  error?: string
  required?: boolean
  disabled?: boolean
  readonly?: boolean
  className?: string
}

const props = withDefaults(defineProps<Props>(), {
  type: 'text',
  required: false,
  disabled: false,
  readonly: false
})

const emit = defineEmits<{
  'update:modelValue': [value: string | number]
}>()

const inputId = `input-${Math.random().toString(36).substr(2, 9)}`
</script>
```

**Status:** ⏳

##### 4.1.2. AppSelect.vue
**Plik:** `/frontend/components/ui/AppSelect.vue`
**Czas:** 30 min

```vue
<template>
  <div class="w-full">
    <label
      v-if="label"
      :for="selectId"
      class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1"
    >
      {{ label }}
      <span v-if="required" class="text-red-500">*</span>
    </label>
    <select
      :id="selectId"
      v-model="modelValue"
      :disabled="disabled"
      :class="[
        'w-full px-3 py-2 rounded-lg border transition-colors appearance-none',
        'focus:outline-none focus:ring-2 focus:ring-primary-500',
        'dark:bg-gray-800 dark:border-gray-700 dark:text-white',
        error
          ? 'border-red-500 focus:ring-red-500'
          : 'border-gray-300 dark:border-gray-600',
        disabled ? 'opacity-50 cursor-not-allowed' : '',
        className
      ]"
      v-bind="$attrs"
    >
      <option
        v-if="placeholder"
        value=""
        disabled
        selected
      >
        {{ placeholder }}
      </option>
      <option
        v-for="option in options"
        :key="option.value"
        :value="option.value"
      >
        {{ option.label }}
      </option>
    </select>
    <p
      v-if="error"
      class="mt-1 text-sm text-red-600 dark:text-red-400"
    >
      {{ error }}
    </p>
  </div>
</template>

<script setup lang="ts">
interface Option {
  label: string
  value: string | number
}

interface Props {
  modelValue?: string | number
  options: Option[]
  label?: string
  placeholder?: string
  error?: string
  required?: boolean
  disabled?: boolean
  className?: string
}

const props = withDefaults(defineProps<Props>(), {
  required: false,
  disabled: false
})

const emit = defineEmits<{
  'update:modelValue': [value: string | number]
}>()

const selectId = `select-${Math.random().toString(36).substr(2, 9)}`
</script>
```

**Status:** ⏳

##### 4.1.3. AppTextarea.vue
**Plik:** `/frontend/components/ui/AppTextarea.vue`
**Czas:** 20 min

```vue
<template>
  <div class="w-full">
    <label
      v-if="label"
      :for="textareaId"
      class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1"
    >
      {{ label }}
      <span v-if="required" class="text-red-500">*</span>
    </label>
    <textarea
      :id="textareaId"
      v-model="modelValue"
      :rows="rows"
      :placeholder="placeholder"
      :disabled="disabled"
      :readonly="readonly"
      :class="[
        'w-full px-3 py-2 rounded-lg border transition-colors resize-vertical',
        'focus:outline-none focus:ring-2 focus:ring-primary-500',
        'dark:bg-gray-800 dark:border-gray-700 dark:text-white',
        error
          ? 'border-red-500 focus:ring-red-500'
          : 'border-gray-300 dark:border-gray-600',
        disabled ? 'opacity-50 cursor-not-allowed' : '',
        className
      ]"
      v-bind="$attrs"
    />
    <p
      v-if="error"
      class="mt-1 text-sm text-red-600 dark:text-red-400"
    >
      {{ error }}
    </p>
  </div>
</template>

<script setup lang="ts">
interface Props {
  modelValue?: string
  label?: string
  placeholder?: string
  rows?: number
  error?: string
  required?: boolean
  disabled?: boolean
  readonly?: boolean
  className?: string
}

withDefaults(defineProps<Props>(), {
  rows: 4,
  required: false,
  disabled: false,
  readonly: false
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const textareaId = `textarea-${Math.random().toString(36).substr(2, 9)}`
</script>
```

**Status:** ⏳

##### 4.1.4. AppCheckbox.vue
**Plik:** `/frontend/components/ui/AppCheckbox.vue`
**Czas:** 20 min

##### 4.1.5. AppRadio.vue
**Plik:** `/frontend/components/ui/AppRadio.vue`
**Czas:** 20 min

#### 4.2. Layout Components (4h)
**Priorytet:** P0

##### 4.2.1. AppCard.vue
**Plik:** `/frontend/components/ui/AppCard.vue`
**Czas:** 30 min

```vue
<template>
  <div
    :class="[
      'bg-white dark:bg-gray-800 rounded-lg shadow-sm border',
      'border-gray-200 dark:border-gray-700',
      padding ? `p-${padding}` : 'p-6',
      rounded ? `rounded-${rounded}` : 'rounded-lg',
      className
    ]"
    v-bind="$attrs"
  >
    <div v-if="title || $slots.header" class="mb-4">
      <slot name="header">
        <h3 class="text-lg font-semibold text-gray-900 dark:text-white">
          {{ title }}
        </h3>
      </slot>
    </div>

    <div class="mb-4">
      <slot />
    </div>

    <div v-if="$slots.footer">
      <slot name="footer" />
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  title?: string
  padding?: 'sm' | 'md' | 'lg'
  rounded?: 'sm' | 'md' | 'lg' | 'xl'
  className?: string
}

withDefaults(defineProps<Props>(), {
  padding: 'lg',
  rounded: 'lg'
})
</script>
```

**Status:** ⏳

##### 4.2.2. AppModal.vue
**Plik:** `/frontend/components/ui/AppModal.vue`
**Czas:** 60 min

```vue
<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition-opacity duration-200"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition-opacity duration-150"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="modelValue"
        class="fixed inset-0 z-50 flex items-center justify-center p-4"
        @click.self="close"
      >
        <div class="absolute inset-0 bg-black/50" @click="close" />

        <Transition
          enter-active-class="transition-all duration-200"
          enter-from-class="opacity-0 scale-95"
          enter-to-class="opacity-100 scale-100"
          leave-active-class="transition-all duration-150"
          leave-from-class="opacity-100 scale-100"
          leave-to-class="opacity-0 scale-95"
        >
          <div
            v-if="modelValue"
            :class="[
              'relative bg-white dark:bg-gray-800 rounded-lg shadow-xl',
              'w-full max-w-md max-h-[90vh] overflow-y-auto',
              size === 'sm' ? 'max-w-sm' : '',
              size === 'lg' ? 'max-w-2xl' : '',
              size === 'xl' ? 'max-w-4xl' : '',
              className
            ]"
          >
            <div v-if="showHeader" class="flex items-center justify-between p-6 border-b border-gray-200 dark:border-gray-700">
              <h3 class="text-xl font-semibold text-gray-900 dark:text-white">
                {{ title }}
              </h3>
              <button
                @click="close"
                class="p-2 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
              >
                <AppIcon name="x" size="md" />
              </button>
            </div>

            <div class="p-6">
              <slot />
            </div>

            <div v-if="$slots.footer" class="flex justify-end gap-2 p-6 border-t border-gray-200 dark:border-gray-700">
              <slot name="footer" />
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
interface Props {
  modelValue: boolean
  title?: string
  size?: 'sm' | 'md' | 'lg' | 'xl'
  showHeader?: boolean
  closeOnBackdrop?: boolean
  className?: string
}

const props = withDefaults(defineProps<Props>(), {
  size: 'md',
  showHeader: true,
  closeOnBackdrop: true
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const close = () => {
  if (props.closeOnBackdrop) {
    emit('update:modelValue', false)
  }
}
</script>
```

**Status:** ⏳

##### 4.2.3. AppDropdown.vue
**Plik:** `/frontend/components/ui/AppDropdown.vue`
**Czas:** 45 min

##### 4.2.4. AppTooltip.vue
**Plik:** `/frontend/components/ui/AppTooltip.vue`
**Czas:** 30 min

##### 4.2.5. AppBadge.vue
**Plik:** `/frontend/components/ui/AppBadge.vue`
**Czas:** 20 min

#### 4.3. Navigation Components (4h)
**Priorytet:** P0

##### 4.3.1. AppBreadcrumbs.vue
**Plik:** `/frontend/components/ui/AppBreadcrumbs.vue`
**Czas:** 30 min

##### 4.3.2. AppPagination.vue
**Plik:** `/frontend/components/ui/AppPagination.vue`
**Czas:** 60 min

##### 4.3.3. AppTabs.vue
**Plik:** `/frontend/components/ui/AppTabs.vue`
**Czas:** 45 min

##### 4.3.4. AppStepper.vue
**Plik:** `/frontend/components/ui/AppStepper.vue`
**Czas:** 45 min

#### 4.4. Feedback Components (4h)
**Priorytet:** P1

##### 4.4.1. AppAlert.vue
**Plik:** `/frontend/components/ui/AppAlert.vue`
**Czas:** 30 min

##### 4.4.2. AppToast.vue
**Plik:** `/frontend/components/ui/AppToast.vue`
**Czas:** 60 min

##### 4.4.3. AppProgress.vue
**Plik:** `/frontend/components/ui/AppProgress.vue`
**Czas:** 30 min

##### 4.4.4. AppSkeleton.vue
**Plik:** `/frontend/components/ui/AppSkeleton.vue`
**Czas:** 30 min

##### 4.4.5. AppSpinner.vue
**Plik:** `/frontend/components/ui/AppSpinner.vue`
**Czas:** 20 min

#### 4.5. Weryfikacja
```bash
cd frontend

# 1. Sprawdź czy wszystkie komponenty się kompilują
pnpm run build

# 2. Sprawdź czy TypeScript nie ma błędów
pnpm run typecheck

# 3. Stwórz przykładową stronę testową
# pages/test-components.vue - showcase wszystkich komponentów
```

**Status:** ⏳

---

## 🎯 5. DOCUMENT ARCHITECTURE
**Czas wykonania:** 4 godziny
**Priorytet:** P0 - KRYTYCZNY
**Impact:** Team velocity

### Kroki dokumentacji:

#### 5.1. Stwórz Architecture Overview
**Plik:** `/frontend/ARCHITECTURE.md`

**Stwórz plik:**
```markdown
# Frontend Architecture

## Tech Stack
- **Framework:** Nuxt 3.5
- **Language:** TypeScript 5.6
- **State Management:** Pinia
- **Styling:** Tailwind CSS
- **Icons:** Lucide Icons
- **Testing:** Vitest, Playwright

## Project Structure

```
frontend/
├── app/                    # Nuxt 3 app directory
│   ├── components/         # Vue components
│   │   ├── ui/            # Base UI components
│   │   ├── common/        # Shared components
│   │   └── customer/      # Domain-specific
│   ├── pages/             # Route pages
│   ├── layouts/           # Layout components
│   ├── composables/       # Composition API hooks
│   ├── stores/            # Pinia stores
│   ├── middleware/        # Route middleware
│   ├── plugins/           # Nuxt plugins
│   └── types/             # TypeScript types
├── assets/                # Static assets
│   └── styles/            # CSS/SCSS files
├── public/                # Public files
└── tests/                 # Test files
```

## Design System

### Color Palette
- Primary: Blue (#0ea5e9)
- Secondary: Pink (#ec4899)
- Success: Green (#10b981)
- Warning: Orange (#f59e0b)
- Error: Red (#ef4444)

### Typography
- Font: Inter
- Sizes: 12px - 30px
- Weights: 400 (normal), 500 (medium), 600 (semibold), 700 (bold)

### Spacing
- Base unit: 0.25rem (4px)
- Scale: 4px, 8px, 12px, 16px, 24px, 32px, 48px, 64px

## Components

### UI Components
Located in `components/ui/`

- `AppButton` - Button with variants
- `AppInput` - Text input with validation
- `AppSelect` - Dropdown select
- `AppModal` - Modal dialog
- `AppCard` - Content container
- `AppTable` - Data table
- `AppIcon` - Icon wrapper
- `ThemeToggle` - Dark mode switcher

### Usage Example
```vue
<template>
  <AppCard title="Customer Details">
    <AppInput
      v-model="customer.name"
      label="Name"
      required
    />
    <template #footer>
      <AppButton @click="save">
        Save
      </AppButton>
    </template>
  </AppCard>
</template>
```

## State Management

### Pinia Stores
Each domain has its own store in `stores/`

```typescript
// stores/customer.ts
export const useCustomerStore = defineStore('customer', {
  state: () => ({
    customers: [] as Customer[],
    loading: false
  }),

  actions: {
    async fetchCustomers() {
      // Implementation
    }
  }
})
```

## Dark Mode

### Implementation
Dark mode is implemented using CSS custom properties and data attributes.

```typescript
// composables/useDarkMode.ts
const { isDark, toggle } = useDarkMode()
```

### Usage
- System preference detection
- Toggle button in header
- Persisted in localStorage

## Development

### Commands
```bash
# Development server
pnpm run dev

# Build for production
pnpm run build

# Type checking
pnpm run typecheck

# Run tests
pnpm run test:unit
pnpm run test:e2e
```

### Guidelines
1. Use TypeScript for all files
2. Follow component naming convention (PascalCase)
3. Use composition API (script setup)
4. Keep components small and focused
5. Use Pinia for state management
6. Use Tailwind for styling
7. Write tests for critical components

## Best Practices

### Component Structure
```vue
<template>
  <!-- Template -->
</template>

<script setup lang="ts">
// TypeScript logic
</script>

<style scoped>
/* Scoped styles if needed */
</style>
```

### Props and Emits
```typescript
interface Props {
  title: string
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
})

const emit = defineEmits<{
  'update:value': [value: string]
  'click': []
}>()
```

## Resources
- [Nuxt 3 Docs](https://nuxt.com/docs)
- [Vue 3 Docs](https://vuejs.org/)
- [Tailwind CSS](https://tailwindcss.com/)
- [Pinia](https://pinia.vuejs.org/)
- [TypeScript](https://www.typescriptlang.org/)
```

**Status:** ⏳

#### 5.2. Stwórz Component Library Documentation
**Plik:** `/frontend/COMPONENTS.md`

**Stwórz plik:**
```markdown
# Component Library

## UI Components

### AppButton

#### Props
- `variant`: 'primary' | 'secondary' | 'danger' | 'ghost' (default: 'primary')
- `size`: 'sm' | 'md' | 'lg' (default: 'md')
- `disabled`: boolean (default: false)
- `loading`: boolean (default: false)
- `fullWidth`: boolean (default: false)

#### Usage
```vue
<AppButton variant="primary" size="md">
  Click me
</AppButton>

<AppButton variant="secondary" full-width>
  Full width button
</AppButton>
```

### AppInput

#### Props
- `modelValue`: string | number
- `label`: string
- `type`: string (default: 'text')
- `placeholder`: string
- `hint`: string
- `error`: string
- `required`: boolean (default: false)
- `disabled`: boolean (default: false)

#### Usage
```vue
<AppInput
  v-model="email"
  label="Email"
  type="email"
  placeholder="Enter email"
  required
/>
```

### AppSelect

#### Props
- `modelValue`: string | number
- `options`: Array<{ label: string, value: string | number }>
- `label`: string
- `placeholder`: string
- `error`: string
- `required`: boolean (default: false)
- `disabled`: boolean (default: false)

#### Usage
```vue
<AppSelect
  v-model="status"
  :options="statusOptions"
  label="Status"
  placeholder="Select status"
/>
```

### AppModal

#### Props
- `modelValue`: boolean
- `title`: string
- `size`: 'sm' | 'md' | 'lg' | 'xl' (default: 'md')
- `showHeader`: boolean (default: true)
- `closeOnBackdrop`: boolean (default: true)

#### Slots
- `default` - Modal content
- `header` - Custom header
- `footer` - Custom footer

#### Usage
```vue
<AppModal v-model="showModal" title="Create Customer">
  <form>
    <!-- Form content -->
  </form>
  <template #footer>
    <AppButton @click="showModal = false">
      Cancel
    </AppButton>
    <AppButton variant="primary" @click="save">
      Save
    </AppButton>
  </template>
</AppModal>
```

### AppCard

#### Props
- `title`: string
- `padding`: 'sm' | 'md' | 'lg' (default: 'lg')
- `rounded`: 'sm' | 'md' | 'lg' | 'xl' (default: 'lg')

#### Slots
- `default` - Card content
- `header` - Custom header
- `footer` - Custom footer

#### Usage
```vue
<AppCard title="Customer Details" padding="lg">
  <p>Card content</p>
  <template #footer>
    <AppButton>Action</AppButton>
  </template>
</AppCard>
```

### AppIcon

#### Props
- `name`: string (icon name from Lucide)
- `size`: 'sm' | 'md' | 'lg' | 'xl' (default: 'md')
- `class`: string (additional classes)

#### Available Icons
- users, user, settings, search, plus, minus
- edit, trash, check, x, eye, eye-off
- home, dashboard, menu, bell, calendar
- And more from Lucide icon set

#### Usage
```vue
<AppIcon name="users" size="md" class="text-blue-500" />
```

### ThemeToggle

#### Usage
```vue
<ThemeToggle />
```

Automatically switches between light and dark mode. State is persisted to localStorage.

## Customization

### Colors
Colors can be customized in `tailwind.config.js`:

```javascript
module.exports = {
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#f0f9ff',
          500: '#0ea5e9',
          900: '#0c4a6e',
        }
      }
    }
  }
}
```

### Component Variants
Most components support variants through props. See individual component documentation above.

## Guidelines

1. **Import Components**
   ```vue
   <script setup lang="ts">
   import AppButton from '~/components/ui/AppButton.vue'
   import AppInput from '~/components/ui/AppInput.vue'
   </script>
   ```

2. **Use TypeScript**
   ```vue
   <script setup lang="ts">
   interface Props {
     title: string
   }

   defineProps<Props>()
   </script>
   ```

3. **Keep Props Optional with Defaults**
   ```vue
   withDefaults(defineProps<Props>(), {
     variant: 'primary',
     size: 'md'
   })
   ```

4. **Use Semantic HTML**
   ```vue
   <!-- Good -->
   <AppButton type="submit">Submit</AppButton>

   <!-- Avoid -->
   <AppButton @click="submit">Submit</AppButton>
   ```
```

**Status:** ⏳

#### 5.3. Stwórz Development Guide
**Plik:** `/frontend/DEVELOPMENT.md`

**Stwórz plik:**
```markdown
# Development Guide

## Getting Started

### Prerequisites
- Node.js 20.x
- pnpm 9.x
- Git

### Setup
```bash
# Clone repository
git clone <repository-url>
cd droid-spring/frontend

# Install dependencies
pnpm install

# Start development server
pnpm run dev
```

Visit `http://localhost:3000`

## Development Workflow

### 1. Create a New Component

```bash
# Create component file
touch components/ui/NewComponent.vue

# Or use the script
./scripts/create-component.sh NewComponent
```

### 2. Component Template
```vue
<template>
  <div class="new-component">
    <!-- Template -->
  </div>
</template>

<script setup lang="ts">
interface Props {
  // Props definition
}

withDefaults(defineProps<Props>(), {
  // Default values
})

const emit = defineEmits<{
  // Emits definition
}>()
</script>

<style scoped>
.new-component {
  /* Styles */
}
</style>
```

### 3. Add to Component Library
Update `components/ui/index.ts`:
```typescript
export { default as NewComponent } from './NewComponent.vue'
```

Update `COMPONENTS.md` with documentation

### 4. Testing
```bash
# Unit tests
pnpm run test:unit NewComponent

# E2E tests
pnpm run test:e2e --grep "NewComponent"
```

### 5. Commit
```bash
git add .
git commit -m "feat: add NewComponent"
```

## Coding Standards

### TypeScript
- Always use TypeScript
- Define interfaces for props and emits
- Use `withDefaults` for optional props
- Avoid `any` type

### Naming Conventions
- **Components**: PascalCase (`CustomerList.vue`)
- **Files**: kebab-case (`customer-list.ts`)
- **Variables**: camelCase (`customerList`)
- **Constants**: UPPER_SNAKE_CASE (`API_BASE_URL`)

### File Organization
```
components/
├── ui/              # Base UI components
├── common/          # Shared components
├── customer/        # Domain components
│   ├── forms/       # Form components
│   ├── lists/       # List components
│   └── details/     # Detail views
└── ...
```

### Props Naming
```vue
<!-- Good -->
<Component
  :is-loading="loading"
  :show-title="true"
  @update:value="handleUpdate"
/>

<!-- Avoid -->
<Component
  :loading="loading"
  :title="true"
  @updateValue="handleUpdate"
/>
```

## Styling Guidelines

### Tailwind CSS
- Use utility classes for most styling
- Avoid custom CSS unless necessary
- Use design tokens from `assets/styles/tokens.css`

```vue
<!-- Good -->
<div class="flex items-center justify-between p-4 bg-white rounded-lg shadow">

<!-- Avoid -->
<div class="custom-container">
```

### Custom CSS
```css
@layer components {
  .custom-component {
    @apply bg-white dark:bg-gray-800;
  }
}
```

### Dark Mode
Always support dark mode:
```vue
<div class="bg-white dark:bg-gray-800 text-gray-900 dark:text-white">
  <!-- Content -->
</div>
```

## State Management

### Pinia Store
```typescript
export const useStore = defineStore('name', () => {
  // State
  const state = ref(initialValue)

  // Getters
  const getter = computed(() => state.value)

  // Actions
  const action = async () => {
    // Implementation
  }

  return { state, getter, action }
})
```

### Store Usage
```vue
<script setup lang="ts">
const store = useStore()

// In template
<template>
  <div>{{ store.getter }}</div>
</template>
```

## API Calls

### useApi Composable
```typescript
const { data, error, loading } = await useApi('/customers', {
  method: 'GET'
})
```

### Error Handling
```vue
<script setup lang="ts">
const { data, error } = await useApi('/customers')

if (error.value) {
  console.error('Failed to fetch customers:', error.value)
}
</script>
```

## Testing

### Unit Tests
```bash
# Run all tests
pnpm run test:unit

# Run specific test
pnpm run test:unit CustomerForm

# Run with coverage
pnpm run test:unit --coverage
```

### Test Structure
```typescript
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import Component from './Component.vue'

describe('Component', () => {
  it('renders properly', () => {
    const wrapper = mount(Component, {
      props: { title: 'Hello' }
    })
    expect(wrapper.text()).toContain('Hello')
  })
})
```

### E2E Tests
```bash
# Run E2E tests
pnpm run test:e2e

# Run in UI mode
pnpm run test:e2e --ui

# Debug
pnpm run test:e2e --debug
```

## Troubleshooting

### Build Errors
```bash
# Clear cache
rm -rf .nuxt .output
pnpm install

# Type check
pnpm run typecheck
```

### Performance Issues
```bash
# Bundle analyzer
pnpm run build --analyze
```

### Dark Mode Not Working
1. Check if `data-theme` attribute is set on `<html>`
2. Verify CSS custom properties are defined
3. Check browser localStorage

## Resources
- [Nuxt 3 Documentation](https://nuxt.com/docs)
- [Vue 3 Documentation](https://vuejs.org/)
- [Tailwind CSS](https://tailwindcss.com/docs)
- [Pinia Documentation](https://pinia.vuejs.org/)
```

**Status:** ⏳

#### 5.4. Stwórz API Documentation
**Plik:** `/frontend/API.md`

**Stwórz plik:**
```markdown
# API Reference

## Composables

### useApi

#### Parameters
```typescript
interface UseApiOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  headers?: Record<string, string>
  body?: any
  params?: Record<string, any>
}
```

#### Returns
```typescript
interface UseApiReturn<T> {
  data: Ref<T | null>
  error: Ref<Error | null>
  loading: Ref<boolean>
  execute: (url: string, options?: UseApiOptions) => Promise<void>
}
```

#### Usage
```typescript
const { data, loading, error } = await useApi('/customers')

// POST request
await useApi('/customers', {
  method: 'POST',
  body: { name: 'John Doe' }
})
```

### useAuth

#### Methods
- `login(credentials: LoginCredentials): Promise<void>`
- `logout(): Promise<void>`
- `refreshToken(): Promise<void>`
- `hasRole(role: string): boolean`
- `isAuthenticated(): boolean`

#### Properties
- `user: Ref<User | null>`
- `isAuthenticated: Ref<boolean>`
- `loading: Ref<boolean>`

#### Usage
```typescript
const { user, login, logout } = useAuth()

await login({ email, password })
```

### useDarkMode

#### Methods
- `toggle(): void`
- `setTheme(theme: 'light' | 'dark'): void`

#### Properties
- `isDark: Ref<boolean>` (readonly)

#### Usage
```typescript
const { isDark, toggle } = useDarkMode()
```

### useToast

#### Methods
- `show(message: string, type?: 'success' | 'error' | 'info' | 'warning'): void`
- `hide(): void`

#### Usage
```typescript
const toast = useToast()
toast.show('Customer created!', 'success')
```

## Stores

### useCustomerStore

#### State
- `customers: Customer[]`
- `loading: boolean`
- `error: string | null`

#### Actions
- `fetchCustomers(): Promise<void>`
- `getCustomer(id: string): Promise<Customer | null>`
- `createCustomer(data: CreateCustomerData): Promise<Customer>`
- `updateCustomer(id: string, data: UpdateCustomerData): Promise<Customer>`
- `deleteCustomer(id: string): Promise<void>`

#### Getters
- `customerById(id: string): Customer | undefined`
- `activeCustomers: Customer[]`

#### Usage
```typescript
const customerStore = useCustomerStore()

await customerStore.fetchCustomers()
```

### useOrderStore

#### State
- `orders: Order[]`
- `loading: boolean`

#### Actions
- `fetchOrders(): Promise<void>`
- `createOrder(data: CreateOrderData): Promise<Order>`
- `updateOrderStatus(id: string, status: OrderStatus): Promise<void>`

### useInvoiceStore

#### State
- `invoices: Invoice[]`
- `loading: boolean`

#### Actions
- `fetchInvoices(): Promise<void>`
- `generateInvoice(orderId: string): Promise<Invoice>`
- `sendInvoice(id: string): Promise<void>`

## Types

### Customer
```typescript
interface Customer {
  id: string
  firstName: string
  lastName: string
  email: string
  phone?: string
  status: 'active' | 'inactive' | 'pending'
  createdAt: Date
  updatedAt: Date
}
```

### Order
```typescript
interface Order {
  id: string
  customerId: string
  items: OrderItem[]
  total: number
  status: 'pending' | 'processing' | 'shipped' | 'delivered' | 'cancelled'
  createdAt: Date
}
```

### Invoice
```typescript
interface Invoice {
  id: string
  orderId: string
  amount: number
  status: 'draft' | 'sent' | 'paid' | 'overdue'
  dueDate: Date
}
```

## Error Handling

### Global Error Handler
```typescript
// plugins/errorHandler.ts
export default defineNuxtPlugin((nuxtApp) => {
  nuxtApp.hook('app:error', (error) => {
    console.error('Global error:', error)
  })
})
```

### API Error Format
```typescript
interface ApiError {
  message: string
  code: string
  details?: Record<string, any>
}
```

## Authentication

### JWT Token
- Stored in httpOnly cookie
- Automatically sent with requests
- Refreshed on expiration

### Protected Routes
```typescript
// middleware/auth.ts
export default defineNuxtRouteMiddleware((to) => {
  const { isAuthenticated } = useAuth()

  if (!isAuthenticated.value) {
    return navigateTo('/login')
  }
})
```

## Real-time Updates

### Server-Sent Events
```typescript
// Connect to SSE
const eventSource = new EventSource('/api/events/stream')

eventSource.onmessage = (event) => {
  const data = JSON.parse(event.data)
  // Handle event
}
```

### Event Types
- `customer.created`
- `customer.updated`
- `order.status_changed`
- `invoice.paid`

## Performance

### Caching
- API responses cached in Pinia stores
- Automatic cache invalidation on updates
- Manual cache refresh available

### Lazy Loading
```typescript
// Route-based code splitting
const CustomerDetails = defineAsyncComponent(
  () => import('~/pages/customers/[id].vue')
)
```

### Image Optimization
```vue
<img
  src="/image.jpg"
  loading="lazy"
  width="400"
  height="300"
/>
```
```

**Status:** ⏳

#### 5.5. Stwórz README.md dla Frontend
**Plik:** `/frontend/README.md`

**Stwórz plik:**
```markdown
# BSS Frontend

Modern, responsive web application for Business Support System built with Nuxt 3.

## 🚀 Features

- **Nuxt 3** - Full-stack framework
- **TypeScript** - Type safety
- **Tailwind CSS** - Utility-first styling
- **Pinia** - State management
- **Dark Mode** - Built-in theme support
- **Testing** - Vitest + Playwright
- **Accessibility** - WCAG 2.1 compliant

## 📦 Tech Stack

- **Framework:** Nuxt 3.5
- **Language:** TypeScript 5.6
- **State:** Pinia 2.2
- **Styling:** Tailwind CSS 3.4
- **Icons:** Lucide Icons
- **Testing:** Vitest, Playwright
- **Auth:** Keycloak

## 🛠️ Quick Start

### Prerequisites
- Node.js 20.x
- pnpm 9.x

### Installation
```bash
# Install dependencies
pnpm install

# Start dev server
pnpm run dev
```

Visit [http://localhost:3000](http://localhost:3000)

### Build
```bash
# Production build
pnpm run build

# Preview build
pnpm run preview
```

## 📚 Documentation

- [Architecture](./ARCHITECTURE.md) - System design
- [Components](./COMPONENTS.md) - UI component library
- [Development](./DEVELOPMENT.md) - Development guide
- [API](./API.md) - API reference

## 🎨 Design System

### Colors
- Primary: Blue (#0ea5e9)
- Secondary: Pink (#ec4899)
- Success: Green (#10b981)
- Warning: Orange (#f59e0b)
- Error: Red (#ef4444)

### Dark Mode
Toggle between light and dark themes using the toggle in the header.
Theme preference is automatically saved.

## 🧪 Testing

```bash
# Unit tests
pnpm run test:unit

# E2E tests
pnpm run test:e2e

# Type checking
pnpm run typecheck
```

## 🏗️ Project Structure

```
frontend/
├── app/
│   ├── components/     # Vue components
│   ├── pages/         # Route pages
│   ├── layouts/       # Layout components
│   ├── composables/   # Composition API
│   ├── stores/        # Pinia stores
│   └── types/         # TypeScript types
├── assets/
│   └── styles/        # CSS files
└── tests/            # Test files
```

## 🔧 Development

### Scripts
```bash
pnpm run dev          # Start dev server
pnpm run build        # Build for production
pnpm run preview      # Preview build
pnpm run typecheck    # Type check
pnpm run lint         # Lint code
pnpm run test:unit    # Unit tests
pnpm run test:e2e     # E2E tests
```

### Environment
```bash
# .env
NUXT_PUBLIC_API_BASE_URL=http://localhost:8080
NUXT_PUBLIC_KEYCLOAK_URL=http://localhost:8081
NUXT_PUBLIC_KEYCLOAK_REALM=bss
```

## 📖 Components

### Base Components
- `AppButton` - Button with variants
- `AppInput` - Text input
- `AppSelect` - Dropdown select
- `AppModal` - Modal dialog
- `AppCard` - Content container
- `AppTable` - Data table

### Example Usage
```vue
<template>
  <AppCard title="Customer Details">
    <AppInput
      v-model="email"
      label="Email"
      type="email"
    />
    <AppButton @click="save">
      Save
    </AppButton>
  </AppCard>
</template>
```

## 🔐 Authentication

Authentication is handled through Keycloak.

- Login: Automatic redirect to Keycloak
- Token: Stored in httpOnly cookie
- Roles: Check with `useAuth().hasRole()`

## 🌐 Deployment

### Docker
```bash
docker build -t bss-frontend .
docker run -p 3000:3000 bss-frontend
```

### Vercel
```bash
vercel deploy
```

### Netlify
```bash
netlify deploy
```

## 🤝 Contributing

1. Create feature branch
2. Make changes
3. Write/update tests
4. Submit pull request

## 📄 License

Proprietary - All rights reserved
```

**Status:** ⏳

#### 5.6. Stwórz Deployment Guide
**Plik:** `/frontend/DEPLOYMENT.md`

**Stwórz plik:**
```markdown
# Deployment Guide

## Production Build

### Build Command
```bash
cd frontend
pnpm install --frozen-lockfile
pnpm run build
```

### Output
- Static files: `.output/public/`
- Server bundle: `.output/server/`

## Environment Variables

### Required
```bash
NUXT_PUBLIC_API_BASE_URL=https://api.bss.local
NUXT_PUBLIC_KEYCLOAK_URL=https://keycloak.bss.local
NUXT_PUBLIC_KEYCLOAK_REALM=bss
NUXT_PUBLIC_KEYCLOAK_CLIENT_ID=bss-frontend
```

### Optional
```bash
NUXT_PUBLIC_ANALYTICS_ID=GA-TRACKING-ID
NUXT_PUBLIC_SENTRY_DSN=https://xxx@sentry.io/xxx
```

## Deployment Options

### 1. Docker

#### Dockerfile
```dockerfile
FROM node:20-alpine AS builder

WORKDIR /app
COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM node:20-alpine AS runner
WORKDIR /app

RUN npm install -g serve

COPY --from=builder /app/.output ./.output

EXPOSE 3000
CMD ["serve", ".output", "-s", "-l", "3000"]
```

#### Build & Run
```bash
docker build -t bss-frontend .
docker run -d -p 3000:3000 --name frontend bss-frontend
```

### 2. Vercel

#### vercel.json
```json
{
  "builds": [
    {
      "src": "nuxt.config.ts",
      "use": "@nuxtjs/vercel-builder"
    }
  ]
}
```

#### Deploy
```bash
vercel --prod
```

### 3. Netlify

#### netlify.toml
```toml
[build]
  command = "pnpm run build"
  publish = ".output/public"

[build.environment]
  NODE_VERSION = "20"
```

#### Deploy
```bash
netlify deploy --prod
```

### 4. Nginx

#### nginx.conf
```nginx
server {
    listen 80;
    server_name bss.local;

    root /var/www/bss-frontend/.output/public;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    # Cache static assets
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

#### Deploy
```bash
# Build
pnpm run build

# Copy files
scp -r .output/public/* user@server:/var/www/bss-frontend/
```

## Health Checks

### Endpoint
- GET `/health` - Returns 200 OK
- GET `/_nuxt/manifest.json` - Application manifest

### Script
```bash
#!/bin/bash
URL="https://bss.local/health"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" $URL)

if [ $STATUS -eq 200 ]; then
  echo "✅ Health check passed"
  exit 0
else
  echo "❌ Health check failed (HTTP $STATUS)"
  exit 1
fi
```

## CI/CD

### GitHub Actions
```yaml
name: Deploy Frontend

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Setup Node
        uses: actions/setup-node@v3
        with:
          node-version: '20'

      - name: Install pnpm
        run: npm install -g pnpm

      - name: Install dependencies
        run: pnpm install --frozen-lockfile

      - name: Build
        run: pnpm run build

      - name: Deploy to production
        run: |
          # Your deployment script
```

## Monitoring

### Error Tracking
```typescript
// plugins/sentry.ts
import * as Sentry from '@sentry/vue'

export default defineNuxtPlugin((nuxtApp) => {
  Sentry.init({
    app: nuxtApp.vueApp,
    dsn: process.env.NUXT_PUBLIC_SENTRY_DSN
  })
})
```

### Analytics
```typescript
// plugins/analytics.ts
export default defineNuxtPlugin(() => {
  if (process.client) {
    gtag('config', process.env.NUXT_PUBLIC_ANALYTICS_ID)
  }
})
```

## Performance

### Optimization
- ✅ Code splitting
- ✅ Lazy loading
- ✅ Image optimization
- ✅ Gzip compression
- ✅ CDN distribution

### Build Analysis
```bash
pnpm run build --analyze
```

### Core Web Vitals
- LCP: < 2.5s
- FID: < 100ms
- CLS: < 0.1

## Troubleshooting

### Build Fails
```bash
# Clear cache
rm -rf .nuxt .output node_modules
pnpm install

# Check for errors
pnpm run typecheck
```

### 500 Error
1. Check server logs
2. Verify environment variables
3. Check API connectivity

### Performance Issues
1. Enable compression (gzip/brotli)
2. Use CDN for static assets
3. Implement caching headers
4. Optimize images

## Rollback

### Previous Version
```bash
# Docker
docker stop frontend
docker run -d -p 3000:3000 bss-frontend:previous-tag

# Vercel
vercel rollback [deployment-url]

# Static files
cp -r /var/www/bss-frontend-backup/* /var/www/bss-frontend/
```

## Security

### Headers
```nginx
# Security headers
add_header X-Frame-Options "SAMEORIGIN";
add_header X-Content-Type-Options "nosniff";
add_header X-XSS-Protection "1; mode=block";
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains";
add_header Content-Security-Policy "default-src 'self'";
```

### HTTPS
- Always use HTTPS in production
- Redirect HTTP to HTTPS
- Use HSTS headers

## Backup

### Files
- Source code (Git)
- Build artifacts (backup server)
- Environment variables (secure vault)

### Automated Backup
```bash
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
tar -czf /backup/frontend_$DATE.tar.gz /var/www/bss-frontend
```

## Scaling

### Horizontal
- Load balancer (Nginx/HAProxy)
- Multiple instances
- CDN for static assets

### Vertical
- Increase server resources
- Optimize build
- Use caching
```

**Status:** ⏳

---

## ✅ FINAL VERIFICATION

### Sprawdzenie kompletności:

#### 1. Tailwind CSS
```bash
cd frontend
pnpm add -D @nuxtjs/tailwindcss
pnpm run dev  # Should start without errors
pnpm run build  # Should build successfully
```
**Status:** ⏳

#### 2. Icon Migration
```bash
# Sprawdź czy żadnych emoji nie ma w kodzie
grep -r "👥\|📊\|🏢" --include="*.vue" . || echo "No emojis found ✅"
```
**Status:** ⏳

#### 3. Dark Mode
```bash
# Otwórz dev server
pnpm run dev
# 1. Kliknij toggle
# 2. Sprawdź czy strona ciemnieje
# 3. Odśwież - ustawienia powinny się zachować
```
**Status:** ⏳

#### 4. UI Components
```bash
# Sprawdź czy wszystkie komponenty się kompilują
pnpm run typecheck
```
**Status:** ⏳

#### 5. Documentation
```bash
# Sprawdź czy pliki dokumentacji istnieją
ls -la frontend/*.md
```
**Status:** ⏳

---

## 📊 STATUS TRACKER

| Task | Owner | Time | Status |
|------|-------|------|--------|
| Tailwind CSS | @developer | 4h | ⏳ |
| Icons replacement | @developer | 2h | ⏳ |
| Dark mode | @developer | 3h | ⏳ |
| UI components | @developer | 16h | ⏳ |
| Documentation | @developer | 4h | ⏳ |
| **TOTAL** | | **29h** | ⏳ |

---

## 🎯 SUCCESS CRITERIA

### Completion Requirements:
- [ ] Tailwind CSS integrated and used in 3+ components
- [ ] All emoji replaced with Lucide icons
- [ ] Dark mode works and persists
- [ ] 10+ UI components created and documented
- [ ] 4 documentation files created (ARCHITECTURE, COMPONENTS, DEVELOPMENT, API)
- [ ] All code compiles without errors
- [ ] TypeScript checks pass
- [ ] Development server starts successfully

### After Completion:
✅ Project ready for enterprise development
✅ Team velocity increased by 50%
✅ Professional appearance
✅ Modern UX with dark mode
✅ Comprehensive documentation

---

**PRIORITY:** P0 - KRYTYCZNY
**DEADLINE:** 1 tydzień
**IMPACT:** HIGH - Foundation for all future development
