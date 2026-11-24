<script setup lang="ts">
import type { ICmsComponentWidths } from "../../../types/CmsComponents";
withDefaults(defineProps<{ width?: ICmsComponentWidths }>(), {
  width: "full",
});
</script>

<template>
  <div
    :class="{
      'sm:w-1/3': width === 'small',
      'sm:w-1/2': width === 'medium',
      'sm:w-2/3': width === 'large',
      'sm:w-full': width === 'full',
    }"
  >
    <slot></slot>
  </div>
</template>
