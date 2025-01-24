<template>
  <InputString
    ref="inputString"
    :id="id"
    :label="label"
    :placeholder="placeholder"
    :valid="valid"
    :hasError="hasError"
    :required="required"
    :disabled="disabled"
    :value="modelValue"
    @update:modelValue="validateInput"
    @focus="$emit('focus')"
    @blur="$emit('blur')"
  />
</template>

<script setup lang="ts">
import type { InputString } from "#build/components";

const HYPERLINK_REGEX =
  /^((https?):\/\/)(www.)?[-a-zA-Z0-9@:%._\\+~#?&//=]{2,256}\.[a-z]{2,6}\b([-a-zA-Z0-9@:%._\\+~#?&//=()]*)\/?$|^$/;

const inputString = ref<InstanceType<typeof InputString>>();

withDefaults(
  defineProps<{
    id: string;
    modelValue: string;
    label?: string;
    placeholder?: string;
    disabled?: boolean;
    required?: boolean;
    hasError?: boolean;
    valid?: boolean;
  }>(),
  {
    disabled: false,
    required: false,
    hasError: false,
  }
);

const emit = defineEmits(["update:modelValue", "error", "focus", "blur"]);

defineExpose({ validate });

function validateInput(value: string) {
  emit("update:modelValue", value);
  validate(value);
}

function validate(value: string) {
  const stringErrors = inputString.value?.validate(value) || [];
  if (HYPERLINK_REGEX.test(value)) {
    emit("error", stringErrors);
  } else {
    emit("error", [{ message: "Invalid hyperlink" }, ...stringErrors]);
  }
}
</script>
