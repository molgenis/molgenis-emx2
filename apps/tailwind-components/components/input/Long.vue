<template>
  <div>
    <InputString
      :id="id"
      :aria-describedby="describedBy"
      :disabled="disabled"
      :placeholder="placeholder"
      :required="required"
      :invalid="invalid"
      v-model="modelValue"
      @focus="$emit('focus')"
      @blur="$emit('blur')"
      @input="handleInputChanged"
      @keypress="handleKeyValidity"
    />
  </div>
</template>

<script setup lang="ts">
import constants from "../../../molgenis-components/src/components/constants";
import {
  flipSign,
  isNumericKey,
} from "../../../molgenis-components/src/components/utils";
import type { IInputProps } from "../../types/types";

const modelValue = defineModel<string>("modelValue", { required: true });

defineProps<
  IInputProps & {
    required?: boolean;
  }
>();

const { CODE_MINUS, CODE_PERIOD, CODE_COMMA } = constants;

const emit = defineEmits(["focus", "blur", "update:modelValue"]);

function handleInputChanged(event: any) {
  const value = event.target?.value;
  if (value?.length) {
    emitIfValid(value);
  } else {
    emit("update:modelValue", null);
  }
}

function emitIfValid(strValue: string) {
  if (strValue?.length) {
    emit("update:modelValue", strValue);
  } else {
    emit("update:modelValue", null);
  }
}

function handleKeyValidity(event: any) {
  const keyCode = event.which ?? event.keyCode;
  if (keyCode === CODE_MINUS) {
    const flipped = flipSign(event.target?.value);
    emitIfValid(flipped);
  }
  if (
    !isNumericKey(event) ||
    keyCode === CODE_PERIOD ||
    keyCode === CODE_COMMA
  ) {
    event.preventDefault();
  }
}
</script>
