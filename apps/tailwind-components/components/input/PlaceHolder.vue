<template>
  <div class="border border-dotted p-2">
    <pre>place holder for field type {{ type }}</pre>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  type: string;
}>();
</script>
