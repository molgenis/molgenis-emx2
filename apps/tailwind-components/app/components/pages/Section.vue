<script setup lang="ts">
import type { Section } from "../../../types/pages";

withDefaults(defineProps<Section>(), {
  enableFullScreenWidth: false,
});
</script>

<template>
  <div :id="id" class="w-full py-8 justify-center items-center">
    <div
      class="m-auto"
      :class="{
        'w-pg-section': !enableFullScreenWidth,
        'w-full': enableFullScreenWidth,
      }"
    >
      <slot></slot>
    </div>
  </div>
</template>
