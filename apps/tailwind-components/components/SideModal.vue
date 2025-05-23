<script setup lang="ts">
import { computed, ref, useId, watch } from "vue";
import type { INotificationType } from "../types/types";
const ariaId = useId();
const props = withDefaults(
  defineProps<{
    slideInRight?: boolean;
    fullScreen?: boolean;
    show?: boolean;
    buttonAlignment?: "left" | "center" | "right";
    includeFooter?: boolean;
    type?: INotificationType;
  }>(),
  {
    show: false,
    slideInRight: false,
    fullScreen: true,
    buttonAlignment: "center",
    includeFooter: true,
    type: "info",
  }
);

const isVisibile = ref(props.show);

function showModal() {
  isVisibile.value = true;
}

function hideModal() {
  isVisibile.value = false;
}

const emit = defineEmits(["close"]);
defineExpose({ showModal, hideModal });

const preAnimation = () => {
  if (props.slideInRight) {
    document.body.classList.add("v-popper_right");
  }
};

const onShow = () => {
  document.body.classList.add("no-scroll");
};

const onHide = () => {
  document.body.classList.remove("no-scroll");
  setTimeout(() => {
    document.body.classList.remove("v-popper_right");
    emit("close");
    isVisibile.value = false;
  }, 150);
};

watch(
  () => props.show,
  (value) => {
    isVisibile.value = value;
  }
);

const roundedClass = props.slideInRight
  ? "rounded-l-50px right-0"
  : "rounded-r-50px";
const fullScreenClass = computed(() =>
  props.fullScreen ? "w-[95vw]" : "lg:w-[33vw] md:w-[50vw] w-[95vw]"
);
const buttonAlignmentSet = {
  left: "justify-left",
  center: "justify-around",
  right: "justify-left",
};
const buttonAlignmentClass = buttonAlignmentSet[props.buttonAlignment];

const bgClass = computed(() => {
  switch (props.type) {
    case "light":
      return "bg-white";
    case "dark":
      return "bg-black";
    case "success":
      return "bg-green-500";
    case "error":
      return "bg-red-500";
    case "warning":
      return "bg-yellow-500";
    case "info":
      return "bg-blue-500";
  }
});
</script>

<template>
  <VDropdown
    :aria-id="ariaId"
    :shown="isVisibile"
    :positioning-disabled="true"
    @show="preAnimation()"
    @apply-show="onShow()"
    @apply-hide="onHide()"
  >
    <slot name="button"></slot>
    <template #popper="{ hide }">
      <div
        :class="`fixed top-8 calc-remaining-max-height overflow-hidden ${bgClass} ${roundedClass} ${fullScreenClass}`"
      >
        <div>
          <div>
            <button @click="hide()" class="absolute top-7 right-7 z-10">
              <BaseIcon name="cross" />
            </button>

            <div class="overflow-auto calc-remaining-max-height pb-12">
              <slot></slot>
            </div>
          </div>

          <div v-if="includeFooter" class="absolute inset-x-0 bottom-0">
            <div
              :class="`flex items-center ${buttonAlignmentClass} px-6 bg-modal-footer h-19`"
            >
              <slot name="footer" :hide="hide"></slot>
            </div>
          </div>
        </div>
      </div>
    </template>
  </VDropdown>
</template>

<style>
.calc-remaining-max-height {
  max-height: calc(100vh - 4rem);
}

.v-popper--theme-dropdown .v-popper__inner {
  background: none;
  border-radius: 0;
  border: 0;
  box-shadow: none;
}

.v-popper__popper--no-positioning {
  position: fixed;
  z-index: 9999;
  top: 0;
  left: 0;
  height: 100%;
  display: flex;
  width: 100%;
}

.v-popper_fullscreen .v-popper__popper--no-positioning {
  width: 100%;
  max-width: none;
}

.v-popper_right .v-popper__popper--no-positioning {
  left: auto;
  right: 0;
}

.v-popper__popper--no-positioning .v-popper__backdrop {
  display: block;
  background: rgba(0 0 0 / 60%);
}

.v-popper__popper--no-positioning .v-popper__wrapper {
  width: 100%;
  pointer-events: auto;
  transition: transform 0.15s ease-out;
}

.v-popper__popper--no-positioning.v-popper__popper--hidden .v-popper__wrapper {
  transform: translateX(-100%);
}
.v-popper_right
  .v-popper__popper--no-positioning.v-popper__popper--hidden
  .v-popper__wrapper {
  transform: translateX(100%);
}

body.no-scroll {
  overflow: hidden;
}
</style>
