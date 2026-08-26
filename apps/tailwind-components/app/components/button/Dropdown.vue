<script setup lang="ts">
import { ref, useTemplateRef, useId, onMounted } from "vue";
import { useEventListener, useFocus, onClickOutside } from "@vueuse/core";
import Button from "../Button.vue";

const ariaId: string = useId();
const isOpen = ref<boolean>(false);
const dropdown = useTemplateRef<HTMLDivElement>("dropdown");
const btnElem = useTemplateRef<HTMLButtonElement>("btnElem");
const modalElem = useTemplateRef<HTMLDivElement>("modalElem");

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

const { focused: buttonFocus } = useFocus(btnElem);

onClickOutside(modalElem, (e: MouseEvent) => {
  if (isOpen.value) {
    isOpen.value = false;
  }
});

function onKeyDown(event: KeyboardEvent) {
  if (isOpen.value && event.key === "Escape") {
    isOpen.value = false;
    buttonFocus.value = true;
  }

  if (event.key === "Tab" || (event.shiftKey && event.key === "Tab")) {
    isOpen.value = false;
  }
}

onMounted(() => {
  useEventListener(dropdown, "keydown", onKeyDown);
});
</script>
<template>
  <div ref="dropdown" class="relative">
    <Button
      ref="btnElem"
      :id="`dropdown-${ariaId}-toggle`"
      type="outline"
      size="medium"
      :icon="icon"
      :icon-position="iconPosition"
      :aria-expanded="isOpen"
      :aria-controls="`dropdown-${ariaId}-content`"
      :aria-haspopup="true"
      @click="isOpen = !isOpen"
    >
      {{ label }}
    </Button>
    <div :id="`dropdown-${ariaId}-content`" ref="modalElem">
      <div
        v-show="isOpen"
        :aria-labelledby="`dropdown-${ariaId}-toggle`"
        class="absolute min-w-full mt-0.5 left-0 z-10 bg-dropdown text-dropdown shadow-md rounded-base border"
      >
        <slot></slot>
      </div>
    </div>
  </div>
</template>
