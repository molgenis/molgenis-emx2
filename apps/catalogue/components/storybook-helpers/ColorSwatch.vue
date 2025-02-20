<script setup>
defineProps({
  color: {
    type: String,
    default: null,
  },
});
</script>

<template>
  <div
    class="rounded-full"
    :style="color !== null ? `background-color: ${color};` : ''"
  >
    {{}}
  </div>
</template>
