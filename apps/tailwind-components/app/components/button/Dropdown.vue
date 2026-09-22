<script setup lang="ts">
import { ref, useTemplateRef, useId, onMounted, watch } from "vue";
import { useEventListener, useFocus } from "@vueuse/core";
import Button from "../Button.vue";
import ActiveContainer from "../wrappers/ActiveContainer.vue";

const ariaId = useId();
const isOpen = ref<boolean>(false);
const menuIsActive = ref<boolean>(false);
const componentIsActive = ref<boolean>(false);
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

const { focused: buttonFocus } = useFocus(btnElem);

function resetFocus() {
  componentIsActive.value = false;
  menuIsActive.value = false;
}

function openClose() {
  isOpen.value = !isOpen.value;
  if (!isOpen.value) {
    resetFocus();
  }
}

function onKeyDown(event: KeyboardEvent) {
  if (isOpen.value && event.key === "Escape") {
    isOpen.value = false;
    buttonFocus.value = true;
  }
}

watch(
  () => [componentIsActive.value, menuIsActive.value],
  (newStatus, oldStatus) => {
    if (!newStatus[0] && !newStatus[1]) {
      isOpen.value = false;
      resetFocus();
    }

    // focusout of menu with interactive elements
    if (oldStatus[0] && oldStatus[1] && newStatus[0] && !newStatus[1]) {
      isOpen.value = false;
      resetFocus();
    }
  }
);

onMounted(() => {
  useEventListener(dropdown, "keydown", onKeyDown);
});
</script>
<template>
  <ActiveContainer
    @isActive="componentIsActive = $event"
    class="relative"
    ref="dropdown"
  >
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
      @click="openClose"
    >
      {{ label }}
    </Button>
    <ActiveContainer
      id="`dropdown-${ariaId}-content`"
      @isActive="menuIsActive = $event"
    >
      <div
        v-show="isOpen"
        :aria-labelledby="`dropdown-${ariaId}-toggle`"
        class="absolute min-w-full mt-0.5 left-0 z-10 bg-dropdown text-dropdown shadow-md rounded-base border"
      >
        <slot></slot>
      </div>
    </ActiveContainer>
  </ActiveContainer>
</template>
