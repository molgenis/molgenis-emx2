<template>
  <div
    class="flex items-center border rounded-input px-2 h-input"
    data-elem="container"
    :class="{
      'cursor-pointer duration-default ease-in-out hover:border-input-hover focus-within:border-input-focused':
        !disabled && !invalid,
      'border-invalid': invalid,
      'border-valid': valid,
      'bg-disabled cursor-not-allowed': disabled,
      'bg-input border-input': !disabled,
    }"
    @click="onInputClick"
  >
    <div class="grow" data-elem="current-file-container">
      <button
        v-if="fileName"
        :id="`${id}-current-file`"
        data-elem="current-value-btn"
        ref="selectedFileButton"
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
        :disabled="disabled"
      >
        <span>{{ fileName }}</span>
        <BaseIcon name="Trash" class="w-4" />
      </button>
    </div>
    <div class="flex-none">
      <button
        :id="`${id}-file-open-btn`"
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
        :disabled="disabled"
      >
        <span>Browse</span>
      </button>
      <input
        :id="`${id}-file-input`"
        ref="fileInput"
        class="sr-only"
        type="file"
        :disabled="disabled"
        @input="onFileInput"
        @focus="$emit('focus')"
        @blur="$emit('blur')"
      />
    </div>
  </div>
  <div v-if="downLoadUrl" class="ml-4">
    <a
      :href="downLoadUrl"
      class="text-link underline"
      target="_blank"
      rel="noopener noreferrer"
      >{{ fileName }}</a
    >
  </div>
</template>

<script lang="ts" setup>
import { computed, useTemplateRef } from "vue";
import type { IInputProps, IFile } from "../../../types/types";
import BaseIcon from "../BaseIcon.vue";

const modelValue = defineModel<IFile | null | File>();
const fileInputElem = useTemplateRef<HTMLInputElement>("fileInput");
const selectedFileButton =
  useTemplateRef<HTMLButtonElement>("selectedFileButton");

defineProps<IInputProps>();

const emit = defineEmits(["focus", "blur", "error", "update:modelValue"]);

function resetModelValue() {
  modelValue.value = null;
}

function showFileInput() {
  fileInputElem.value?.click();
}

function onInputClick(event: Event) {
  const target = event.target as Node;
  const isRefElemNode =
    selectedFileButton.value?.contains(target) ||
    selectedFileButton.value === target;

  if (isRefElemNode) {
    resetModelValue();
  } else {
    showFileInput();
  }
}

function onFileInput(event: Event) {
  const files = (event.target as HTMLInputElement)?.files as FileList;

  if (files.length) {
    const file = files?.item(0) as File;
    modelValue.value = file;
  }
}

const fileName = computed(() => {
  const value = modelValue.value;
  if (value instanceof File) {
    return value.name;
  }
  return value?.filename || "";
});

const downLoadUrl = computed(() => {
  const value = modelValue.value;
  if (
    value !== null &&
    value !== undefined &&
    !(value instanceof File) &&
    "url" in value
  ) {
    // the file is stored remotely
    return value.url;
  } else if (value instanceof File && window && window.URL) {
    // the file is moved to the browser from local system
    return URL.createObjectURL(value);
  } else {
    // no file selected or file is cleared
    return null;
  }
});
</script>
