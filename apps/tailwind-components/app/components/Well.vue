<script>
//this script tag is meant to allow export of this css
export const WELL_BASE =
  "whitespace-nowrap bg-button-filter text-button-filter rounded-input border border-button-filter";
</script>

<script setup>
import { WELL_BASE } from "./Well.vue";
</script>

<template>
  <span
    :class="`inline-flex items-center gap-1.5 px-2 py-0.5 text-body-sm ${WELL_BASE}`"
  >
    <slot />
  </span>
</template>
