<template>
  <InputString
    :id="id"
    :aria-describedby="describedBy"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :placeholder="placeholder"
    :required="required"
    :modelValue="modelValue"
    @focus="$emit('focus')"
    @blur="$emit('blur')"
    @update:modelValue="handleInputChanged"
    @keypress="handleKeyValidity"
  />
</template>

<script setup lang="ts">
import type { IInputProps } from "../../../types/types";
import constants from "../../../../molgenis-components/src/components/constants";
import {
  flipMinusSign,
  isNumericKey,
} from "../../../../molgenis-components/src/components/utils";
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

function handleInputChanged(value?: string | number | null) {
  if (value !== 0 && !value) {
    emit("update:modelValue", null);
  } else {
    const noCommaValue = value?.toString().replace(",", ".");
    const numericValue =
      typeof noCommaValue === "string"
        ? Number.parseInt(noCommaValue)
        : noCommaValue;
    emit("update:modelValue", numericValue);
  }
}

function handleKeyValidity(event: any) {
  const keyCode = event.which ?? event.keyCode;
  if (keyCode === CODE_MINUS) {
    const flipped = flipMinusSign(event.target?.value);
    if (flipped && flipped !== "-") {
      emit("update:modelValue", Number.parseInt(flipped));
    } else {
      emit("update:modelValue", flipped);
    }
  }
  if (keyCode === CODE_PERIOD && event.target?.value.indexOf(".") > -1) {
    event.preventDefault();
  }
  if (!isNumericKey(event)) {
    event.preventDefault();
  }
}
</script>
