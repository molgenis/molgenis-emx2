<template>
  <InputRef
    :id="id"
    :modelValue="condition"
    @update:modelValue="onUpdateCondition"
    :tableName="tableName"
    :schemaName="schemaName" />
</template>

<script>
import InputRef from "../forms/InputRef.vue";

export default {
  name: "RefFilter",
  components: { InputRef },
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
