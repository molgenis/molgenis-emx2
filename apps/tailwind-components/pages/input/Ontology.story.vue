<script setup lang="ts">
import { ref } from "vue";

const ontologySchemaId = ref<string>("pet store");
const ontologyTableId = ref<string>("Tag");
const modelValue = ref("green");
const modelValueArray = ref(["colors", "mammals"]);
</script>

<template>
  <InputTestContainer
    show-state
    showBlurCount
    showFocusCount
    v-slot="{ invalid, valid, disabled, onBlur, onFocus }"
  >
    <h2 class="text-title">Ontology single</h2>
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
    <h2 class="mt-4 text-title">Ontology array</h2>
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
  </InputTestContainer>
</template>
