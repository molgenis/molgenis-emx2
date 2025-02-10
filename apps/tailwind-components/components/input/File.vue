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
        v-if="modelValue?.file"
        type="filterWell"
        size="small"
        icon="Trash"
        iconPosition="right"
        class="[&_svg]:w-4"
        @click="onFilterWellClick"
      >
        {{ modelValue.file.name }}
      </Button>
    </div>
    <div class="flex-none">
      <button
        :for="`${id}-file-input`"
        class="flex justify-center items-center h-10 px-5 text-heading-xl tracking-widest uppercase font-display duration-default ease-in-out border rounded-input bg-button-filter text-button-filter border-button-filter"
        :class="{
          'border-invalid text-invalid bg-invalid hover:bg-invalid hover:text-invalid':
            invalid,
          'text-disabled bg-disabled hover:text-disabled cursor-not-allowed':
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
        :required="required"
        :disabled="disabled"
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

const modelValue = defineModel<columnValueObject | null>({ file: null });
const fileInputElem = useTemplateRef<HTMLInputElement>("fileInputElem");

defineProps<IInputProps>();

const emit = defineEmits(["focus", "blur", "error", "update:modelValue"]);

function showFileInput() {
  fileInputElem.value.click();
}

function onChange(event: Event) {
  const files = (event.target as HTMLInputElement)?.files;
  if (files) {
    modelValue.value = { file: files.item(0) as File };
  }
}

function onFilterWellClick() {
  modelValue.value = { file: null };
}
</script>
