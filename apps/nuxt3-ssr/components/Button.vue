<!-- eslint-disable vue/multi-word-component-names -->
<script setup>
import BaseIcon from "./BaseIcon.vue";
import { computed } from "vue";

const props = defineProps({
  type: {
    type: String,
    default: "primary",
    enum: [
      "primary",
      "secondary",
      "tertiary",
      "outline",
      "disabled",
      "filterWell",
    ],
  },
  size: {
    type: String,
    default: "medium",
    enum: ["small", "medium", "large"],
  },
  label: {
    type: String,
    default: "",
  },
  icon: {
    type: String,
  },
  iconPosition: {
    type: String,
    default: "left",
    enum: ["left", "right"],
  },
  disabled: {
    type: Boolean,
  },
});

const COLOR_MAPPING = {
  primary:
    "tracking-widest uppercase font-display bg-button-primary text-button-primary border-button-primary hover:bg-button-primary-hover hover:text-button-primary-hover hover:border-button-primary-hover",
  secondary:
    "tracking-widest uppercase font-display bg-button-secondary text-button-secondary border-button-secondary hover:bg-button-secondary-hover hover:text-button-secondary-hover hover:border-button-secondary-hover",
  tertiary:
    "tracking-widest uppercase font-display bg-button-tertiary text-button-tertiary border-button-tertiary hover:bg-button-tertiary-hover hover:text-button-tertiary-hover hover:border-button-tertiary-hover",
  outline:
    "tracking-widest uppercase font-display bg-button-outline text-button-outline border-button-outline hover:bg-button-outline-hover hover:text-button-outline-hover hover:border-button-outline-hover",
  disabled:
    "tracking-widest uppercase font-display bg-button-disabled text-button-disabled border-button-disabled hover:bg-button-disabled-hover hover:text-button-disabled-hover hover:border-button-disabled-hover",
  filterWell:
    "whitespace-nowrap bg-blue-50 text-blue-500 border-blue-50 hover:bg-white hover:border-white",
};

const SIZE_MAPPING = {
  tiny: "h-8 px-5 text-heading-sm gap-3",
  small: "h-10.5 px-5 text-heading-lg gap-3",
  medium: "h-14 px-7.5 text-heading-xl gap-4",
  large: "h-18 px-8.75 text-heading-xl gap-5",
};

const ICON_POSITION_MAPPING = {
  left: "",
  right: "flex-row-reverse",
};

const colorClasses = computed(() => {
  return props.disabled ? COLOR_MAPPING["disabled"] : COLOR_MAPPING[props.type];
});

const sizeClasses = computed(() => {
  return SIZE_MAPPING[props.size];
});

const iconPositionClass = computed(() => {
  return ICON_POSITION_MAPPING[props.iconPosition];
});
</script>

<template>
  <button
    :class="`${colorClasses} ${sizeClasses} ${iconPositionClass} transition-colors`"
    class="flex items-center border rounded-full"
  >
    <span v-if="icon">
      <BaseIcon :name="icon" />
    </span>
    <span>{{ label }}<slot /></span>
  </button>
</template>
