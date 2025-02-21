<script setup lang="ts">
const searchValue = ref("");
const inverted = ref(false);
</script>
<template>
  <div class="p-5">
    <FilterSearch v-model="searchValue" :inverted="inverted" />
  </div>

  <div class="mt-4">
    <hr />
    <p class="text-title">Search value: {{ searchValue }}</p>
    <span class="text-title">
      (note: the search input has a fixed 500ms update delay)</span
    >
    <div class="mt-4">
      <button
        @click="searchValue = ''"
        class="bg-blue-500 hover:bg-blue-700 text-title py-2 px-4 rounded"
      >
        clear value
      </button>
    </div>
    <div>
      <input id="inverted" type="checkbox" v-model="inverted" />
      <label class="ml-1 text-title" for="inverted">inverted colors</label>
    </div>
  </div>
</template>
