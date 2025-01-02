<script setup lang="ts">
const schemaId = ref("pet store");
const tableId = ref("Pet");
const labelTemplate = ref("${name}");
const value = ref([{ name: "spike" }]);
const value2 = ref([{ name: "spike" }]);
</script>

<template>
  <div>
    REF_ARRAY:
    <InputRef
      id="story-ref-array"
      v-model="value"
      :refSchemaId="schemaId"
      :refTableId="tableId"
      :limit="5"
      :refLabel="labelTemplate"
    />
  </div>
  <div>
    REF:
    <InputRef
      id="story-ref"
      v-model="value2"
      :refSchemaId="schemaId"
      :refTableId="tableId"
      :limit="5"
      :refLabel="labelTemplate"
      :isArray="false"
    />
  </div>
</template>
