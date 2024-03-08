<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    value: number;
    label?: string;
    suffix?: string;
    valuePrefix?: string;
  }>(),
  {
    value: 0,
    label: "result",
    suffix: "s",
    valuePrefix: null,
  }
);

const pluralizedLabel = computed(() => {
  return props.value > 1 || props.value === 0
    ? `${props.label}${props.suffix}`
    : props.label;
});
</script>

<template>
  <div class="mt-1 mb-0 lg:mb-3 text-body-lg flex flex-col text-title">
    <p class="search-results-count">
      {{ valuePrefix }} {{ value }} {{ pluralizedLabel }}
    </p>
  </div>
</template>
