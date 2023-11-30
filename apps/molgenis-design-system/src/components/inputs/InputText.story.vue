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
        :disabled="true"
      />
    </Variant>
    <Variant title="Required">
      <InputText
        id="search-patients-required"
        :label="label"
        :placeholder="placeholder"
        :required="true"
      >
        <template v-slot:description>
          <p class="text-gray-600/80">Search for patient identifier, affiliation, etc.</p>
        </template>
      </InputText>
    </Variant>
    <Variant title="Valid">
      <InputText
        id="search-patients-valid"
        :label="label"
        :placeholder="placeholder"
        :valid="true"
      />
    </Variant>
    <Variant title="Error">
      <InputText
        id="search-patients-error"
        :label="label"
        :placeholder="placeholder"
        :has-error="true"
        error="Search term cannot be blank"
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
