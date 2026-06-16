<template>
  <InputTestContainer
    show-placeholder
    show-state
    v-slot="{ placeholder, valid, invalid, disabled }"
  >
    <InputInt
      id="input-int"
      v-model="demoValue"
      :placeholder="placeholder"
      :valid="valid"
      :invalid="invalid"
      :disabled="disabled"
    />
    {{ typeof demoValue }}
    {{ JSON.stringify(demoValue) }}
  </InputTestContainer>
</template>

<script setup lang="ts">
import { ref } from "vue";

const demoValue = ref<string | number | null | undefined>();
</script>
