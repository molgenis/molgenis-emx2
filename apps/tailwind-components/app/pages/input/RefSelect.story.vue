<script setup lang="ts">
import { ref, computed } from "vue";
import type { recordValue } from "../../../../metadata-utils/src/types";

const schemaId = ref<string>("pet store");
const tableId = ref<string>("Pet");
const labelTemplate = ref<string>("${name}");

const multiselect = ref<boolean>(false);

const modelValueDefined = computed(() => {
  const defaultValue: recordValue = { name: "spike" };
  if (multiselect.value) {
    return [defaultValue];
  } else {
    return defaultValue;
  }
});
const modelValueUndefined = computed(() => {
  if (multiselect.value) {
    return [];
  } else {
    return;
  }
});
</script>

<template>
  <InputTestContainer
    v-slot="{ invalid, valid, disabled, multiple, required }"
    :show-state="true"
    :show-multiple="true"
    :show-required="true"
    @multiple="(value:boolean) => multiselect = value"
  >
    <div class="mb-6">
      <label for="story-ref-dropdown-model-value-defined">
        <span class="text-title font-bold">Select pets by name</span>
        <span class="text-disabled text-body-sm ml-3" v-if="required">
          Required
        </span>
      </label>
      <InputRefSelect
        id="story-ref-dropdown-model-value-defined"
        v-model="modelValueDefined"
        placeholder="Select a pet"
        :refSchemaId="schemaId"
        :refTableId="tableId"
        :refLabel="labelTemplate"
        :limit="5"
        :multiselect="multiselect"
        :valid="valid"
        :invalid="invalid"
        :disabled="disabled"
        :required="true"
      />
      <div class="mt-4">
        <h3 class="text-heading-md mb-2">Output</h3>
        <output
          class="block w-full mt-2 border bg-input text-input py-3 px-2 pl-6"
        >
          <code>Value: {{ modelValueDefined }}</code>
        </output>
      </div>
    </div>
    <div>
      <label for="story-ref-dropdown-model-value-undefined">
        <span class="text-title font-bold"
          >Select pets by name (no initial value)</span
        >
        <span class="text-disabled text-body-sm ml-3" v-if="required">
          Required
        </span>
      </label>
      <InputRefSelect
        id="story-ref-dropdown-model-value-undefined"
        v-model="modelValueUndefined"
        placeholder="Select a pet"
        :refSchemaId="schemaId"
        :refTableId="tableId"
        :refLabel="labelTemplate"
        :limit="5"
        :multiselect="multiselect"
        :valid="valid"
        :invalid="invalid"
        :disabled="disabled"
        :required="true"
      />
      <div class="mt-4">
        <h3 class="text-heading-md mb-2">Output</h3>
        <output
          class="block w-full mt-2 border bg-input text-input py-3 px-2 pl-6"
        >
          <code>Value: {{ modelValueUndefined }}</code>
        </output>
      </div>
    </div>
  </InputTestContainer>
</template>
