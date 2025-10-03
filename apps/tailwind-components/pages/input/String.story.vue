<script setup lang="ts">
import { ref } from "vue";

const demoValue = ref("test");
</script>

<template>
  <InputTestContainer
    show-placeholder
    show-state
    v-slot="{ placeholder, valid, invalid, disabled }"
  >
    <InputString
      id="input-string-default"
      v-model="demoValue"
      :placeholder="placeholder"
      :valid="valid"
      :invalid="invalid"
      :disabled="disabled"
    />
  </InputTestContainer>
</template>
