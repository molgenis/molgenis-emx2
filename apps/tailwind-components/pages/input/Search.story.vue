<script setup lang="ts">
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
  </InputTestContainer>
</template>
