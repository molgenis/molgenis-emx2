<template>
  <div
    class="flex items-center border-2 border-input p-2"
    :class="{
      'cursor-pointer duration-default ease-in-out hover:border-input-hover focus-within:border-input-focused':
        !disabled && !invalid,
      'border-invalid': invalid,
      'border-valid': valid,
      'bg-disabled cursor-not-allowed': disabled,
    }"
  >
    <div class="grow">
      <button
        v-if="modelValue"
        class="flex justify-center items-center h-10.5 px-5 text-heading-lg gap-3 tracking-widest uppercase font-display duration-default ease-in-out border rounded-input"
        :class="{
          'text-disabled bg-disabled hover:text-disabled cursor-not-allowed':
            disabled,
          'bg-button-filter text-button-filter border-button-filter hover:bg-button-filter-hover hover:border-button-filter-hover':
            !disabled,
          'border-invalid text-invalid bg-invalid hover:bg-invalid hover:text-invalid':
            invalid,
          'border-valid text-valid bg-valid hover:bg-valid hover:text-valid':
            valid,
        }"
        @click="onFilterWellClick"
        :disabled="disabled"
      >
        <span>{{ modelValue.filename }}</span>
        <BaseIcon name="Trash" class="w-4" />
      </button>
    </div>
    <div class="flex-none">
      <button
        class="flex justify-center items-center h-10 px-5 text-heading-xl tracking-widest uppercase font-display duration-default ease-in-out border rounded-input bg-button-filter text-button-filter border-button-filter"
        :class="{
          'border-invalid text-invalid bg-invalid hover:bg-invalid hover:text-invalid':
            invalid,
          'border-valid text-valid bg-valid hover:bg-valid hover:text-valid':
            valid,
          'text-disabled bg-disabled border-disabled hover:text-disabled cursor-not-allowed':
            disabled,
          'hover:bg-button-primary hover:text-button-primary cursor-pointer':
            !disabled,
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
        :disabled="disabled"
        @change="onChange"
        @focus="$emit('focus')"
        @blur="$emit('blur')"
      />
    </div>
  </div>
</template>

<script lang="ts" setup>
import type { IInputProps, IFile } from "~/types/types";

const modelValue = defineModel<IFile | null>();
const fileInputElem = useTemplateRef<HTMLInputElement>("fileInputElem");

defineProps<IInputProps>();

const emit = defineEmits(["focus", "blur", "error", "update:modelValue"]);

function showFileInput() {
  fileInputElem.value?.click();
}

function onChange(event: Event) {
  const files = (event.target as HTMLInputElement)?.files;
  if (files) {
    const file = files.item(0) as File;
    modelValue.value = {
      filename: file.name,
      size: file.size,
      extension: file.type,
    };
  }
}

function onFilterWellClick() {
  modelValue.value = null;
}
</script>
