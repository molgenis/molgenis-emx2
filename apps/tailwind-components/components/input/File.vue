<template>
  <div class="flex justify-end items-center border border-input py-2 px-4">
    <div class="">
      <Button
        v-if="fileToImport"
        type="filterWell"
        size="small"
        icon="Trash"
        iconPosition="right"
        class="[&_svg]:w-4"
        @click="onFilterWellClick"
      >
        <span class="sr-only fixed">remove file</span>
        {{ fileToImport.name }}
      </Button>
    </div>
    <label
      :for="`${id}-file-input`"
      class="py-5 px-7.5 text-heading-xl tracking-widest uppercase font-display border rounded-input bg-button-outline text-button-outline border-button-outline hover:bg-button-primary hover:text-button-primary"
      :class="{
        'border-invalid text-invalid': hasError,
        'border-valid text-valid': valid,
        'border-disabled text-disabled bg-disabled': disabled,
        'bg-white': !disabled,
      }"
    >
      {{ label }}
    </label>
    <input
      :id="`${id}-file-input`"
      class="sr-only"
      type="file"
      :name="id"
      :required="required"
      :disabled="disabled"
      :value="modelValue"
      @change="onChange"
      @focus="$emit('focus')"
      @blur="$emit('blur')"
    />
  </div>
</template>

<script lang="ts" setup>
import type { columnValue } from "../../../metadata-utils/src/types";

const props = withDefaults(
  defineProps<{
    id: string;
    label?: string;
    modelValue?: string;
    disabled?: boolean;
    required?: boolean;
    valid?: boolean;
    hasError?: boolean;
  }>(),
  {
    label: "Browse",
    disabled: false,
    required: false,
    hasError: false,
  }
);

interface IFile {
  name: string;
  file: File;
}

const fileToImport = ref<IFile>();
const emit = defineEmits(["focus", "blur", "error", "update:modelValue"]);
defineExpose({ validate });

function onChange(event: Event) {
  const files = (event.target as HTMLInputElement)?.files;
  console.log(files.item(0))
  if (files) {
    fileToImport.value = { name: files.item(0).name, file: files.item(0) };
  }

  emit("update:modelValue", fileToImport.value);
}

function onFilterWellClick(event: Event) {
  fileToImport.value = null;
  emit("update:modelValue", fileToImport.value);
}

function validate(value: columnValue) {
  if (props.required && value === "") {
    const errors = [
      { message: `${props.label || props.id} required to complete the form` },
    ];
    emit("error", errors);
    return errors;
  } else {
    emit("error", []);
    return [];
  }
}
</script>
