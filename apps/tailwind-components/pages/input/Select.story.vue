<script setup lang="ts">
import { ref } from "vue";

const fieldValue = ref("");
const options = ["option 1", "option 2", "option 3", "option 4"];
</script>

<template>
  <InputTestContainer
    show-required
    show-placeholder
    show-state
    v-slot="{ placeholder, valid, invalid, disabled, required }"
  >
    <InputSelect
      id="story-input-1"
      :options="options"
      v-model="fieldValue"
      :required="required"
      :valid="valid"
      :invalid="invalid"
      :disabled="disabled"
      :placeholder="placeholder"
    />
  </InputTestContainer>
</template>
