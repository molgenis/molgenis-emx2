<template>
  <FormFields
    id="form-decimal"
    v-model="data"
    schemaId="schema-decimal"
    :metadata="{
      label: 'Decimal',
      id: 'decimal',
      tableType: 'my kind of table',
      columns: [
        {
          id: 'input-decimal',
          columnType: 'DECIMAL',
          label: 'Input a decimal',
        },
      ],
    }"
    :data="[]"
  />
</template>

<script setup lang="ts">
import type { recordValue } from "../../../metadata-utils/src/types";

const data = ref<recordValue>({ "input-decimal": undefined });
</script>
