<template>
  <InputOntology
    :id="id"
    :modelValue="condition"
    @update:modelValue="onUpdateCondition"
    :tableId="tableId"
    :schemaId="schemaId"
    :isMultiSelect="true"
    :showExpanded="true"
  />
</template>

<script>
import InputOntology from "../forms/InputOntology.vue";

export default {
  name: "InputOntologyFilter",
  components: { InputOntology },
  props: {
    id: {
      type: String,
      required: true,
    },
    condition: {
      type: Object,
      required: false,
    },
    schemaId: {
      required: false,
      type: String,
    },
    tableId: {
      type: String,
      required: true,
    },
  },
  methods: {
    onUpdateCondition(newValue) {
      if (newValue === null) {
        this.$emit("clearCondition", newValue);
      } else {
        this.$emit("updateCondition", newValue);
      }
    },
  },
};
</script>
