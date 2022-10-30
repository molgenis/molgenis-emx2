<template>
  <InputOntology
    :id="id"
    :value="condition"
    @update:modelValue="onUpdateCondition"
    :tableName="tableName"
    :graphqlURL="graphqlURL"
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
    graphqlURL: {
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
