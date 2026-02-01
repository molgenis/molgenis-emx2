<script setup lang="ts">
import { computed } from "vue";
import type { ButtonSize } from "../../types/types";
import BaseIcon from "./BaseIcon.vue";

const props = withDefaults(
  defineProps<{
    options: { name: string; label: string; icon: string }[];
    size?: ButtonSize;
  }>(),
  {
    size: "medium",
  }
);

const modelValue = defineModel<string>({ required: true });

const SIZE_MAPPING: Record<ButtonSize, string> = {
  tiny: "h-8 px-3 text-heading-sm gap-1.5",
  small: "h-10.5 px-4 text-heading-lg gap-2",
  medium: "h-14 px-6 text-heading-xl gap-3",
  large: "h-18 px-8 text-heading-xl gap-4",
};

const ICON_SIZE_MAPPING: Record<ButtonSize, string> = {
  tiny: "w-3 h-3",
  small: "w-4 h-4",
  medium: "w-5 h-5",
  large: "w-6 h-6",
};

const sizeClasses = computed(() => SIZE_MAPPING[props.size]);
const iconClasses = computed(() => ICON_SIZE_MAPPING[props.size]);

function isFirst(idx: number): boolean {
  return idx === 0;
}

function isLast(idx: number): boolean {
  return idx === props.options.length - 1;
}
</script>

<template>
  <div class="flex self-center w-fit">
    <button
      v-for="(option, idx) in options"
      :key="option.name"
      :class="[
        sizeClasses,
        modelValue === option.name
          ? 'bg-toggle-active text-toggle-active border-toggle-active'
          : 'bg-toggle-inactive text-toggle-inactive border-toggle-inactive hover:text-toggle-hover hover:border-toggle-hover',
        isFirst(idx) ? 'rounded-l-full' : '-ml-px',
        isLast(idx) ? 'rounded-r-full' : ''
      ]"
      class="flex items-center tracking-widest uppercase font-display border transition-all duration-default"
      @click="modelValue = option.name"
    >
      <BaseIcon :name="option.icon" :class="iconClasses" />
      {{ option.label }}
    </button>
  </div>
</template>
