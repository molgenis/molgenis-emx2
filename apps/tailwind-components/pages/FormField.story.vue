<template>
  <InputTestContainer
    show-placeholder
    show-state
    show-required
    show-error-message
  >
    <template
      v-slot:default="{
        placeholder,
        valid,
        invalid,
        disabled,
        required,
        errorMessage,
      }"
    >
      <template v-for="type in Object.keys(demoValue)">
        <FormField
          :type="type as CellValueType"
          v-model="demoValue[type]"
          :id="'input-' + type"
          :placeholder="placeholder"
          :valid="valid"
          :invalid="invalid"
          :disabled="disabled"
          :errorMessage="errorMessage || null"
          :options="demoOptions"
          :label="'Demo input for type=' + type"
          :required="required"
          refSchemaId="pet store"
          :refTableId="type.startsWith('ontology') ? 'Tag' : 'Pet'"
          refLabel="${name}"
          description="here a demo description to see that that works too"
          @blur="blurCount++"
          @focus="focusCount++"
          class="mb-3"
        />
      </template>
    </template>
    <template #settings>
      <div class="mt-4">blurCount: {{ blurCount }}</div>
      <div class="mt-4">focusCount: {{ focusCount }}</div>
      <div class="mt-4">value: {{ demoValue }}</div>
    </template>
  </InputTestContainer>
</template>

<script setup lang="ts">
import { ref } from "vue";
import type { CellValueType } from "../../metadata-utils/src/types";

const demoValue = ref<Record<string, any>>({
  string: "test string",
  checkbox: [1],
  radio: 1,
  select: 1,
  text: "some demo text",
  ref: null,
  bool: true,
  int: 42,
  decimal: -13.37,
  long: "37",
  string_array: ["Hello", "World"],
  decimal_array: [3.14159265],
  ontology: { name: "green" },
  ontology_array: [{ name: "green" }, { name: "mammals" }],
});

const focusCount = ref(0);
const blurCount = ref(0);

const demoOptions = ref([
  { label: "option1", value: 1 },
  { label: "option2", value: 2 },
]);
</script>
