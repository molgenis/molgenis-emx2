<template>
  <label
    :for="`${id}-file-input`"
    class="inline-flex justify-center items-center h-14 px-7.5 text-heading-xl tracking-widest uppercase font-display bg-button-secondary text-button-secondary border-button-secondary hover:bg-button-secondary-hover hover:text-button-secondary-hover hover:border-button-secondary-hover"
    :class="{
      'border-invalid text-invalid': hasError,
      'border-valid text-valid': valid,
      'border-disabled text-disabled bg-disabled': disabled,
      'bg-white': !disabled,
    }"
  >
    <input
      :id="`${id}-file-input`"
      class="sr-only"
      type="file"
      :name="id"
      :required="required"
      :disabled="disabled"
      :multiple="multiple"
      :value="modelValue"
      @change="onChange"
      @focus="$emit('focus')"
      @blur="$emit('blur')"
    />
    <span>{{ placeholder }}</span>
  </label>
  <div v-if="filesToImport" class="mt-2">
    <Button
      v-for="file in filesToImport"
      type="filterWell"
      size="tiny"
      icon="Trash"
      iconPosition="right"
      class="mt-2 [&_svg]:w-5"
      :data-filename="file.name"
      @click="onFilterWellClick"
    >
      {{ file.name }}
    </Button>
  </div>
</template>

<script lang="ts" setup>
import type { columnValue } from "../../../metadata-utils/src/types";

const props = withDefaults(
  defineProps<{
    id: string;
    label?: string;
    modelValue?: string;
    placeholder?: string;
    disabled?: boolean;
    required?: boolean;
    valid?: boolean;
    hasError?: boolean;
    multiple?: boolean;
  }>(),
  {
    placeholder: "Upload file",
    disabled: false,
    required: false,
    hasError: false,
    valid: false,
    multiple: true,
  }
);

interface IFile {
  name: string;
  lastModified: string;
  size: number;
  type: string;
}

const filesToImport = ref();
const emit = defineEmits(["focus", "blur", "error", "update:modelValue"]);
defineExpose({ validate });

function onChange(event: Event) {
  const files = (event.target as HTMLInputElement)?.files;
  if (files) {
    filesToImport.value = Array.from(files);
  }

  emit("update:modelValue", filesToImport.value);
}

function onFilterWellClick(event: Event) {
  if (filesToImport.value?.length === 1) {
    filesToImport.value = [];
  } else {
    const button = (event.target as HTMLElement).closest(
      "button"
    ) as HTMLButtonElement;
    const filename = button.dataset.filename;
    filesToImport.value = filesToImport.value.filter(
      (file: IFile) => file.name !== filename
    );
  }

  emit("update:modelValue", filesToImport.value);
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
