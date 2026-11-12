<script setup lang="ts">
withDefaults(
  defineProps<{
    allowLineBreak?: boolean;
    stacked?: boolean;
    label?: string;
  }>(),
  { allowLineBreak: false, stacked: false }
);
</script>

<template>
  <td
    class="text-table-row first:font-bold first:text-link"
    :class="
      stacked
        ? 'block px-0 py-0.5 sm:table-cell sm:py-2.5 sm:px-2.5 sm:border-b sm:border-gray-200 sm:first:pl-2.5 sm:last:pr-2.5'
        : 'py-2.5 px-2.5 border-b border-gray-200 first:pl-0 last:pr-0 sm:first:pl-2.5 sm:last:pr-2.5'
    "
  >
    <span
      v-if="stacked && label"
      class="sm:hidden font-normal text-table-row opacity-70"
    >
      {{ label }}
    </span>
    <div :class="{ 'whitespace-nowrap': allowLineBreak }"><slot></slot></div>
  </td>
</template>
