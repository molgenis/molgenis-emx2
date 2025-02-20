<script setup lang="ts">
const inverted = ref<boolean>(false);
const searchValue = ref<string>("");
</script>

<template>
  <div class="flex flex-row gap-2 mb-4">
    <div class="basis-3/5">
      <InputSearch
        v-model="searchValue"
        :inverted="inverted"
        id="search-input"
      />
      value: {{ searchValue }}
    </div>
  </div>
</template>
