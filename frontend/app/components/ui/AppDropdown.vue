<template>
  <div class="dropdown" :class="{ 'dropdown--open': isOpen }">
    <div class="dropdown__trigger" @click="toggle">
      <slot name="trigger">
        <AppButton :variant="triggerVariant" :size="triggerSize">
          {{ triggerText }}
          <Icon name="lucide:chevron-down" :size="16" class="dropdown__chevron" />
        </AppButton>
      </slot>
    </div>

    <Teleport to="body">
      <Transition name="dropdown">
        <div
          v-if="isOpen"
          ref="dropdownRef"
          class="dropdown__menu"
          :style="menuStyle"
          @click.stop
        >
          <div class="dropdown__header" v-if="$slots.header">
            <slot name="header" />
          </div>

          <ul class="dropdown__list">
            <li
              v-for="(item, index) in items"
              :key="index"
              class="dropdown__item"
              :class="{ 'dropdown__item--disabled': item.disabled }"
              @click="handleItemClick(item)"
            >
              <Icon
                v-if="item.icon"
                :name="item.icon"
                :size="16"
                class="dropdown__item-icon"
              />
              <span class="dropdown__item-text">{{ item.label }}</span>
              <Icon
                v-if="item.badge"
                name="lucide:circle"
                :size="8"
                class="dropdown__item-badge"
              />
            </li>
          </ul>

          <div class="dropdown__footer" v-if="$slots.footer">
            <slot name="footer" />
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import AppButton from './AppButton.vue'

interface DropdownItem {
  label: string
  value?: any
  icon?: string
  disabled?: boolean
  badge?: boolean
}

interface Props {
  items?: DropdownItem[]
  triggerText?: string
  triggerVariant?: 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger' | 'success' | 'warning'
  triggerSize?: 'xs' | 'sm' | 'md' | 'lg' | 'xl'
  align?: 'left' | 'right' | 'center'
  offset?: number
}

const props = withDefaults(defineProps<Props>(), {
  items: () => [],
  triggerVariant: 'outline',
  triggerSize: 'md',
  align: 'left',
  offset: 8
})

const emit = defineEmits<{
  select: [item: DropdownItem, index: number]
  toggle: [isOpen: boolean]
}>()

const isOpen = ref(false)
const dropdownRef = ref<HTMLElement | null>(null)
const triggerRef = ref<HTMLElement | null>(null)

const menuStyle = computed(() => {
  if (!isOpen.value || !triggerRef.value) return {}

  const trigger = triggerRef.value
  const rect = trigger.getBoundingClientRect()
  const menu = dropdownRef.value?.getBoundingClientRect()

  if (!menu) return {}

  const top = rect.bottom + window.scrollY + props.offset
  const left = rect.left + window.scrollX

  let calculatedLeft = left

  if (props.align === 'right') {
    calculatedLeft = rect.right + window.scrollX - menu.width
  } else if (props.align === 'center') {
    calculatedLeft = rect.left + window.scrollX + (rect.width - menu.width) / 2
  }

  // Ensure menu doesn't go off-screen
  const rightEdge = calculatedLeft + menu.width
  if (rightEdge > window.innerWidth) {
    calculatedLeft = window.innerWidth - menu.width - 16
  }

  if (calculatedLeft < 16) {
    calculatedLeft = 16
  }

  return {
    position: 'absolute',
    top: `${top}px`,
    left: `${calculatedLeft}px`,
    zIndex: 9999
  }
})

const toggle = () => {
  isOpen.value = !isOpen.value
  emit('toggle', isOpen.value)

  if (isOpen.value) {
    document.addEventListener('click', handleClickOutside)
  } else {
    document.removeEventListener('click', handleClickOutside)
  }
}

const handleClickOutside = (event: MouseEvent) => {
  if (!dropdownRef.value || !triggerRef.value) return

  const target = event.target as HTMLElement
  const isClickInside =
    dropdownRef.value.contains(target) || triggerRef.value.contains(target)

  if (!isClickInside) {
    close()
  }
}

const close = () => {
  isOpen.value = false
  document.removeEventListener('click', handleClickOutside)
  emit('toggle', isOpen.value)
}

const handleItemClick = (item: DropdownItem) => {
  if (item.disabled) return
  emit('select', item, props.items.indexOf(item))
  close()
}

onMounted(() => {
  // Find trigger element
  const dropdown = document.querySelector('.dropdown')
  if (dropdown) {
    triggerRef.value = dropdown.querySelector('.dropdown__trigger') as HTMLElement
  }
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.dropdown {
  position: relative;
  display: inline-block;
}

.dropdown__trigger {
  display: inline-block;
}

.dropdown__chevron {
  transition: transform var(--transition-fast) var(--transition-timing);
}

.dropdown--open .dropdown__chevron {
  transform: rotate(180deg);
}

.dropdown__menu {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-xl);
  min-width: 200px;
  max-width: 300px;
  overflow: hidden;
}

.dropdown__header {
  padding: var(--space-3) var(--space-4);
  border-bottom: 1px solid var(--color-border);
  background: var(--color-surface-alt);
}

.dropdown__list {
  list-style: none;
  margin: 0;
  padding: var(--space-1) 0;
}

.dropdown__item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  cursor: pointer;
  transition: background-color var(--transition-fast) var(--transition-timing);
  user-select: none;
}

.dropdown__item:hover:not(.dropdown__item--disabled) {
  background: var(--color-surface-alt);
}

.dropdown__item--disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.dropdown__item-icon {
  flex-shrink: 0;
  color: var(--color-text-secondary);
}

.dropdown__item-text {
  flex: 1;
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
}

.dropdown__item-badge {
  flex-shrink: 0;
  color: var(--color-primary);
}

.dropdown__footer {
  padding: var(--space-3) var(--space-4);
  border-top: 1px solid var(--color-border);
  background: var(--color-surface-alt);
}

/* Transitions */
.dropdown-enter-active,
.dropdown-leave-active {
  transition: all var(--transition-fast) var(--transition-timing);
  transform-origin: top center;
  opacity: 1;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: scaleY(0.95) translateY(-10px);
}

.dropdown-enter-to,
.dropdown-leave-from {
  opacity: 1;
  transform: scaleY(1) translateY(0);
}
</style>
