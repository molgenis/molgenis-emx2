<script setup lang="ts">
import { ref } from "vue";

const searchValueLarge = ref<string>("this is a large sized search input");
const searchValueMedium = ref<string>("this is a medium sized search input");
const searchValueSmall = ref<string>("this is a small sized search input");
const searchValueTiny = ref<string>("this is a tiny sized search input");

import ComponentOutput from "../../components/story/ComponentOutput.vue";
</script>

<template>
  <InputTestContainer
    show-placeholder
    show-state
    v-slot="{ placeholder, valid, invalid, disabled }"
  >
    <div class="flex flex-col flex-nowrap gap-10">
      <div>
        <label for="search-input-large" class="block text-title mb-2">
          <span class="font-bold">Search</span>
          <span class="ml-1 text-heading-sm">(size=large)</span>
        </label>
        <InputSearch
          id="search-input-large"
          v-model="searchValueLarge"
          size="large"
          :placeholder="placeholder"
          :valid="valid"
          :invalid="invalid"
          :disabled="disabled"
        />
        <ComponentOutput class="mt-4">
          <code>Value: {{ searchValueLarge }}</code>
        </ComponentOutput>
      </div>
      <div>
        <label for="search-input-medium" class="block text-title mb-2">
          <span class="font-bold">Search</span>
          <span class="ml-1 text-heading-sm">(size=medium, default)</span>
        </label>
        <InputSearch
          id="search-input-medium"
          v-model="searchValueMedium"
          :placeholder="placeholder"
          :valid="valid"
          :invalid="invalid"
          :disabled="disabled"
        />
        <ComponentOutput class="mt-4">
          <code>Value: {{ searchValueMedium }}</code>
        </ComponentOutput>
      </div>
      <div>
        <label for="search-input-small" class="block text-title mb-2">
          <span class="font-bold">Search</span>
          <span class="ml-1 text-heading-sm">(size=small)</span>
        </label>
        <InputSearch
          id="search-input-small"
          v-model="searchValueSmall"
          size="small"
          :placeholder="placeholder"
          :valid="valid"
          :invalid="invalid"
          :disabled="disabled"
        />
        <ComponentOutput class="mt-4">
          <code>Value: {{ searchValueSmall }}</code>
        </ComponentOutput>
      </div>
      <div>
        <label for="search-input-tiny" class="block text-title mb-2">
          <span class="font-bold">Search</span>
          <span class="ml-1 text-heading-sm">(size=tiny)</span>
        </label>
        <InputSearch
          id="search-input-tiny"
          v-model="searchValueTiny"
          size="tiny"
          :placeholder="placeholder"
          :valid="valid"
          :invalid="invalid"
          :disabled="disabled"
        />
        <ComponentOutput class="mt-4">
          <code>Value: {{ searchValueTiny }}</code>
        </ComponentOutput>
      </div>
    </div>
  </InputTestContainer>
</template>
