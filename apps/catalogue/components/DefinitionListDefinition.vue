<script setup lang="ts">
withDefaults(
  defineProps<{
    small?: boolean;
  }>(),
  {
    small: false,
  }
);
</script>
<template>
  <dd class="col-span-2" :class="{ 'mb-2.5': small }">
    <slot></slot>
  </dd>
</template>
