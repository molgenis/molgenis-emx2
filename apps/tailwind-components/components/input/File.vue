<template>
  <div
    class="flex items-center border border-input p-2"
    :class="{
      'cursor-pointer duration-default ease-in-out hover:border-input-hover hover:shadow-input-hover focus:border-input-hover focus:shadow-input-hover focus-within:border-input-hover focus-within:shadow-input-hover':
        !disabled,
    }"
  >
    <div class="grow">
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
    <div class="flex-none">
      <label
        :for="`${id}-file-input`"
        class="flex justify-center items-center h-10 px-5 text-heading-xl tracking-widest uppercase font-display duration-default ease-in-out border rounded-input bg-button-filter text-button-filter border-button-filter"
        :class="{
          'border-invalid text-invalid': hasError,
          'text-disabled bg-disabled': disabled,
          'hover:bg-button-primary hover:text-button-primary': !disabled,
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

function onChange(event: Event) {
  const files = (event.target as HTMLInputElement)?.files;
  console.log(files.item(0));
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

defineExpose({ validate });
</script>
