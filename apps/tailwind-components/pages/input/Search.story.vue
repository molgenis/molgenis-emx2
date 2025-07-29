<script setup lang="ts">
import { ref } from "vue";

const searchValue1 = ref<string>("");
const searchValue2 = ref<string>("");
const searchValue3 = ref<string>("");
</script>

<template>
  <InputTestContainer
    show-placeholder
    show-state
    v-slot="{ placeholder, valid, invalid, disabled }"
  >
    <label for="search-input"> Search </label>
    <InputSearch
      v-model="searchValue1"
      :placeholder="placeholder"
      :valid="valid"
      :invalid="invalid"
      :disabled="disabled"
      id="search-input"
    />
    value: {{ searchValue1 }}
    <h2>large</h2>
    <InputSearch
      v-model="searchValue2"
      size="large"
      :placeholder="placeholder"
      :valid="valid"
      :invalid="invalid"
      :disabled="disabled"
      id="search-input"
    />
    value: {{ searchValue2 }}
    <h2>tiny</h2>
    <InputSearch
      v-model="searchValue3"
      size="tiny"
      :placeholder="placeholder"
      :valid="valid"
      :invalid="invalid"
      :disabled="disabled"
      id="search-input"
    />
    value: {{ searchValue3 }}
  </InputTestContainer>
</template>
