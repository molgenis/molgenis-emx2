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
import constants from "../../constants";
import { isNumericKey, flipSign } from "../../utils";

const { CODE_MINUS } = constants;

export default {
  extends: BaseInput,
  methods: {
    emitIfValid(event) {
      const value = event.target.value;
      if (isNaN(value) || !value) {
        this.$emit("update:modelValue", null);
      } else {
        this.$emit("update:modelValue", parseInt(value));
      }
    },
    handleKeyValidity(event) {
      const keyCode = event.which ? event.which : event.keyCode;
      if (keyCode === CODE_MINUS) {
        this.$emit("update:modelValue", parseInt(flipSign(event.target.value)));
      }
      if (!isNumericKey(event)) {
        event.preventDefault();
      }
    },
  },
};
</script>
