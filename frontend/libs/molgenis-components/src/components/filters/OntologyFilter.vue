<template>
  <InputOntology
    :id="id"
    :modelValue="condition"
    @update:modelValue="onUpdateCondition"
    :tableName="tableName"
    :schemaName="schemaName"
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
    schemaName: {
      required: false,
      type: String,
    },
    tableName: {
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
