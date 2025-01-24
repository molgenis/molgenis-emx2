<template>
  <InputString
    :id="id"
    :label="label"
    :placeholder="placeholder"
    :valid="valid"
    :hasError="hasError"
    :required="required"
    :disabled="disabled"
    :value="modelValue"
    @update:modelValue="validateInput"
  />
</template>

<script setup lang="ts">
const HYPERLINK_REGEX =
  /^((https?):\/\/)(www.)?[-a-zA-Z0-9@:%._\\+~#?&//=]{2,256}\.[a-z]{2,6}\b([-a-zA-Z0-9@:%._\\+~#?&//=()]*)\/?$|^$/;

const props = withDefaults(
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

const { modelValue } = toRefs(props);
const emit = defineEmits(["update:modelValue", "error"]);

function validateInput(value: string) {
  emit("update:modelValue", value);
  validate();
}

function validate() {
  if (HYPERLINK_REGEX.test(modelValue.value)) {
    emit("error", []);
  } else {
    emit("error", [{ message: "Invalid hyperlink" }]);
  }
}

defineExpose({
  validate,
});
</script>
