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
import { flipSign, isNumericKey } from "../../utils";
import { isRequired } from "../formUtils/formUtils";
import BaseInput from "./BaseInput.vue";

const { CODE_MINUS, CODE_PERIOD } = constants;

export default {
  extends: BaseInput,
  methods: {
    handleInputChanged(event: any) {
      const value = event.target?.value;
      if (!value) {
        this.$emit("update:modelValue", null);
      } else {
        this.emitIfValid(value);
      }
    },
    emitIfValid(strValue: string | null) {
      if (strValue === null) {
        this.$emit("update:modelValue", null);
        return;
      }
      const noCommaValue = strValue.replace(",", "");
      const value = parseFloat(noCommaValue);
      if (!isNaN(value)) {
        this.$emit("update:modelValue", value);
      } else {
        this.$emit("update:modelValue", strValue);
      }
    },
    handleKeyValidity(event: any) {
      const keyCode = event.which ?? event.keyCode;
      if (keyCode === CODE_MINUS) {
        const flipped = flipSign(event.target?.value);
        this.emitIfValid(flipped);
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
