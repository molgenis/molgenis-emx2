<script setup lang="ts">
import { ref, watch } from "vue";

const props = defineProps<{
  saving?: boolean;
}>();

const done = ref(false);
watch(
  () => props.saving,
  (newVal) => {
    if (newVal === true) {
      done.value = false;
    }
    if (newVal === false) {
      done.value = true;
      setTimeout(() => {
        done.value = false;
      }, 1000);
    }
  }
);
</script>

<template>
  <BaseIcon
    v-if="saving && !done"
    class="inline animate-spin"
    name="ProgressActivity"
  />
  <BaseIcon v-if="done" class="inline animate-pulse" name="Check" />
</template>
