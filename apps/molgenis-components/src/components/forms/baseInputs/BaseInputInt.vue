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
    :required="isRequired(required)"
    @keypress="handleKeyValidity"
    @input="emitIfValid"
  />
</template>

<script lang="ts">
import BaseInput from "./BaseInput.vue";
import constants from "../../constants";
import { isNumericKey, flipSign } from "../../utils";
import { isRequired } from "../formUtils/formUtils";

const { CODE_MINUS } = constants;

export default {
  extends: BaseInput,
  methods: {
    emitIfValid(event: any) {
      const value = event.target.value;
      if (isNaN(value) || !value) {
        this.$emit("update:modelValue", null);
      } else {
        this.$emit("update:modelValue", parseInt(value));
      }
    },
    handleKeyValidity(event: any) {
      const keyCode = event.which ? event.which : event.keyCode;
      if (keyCode === CODE_MINUS) {
        const flipped = flipSign(event.target.value);
        if (typeof flipped === "string") {
          this.$emit("update:modelValue", parseInt(flipped));
        } else {
          this.$emit("update:modelValue", flipped);
        }
      }
      if (!isNumericKey(event) || event.key === ".") {
        event.preventDefault();
      }
    },
    isRequired,
  },
};
</script>
