<script setup lang="ts">
import type { IInputProps } from "~/types/types";
const modelValue = defineModel<string | number>();
defineProps<
  IInputProps & {
    type?: string;
  }
>();

const emit = defineEmits(["update:modelValue", "focus", "blur"]);

let timeoutID: number | NodeJS.Timeout | undefined = undefined;
function handleInput(input: string) {
  clearTimeout(timeoutID);
  timeoutID = setTimeout(() => {
    emit("update:modelValue", input);
  }, 500);
}
</script>
<template>
  <div
    class="relative flex items-center w-full h-[56px] border outline-none rounded-input"
    :class="{
      'bg-input border-valid text-valid': valid && !disabled,
      'bg-input border-invalid text-invalid': invalid && !disabled,
      'border-disabled text-disabled bg-disabled cursor-not-allowed': disabled,
      'bg-disabled border-valid text-valid cursor-not-allowed':
        valid && disabled,
      'bg-disabled border-invalid text-invalid cursor-not-allowed':
        invalid && disabled,
      'bg-input text-input hover:border-input-hover focus-within:border-input-focused':
        !disabled && !invalid && !valid,
    }"
  >
    <div class="w-[44px] ps-3 text-center pointer-events-none">
      <BaseIcon
        name="search"
        :width="21"
        class="text-input"
        :class="{
          'text-valid': valid,
          'text-invalid': invalid,
          'text-disabled': disabled,
        }"
      />
    </div>
    <input
      :id="id"
      type="search"
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
      @input="(event) => handleInput((event.target as HTMLInputElement).value)"
      class="w-full h-[100%] pr-4 pl-2 outline-none text-current bg-transparent"
      :class="{
        'cursor-not-allowed': disabled,
      }"
    />
  </div>
</template>
