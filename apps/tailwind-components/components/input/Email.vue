<template>
  <InputString
    type="email"
    v-bind="$props"
    v-model="modelValue"
    @update:modelValue="validateInput"
    @focus="$emit('focus')"
    @blur="$emit('blur')"
  />
</template>

<script setup lang="ts">
import type { InputString } from "#build/components";
import { type InputProps, InputPropsDefaults } from "~/types/types";

const EMAIL_REGEX =
  /^(([^<>()\\[\]\\.,;:\s@"]+(\.[^<>()\\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$|^$/;

const inputString = ref<InstanceType<typeof InputString>>();
const modelValue = defineModel<string>();
withDefaults(defineProps<InputProps>(), {
  ...InputPropsDefaults,
});

const emit = defineEmits(["update:modelValue", "error", "focus", "blur"]);

defineExpose({ validate });

function validateInput(value: string) {
  emit("update:modelValue", value);
  validate(value);
}

function validate(value: string) {
  const stringErrors = inputString.value?.validate(value) || [];
  if (EMAIL_REGEX.test(value)) {
    emit("error", stringErrors);
  } else {
    emit("error", [{ message: "Invalid email" }, ...stringErrors]);
  }
}
</script>
