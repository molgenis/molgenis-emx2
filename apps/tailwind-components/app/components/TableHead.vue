<!-- eslint-disable vue/multi-word-component-names -->
<script setup>
defineProps({
  fullWidth: {
    type: Boolean,
  },
  noWrap: {
    type: Boolean,
  },
});
</script>

<template>
  <th
    class="py-2.5 px-2.5 text-left border-b border-gray-200 first:pl-0 last:pr-0 sm:first:pl-2.5 sm:last:pr-2.5"
    :class="{ 'whitespace-nowrap': noWrap, 'w-full': fullWidth }"
  >
    <slot></slot>
  </th>
</template>
