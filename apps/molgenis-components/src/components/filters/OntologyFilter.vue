<template>
  <div>
    <InputOntology
      :id="id"
      :value="condition"
      @input="onUpdateCondition"
      :tableName="tableName"
      :graphqlURL="graphqlURL"
      :isMultiSelect="true"
      :showExpanded="true"
    />
    {{ JSON.stringify(condition, null, 2) }}
  </div>
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
      type: Array,
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
