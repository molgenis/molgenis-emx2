<script setup lang="ts">
const file = ref();
const state = ref<string>();
</script>

<template>
  <h2 class="text-heading-2xl">File input</h2>
  <p>Select one or more files to import.</p>
  <form class="">
      <legend class="mb-2 text-heading-lg">
      Configure the component
      </legend>
      <div class="p-2 grow">
        <InputLabel for="listbox-component-state" class="pl-0">
          Change the component state
        </InputLabel>
        <InputRadioGroup
          id="listbox-component-state"
          v-model="state"
          :radioOptions="[
            {
              value: 'disabled',
              label: 'Disabled: disable interactivity with the component',
            },
            {
              value: 'error',
              label: 'Error: simulate an instance where an error occurred',
            },
          ]"
          :showClearButton="true"
        />
      </div>
  </form>
  <h3 class="text-heading-lg my-2">Demo</h3>
  <form @submit.prevent>
    <InputFile
      id="file-input-demo"
      label="Import file"
      :disabled="state === 'disabled'"
      :hasError="state === 'error'"
      @update:model-value="(value) => file =value"
    />
  </form>
  <h3 class="text-heading-lg my-2">Data output</h3>
    <output
      class="block w-full mt-2 bg-gray-100 py-3 px-2 pl-6 h-60 overflow-y-scroll shadow-inner"
    >
      <pre class="indent-[-5em]">
        {{ file }}
      </pre>
    </output>
</template>
