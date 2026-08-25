<script setup lang="ts">
import { ref, useTemplateRef, useId, onBeforeUnmount, watch } from "vue";
import { onClickOutside, useFocusWithin } from "@vueuse/core";
import Button from "../Button.vue";

const ariaId: string = useId();
const isOpen = ref<boolean>(false);
const dropdownElem = useTemplateRef<HTMLDivElement>("dropdown");

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

const { focused } = useFocusWithin(dropdownElem);
watch(focused, (focused) => {
  if (!focused && isOpen.value) {
    isOpen.value = false;
  }
});

const stop = onClickOutside(dropdownElem, (event: Event) => {
  const target = event.target as HTMLElement;
  if (!dropdownElem.value?.contains(target) && dropdownElem.value !== target) {
    isOpen.value = false;
  }
});

onBeforeUnmount(() => stop());
</script>
<template>
  <div ref="dropdown" class="relative">
    <Button
      :id="`dropdown-${ariaId}-toggle`"
      type="outline"
      size="medium"
      :icon="icon"
      :icon-position="iconPosition"
      @click="isOpen = !isOpen"
      :aria-expanded="isOpen"
      :aria-controls="`dropdown-${ariaId}-content`"
    >
      {{ label }}
    </Button>
    <div
      :id="`dropdown-${ariaId}-content`"
      class="absolute w-full bottom mt-0.5 left-0 z-10 bg-dropdown text-dropdown shadow-md rounded-base border transition-opacity ease-in-out duration-150"
      :class="{
        'hidden invisible opacity-0': !isOpen,
        'visible opacity-100': isOpen,
      }"
    >
      <slot></slot>
    </div>
  </div>
</template>
