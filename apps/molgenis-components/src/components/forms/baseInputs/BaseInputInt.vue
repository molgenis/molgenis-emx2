<template>
  <input
    :id="id"
    type="number"
    step="1"
    :value="value"
    class="form-control"
    :class="{ 'is-invalid': errorMessage }"
    :aria-describedby="id + 'Help'"
    :placeholder="placeholder"
    :readonly="readonly"
    :required="required"
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
      const value = parseInt(event.target.value);
      if (event.target.value === "") {
        this.$emit("input", null);
      }
      if (!isNaN(value)) {
        this.$emit("input", value);
      }
    },
    handleKeyValidity(event) {
      if (!isNumericKey(event)) event.preventDefault();
    },
  },
};
</script>
