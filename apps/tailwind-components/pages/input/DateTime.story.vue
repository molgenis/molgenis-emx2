<script setup lang="ts">
import { ref } from "vue";
import type { DateValue } from "../../../metadata-utils/src/types";

function currentDateTime(): string {
  const now: string = new Date().toISOString();
  const date: string = now.split("T")[0];
  const time: string = now.split("T")[1].split(".")[0];
  return [date, time].join(" ");
}

const date = ref<DateValue>(currentDateTime());
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
      :value="(date as string)"
      :placeholder="placeholder"
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
