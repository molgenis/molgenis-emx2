<template>
  <input
    :id="id"
    type="text"
    :value="modelValue"
    class="form-control"
    :class="{ 'is-invalid': errorMessage }"
    :aria-describedby="id + 'Help'"
    :placeholder="placeholder"
    :readonly="readonly"
    :required="isRequired(required)"
    @keypress="handleKeyValidity"
    @input="handleInputChanged"
  />
</template>

<script lang="ts">
import constants from "../../constants";
import { flipMinusSign, isNumericKey } from "../../utils";
import { isRequired } from "../formUtils/formUtils";
import BaseInput from "./BaseInput.vue";

const { CODE_MINUS, CODE_PERIOD } = constants;

export default {
  extends: BaseInput,
  methods: {
    handleInputChanged(event: any) {
      const value = event.target?.value;
      this.$emit("update:modelValue", value ? value : null);
    },
    handleKeyValidity(event: any) {
      const keyCode = event.which ?? event.keyCode;
      if (keyCode === CODE_MINUS) {
        this.$emit("update:modelValue", flipMinusSign(event.target?.value));
      }
      if (keyCode === CODE_PERIOD && event.target?.value.indexOf(".") > -1) {
        event.preventDefault();
      }
      if (!isNumericKey(event)) {
        event.preventDefault();
      }
    },
    isRequired,
  },
};
</script>
