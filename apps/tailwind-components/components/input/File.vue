<template>
  <label
    :for="`${id}-file-input`"
    class="inline-flex justify-center items-center gap-1 h-14 px-7.5 text-heading-xl tracking-widest uppercase font-display border rounded-input bg-button-outline text-button-outline border-button-outline hover:bg-button-outline-hover hover:text-button-outline-hover hover:border-button-outline-hover"
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
    <BaseIcon name="upload-file" class="ml-1" />
    <span>{{ label }}</span>
  </label>
  <div v-if="filesToImport" class="flex flex-row flex-wrap gap-2 mt-2">
    <Button
      v-for="file in filesToImport"
      type="filterWell"
      size="tiny"
      icon="Trash"
      iconPosition="right"
      class="[&_svg]:w-4"
      :data-filename="file.name"
      @click="onFilterWellClick"
    >
      <span class="sr-only fixed">remove file</span>
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
    disabled?: boolean;
    required?: boolean;
    valid?: boolean;
    hasError?: boolean;
    multiple?: boolean;
  }>(),
  {
    label: "Upload file",
    disabled: false,
    required: false,
    hasError: false,
    valid: false,
    multiple: true,
  }
);

interface IFile {
  name: string;
  file: File;
}

const filesToImport = ref<IFile[]>();
const emit = defineEmits(["focus", "blur", "error", "update:modelValue"]);
defineExpose({ validate });

function onChange(event: Event) {
  const files = (event.target as HTMLInputElement)?.files;
  if (files) {
    filesToImport.value = [...files].map(file=> {
      return {
        name: file.name,
        file: file
      }
    });
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
    filesToImport.value = filesToImport.value?.filter(
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
