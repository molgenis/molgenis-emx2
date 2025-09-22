<template>
  <Button
    type="text"
    size="small"
    icon="arrow-left"
    icon-position="left"
    class="mb-4"
  >
    <slot />
  </Button>
</template>
<script setup lang="ts">
import Button from "~/components/Button.vue";
</script>
