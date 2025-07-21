<script setup lang="ts">
import { ref } from "vue";

const schemaId = ref<string>("pet store");
const tableId = ref<string>("Pet");
const labelTemplate = ref<string>("${name}");
const value = ref([{ name: "spike" }]);

const refExampleError = ref<boolean>(false);

function onBlur() {
  if (!value.value.length) {
    refExampleError.value = true;
  } else {
    refExampleError.value = false;
  }
}
</script>

<template>
  <InputTestContainer show-state v-slot="{ invalid, valid, disabled }">
    <label for="story-ref-dropdown">
      <span class="text-title font-bold">Select pets by name</span>
      <span class="text-disabled text-body-sm ml-3"> Required </span>
    </label>
    <InputRefDropdown
      id="story-ref-dropdown"
      v-model="value"
      placeholder="Select a pet"
      :refSchemaId="schemaId"
      :refTableId="tableId"
      :limit="5"
      :refLabel="labelTemplate"
      :valid="valid"
      :invalid="refExampleError || invalid"
      :disabled="disabled"
      :required="true"
      @update:modelValue="refExampleError ? onBlur() : null"
      @blur="onBlur"
    />
  </InputTestContainer>
</template>
