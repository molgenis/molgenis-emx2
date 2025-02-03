<script setup lang="ts">
import type { InputState } from "~/types/types";

const placeholder = ref("");
const state = ref<InputState>("default");
</script>

<template>
  <div class="flex flex-col md:flex-row">
    <div class="md:w-2/3 p-4">
      <slot name="default" :state="state" :placeholder="placeholder"></slot>
    </div>
    <div class="md:w-1/3 p-4 sticky top-0">
      <FieldSet label="input prop settings">
        <FormField
          type="string"
          id="test-placeholder"
          v-model="placeholder"
          label="Placeholder"
          description="Placeholder of the input, if applicable"
        />
        <FormField
          type="radio"
          id="test-state"
          v-model="state"
          label="state"
          description="State of the input"
          :options="[
            { value: 'default', label: 'default' },
            { value: 'invalid', label: 'invalid' },
            { value: 'valid', label: 'valid' },
            { value: 'disabled', label: 'disabled' },
          ]"
        />
        <slot name="settings"></slot>
      </FieldSet>
    </div>
  </div>
</template>
