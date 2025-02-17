<template>
  <div>
    <InputString
      :id="id"
      :aria-describedby="describedBy"
      :disabled="disabled"
      :placeholder="placeholder"
      :required="required"
      v-model="modelValue"
      @focus="$emit('focus')"
      @blur="$emit('blur')"
      @input="handleInputChanged"
      @keypress="handleKeyValidity"
    />
  </div>
</template>

<script setup lang="ts">
import type { IInputProps } from "~/types/types";
import constants from "../../../molgenis-components/src/components/constants";
import {
  flipSign,
  isNumericKey,
} from "../../../molgenis-components/src/components/utils";

const modelValue = defineModel<string | number>();
defineProps<
  IInputProps & {
    required?: boolean;
  }
>();

const { CODE_MINUS, CODE_PERIOD } = constants;

const emit = defineEmits(["focus", "blur", "update:modelValue"]);

function handleInputChanged(event: any) {
  const value = event.target?.value;
  if (!value) {
    emit("update:modelValue", null);
  } else {
    emitIfValid(value);
  }
}

function emitIfValid(strValue: string) {
  const noCommaValue = strValue.replace(",", "");
  const value = parseFloat(noCommaValue);
  if (!isNaN(value)) {
    emit("update:modelValue", value);
  } else {
    emit("update:modelValue", strValue);
  }
}

function handleKeyValidity(event: any) {
  const keyCode = event.which ?? event.keyCode;
  if (keyCode === CODE_MINUS) {
    const flipped = flipSign(event.target?.value);
    emitIfValid(flipped);
  }
  if (keyCode === CODE_PERIOD && event.target?.value.indexOf(".") > -1) {
    event.preventDefault();
  }
  if (!isNumericKey(event)) {
    event.preventDefault();
  }
}
</script>
