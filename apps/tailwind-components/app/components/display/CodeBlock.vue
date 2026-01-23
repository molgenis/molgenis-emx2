<script setup lang="ts">
const props = defineProps<{
  content?: string;
}>();
</script>
<template>
  <div class="min-h-50 h-50 p-4 bg-code-output text-title overflow-x-scroll">
    <pre>{{ props.content }}</pre>
  </div>
</template>
