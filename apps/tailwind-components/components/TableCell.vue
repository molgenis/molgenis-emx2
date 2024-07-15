<!-- eslint-disable vue/multi-word-component-names -->
<script setup>
defineProps({
  allowLineBreak: {
    type: Boolean,
  },
});
</script>

<template>
  <td
    class="py-2.5 px-2.5 border-b border-gray-200 first:text-blue-500 first:font-bold first:pl-0 last:pr-0 sm:first:pl-2.5 sm:last:pr-2.5"
  >
    <div :class="{ 'whitespace-nowrap': allowLineBreak }"><slot></slot></div>
  </td>
</template>
