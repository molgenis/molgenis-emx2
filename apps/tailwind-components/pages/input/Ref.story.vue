<script setup lang="ts">
const schemaId = ref<string>("pet store");
const tableId = ref<string>("Pet");
const labelTemplate = ref<string>("${name}");
const value = ref([{ name: "spike" }]);
const value2 = ref([{ name: "spike" }]);
</script>

<template>
  <InputTestContainer show-state v-slot="{ invalid, valid, disabled }">
    <div>
      <h3>Ref array example</h3>
      <InputLabel for="story-ref-array"> Select pets by name </InputLabel>
      <InputRef
        id="story-ref-array"
        v-model="value"
        :refSchemaId="schemaId"
        :refTableId="tableId"
        :limit="5"
        :refLabel="labelTemplate"
        :valid="valid"
        :invalid="invalid"
        :disabled="disabled"
      />
      <div class="pt-5">value selected: {{ value }}</div>
    </div>
    <div class="pt-5">
      <h3>Ref example</h3>
      <InputLabel for="story-ref"> Select pets by name </InputLabel>
      <InputRef
        id="story-ref"
        v-model="value2"
        :refSchemaId="schemaId"
        :refTableId="tableId"
        :limit="5"
        :refLabel="labelTemplate"
        :isArray="false"
        :valid="valid"
        :invalid="invalid"
        :disabled="disabled"
      />
    </div>
    <div class="pt-5">value selected: {{ value2 }}</div>
  </InputTestContainer>
</template>
