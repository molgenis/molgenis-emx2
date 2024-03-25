<script setup lang="ts">
const ariaId = useId();

withDefaults(
  defineProps<{
    shown: boolean;
    autoHide?: boolean;
    includeFooter?: boolean;
  }>(),
  {
    autoHide: true,
    includeFooter: false,
  }
);

const emit = defineEmits(["close"]);

const preAnimation = () => {
  document.body.classList.add("v-popper_bottom");
};

const onShow = () => {
  document.body.classList.add("no-scroll");
};

const onHide = () => {
  document.body.classList.remove("no-scroll");
  setTimeout(() => {
    document.body.classList.remove("v-popper_bottom");
  }, 150);
  emit("close");
};
</script>

<template>
  <VDropdown
    :aria-id="ariaId"
    :shown="shown"
    :positioning-disabled="true"
    @show="preAnimation()"
    @apply-show="onShow()"
    @apply-hide="onHide()"
    :autoHide="autoHide"
  >
    <slot name="button"></slot>
    <template #popper="{ hide }">
      <div class="flex justify-center">
        <div
          class="fixed top-10 bg-white overflow-hidden rounded-50px w-[60vw] min-h-[50vh] max-h-[90vh]"
        >
          <header class="py-[36px] px-[50px]">
            <slot name="header"></slot>
          </header>
          <button @click="hide()" class="absolute top-7 right-8 p-1">
            <BaseIcon name="cross" />
          </button>
          <div class="px-[50px] calc-remaining-max-height overflow-auto">
            <slot></slot>
          </div>
          <footer v-if="includeFooter" class="">
            <!-- <div class="`flex items-center left px-[50px] bg-modal-footer h-19`"></div> -->
            <div class="bg-modal-footer px-[50px] py-3">
              <slot name="footer"></slot>
            </div>
          </footer>
        </div>
      </div>
    </template>
  </VDropdown>
</template>

<style>
.calc-remaining-max-height {
  max-height: calc(80vh - 4rem);
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

.v-popper_bottom .v-popper__popper--no-positioning {
  top: auto;
  bottom: 0;
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
  transform: translateY(-100%);
}

.v-popper_bottom
  .v-popper__popper--no-positioning.v-popper__popper--hidden
  .v-popper__wrapper {
  transform: translateY(100%);
}

body.no-scroll {
  overflow: hidden;
}
</style>
