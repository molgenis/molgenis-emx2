<template>
  <FormFields
    id="form-int"
    v-model="data"
    schemaId="schema-int"
    :metadata="{
      label: 'Int',
      id: 'int',
      tableType: 'my kind of table',
      columns: [
        {
          id: 'input-int',
          columnType: 'INT',
          label: 'Input a int',
        },
      ],
    }"
    :data="[]"
  />
</template>

<script setup lang="ts">
import type { recordValue } from "../../../metadata-utils/src/types";

const data = ref<recordValue>({ "input-int": undefined });
</script>
