<script setup lang="ts">
import { ref, watch, computed, onUnmounted } from "vue";

enum SaveState {
  READY,
  SAVING,
  SAVED,
  DONE,
}

const props = defineProps<{
  saving?: boolean;
}>();

const status = ref<SaveState>(SaveState.READY);
let animationTimeout: ReturnType<typeof setTimeout> | null = null;
let resetTimeout: ReturnType<typeof setTimeout> | null = null;

const clearTimers = () => {
  if (animationTimeout) clearTimeout(animationTimeout);
  if (resetTimeout) clearTimeout(resetTimeout);
};

const statusText = computed(() => {
  if (status.value === SaveState.SAVING) return "Saving";
  if (status.value === SaveState.SAVED || status.value === SaveState.DONE)
    return "Saved";
  return "";
});

watch(
  () => props.saving,
  (isSaving, wasSaving) => {
    if (isSaving) {
      clearTimers();
      status.value = SaveState.SAVING;
    } else if (wasSaving) {
      status.value = SaveState.SAVED;

      animationTimeout = setTimeout(() => {
        status.value = SaveState.DONE;
      }, 1000);

      resetTimeout = setTimeout(() => {
        status.value = SaveState.READY;
      }, 2000);
    }
  },
  { immediate: true }
);

onUnmounted(clearTimers);
</script>

<template>
  <div
    role="status"
    :class="{
      'flex justify-center items-center bg-content rounded-full p-2.5':
        status !== SaveState.READY,
    }"
  >
    <BaseIcon
      v-if="status === SaveState.SAVING"
      class="inline animate-spin"
      name="ProgressActivity"
    />
    <BaseIcon
      v-else-if="status === SaveState.SAVED || status === SaveState.DONE"
      class="inline"
      :class="{ 'animate-ping': status === SaveState.DONE }"
      name="Check"
    />
    <span class="sr-only">{{ statusText }}</span>
  </div>
</template>
