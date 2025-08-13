<script setup lang="ts">
import { useTemplateRef } from "vue";
import type { IInputProps, ButtonSize } from "../../types/types";

const modelValue = defineModel<string | number>();
const search = useTemplateRef<HTMLInputElement>("search");

withDefaults(
  defineProps<
    IInputProps & {
      type?: string;
      size?: ButtonSize;
    }
  >(),
  {
    size: "medium",
  }
);

defineExpose({ search });

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
    class="relative flex items-center border outline-none rounded-input"
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
      'h-input-tiny pl-5 pr-5 text-heading-sm gap-2': size === 'tiny',
      'h-input-small pl-5 pr-5 text-heading-sm gap-3': size === 'small',
      'h-input pl-5 pr-7.5 text-heading-md gap-4': size === 'medium',
      'h-input-large pl-5 pr-8.75 text-heading-lg gap-5': size === 'large',
    }"
  >
    <div class="w-auto text-center pointer-events-none">
      <BaseIcon
        name="search"
        :class="{
          'text-valid': valid,
          'text-invalid': invalid,
          'text-disabled': disabled,
          'text-input': !disabled && !valid && !invalid,
          'w-[12px]': size === 'tiny',
          'w-[16px]': size === 'small',
          'w-[21px]': size === 'medium',
          'w-[26px]': size === 'large',
        }"
      />
    </div>
    <input
      :id="id"
      ref="search"
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
