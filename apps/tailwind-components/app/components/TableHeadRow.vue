<!-- eslint-disable vue/multi-word-component-names -->
<script setup lang="ts">
withDefaults(defineProps<{ stacked?: boolean }>(), { stacked: false });
</script>

<template>
  <tr :class="{ 'sr-only sm:not-sr-only sm:table-row': stacked }">
    <slot></slot>
  </tr>
</template>
