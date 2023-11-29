<template>
  <Story title="Inputs/Text" :layout="{ type: 'grid', width: '400px' }">
    <Variant title="Default">
      <InputText
        id="search-patients-default"
        :label="label"
        :placeholder="placeholder"
      />
    </Variant>
    <Variant title="Disabled">
      <InputText
        id="search-patients-disabled"
        :label="label"
        :placeholder="placeholder"
        :is-disabled="true"
      />
    </Variant>
    <Variant title="Required">
      <InputText
        id="search-patients-required"
        :label="label"
        :placeholder="placeholder"
        :is-required="true"
      />
    </Variant>
    <Variant title="Valid">
      <InputText
        id="search-patients-valid"
        :label="label"
        :placeholder="placeholder"
        :is-valid="true"
      />
    </Variant>
    <Variant title="Error">
      <InputText
        id="search-patients-error"
        :label="label"
        :placeholder="placeholder"
        :has-error="true"
      />
    </Variant>
  </Story>
</template>
<script setup lang="ts">
import InputText from "./InputText.vue";

const label = "Search for patients";
const placeholder =
  "Enter one or more identifiers; e.g., 'A-001' or 'A-001,A-002'";
</script>
