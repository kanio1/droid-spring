<template>
  <Icon
    :icon="name"
    :width="size || width"
    :height="size || height"
    :class="iconClass"
    v-bind="$attrs"
  />
</template>

<script setup lang="ts">
import { Icon as IconifyIcon } from '@iconify/vue'
import { computed } from 'vue'

interface Props {
  name: string
  size?: string | number
  width?: string | number
  height?: string | number
  class?: string
}

const props = withDefaults(defineProps<Props>(), {
  size: 20,
  width: 20,
  height: 20
})

const iconClass = computed(() => props.class || '')
</script>
