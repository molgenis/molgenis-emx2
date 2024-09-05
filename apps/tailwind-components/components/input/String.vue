<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    id: string;
    label?: string;
    value?: string;
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

const emit = defineEmits(["focus", "error", "update:modelValue"]);
defineExpose({ validate });

function validate(value: string) {
  if (props.required && value === "") {
    emit("error", [
      { message: `${props.label || props.id} required to complete the form` },
    ]);
  } else {
    emit("error", []);
  }
}

function onInput(event: Event) {
  const inputElement = event.target as HTMLInputElement;
  emit("update:modelValue", inputElement.value);
  validate(inputElement.value);
}
</script>

<template>
  <input
    :id="id"
    :required="required"
    :placeholder="placeholder"
    class="w-full pr-4 font-sans text-black text-gray-300 outline-none rounded-search-input h-10 ring-red-500 pl-3 shadow-search-input focus:shadow-search-input hover:shadow-search-input search-input-mobile border"
    :class="{
      'border-invalid text-invalid': hasError,
      'border-valid text-valid': valid,
      'border-disabled text-disabled bg-disabled': disabled,
      'bg-white': !disabled,
    }"
    :value="value"
    @input="onInput"
    @focus="$emit('focus')"
    @blur="validate(value || '')"
  />
</template>
