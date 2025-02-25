<template>
  <input
    :id="id"
    :value="modelValue"
    class="form-control"
    :class="{ 'is-invalid': errorMessage }"
    :aria-describedby="id + 'Help'"
    :placeholder="placeholder"
    :readonly="readonly"
    :required="isRequired(required)"
    @keypress="handleKeyValidity($event)"
    @input="emitIfValid($event)"
  />
</template>

<script lang="ts">
import constants from "../../constants";
import { flipSign, isNumericKey } from "../../utils";
import { isRequired } from "../formUtils/formUtils";
import BaseInput from "./BaseInput.vue";

const { CODE_MINUS } = constants;

export default {
  extends: BaseInput,
  methods: {
    emitIfValid(event: any) {
      const value = event.target.value;
      if (value?.length) {
        this.$emit("update:modelValue", value);
      } else {
        this.$emit("update:modelValue", null);
      }
    },
    handleKeyValidity(event: any) {
      const keyCode = event.which ?? event.keyCode;
      if (keyCode === CODE_MINUS) {
        this.$emit("update:modelValue", flipSign(event.target.value));
      }
      if (!isNumericKey(event)) {
        event.preventDefault();
      }
    },
    isRequired,
  },
  emits: ["update:modelValue"],
};
</script>
