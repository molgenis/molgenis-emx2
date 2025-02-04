<script setup lang="ts">
const file = ref();
const state = ref<string>();
</script>

<template>
  <h2 class="text-heading-2xl">File input Eample</h2>
  <form @submit.prevent>
    <label for="file-input-demo" class="block mb-2">
      Select one or more files to import.
    </label>
    <InputFile
      id="file-input-demo"
      @update:model-value="(value) => (file = value)"
    />
  </form>
  <h3 class="text-heading-lg my-2">Data output</h3>
  <output
    class="block w-full mt-2 bg-gray-100 py-3 px-2 pl-6 h-30 overflow-y-scroll shadow-inner"
  >
    <pre class="indent-[-5em]">
        {{ file }}
      </pre
    >
  </output>
</template>
