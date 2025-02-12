<template>
  <h2>Email component</h2>
  <p>Using a form with input of type email you can have email validation</p>
  <FormFields
    id="email-form"
    schemaId="schema-email"
    v-model="formValues"
    :metadata="{
      id: 'email-form',
      label: 'Email Form',
      tableType: 'FORM',
      columns: [
        {
          id: 'input-email',
          columnType: 'EMAIL',
          label: 'Input an email address',
        },
      ],
    }"
  />
</template>

<script setup lang="ts">
const formValues = ref();
</script>
