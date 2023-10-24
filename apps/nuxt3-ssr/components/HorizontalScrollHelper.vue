<script setup>
import { computed } from "vue";

const props = defineProps({
  addFade: {
    type: Boolean,
  },
  addScrollButton: {
    type: Boolean,
  },
});
</script>

<template>
  <div class="relative">
    <div
      class="absolute z-10 right-0 inset-y-0 w-10 bg-gradient-to-r from-transparent to-white pointer-events-none"
    ></div>
    <div class="overflow-x-auto">
      <slot></slot>
    </div>
  </div>
</template>
