<template>
  <p>
    The <code>Hyperlink Component</code> enables you to use the String component
    with the added validation of the input being a hyperlink.
  </p>
  <FormFields
    id="form-hyperlink"
    schemaId="schema-hyperlink"
    :metadata="{
      label: 'Hyperlink',
      id: 'hyperlink',
      tableType: 'my kind of table',
      columns: [
        {
          id: 'input-hyperlink',
          columnType: 'HYPERLINK',
          label: 'Input a hyperlink',
        },
      ],
    }"
    :data="[]"
  />
</template>

<script setup lang="ts"></script>
