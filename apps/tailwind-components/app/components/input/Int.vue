<template>
  <InputString
    :id="id"
    :ariaDescribedby="describedBy"
    :valid="valid"
    :invalid="invalid"
    :disabled="disabled"
    :placeholder="placeholder"
    :required="required"
    :modelValue="modelValue"
    @focus="$emit('focus')"
    @blur="$emit('blur')"
    @update:modelValue="handleInput"
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
    const flipped: string = flipSign(event.target?.value);
    if (flipped && flipped !== "-") {
      emit("update:modelValue", Number.parseInt(flipped));
    } else {
      emit("update:modelValue", flipped);
    }
  }
  if (!isNumericKey(event) || keyCode === CODE_PERIOD) {
    event.preventDefault();
  }
}

function handleInput(inputValue?: string | number | null) {
  if ((typeof inputValue !== "number" && !inputValue) || inputValue === "-") {
    emit("update:modelValue", inputValue);
  } else {
    const numericValue =
      typeof inputValue === "string" ? Number.parseInt(inputValue) : inputValue;
    emit("update:modelValue", numericValue);
  }
}
</script>
