<script setup lang="ts">
import { useId } from "vue";

const ariaId = useId();

withDefaults(
  defineProps<{
    label: string;
    icon?: string;
    iconPosition?: "left" | "right";
  }>(),
  {
    icon: "caret-down",
    iconPosition: "right",
  }
);
</script>
<template>
  <VDropdown
    :aria-id="ariaId"
    :distance="2"
    :skidding="4"
    placement="bottom-start"
  >
    <Button
      type="outline"
      size="medium"
      :icon="icon"
      :icon-position="iconPosition"
      >{{ label }}</Button
    >
    <template #popper>
      <slot></slot>
    </template>
  </VDropdown>
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
