<script setup lang="ts">
const searchValue = ref("");
</script>
<template>
  <FilterSearch v-model="searchValue" />

  <div class="mt-4">
    <hr />
    <p>Search value: {{ searchValue }}</p>
    <span> (note: the search inoujt has a fixed 500ms update delay)</span>
    <div class="mt-4">
      <button
        @click="searchValue = ''"
        class="bg-blue-500 hover:bg-blue-700 text-white py-2 px-4 rounded"
      >
        clear value
      </button>
    </div>
  </div>
</template>
