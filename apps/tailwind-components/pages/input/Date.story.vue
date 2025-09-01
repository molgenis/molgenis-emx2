<script setup lang="ts">
import { ref } from "vue";
const date = ref<Date | string | null | undefined>();
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
      :v-model="date"
      :value="date"
      :valid="valid"
      :invalid="invalid"
      :disabled="disabled"
    />
  </InputTestContainer>
</template>
