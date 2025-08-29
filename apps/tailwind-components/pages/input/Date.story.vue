<script setup lang="ts">
import { ref } from "vue";
const date = ref<string>("2025-06-01");
</script>

<template>
  <InputTestContainer
    show-placeholder
    show-state
    v-slot="{ placeholder, valid, invalid, disabled }"
  >
    <label for="input-date" class="text-title"> Enter a date </label>
    <InputDate
      id="input-date"
      :placeholder="placeholder"
      v-model="date"
      :valid="valid"
      :invalid="invalid"
      :disabled="disabled"
    />
  </InputTestContainer>
</template>
