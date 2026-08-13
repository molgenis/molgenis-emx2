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
  <div class="mx-auto lg:px-[30px] px-0" :class="{ 'max-w-lg': !props.wide }">
    <slot></slot>
  </div>
</template>
