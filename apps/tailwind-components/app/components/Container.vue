<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    wide?: boolean;
  }>(),
  {
    wide: false,
  }
);
</script>
<template>
  <div class="mx-auto px-5 lg:px-[30px]" :class="{ 'max-w-lg': !props.wide }">
    <slot></slot>
  </div>
</template>
