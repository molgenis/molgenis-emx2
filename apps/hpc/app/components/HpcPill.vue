<template>
  <NuxtLink v-if="to" :to="to" :class="pillClass">
    <slot />
  </NuxtLink>
  <span v-else :class="pillClass">
    <slot />
  </span>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = withDefaults(
  defineProps<{
    to?: string;
    compact?: boolean;
  }>(),
  {
    to: undefined,
    compact: false,
  }
);

const pillClass = computed(() => {
  const base =
    "inline-flex items-center gap-1 rounded-full border border-input bg-content/50 text-title";
  const size = props.compact
    ? "px-2 py-0.5 text-xs font-medium"
    : "px-3 py-1 text-sm";
  const interactive = props.to
    ? "transition-colors hover:bg-hover hover:border-input-focused focus-visible:bg-hover focus-visible:border-input-focused"
    : "";
  return [base, size, interactive].join(" ").trim();
});
</script>
