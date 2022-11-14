<!-- eslint-disable vue/multi-word-component-names -->
<script setup>
import pkg from "floating-vue";
const { Tooltip } = pkg;
const VTooltip = Tooltip;
import BaseIcon from "./BaseIcon.vue";
import "floating-vue/dist/style.css";

defineProps({
  label: {
    type: String,
    required: true,
  },
  content: {
    type: String,
    required: true,
  },
});
</script>

<template>
  <div class="flex items-center justify-center w-6 h-6">
    <Tooltip :showTriggers="['hover', 'touch']" :distance="12">
      <button class="w-6 h-6 text-blue-200">
        <BaseIcon name="info" />
        <span class="sr-only" v-if="label">{{ label }}</span>
      </button>

      <template #popper>
        {{ content }}
      </template>
    </Tooltip>
  </div>
</template>

<style>
.v-popper--theme-tooltip {
  @apply text-body-sm font-sans flex items-center justify-center;
}

.v-popper--theme-tooltip .v-popper__inner {
  @apply p-3 bg-black;
}

.v-popper__popper .v-popper__arrow-inner,
.v-popper__popper .v-popper__arrow-outer {
  @apply border-black;
}

.v-popper--theme-tooltip {
  @apply max-w-tooltip;
}
</style>
