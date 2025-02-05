<script setup lang="ts">
const id = "story-input-text-area-1";
const fieldValue = ref<string>("");
</script>

<template>
  <InputTestContainer
    show-placeholder
    show-state
    v-slot="{ placeholder, state }"
  >
    <InputTextArea
      :id="id"
      v-model="fieldValue"
      :state="state"
      :placeholder="placeholder"
    />
  </InputTestContainer>
</template>
