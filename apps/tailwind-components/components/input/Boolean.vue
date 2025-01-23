<template>
  <InputRadioGroup
    :id="id"
    :modelValue="props.modelValue"
    :radioOptions="radioOptions"
    :showClearButton="true"
    align="horizontal"
    @update:modelValue="onInput"
    @focus="$emit('focus')"
    @blur="$emit('blur')"
  />
</template>

<script lang="ts" setup>
import type { RadioOptionsDataIF } from "~/types/types";

const props = withDefaults(
  defineProps<{
    id: string;
    label?: string;
    modelValue: boolean | null;
    placeholder?: string;
    disabled?: boolean;
    required?: boolean;
    valid?: boolean;
    hasError?: boolean;
    trueLabel?: string;
    falseLabel?: string;
  }>(),
  {
    disabled: false,
    required: false,
    hasError: false,
    valid: false,
    trueLabel: "True",
    falseLabel: "False",
  }
);

const emit = defineEmits([
  "focus",
  "blur",
  "error",
  "update:modelValue",
  "input",
]);
defineExpose({ validate });

function validate(value: boolean | null) {
  if (props.required && value === null) {
    emit("error", [
      { message: `${props.label || props.id} required to complete the form` },
    ]);
  } else {
    emit("error", []);
  }
}

const radioOptions = ref<RadioOptionsDataIF[]>([
  { value: true, label: props.trueLabel },
  { value: false, label: props.falseLabel },
]);

const modelValue = ref<boolean | undefined>(undefined);

function onInput(value: string | boolean) {
  const booleanValue =
    value === "true" ? true : value === "false" ? false : null;
  emit("update:modelValue", booleanValue);
  validate(booleanValue);
}
</script>
