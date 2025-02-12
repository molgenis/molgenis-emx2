<template>
  <p>
    The <code>Hyperlink Component</code> enables you to use the String component
    with the added validation of the input being a hyperlink.
  </p>
  <FormFields
    :metadata="{
      columns: [
        {
          id: 'input-hyperlink',
          columnType: 'HYPERLINK',
          label: 'Input a hyperlink',
        },
      ],
    }"
  />
</template>

<script setup lang="ts"></script>
