<template>
  <Button
    type="text"
    size="small"
    icon="arrow-left"
    icon-position="left"
    class="mb-4"
  >
    go back to section '<slot />'
  </Button>
</template>
<script setup lang="ts">
import Button from "../Button.vue";
</script>
