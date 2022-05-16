<template>
  <input
    :id="id"
    type="number"
    step="1"
    :value="value"
    class="form-control"
    :aria-describedby="id + 'Help'"
    :placeholder="placeholder"
    @keypress="handleKeyValidity"
    @input="emitIfValid"
  />
</template>

<script>
import BaseInput from "./BaseInput.vue";
import { isNumericKey } from "../../utils";

export default {
  extends: BaseInput,
  methods: {
    emitIfValid(event) {
      const value = parseFloat(event.target.value);
      if (event.target.value === "") {
        this.$emit("input", null);
      } else if (!isNaN(value)) {
        this.$emit("input", value);
      }
    },
    handleKeyValidity(event) {
      if (!isNumericKey(event)) {
        event.preventDefault();
      }
    },
  },
};
</script>
