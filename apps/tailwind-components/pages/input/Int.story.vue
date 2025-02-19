<template>
  <FormFields
    id="form-int"
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

<script setup lang="ts"></script>
