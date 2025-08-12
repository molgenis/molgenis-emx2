<script lang="ts" setup>
withDefaults(
  defineProps<{
    truncate?: boolean;
  }>(),
  {
    truncate: true,
  }
);
</script>

<template>
  <td
    class="p-2.5 border-b border-gray-200 max-w-60 w-60 min-h-4 text-left"
    :class="{
      truncate: truncate,
    }"
  >
    <slot></slot>
  </td>
</template>
