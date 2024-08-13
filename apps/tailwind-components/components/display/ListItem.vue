<template>
  <li
    :class="{
      'pr-12.5 pb-[5px]': props.type === 'standard',
      'pr-12.5 pb-2.5': props.type === 'link',
    }"
  >
    <slot></slot>
  </li>
</template>

<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    type?: "standard" | "link";
  }>(),
  {
    type: "standard",
  }
);
</script>
