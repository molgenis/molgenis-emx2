<template>
  <Story title="Inputs/Select" :layout="{type: 'grid', width: '400px'}">
    <Variant title="Default">
      <SelectInput 
        id="default-options"
        label="Select an option"
        :options="options"
        option_label="name"
        option_value="code"
      />
    </Variant>
    <Variant title="Required">
      <SelectInput 
        id="default-options"
        label="Select an option"
        :required="true"
        :options="options"
        option_label="name"
        option_value="code"
      />
    </Variant>
    <Variant title="Disabled">
      <SelectInput 
        id="default-options"
        label="Select an option"
        :disabled="true"
        :options="options"
        option_label="name"
        option_value="code"
      />
    </Variant>
    <Variant title="Valid">
      <SelectInput 
        id="default-options"
        label="Select an option"
        :valid="true"
        :options="options"
        option_label="name"
        option_value="code"
      />
    </Variant>
    <Variant title="Error">
      <SelectInput 
        id="default-options"
        label="Select an option"
        error="No option selected"
        :options="options"
        option_label="name"
        option_value="code"
      />
    </Variant>
  </Story>
</template>

<script setup lang="ts">
import SelectInput from "./InputSelect.vue";

const options = [
  { code: 'A', name: 'Option A'},
  { code: 'B', name: 'Option B'},
  { code: 'C', name: 'Option C'},
]
</script>
