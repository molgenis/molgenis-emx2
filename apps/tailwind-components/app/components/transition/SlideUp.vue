<script setup lang="ts">
import { onBeforeUnmount, Transition, watch } from "vue";

const props = withDefaults(
  defineProps<{
    autoHide?: boolean;
    timeoutS?: number;
  }>(),
  {
    autoHide: false,
    timeoutS: 3,
  }
);

const visible = defineModel("visible", {
  required: false,
});

watch(visible, (newVal) => {
  if (newVal) {
    triggerSlide();
  }
});

const emit = defineEmits(["closed"]);

let timer: ReturnType<typeof setTimeout> | null = null;

const triggerSlide = () => {
  visible.value = true;
  if (timer !== null) {
    clearTimeout(timer);
  }
  if (!props.autoHide) return;
  timer = setTimeout(() => {
    visible.value = false;
    timer = null;
    emit("closed");
  }, props.timeoutS * 1000);
};

onBeforeUnmount(() => {
  if (timer !== null) {
    clearTimeout(timer);
    timer = null;
  }
});
</script>

<template>
  <Transition name="slide-up" v-show="visible">
    <slot />
  </Transition>
</template>

<style scoped>
.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.25s ease-out;
}

.slide-up-enter-from {
  opacity: 0;
  transform: translateY(62px);
}

.slide-up-leave-to {
  opacity: 0;
  transform: translateY(62px);
}
</style>
