<script setup lang="ts">
const ontologySchemaId = ref<string>("pet store");
const ontologyTableId = ref<string>("Tag");
const modelValue = ref("green");
const modelValueArray = ref(["colors", "mammals"]);
</script>

<template>
  <InputTestContainer show-state v-slot="{ invalid, valid, disabled }">
    <h2>Ontology single</h2>
    <InputOntology
      id="test-ontology-input-id"
      v-model="modelValue"
      :ontologySchemaId="ontologySchemaId"
      :ontologyTableId="ontologyTableId"
      :invalid="invalid"
      :valid="valid"
      :disabled="disabled"
    />
    modelValue = {{ modelValue }}
    <h2>Ontology array</h2>
    <InputOntology
      id="test-ontology-array-input-id"
      :isArray="true"
      v-model="modelValueArray"
      :ontologySchemaId="ontologySchemaId"
      :ontologyTableId="ontologyTableId"
      :invalid="invalid"
      :valid="valid"
      :disabled="disabled"
    />
    modelValueArray = {{ modelValueArray }}
  </InputTestContainer>
</template>
