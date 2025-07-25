<script setup lang="ts">
import { ref } from "vue";

const schemaId = ref<string>("pet store");
const tableId = ref<string>("Pet");
const labelTemplate = ref<string>("${name}");
const value = ref([{ name: "spike" }]);
</script>

<template>
  <InputTestContainer
    show-state
    v-slot="{ invalid, valid, disabled, multiple, required }"
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
      id="story-ref-dropdown"
      v-model="value"
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
  </InputTestContainer>
</template>
