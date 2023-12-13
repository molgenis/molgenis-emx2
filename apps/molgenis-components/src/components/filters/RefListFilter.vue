<template>
  <InputRefList
    :id="id"
    :modelValue="condition"
    @update:modelValue="onUpdateCondition"
    :tableId="tableId"
    :schemaId="schemaId"
    :refLabel="refLabel"
  />
</template>

<script>
import InputRefList from "../forms/InputRefList.vue";

export default {
  name: "RefListFilter",
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
    schemaId: {
      required: false,
      type: String,
    },
    tableId: {
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
