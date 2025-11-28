<script setup lang="ts">
import { ref } from "vue";

const ontologySchemaId = ref<string>("pet store");
const ontologyTableId = ref<string>("Tag");
const modelValue = ref("green");
const modelValueArray = ref(["colors", "mammals"]);

const ontologySchemaId2 = ref<string>("CatalogueOntologies");
const ontologyTableId2 = ref<string>("Countries");
const modelValue2 = ref("Andorra");
const modelValueArray2 = ref(["Andorra", "Spain"]);

const ontologySchemaId3 = ref<string>("CatalogueOntologies");
const ontologyTableId3 = ref<string>("Keywords");
const modelValue3 = ref("Maternal height");
const modelValueArray3 = ref(["Maternal height", "Country of cohort"]);
</script>

<template>
  <InputTestContainer
    show-state
    showBlurCount
    showFocusCount
    v-slot="{ invalid, valid, disabled, onBlur, onFocus }"
  >
    <h2 class="text-title">Small ontology single</h2>
    <div class="mt-4">
      <InputOntology
        id="test-ontology-input-id"
        v-model="modelValue"
        :ontologySchemaId="ontologySchemaId"
        :ontologyTableId="ontologyTableId"
        :invalid="invalid"
        :valid="valid"
        :disabled="disabled"
        @blur="onBlur"
        @focus="onBlur"
      />
      <p class="mt-4">modelValue = {{ modelValue }}</p>
    </div>
    <h2 class="mt-4 text-title">Small ontology array</h2>
    <div class="mt-4">
      <InputOntology
        id="test-ontology-array-input-id"
        :isArray="true"
        v-model="modelValueArray"
        :ontologySchemaId="ontologySchemaId"
        :ontologyTableId="ontologyTableId"
        :invalid="invalid"
        :valid="valid"
        :disabled="disabled"
        @blur="onBlur"
        @focus="onFocus"
      />
      <p class="mt-4">modelValueArray = {{ modelValueArray }}</p>
    </div>
    <h2 class="text-title">Large flat ontology single</h2>
    <div class="mt-4">
      <InputOntology
        id="test-ontology-input-id"
        v-model="modelValue2"
        :ontologySchemaId="ontologySchemaId2"
        :ontologyTableId="ontologyTableId2"
        :invalid="invalid"
        :valid="valid"
        :disabled="disabled"
        @blur="onBlur"
        @focus="onBlur"
      />
      <p class="mt-4">modelValue = {{ modelValue }}</p>
    </div>
    <h2 class="mt-4 text-title">Large flat ontology array</h2>
    <div class="mt-4">
      <InputOntology
        id="test-ontology-array-input-id"
        :isArray="true"
        v-model="modelValueArray2"
        :ontologySchemaId="ontologySchemaId2"
        :ontologyTableId="ontologyTableId2"
        :invalid="invalid"
        :valid="valid"
        :disabled="disabled"
        @blur="onBlur"
        @focus="onFocus"
      />
      <p class="mt-4">modelValueArray = {{ modelValueArray }}</p>
    </div>

    <h2 class="text-title">Large deep ontology single</h2>
    <div class="mt-4">
      <InputOntology
        id="test-ontology-input-id"
        v-model="modelValue3"
        :ontologySchemaId="ontologySchemaId3"
        :ontologyTableId="ontologyTableId3"
        :invalid="invalid"
        :valid="valid"
        :disabled="disabled"
        @blur="onBlur"
        @focus="onBlur"
      />
      <p class="mt-4">modelValue = {{ modelValue }}</p>
    </div>
    <h2 class="mt-4 text-title">Large deep ontology array</h2>
    <div class="mt-4">
      <InputOntology
        id="test-ontology-array-input-id"
        :isArray="true"
        v-model="modelValueArray3"
        :ontologySchemaId="ontologySchemaId3"
        :ontologyTableId="ontologyTableId3"
        :invalid="invalid"
        :valid="valid"
        :disabled="disabled"
        @blur="onBlur"
        @focus="onFocus"
      />
      <p class="mt-4">modelValueArray = {{ modelValueArray }}</p>
    </div>
  </InputTestContainer>
</template>
