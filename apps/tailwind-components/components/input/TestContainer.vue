<script setup lang="ts">
import type { InputState } from "~/types/types";
defineProps<{
  showState?: boolean;
  showPlaceholder?: boolean;
  showRequired?: boolean;
  showErrorMessage?: boolean;
}>();
const placeholder = ref("");
const state = ref<InputState>("default");
const errorMessage = ref("");
const required = ref(false);
</script>

<template>
  <div class="flex flex-row flex-grow">
    <div class="w-2/3 p-4">
      <slot
        name="default"
        :state="state"
        :placeholder="placeholder"
        :required="required"
        :errorMessage="errorMessage"
      ></slot>
    </div>
    <div class="w-1/3 p-4 sticky top-0">
      <FieldSet label="input prop settings">
        <FormField
          v-if="showPlaceholder"
          type="string"
          id="test-placeholder"
          v-model="placeholder"
          label="Placeholder"
          description="Placeholder of the input, if applicable"
        />
        <FormField
          v-if="showState"
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
        <FormField
          v-if="showErrorMessage"
          type="string"
          label="errorMessage"
          v-model="errorMessage"
          id="test-container-error-message"
          description="Type here an error message to see how that looks"
        />
        <FormField
          v-if="showRequired"
          type="bool"
          label="required"
          v-model="required"
          id="test-container-required"
          description="set to true to show required tags"
        />
        <slot name="settings"></slot>
      </FieldSet>
    </div>
  </div>
</template>
