<script setup lang="ts">
const inverted = ref(false);
const searchValue = ref("");
</script>

<template>
  <div class="flex flex-row gap-2 mb-4">
    <div class="basis-3/5">
      <InputSearch v-model="searchValue" :inverted="inverted" />
      value: {{ searchValue }}
    </div>
  </div>
</template>
