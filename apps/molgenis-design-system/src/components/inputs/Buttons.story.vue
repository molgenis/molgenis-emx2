<template>
  <Story title="Inputs/Buttons" :layout="{ type: 'grid', width: '400x' }">
    <Variant title="Submit Button">
      <InputButton label="Do Something" type="submit" />
    </Variant>
    <Variant title="Default Button">
      <InputButton label="Do Something" />
    </Variant>
    <Variant title="Disabled Button">
      <InputButton label="Do Something" :is-disabled="true" />
    </Variant>
  </Story>
</template>

<script setup>
import InputButton from "./Buttons.vue";
</script>
