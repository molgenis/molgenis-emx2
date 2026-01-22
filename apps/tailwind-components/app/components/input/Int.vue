<template>
  <InputString
    :id="id"
    :aria-describedby="describedBy"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :placeholder="placeholder"
    :required="required"
    v-model="modelValue"
    @focus="$emit('focus')"
    @blur="$emit('blur')"
    @input="handleInput"
    @keypress="handleKeyValidity"
  />
</template>

<script setup lang="ts">
import constants from "../../../../molgenis-components/src/components/constants";
import {
  flipSign,
  isNumericKey,
} from "../../../../molgenis-components/src/components/utils";
import type { IInputProps } from "../../../types/types";
import InputString from "./String.vue";

const modelValue = defineModel<string | number | undefined | null>({
  required: true,
});

defineProps<
  IInputProps & {
    required?: boolean;
  }
>();

const { CODE_MINUS, CODE_PERIOD } = constants;

const emit = defineEmits(["focus", "blur", "update:modelValue"]);

function handleKeyValidity(event: any) {
  const keyCode = event.which ?? event.keyCode;
  if (keyCode === CODE_MINUS) {
    const flipped = flipSign(event.target?.value);
    emit("update:modelValue", flipped);
  }
  if (!isNumericKey(event) || keyCode === CODE_PERIOD) {
    event.preventDefault();
  }
}

function handleInput(event: any) {
  const inputValue = event.target?.value;
  const numericValue = inputValue !== "" ? Number(inputValue) : null;
  emit("update:modelValue", numericValue);
}
</script>
