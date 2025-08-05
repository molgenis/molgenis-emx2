<script setup lang="ts">
import { ref } from "vue";

const schemaId = ref<string>("pet store");
const tableId = ref<string>("Pet");
const labelTemplate = ref<string>("${name}");
const value_singular = ref({ name: "spike" });
const value_multiple = ref([{ name: "spike" }]);
const value_undefined = ref();
</script>

<template>
  <InputTestContainer
    v-slot="{ invalid, valid, disabled, multiple, required }"
    :show-state="true"
    :show-multiple="true"
    :show-required="true"
  >
    <label for="story-ref-dropdown">
      <span class="text-title font-bold">Select pets by name</span>
      <span class="text-disabled text-body-sm ml-3" v-if="required">
        Required
      </span>
    </label>
    <InputRefSelect
      v-if="multiple"
      id="story-ref-dropdown"
      v-model="value_multiple"
      placeholder="Select a pet"
      :refSchemaId="schemaId"
      :refTableId="tableId"
      :refLabel="labelTemplate"
      :limit="5"
      :multiselect="multiple"
      :valid="valid"
      :invalid="invalid"
      :disabled="disabled"
      :required="true"
    />
    /> value: {{ multiple ? value_multiple : value_singular }}
    <br />
    <label for="story-ref-dropdown">
      <span class="text-title font-bold"
        >Select pets by name (no initial value selected)</span
      >
      <span class="text-disabled text-body-sm ml-3" v-if="required">
        Required
      </span>
    </label>
    <InputRefSelect
      id="story-ref-dropdown-no-value"
      v-model="value_undefined"
      placeholder="Select a pet"
      :refSchemaId="schemaId"
      :refTableId="tableId"
      :refLabel="labelTemplate"
      :limit="5"
      :multiselect="multiple"
      :valid="valid"
      :invalid="invalid"
      :disabled="disabled"
      :required="true"
    />
    value: {{ value_undefined }}
  </InputTestContainer>
</template>
