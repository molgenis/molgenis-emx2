<template>
  <FormFields
    id="form-long"
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

<script setup lang="ts"></script>
