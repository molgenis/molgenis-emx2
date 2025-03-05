<script setup lang="ts">
const ontologySchemaId = ref<string>("CatalogueOntologies");
const ontologyTableId = ref<string>("Diseases");
const modelValue = ref("green");
const modelValueArray = ref(["green"]);
</script>

<template>
  <h2>Ontology single</h2>
  <InputOntology
    id="test-ontology-input-id"
    v-model="modelValue"
    :ontologySchemaId="ontologySchemaId"
    :ontologyTableId="ontologyTableId"
  />
  modelValue = {{ modelValue }}
  <h2>Ontology array</h2>
  <InputOntology
    id="test-ontology-array-input-id"
    :isArray="true"
    v-model="modelValueArray"
    :ontologySchemaId="ontologySchemaId"
    :ontologyTableId="ontologyTableId"
  />
  modelValueArray = {{ modelValueArray }}
</template>
