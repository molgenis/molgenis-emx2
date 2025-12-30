<script setup lang="ts">
import { ref } from "vue";

const id = "story-input-text-area-1";
const fieldValue = ref<string>("");
</script>

<template>
  <InputTestContainer
    show-placeholder
    show-state
    v-slot="{ placeholder, valid, invalid, disabled }"
  >
    <InputTextArea
      :id="id"
      v-model="fieldValue"
      :valid="valid"
      :invalid="invalid"
      :disabled="disabled"
      :placeholder="placeholder"
    />
  </InputTestContainer>
</template>
