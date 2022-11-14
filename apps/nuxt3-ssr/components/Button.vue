<!-- eslint-disable vue/multi-word-component-names -->
<script setup>
import BaseIcon from "./BaseIcon.vue";
import { computed } from "vue";

const props = defineProps({
  type: {
    type: String,
    default: "primary",
    enum: ["primary", "secondary", "tertiary", "outline"],
  },
  size: {
    type: String,
    default: "medium",
    enum: ["small", "medium", "large"],
  },
  label: {
    type: String,
    required: true,
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
    "bg-button-primary text-button-primary border-button-primary hover:bg-button-primary-hover hover:text-button-primary-hover hover:border-button-primary-hover",
  secondary:
    "bg-button-secondary text-button-secondary border-button-secondary hover:bg-button-secondary-hover hover:text-button-secondary-hover hover:border-button-secondary-hover",
  tertiary:
    "bg-button-tertiary text-button-tertiary border-button-tertiary hover:bg-button-tertiary-hover hover:text-button-tertiary-hover hover:border-button-tertiary-hover",
  outline:
    "bg-button-outline text-button-outline border-button-outline hover:bg-button-outline-hover hover:text-button-outline-hover hover:border-button-outline-hover",
  disabled:
    "bg-button-disabled text-button-disabled border-button-disabled hover:bg-button-disabled-hover hover:text-button-disabled-hover hover:border-button-disabled-hover",
};

const SIZE_MAPPING = {
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
  <button :class="`${colorClasses} ${sizeClasses} ${iconPositionClass}`"
    class="flex tracking-widest uppercase font-display rounded-full border items-center">
    <span v-if="icon">
      <BaseIcon :name="icon" />
    </span>
    <span>{{ label }}</span>
  </button>
</template>
