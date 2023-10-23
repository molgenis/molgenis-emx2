<template>
  <InputRef
    :id="id"
    :modelValue="condition"
    @update:modelValue="onUpdateCondition"
    :tableId="tableId"
    :schemaId="schemaId"
  />
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
