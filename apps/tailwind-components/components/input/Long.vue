<template>
  <div>
    <InputString
      :id="id"
      :aria-describedby="describedBy"
      :disabled="disabled"
      :placeholder="placeholder"
      :required="required"
      :invalid="invalid || !!bigIntError"
      v-model="modelValue"
      @focus="$emit('focus')"
      @blur="$emit('blur')"
      @input="handleInputChanged"
      @keypress="handleKeyValidity"
    />
    <div class="text-invalid">{{ bigIntError }}</div>
  </div>
</template>

<script setup lang="ts">
import type { IInputProps } from "../../types/types";
import constants from "../../../molgenis-components/src/components/constants";
import {
  flipSign,
  getBigIntError,
  isNumericKey,
} from "../../../molgenis-components/src/components/utils";

const modelValue = defineModel<string>("modelValue", { required: true });

defineProps<
  IInputProps & {
    required?: boolean;
  }
>();

const { CODE_MINUS } = constants;

const emit = defineEmits(["focus", "blur", "update:modelValue"]);

const bigIntError = computed(() => {
  if (modelValue.value) {
    return getBigIntError(modelValue.value);
  }
});

function handleInputChanged(event: any) {
  const value = event.target?.value;
  if (value?.length) {
    emitIfValid(value);
  } else {
    emit("update:modelValue", null);
  }
}

function emitIfValid(strValue: string) {
  const noCommaValue = strValue.replace(",", "");
  const noPeriodValue = noCommaValue.replace(".", "");
  if (noPeriodValue?.length) {
    emit("update:modelValue", noPeriodValue);
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
  if (!isNumericKey(event)) {
    event.preventDefault();
  }
}
</script>
