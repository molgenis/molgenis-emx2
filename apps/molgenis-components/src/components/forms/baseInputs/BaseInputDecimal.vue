<template>
  <input
    :id="id"
    type="number"
    step="1"
    :value="modelValue"
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
import { isNumericKey, flipSign } from "../../utils";
import constants from "../../constants";

const { CODE_MINUS } = constants;

export default {
  extends: BaseInput,
  methods: {
    emitIfValid(event) {
      const value = parseFloat(event.target.value);
      if (event.target?.value === null) {
        this.$emit("update:modelValue", null);
      } else if (!isNaN(value)) {
        this.$emit("update:modelValue", value);
      }
    },
    handleKeyValidity(event) {
      const keyCode = event.which ? event.which : event.keyCode;
      if (keyCode === CODE_MINUS) {
        this.$emit("update:modelValue", parseFloat(flipSign(parseFloat(event.target.value))));
      }
      if (!isNumericKey(event)) {
        event.preventDefault();
      }
    },
  },
};
</script>
