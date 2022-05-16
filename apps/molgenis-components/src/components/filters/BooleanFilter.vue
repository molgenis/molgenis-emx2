<template>
  <InputBoolean
    :id="id"
    :value="condition"
    @input="onUpdateCondition"
    @clearCondition="$emit('clearCondition')"
  />
</template>

<script>
import InputBoolean from "../forms/InputBoolean.vue";

export default {
  name: "BooleanFilter",
  components: { InputBoolean },
  props: {
    id: {
      type: String,
      required: true,
    },
    name: {
      type: String,
      required: false,
      default: function () {
        return this.id;
      },
    },
    condition: {
      type: Boolean,
      required: false,
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

<style>
</style>