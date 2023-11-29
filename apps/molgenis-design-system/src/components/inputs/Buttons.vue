<script setup lang="ts">
enum Sizes {
  xs = "xs",
  sm = "sm",
  base = "base",
  lg = "lg",
  xl = "xl",
  "2xl" = "2xl",
  "3xl" = "3xl",
}

const buttonSizes = Object.values(Sizes);

interface ButtonProps {
  label: string;
  type: "button" | "reset" | "submit";
  context?: "primary" | "secondary" | "outline" | "tertiary";
  size?: Sizes;
  isDisabled?: boolean;
}

withDefaults(defineProps<ButtonProps>(), {
  type: "button",
  context: "secondary",
  size: Sizes.base,
  isDisabled: false,
});
</script>

<template>
  <button
    :type="type"
    :data-type="type"
    :data-context="context"
    :disabled="isDisabled"
    :class="`
      block
      w-full
      border
      rounded-full
      border-transparent
      text-center
      hover:brightness-110

    data-[context='primary']:bg-button-primary
    data-[context='primary']:text-button-primary
    data-[context='secondary']:bg-button-secondary
    data-[context='secondary']:text-button-secondary
    data-[context='outline']:bg-button-outline
    data-[context='outline']:text-button-outline
    data-[context='outline']:border-button-outline
    data-[context='tertiary']:bg-button-tertiary
    data-[context='tertiary']:text-button-tertiary
      
    [&:disabled]:bg-button-disabled
    [&:disabled]:text-button-disabled
    [&:disabled]:hover:filter-none
    `"
  >
    <div
      :class="`
      block w-full px-3
      py-${buttonSizes.findIndex((value) => value === size)}
      [&_svg]:mr-2
    `"
    >
      <slot name="icon"></slot>
      <span
        :class="`
          uppercase
          font-semibold
          tracking-widest
          text-heading-${size}
        `"
      >
        {{ label }}
      </span>
    </div>
  </button>
</template>
