<script setup lang="ts">
const file = ref();
</script>

<template>
  <InputTestContainer show-state v-slot="{ valid, invalid, disabled }">
    <label for="file-input-demo-file-input" class="block mb-2"
      >Select a file to import.</label
    >
    <InputFile
      id="file-input-demo"
      :valid="valid"
      :invalid="invalid"
      :disabled="disabled"
      @update:modelValue="(value) => (file = value)"
    />
  </InputTestContainer>
  <form @submit.prevent></form>
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
