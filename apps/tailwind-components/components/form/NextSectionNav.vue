<template>
  <Button
    type="text"
    size="small"
    icon="arrow-right"
    icon-position="right"
    class="mb-4 justify-self-end"
  >
    go forward to section '<slot />'
  </Button>
</template>
<script setup lang="ts">
import Button from "~/components/Button.vue";
</script>
