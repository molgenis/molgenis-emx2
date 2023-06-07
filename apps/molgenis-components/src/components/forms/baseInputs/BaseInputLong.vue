<template>
  <input
    :id="id"
    :value="modelValue"
    class="form-control"
    :class="{ 'is-invalid': errorMessage || bigIntError }"
    :aria-describedby="id + 'Help'"
    :placeholder="placeholder"
    :readonly="readonly"
    :required="required"
    @keypress="handleKeyValidity($event)"
    @input="emitIfValid($event)" />
</template>

<script>
import BaseInput from "./BaseInput.vue";
import constants from "../../constants";
import { isNumericKey, flipSign, getBigIntError } from "../../utils";

const { CODE_MINUS } = constants;

export default {
  extends: BaseInput,
  methods: {
    emitIfValid(event) {
      const value = event.target.value;
      if (value?.length) {
        this.$emit("update:modelValue", value);
      } else {
        this.$emit("update:modelValue", null);
      }
    },
    handleKeyValidity(event) {
      const keyCode = event.which ? event.which : event.keyCode;
      if (keyCode === CODE_MINUS) {
        this.$emit("update:modelValue", flipSign(event.target.value));
      }
      if (!isNumericKey(event)) {
        event.preventDefault();
      }
    },
  },
  computed: {
    bigIntError() {
      if (this.modelValue) {
        return getBigIntError(this.modelValue);
      }
    },
  },
  emits: ["update:modelValue"],
};
</script>
