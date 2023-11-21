<template>
  <Story title="Inputs/String" :layout="{ type: 'grid', width: '400px' }">
    <Variant title="Default">
      <InputString
        id="search-patients-default"
        :label="label"
        :placeholder="placeholder"
      />
    </Variant>
    <Variant title="Disabled">
      <InputString
        id="search-patients-disabled"
        :label="label"
        :placeholder="placeholder"
        :is-disabled="true"
      />
    </Variant>
    <Variant title="Required">
      <InputString
        id="search-patients-required"
        :label="label"
        :placeholder="placeholder"
        :is-required="true"
      />
    </Variant>
    <Variant title="Valid">
      <InputString
        id="search-patients-valid"
        :label="label"
        :placeholder="placeholder"
        :is-valid="true"
      />
    </Variant>
    <Variant title="Error">
      <InputString
        id="search-patients-error"
        :label="label"
        :placeholder="placeholder"
        :has-error="true"
      />
    </Variant>
  </Story>
</template>
<script setup lang="ts">
import InputString from "./InputString.vue";

const label="Search for patients"
const placeholder="Enter one or more identifiers; e.g., 'A-001' or 'A-001,A-002'"
</script>
