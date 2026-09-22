<script setup lang="ts">
import { ref } from "vue";
import type { DateValue } from "../../../../metadata-utils/src/types";
const date = ref<DateValue>(new Date().toISOString().split("T")[0]);
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
      :modelValue="date"
      :valid="valid"
      :invalid="invalid"
      :disabled="disabled"
      @update:model-value="(value: DateValue) => (date = value)"
    />
    <h3 class="mt-10 mb-2 text-title">Component output</h3>
    <StoryComponentOutput>
      {{ date }}
    </StoryComponentOutput>
  </InputTestContainer>
</template>
