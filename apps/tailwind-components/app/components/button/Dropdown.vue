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
      :aria-hidden="!isOpen"
      class="absolute bottom mt-0.5 left-0 z-10 bg-dropdown text-dropdown shadow-md rounded-base border transition-opacity ease-in-out motion-safe:duration-150 motion-reduce:duration-0"
      :class="{
        'invisible opacity-0': !isOpen,
        'visible opacity-100': isOpen,
      }"
    >
      <slot></slot>
    </div>
  </div>
  <!-- <VDropdown
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
    >
      {{ label }}
    </Button>
    <template #popper>
      <div class="bg-dropdown text-dropdown">
        <slot />
      </div>
    </template>
  </VDropdown> -->
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
