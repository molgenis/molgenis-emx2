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
    class="p-2.5 border-b border-gray-200 min-h-8 text-left"
    :class="{
      truncate: truncate,
    }"
  >
    <slot></slot>
  </td>
</template>
