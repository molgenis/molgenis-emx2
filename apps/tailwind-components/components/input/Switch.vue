<script lang="ts" setup>
import { ref } from "vue";
import { BaseIcon } from "#components";
import type { IInputProps } from "~/types/types";
import type { IInputValue } from "../../../metadata-utils/src/types";

interface SwitchOption {
  value: IInputValue;
  icon: string;
  label?: string;
}

defineProps<
  IInputProps & {
    options: SwitchOption[];
  }
>();

const emits = defineEmits(["update:modelValue"]);

const modelValue = defineModel<IInputValue>();
</script>

<template>
  <div
    class="relative flex items-center border rounded w-fit group bg-button-switch"
  >
    <div v-for="option in options" class="w-[36px] z-10">
      <input
        :id="`${id}-switch-${option.value}`"
        class="peer sr-only"
        type="radio"
        :name="id"
        :value="option.value"
        v-model="modelValue"
        @input="emits('update:modelValue', modelValue)"
      />
      <label
        :for="`${id}-switch-${option.value}`"
        class="cursor-pointer block transition-all duration-700 ease-in-out bg-button-switch text-button-switch border border-transparent rounded hover:bg-button-switch-hover hover:text-button-switch-hover hover:border-button-switch-hover group-hover:peer-checked:border-transparent group-hover:peer-checked:hover:border-button-switch-hover peer-checked:bg-button-switch-selected peer-checked:text-button-switch-selected peer-checked:border-button-switch-selected peer-checked:hover:bg-button-switch-hover peer-checked:hover:text-button-switch-hover peer-checked:hover:border-button-switch-hover"
      >
        <span class="sr-only">{{ option.label || option.value }}</span>
        <BaseIcon :name="option.icon" :width="36" class="p-2 text-current" />
      </label>
    </div>
  </div>
</template>
