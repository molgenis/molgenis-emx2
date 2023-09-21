<script setup lang="ts">
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

withDefaults(
  defineProps<{
    show?: boolean;
  }>(),
  {
    show: undefined,
  }
);

const emit = defineEmits(["close"]);
</script>

<template>
  <VDropdown
    :shown="show"
    :positioning-disabled="true"
    @show="preAnimation()"
    @apply-show="onShow()"
    @apply-hide="onHide()"
    :autoHide="false"
  >
    <slot name="button"></slot>
    <template #popper="{ hide }">
      <div class="flex justify-center">
        <div
          class="fixed bottom-0 bg-white overflow-hidden rounded-t-50px w-[95vw]"
        >
          <div class="w-full overflow-auto">
            <button @click="hide()" class="absolute top-7 right-8">
              <BaseIcon name="cross" />
            </button>

            <slot></slot>
          </div>
        </div>
      </div>
    </template>
  </VDropdown>
</template>

<style>
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
