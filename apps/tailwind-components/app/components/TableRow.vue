<script setup lang="ts">
withDefaults(defineProps<{ disabled?: boolean; stacked?: boolean }>(), {
  disabled: false,
  stacked: false,
});
</script>

<template>
  <tr
    :class="{
      'sm:hover:bg-hover sm:hover:cursor-pointer': !disabled,
      'active:bg-hover': stacked && !disabled,
      'block py-2.5 border-b border-gray-200 sm:table-row sm:py-0 sm:border-b-0':
        stacked,
    }"
  >
    <slot></slot>
  </tr>
</template>
