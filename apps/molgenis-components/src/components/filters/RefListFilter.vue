<template>
  <InputRefList
    :id="id"
    :modelValue="condition"
    @update:modelValue="onUpdateCondition"
    :tableName="tableName"
    :schemaName="schemaName"
    :refLabel="refLabel" />
</template>

<script>
import InputRefList from "../forms/InputRefList.vue";

export default {
  name: "InputRefListFilter",
  components: { InputRefList },
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
    refLabel: {
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
