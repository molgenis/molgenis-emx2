<template>
  <div
    class="flex items-center border border-input p-2"
    :class="{
      'cursor-pointer duration-default ease-in-out hover:border-input-hover hover:shadow-input-hover focus:border-input-hover focus:shadow-input-hover focus-within:border-input-hover focus-within:shadow-input-hover':
        !disabled && !invalid,
      'border-invalid': invalid,
    }"
  >
    <div class="grow">
      <Button
        v-if="modelValue"
        type="filterWell"
        size="small"
        icon="Trash"
        iconPosition="right"
        class="[&_svg]:w-4"
        @click="onFilterWellClick"
      >
        {{ modelValue.name }}
      </Button>
    </div>
    <div class="flex-none">
      <button
        :for="`${id}-file-input`"
        class="flex justify-center items-center h-10 px-5 cursor-pointer text-heading-xl tracking-widest uppercase font-display duration-default ease-in-out border rounded-input bg-button-filter text-button-filter border-button-filter"
        :class="{
          'border-invalid text-invalid bg-invalid': invalid,
          'text-disabled bg-disabled hover:text-disabled': disabled,
          'hover:bg-button-primary hover:text-button-primary': !disabled,
        }"
        @click="showFileInput"
      >
        <span>Browse</span>
      </button>
      <input
        :id="`${id}-file-input`"
        ref="fileInputElem"
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
import type { IInputProps } from "~/types/types";
import type { columnValue } from "../../../metadata-utils/src/types";

interface IFile {
  name: string;
  file: File;
}

interface IFileInput extends IInputProps {
  label?: string;
}

const modelValue = defineModel<IFileInput | null>();
const fileInputElem = useTemplateRef<HTMLInputElement>("fileInputElem");

const props = withDefaults(defineProps<IFileInput>(), {
  label: "Browse",
});

const emit = defineEmits(["focus", "blur", "error", "update:modelValue"]);

function showFileInput() {
  fileInputElem.value.click();
}

function onChange(event: Event) {
  const files = (event.target as HTMLInputElement)?.files;
  if (files) {
    const selectedFile = files.item(0) as File;
    modelValue.value = { name: selectedFile.name, file: selectedFile };
  }

  emit("update:modelValue", modelValue.value);
}

function onFilterWellClick() {
  modelValue.value = null;
  emit("update:modelValue", modelValue.value);
}
</script>
