<template>
  <div class="mt-1 mb-0 lg:mb-3 text-body-lg flex flex-col text-title">
    <p>{{ value }} {{ pluralizedLabel }}</p>
  </div>
</template>

<script setup lang="ts">
interface Props {
  value: Number;
  label?: String;
  suffix?: String;
}

const props = withDefaults(defineProps<Props>(), {
  value: 0,
  label: "result",
  suffix: "s",
});

let pluralizedLabel = computed(() => {
  return props.value > 1 || props.value === 0
    ? `${props.label}${props.suffix}`
    : label;
});
</script>
