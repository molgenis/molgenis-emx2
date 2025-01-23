<template>
  <InputString
    :id="id"
    :label="label"
    :placeholder="placeholder"
    :valid="valid"
    :hasError="hasError || !valid"
    :required="required"
    :disabled="disabled"
    :model-value="modelValue"
    @update:model-value="validateInput"
  />
</template>

<script setup lang="ts">
import InputString from "./String.vue";
import { constants } from "molgenis-components";

const { HYPERLINK_REGEX } = constants;

withDefaults(
  defineProps<{
    id: string;
    label?: string;
    placeholder?: string;
    modelValue: string;
    disabled?: boolean;
    required?: boolean;
    hasError?: boolean;
  }>(),
  {
    disabled: false,
    required: false,
    hasError: false,
  }
);

const valid = ref(false);

function validateInput(value: string) {
  if (HYPERLINK_REGEX.test(value)) {
    valid.value = true;
  } else {
    valid.value = false;
  }
}

defineExpose({
  valid,
});
</script>
