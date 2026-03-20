<script setup lang="ts">
import { useId } from "vue";
import Button from "../Button.vue";
import { Dropdown, type Placement } from "floating-vue";
import type { P } from "vitest/dist/chunks/environment.d8YfPkTm.js";

const ariaId = useId();

withDefaults(
  defineProps<{
    label: string;
    icon?: string;
    iconPosition?: "left" | "right";
    iconOnly?: boolean;
    placement?: Placement;
  }>(),
  {
    icon: "caret-down",
    iconPosition: "right",
    iconOnly: false,
    placement: "bottom-start",
  }
);
</script>
<template>
  <Dropdown :aria-id="ariaId" :distance="1" :placement="placement">
    <Button
      type="outline"
      size="medium"
      :icon="icon"
      :icon-position="iconPosition"
      :icon-only="iconOnly"
      :label="label"
      >{{ label }}</Button
    >
    <template #popper>
      <slot></slot>
    </template>
  </Dropdown>
</template>

<style>
div.v-popper__wrapper
  > div.v-popper__arrow-container
  > div.v-popper__arrow-outer {
  display: none;
}
div.v-popper__wrapper
  > div.v-popper__arrow-container
  > div.v-popper__arrow-inner {
  display: none;
}
div.v-popper__wrapper
  > div.v-popper__arrow-container
  > div.v-popper__arrow-container {
  display: none;
}
</style>
