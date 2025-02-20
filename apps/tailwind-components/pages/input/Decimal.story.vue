<template>
  <FormFields
    id="form-decimal"
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

<script setup lang="ts"></script>
