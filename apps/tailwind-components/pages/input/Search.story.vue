<script setup lang="ts">
import { ref } from "vue";

const searchValue = ref<string>("");
</script>

<template>
  <InputTestContainer
    show-placeholder
    show-state
    v-slot="{ placeholder, valid, invalid, disabled }"
  >
    <label for="search-input"> Search </label>
    <InputSearch
      v-model="searchValue"
      :placeholder="placeholder"
      :valid="valid"
      :invalid="invalid"
      :disabled="disabled"
      id="search-input"
    />
    value: {{ searchValue }}
    <h2>large</h2>
    <InputSearch
        v-model="searchValue"
        size="large"
        :placeholder="placeholder"
        :valid="valid"
        :invalid="invalid"
        :disabled="disabled"
        id="search-input"
    />
    value: {{ searchValue }}
    <h2>tiny</h2>
    <InputSearch
        v-model="searchValue"
        size="tiny"
        :placeholder="placeholder"
        :valid="valid"
        :invalid="invalid"
        :disabled="disabled"
        id="search-input"
    />
    value: {{ searchValue }}
  </InputTestContainer>
</template>
