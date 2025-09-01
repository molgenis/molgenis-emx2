<script setup lang="ts">
import { ref } from "vue";
const date = ref<Date | string | null>(null);
</script>

<template>
  <InputTestContainer
    show-placeholder
    show-state
    v-slot="{ placeholder, valid, invalid, disabled }"
  >
    <label for="input-date" class="text-title"> Enter a date </label>
    <InputDateTime
      id="input-date-time"
      v-model="date"
      :placeholder="placeholder"
      :valid="valid"
      :invalid="invalid"
      :disabled="disabled"
    />
  </InputTestContainer>
</template>
