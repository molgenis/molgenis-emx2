<template>
  <FormFields
    id="form-long"
    v-model="data"
    schemaId="schema-long"
    :metadata="{
      label: 'Long',
      id: 'long',
      tableType: 'my kind of table',
      columns: [
        {
          id: 'input-long',
          columnType: 'LONG',
          label: 'Input a long',
        },
      ],
    }"
    :data="[]"
  />
</template>

<script setup lang="ts">
import type { recordValue } from "../../../metadata-utils/src/types";

const data = ref<recordValue>({ "input-long": undefined });
</script>
