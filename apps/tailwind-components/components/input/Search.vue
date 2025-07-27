<script setup lang="ts">
import {computed, ref, useTemplateRef} from "vue";
import type { IInputProps, ButtonSize } from "../../types/types";

const modelValue = defineModel<string | number>();
const search = useTemplateRef<HTMLInputElement>("search");

const props = withDefaults(
defineProps<
  IInputProps & {
    type?: string;
    size?: ButtonSize
  }
>(), {
      size: "medium"
    });

defineExpose({ search });

const emit = defineEmits(["update:modelValue", "focus", "blur"]);

let timeoutID: number | NodeJS.Timeout | undefined = undefined;
function handleInput(input: string) {
  clearTimeout(timeoutID);
  timeoutID = setTimeout(() => {
    emit("update:modelValue", input);
  }, 500);
}

const HEIGHT_MAPPING = {
  tiny: "h-8 px-5 text-heading-sm gap-3",
  small: "h-10.5 px-5 text-heading-lg gap-3",
  medium: "h-14 px-7.5 text-heading-xl gap-4",
  large: "h-18 px-8.75 text-heading-xl gap-5",
}

const heightClasses = computed(() => {
  return HEIGHT_MAPPING[props.size];
});

</script>
<template>
  <div
    class="relative flex items-center border outline-none rounded-input"
    :class="[heightClasses, {
      'bg-input border-valid text-valid': valid && !disabled,
      'bg-input border-invalid text-invalid': invalid && !disabled,
      'border-disabled text-disabled bg-disabled cursor-not-allowed': disabled,
      'bg-disabled border-valid text-valid cursor-not-allowed':
        valid && disabled,
      'bg-disabled border-invalid text-invalid cursor-not-allowed':
        invalid && disabled,
      'bg-input text-input hover:border-input-hover focus-within:border-input-focused':
        !disabled && !invalid && !valid,
    }]"
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
