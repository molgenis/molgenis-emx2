<template>
  <div class="space-y-2">
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
      <div class="grow min-w-0">
        <span
          v-if="selectedFiles.length"
          class="text-sm text-input truncate block"
          :title="selectedFileNames"
        >
          {{ selectedFiles.length }} file(s) selected
        </span>
        <span v-else class="text-sm text-definition-list-term">
          {{ placeholder }}
        </span>
      </div>
      <div class="flex-none">
        <button
          :id="`${id}-files-open-btn`"
          type="button"
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
          :id="`${id}-files-input`"
          ref="fileInput"
          class="sr-only"
          type="file"
          multiple
          :disabled="disabled"
          @change="onFileInput"
          @focus="$emit('focus')"
          @blur="$emit('blur')"
        />
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, useTemplateRef } from "vue";
import type { IInputProps } from "../../../types/types";

const modelValue = defineModel<File[] | null>();

withDefaults(
  defineProps<
    IInputProps & {
      placeholder?: string;
    }
  >(),
  {
    placeholder: "Select one or more files",
  }
);

const emit = defineEmits(["focus", "blur", "error", "update:modelValue"]);

const fileInput = useTemplateRef<HTMLInputElement>("fileInput");

const selectedFiles = computed(() => modelValue.value ?? []);
const selectedFileNames = computed(() =>
  selectedFiles.value.map((file) => file.name).join(", ")
);

function onInputClick() {
  if (!fileInput.value) return;
  fileInput.value.click();
}

function onFileInput(event: Event) {
  const files = (event.target as HTMLInputElement)?.files;
  modelValue.value = files ? Array.from(files) : [];
}
</script>
