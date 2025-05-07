<script setup lang="ts">
import { computed, watchEffect } from "vue";
import type {
  ButtonIconPosition,
  ButtonSize,
  ButtonType,
} from "../types/types.ts";

const props = withDefaults(
  defineProps<{
    type?: ButtonType;
    size?: ButtonSize;
    label?: string;
    icon?: string;
    iconPosition?: ButtonIconPosition;
    disabled?: boolean;
    iconOnly?: boolean;
    tooltip?: string;
  }>(),
  {
    type: "primary",
    size: "medium",
    label: "",
    iconPosition: "left",
    disabled: false,
    iconOnly: false,
  }
);

watchEffect(() => {
  if (props.iconOnly && (props.label === "" || props.label === undefined)) {
    console.error("Icon only buttons must have a label");
  }
});

const COLOR_MAPPING = {
  primary:
    "tracking-widest uppercase font-display bg-button-primary text-button-primary border-button-primary hover:bg-button-primary-hover hover:text-button-primary-hover hover:border-button-primary-hover",
  secondary:
    "tracking-widest uppercase font-display bg-button-secondary text-button-secondary border-button-secondary hover:bg-button-secondary-hover hover:text-button-secondary-hover hover:border-button-secondary-hover",
  tertiary:
    "tracking-widest uppercase font-display bg-button-tertiary text-button-tertiary border-button-tertiary hover:bg-button-tertiary-hover hover:text-button-tertiary-hover hover:border-button-tertiary-hover",
  text:
    "group flex items-center text-button-text cursor-pointer disabled:cursor-not-allowed disabled:text-disabled border-none h-auto !p-[inherit]",
  outline:
    "tracking-widest uppercase font-display bg-button-outline text-button-outline border-button-outline hover:bg-button-outline-hover hover:text-button-outline-hover hover:border-button-outline-hover",
  disabled:
    "tracking-widest uppercase font-display bg-button-disabled text-button-disabled border-button-disabled hover:bg-button-disabled-hover hover:text-button-disabled-hover hover:border-button-disabled-hover",
  filterWell:
    "whitespace-nowrap bg-button-filter text-button-filter border-button-filter hover:bg-button-filter-hover hover:border-button-filter-hover",
  inline:
    "tracking-widest bg-none text-button-inline border-none hover:text-button-secondary",
};

const TEXT_MAPPING =  "text-title-contrast group-enabled:hover:underline";

const SIZE_MAPPING = {
  tiny: "h-8 px-5 text-heading-sm gap-3",
  small: "h-10.5 px-5 text-heading-lg gap-3",
  medium: "h-14 px-7.5 text-heading-xl gap-4",
  large: "h-18 px-8.75 text-heading-xl gap-5",
};

const ICON_ONLY_SIZE_MAPPING = {
  tiny: "p-[8px] h-8 w-8",
  small: "p-[5px] h-10 w-10",
  medium: "p-[8px] h-14 w-14",
  large: "p-[8px] h-18 w-18",
};

const ICON_SIZE_MAPPING = {
  tiny: 12,
  small: 18,
  medium: 24,
  large: 36,
};

const ICON_POSITION_MAPPING = {
  left: "",
  right: "flex-row-reverse",
};

const colorClasses = computed(() => {
  return props.disabled ? COLOR_MAPPING["disabled"] : COLOR_MAPPING[props.type];
});

const sizeClasses = computed(() => {
  return props.iconOnly
    ? ICON_ONLY_SIZE_MAPPING[props.size]
    : SIZE_MAPPING[props.size];
});

const iconPositionClass = computed(() => {
  return ICON_POSITION_MAPPING[props.iconPosition];
});

const iconSize = computed(() => {
  return ICON_SIZE_MAPPING[props.size];
});

const tooltipText = computed(() => {
  return props.tooltip || props.iconOnly ? props.label : "";
});
</script>

<template>
  <button
    v-tooltip.bottom="tooltipText"
    class="flex items-center justify-center border rounded-input group-[.button-bar]:rounded-none group-[.button-bar]:first:rounded-l-input group-[.button-bar]:last:rounded-r-input duration-default ease-in-out"
    :class="`${colorClasses} ${sizeClasses} ${iconPositionClass} transition-colors`"
  >
    <BaseIcon v-if="icon" :name="icon" :width="iconSize" />
    <span :class="`${(type==='text'?TEXT_MAPPING:'')} ${iconOnly?'sr-only':''}`">{{ label }}<slot /></span>
  </button>
</template>
