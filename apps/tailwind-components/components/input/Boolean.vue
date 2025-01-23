<template>
  <InputRadioGroup
    :id="id"
    :modelValue="props.modelValue"
    @update:modelValue="onInput"
    @focus="$emit('focus')"
    @blur="$emit('blur')"
    :radioOptions="radioOptions"
    align="horizontal"
    :showClearButton="true"
  />

  {{ modelValue }}
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
  }>(),
  {
    disabled: false,
    required: false,
    hasError: false,
    valid: false,
  }
);

const emit = defineEmits(["focus", "blur", "error", "update:modelValue"]);
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
  { value: true, label: "True" },
  { value: false, label: "False" },
]);

const modelValue = ref<boolean | undefined>(undefined);

function onInput(value: string | boolean) {
  const booleanValue =
    value === "true" ? true : value === "false" ? false : null;
  emit("update:modelValue", booleanValue);
  validate(booleanValue);
}
</script>
