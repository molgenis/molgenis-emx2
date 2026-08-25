<script setup lang="ts">
import { ref, useTemplateRef, useId, onMounted, watch, nextTick } from "vue";
import { useFocusWithin, useEventListener } from "@vueuse/core";
import Button from "../Button.vue";

const ariaId: string = useId();
const isOpen = ref<boolean>(false);
const dropdown = useTemplateRef<HTMLDivElement>("dropdown");
const btnElem = useTemplateRef<HTMLButtonElement>("btnElem");

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

const { focused } = useFocusWithin(dropdown);
watch(focused, (focused) => {
  if (!focused && isOpen.value) {
    isOpen.value = false;
    // focusButton();
  }
});

function onKeyDown(event: KeyboardEvent) {
  if (event.code === "Escape" && isOpen.value) {
    isOpen.value = false;
    // focusButton();
  }
}

// function focusButton () {
//   if (btnElem.value) {
//     btnElem.value
//   }
// }

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
    <div ref="modalElem">
      <div
        v-if="isOpen"
        :id="`dropdown-${ariaId}-content`"
        :aria-labelledby="`dropdown-${ariaId}-toggle`"
        class="absolute min-w-full mt-0.5 left-0 z-10 bg-dropdown text-dropdown shadow-md rounded-base border"
      >
        <slot></slot>
      </div>
    </div>
  </div>
</template>
