<script setup lang="ts">
const schemaId = ref("pet store");
const tableId = ref("Pet");
const labelTemplate = ref("${name}");
const value = ref([{ name: "spike" }]);
</script>

<template>
  <div class="flex">
    <div class="m-4 flex-3">
      <div>
        <InputRefArray
          id="story-ref"
          v-model="value"
          :schemaId="schemaId"
          :tableId="tableId"
          :labelTemplate="labelTemplate"
        />
        {{ value }}
      </div>
    </div>
  </div>
</template>
