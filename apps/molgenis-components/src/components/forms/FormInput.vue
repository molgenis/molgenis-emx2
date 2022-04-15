<template>
  <component
    :is="inputType"
    :id="id"
    :value="value"
    :isMultiSelect="isMultiSelect"
    v-bind="$props"
    @input="$emit('input', $event)"
  />
</template>
<script>
import BaseInput from "./BaseInput.vue";
import ArrayInput from "./ArrayInput.vue";
import InputString from "./InputString.vue";
import InputOntology from "./InputOntology.vue";

export default {
  name: "FormInput",
  extends: BaseInput,
  props: {
    /* to pass options hardcode*/
    options: Array,
  },
  computed: {
    isMultiSelect() {
      return this.type === "ONTOLOGY_ARRAY";
    },
    inputType() {
      return {
        STRING: InputString,
        STRING_ARRAY: ArrayInput,
        ONTOLOGY: InputOntology,
        ONTOLOGY_ARRAY: InputOntology,
      }[this.type];
    },
  },
};
</script>
