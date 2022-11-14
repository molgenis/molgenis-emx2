<!-- eslint-disable vue/multi-word-component-names -->
<script setup>
import BaseIcon from "./BaseIcon.vue";

defineProps({
  crumbs: {
    type: Object,
    required: true,
  },
});
</script>

<template>
  <a href="#" class="text-white lg:hidden">
    <span class="sr-only">Go up one leven</span>
    <BaseIcon name="arrow-left" />
  </a>
  <nav class="items-center justify-center hidden gap-3 lg:flex font-display text-heading-lg">
    <template v-for="(url, label, index) in crumbs" :key="label">
      <a :href="url" class="text-white">{{ label }}</a>
      <span v-if="index < Object.keys(crumbs).length - 1" class="text-white">
        <BaseIcon name="caret-right" />
      </span>
    </template>
  </nav>
</template>
