<script setup lang="ts">
import { useId } from "vue";
import BaseIcon from "../../../tailwind-components/app/components/BaseIcon.vue";

const ariaId = useId();
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
    :aria-id="ariaId"
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
          class="fixed bottom-0 bg-white overflow-hidden rounded-t-alt w-[95vw]"
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
body.no-scroll {
  overflow: hidden;
}
</style>
