<script setup lang="ts">
import { onBeforeUnmount, onMounted, onUnmounted, watchEffect } from "vue";
import BaseIcon from "./BaseIcon.vue";
import { Teleport } from "vue";
import { registerModal } from "../utils/modalManager";
import { UseFocusTrap } from "@vueuse/integrations/useFocusTrap/component";

withDefaults(
  defineProps<{
    title?: string;
    subtitle?: string;
    maxWidth?: string;
    type?: "center" | "left" | "right";
    backgroundAccessible?: boolean;
  }>(),
  {
    type: "center",
    maxWidth: "max-w-xl",
    backgroundAccessible: false,
  }
);

const visible = defineModel("visible", {
  required: true,
});

const emit = defineEmits(["closed"]);

// needed for case where modal is show/hidden but not added /removed from DOM
watchEffect(() => {
  if (typeof document === "undefined" || !document.body) {
    // short circuit if document is not available (e.g. during SSR)
    return;
  }
  if (visible.value) {
    document.body.style.overflow = "hidden";
  } else {
    document.body.style.overflow = "";
  }
});

// needed for case where modal is added /removed ( versus show/hide)
onBeforeUnmount(() => {
  document.body.style.overflow = "";
});

let unregister: (() => void) | undefined;

onMounted(() => {
  unregister = registerModal(() => {
    visible.value = false;
    emit("closed");
  });
});

onUnmounted(() => {
  unregister?.();
});

function hide() {
  visible.value = false;
}
</script>

<template>
  <ClientOnly>
    <Teleport to="body">
      <Transition :name="`modal-${type}`">
        <div v-if="visible">
          <OptionalFocusTrap
            :enabled="!backgroundAccessible"
            target="modal-title"
          >
            <div
              role="dialog"
              aria-labelledby="modal-title"
              :aria-modal="true"
              ref="dialog"
              class="fixed min-h-lvh w-full top-0 left-0 flex z-50 overscroll-contain"
              :class="{ 'pointer-events-none': backgroundAccessible }"
            >
              <div
                v-if="!backgroundAccessible"
                id="backdrop"
                @click="visible = false"
                class="w-full h-full absolute left-0 bg-black/60 overscroll-contain"
                tabindex="-1"
              />

              <div
                class="bg-modal w-3/4 relative rounded-theme h-[95vh] flex flex-col pointer-events-auto overflow-auto"
                :class="[
                  {
                    'm-auto': type === 'center',
                    'ml-auto rounded-r-none': type === 'right',
                    'mr-auto rounded-l-none': type === 'left',
                    'shadow-no-background-modal': backgroundAccessible,
                  },
                  maxWidth,
                ]"
              >
                <slot name="header">
                  <header
                    class="pt-8 px-8 flex-none overflow-y-auto border-b border-divider"
                  >
                    <div
                      v-if="subtitle"
                      class="text-title-contrast overflow-y-auto"
                    >
                      {{ subtitle }}
                    </div>
                    <h2
                      v-if="title"
                      id="modal-title"
                      ref="modal-title"
                      class="mb-5 uppercase text-heading-4xl font-display text-title-contrast"
                    >
                      {{ title }}
                    </h2>

                    <button
                      @click="visible = false"
                      aria-label="Close modal"
                      class="absolute top-7 right-8 p-1"
                    >
                      <BaseIcon class="text-button-input-toggle" name="cross" />
                    </button>
                  </header>
                </slot>

                <div
                  class="flex-1 flex flex-col min-h-0 overflow-y-auto"
                  id="modal-content"
                >
                  <slot />
                </div>

                <footer
                  class="bg-modal-footer px-8 rounded-b-theme border-t border-divider flex-none z-50 overflow-y-auto"
                  :class="[
                    {
                      'rounded-r-none': type === 'right',
                      'rounded-l-none': type === 'left',
                    },
                  ]"
                >
                  <slot name="footer" :hide="hide" />
                </footer>
              </div>
            </div>
          </OptionalFocusTrap>
        </div>
      </Transition>
    </Teleport>
  </ClientOnly>
</template>

<style lang="css" scoped>
.modal-center-enter-active .bg-modal,
.modal-center-leave-active,
.modal-center-leave-active .bg-modal,
.modal-left-enter-active .bg-modal,
.modal-left-leave-active,
.modal-left-leave-active .bg-modal,
.modal-right-enter-active .bg-modal,
.modal-right-leave-active,
.modal-right-leave-active .bg-modal {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.modal-center-enter-from .bg-modal,
.modal-center-leave-to .bg-modal {
  opacity: 0;
  transform: translateY(-15%);
}
.modal-left-enter-from .bg-modal,
.modal-left-leave-to .bg-modal {
  opacity: 0;
  transform: translateX(-15%);
}
.modal-right-enter-from .bg-modal,
.modal-right-leave-to .bg-modal {
  opacity: 0;
  transform: translateX(15%);
}
</style>
