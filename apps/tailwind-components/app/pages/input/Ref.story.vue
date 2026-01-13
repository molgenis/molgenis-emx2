<script setup lang="ts">
import { ref } from "vue";

const schemaId = ref<string>("pet store");
const tableId = ref<string>("Pet");
const labelTemplate = ref<string>("${name}");
const value = ref([{ name: "spike" }]);
const value2 = ref([{ name: "spike" }]);

const schemaId2 = ref<string>("CatalogueOntologies");
const tableId2 = ref<string>("Countries");
const labelTemplate2 = ref<string>("${name}");
const value3 = ref([{ name: "Belgium" }]);
const value4 = ref([{ name: "Belgium" }]);

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
    <div>
      <label for="story-ref-array">
        <span class="text-title font-bold">Select pets by name</span>
        <span class="text-disabled text-body-sm ml-3"> Required </span>
      </label>
      <InputRef
        id="story-ref-array"
        v-model="value"
        :refSchemaId="schemaId"
        :refTableId="tableId"
        :limit="10"
        :refLabel="labelTemplate"
        :valid="valid"
        :invalid="refExampleError || invalid"
        :disabled="disabled"
        @update:modelValue="refExampleError ? onBlur() : null"
        @blur="onBlur"
      />
      <div id="story-ref-array-error-container">
        <Message
          id="story-ref-array-error"
          :invalid="refExampleError"
          v-if="refExampleError"
        >
          <span>Field is required</span>
        </Message>
        <Message id="story-ref-array-error" :invalid="invalid" v-if="invalid">
          <span>A generic error message</span>
        </Message>
      </div>
      <div class="pt-5">value selected: {{ value }}</div>
    </div>
    <div class="pt-5">
      <label for="story-ref-array">
        <span class="text-title font-bold">Select pets by name</span>
      </label>
      <InputRef
        id="story-ref"
        v-model="value2"
        :refSchemaId="schemaId"
        :refTableId="tableId"
        :limit="10"
        :refLabel="labelTemplate"
        :isArray="false"
        :valid="valid"
        :invalid="invalid"
        :disabled="disabled"
      />
    </div>
    <div class="pt-5">value selected: {{ value2 }}</div>
    <div class="pt-5">
      <label for="story-ref-array">
        <span class="text-title font-bold">Example of a long ref</span>
      </label>
      <InputRef
        id="story-ref"
        :limit="0"
        v-model="value3"
        :refSchemaId="schemaId2"
        :refTableId="tableId2"
        :refLabel="labelTemplate2"
        :isArray="false"
        :valid="valid"
        :invalid="invalid"
        :disabled="disabled"
      />
    </div>
    <div class="pt-5">value selected: {{ value3 }}</div>
    <div class="pt-5">
      <label for="story-ref-array">
        <span class="text-title font-bold">Example of a long ref</span>
      </label>
      <InputRef
        id="story-ref"
        :limit="0"
        v-model="value4"
        :refSchemaId="schemaId2"
        :refTableId="tableId2"
        :refLabel="labelTemplate2"
        :isArray="true"
        :valid="valid"
        :invalid="invalid"
        :disabled="disabled"
      />
    </div>
    <div class="pt-5">value selected: {{ value4 }}</div>
  </InputTestContainer>
</template>
