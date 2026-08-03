<script setup lang="ts">
import { onMounted, ref, watch } from "vue";

const props = withDefaults(
  defineProps<{
    indeterminate?: boolean;
    preventClick?: boolean;
  }>(),
  {
    indeterminate: false,
    preventClick: false,
  }
);
const modelValue = defineModel<string | boolean>();
const checkboxRef = ref<HTMLInputElement | null>(null);

function syncIndeterminate() {
  if (checkboxRef.value) {
    checkboxRef.value.indeterminate = props.indeterminate ?? false;
  }
}

onMounted(syncIndeterminate);

watch(() => props.indeterminate, syncIndeterminate);

function preventClick(event: MouseEvent) {
  if (props.preventClick) {
    event.preventDefault();
  }
}
</script>

<template>
  <input
    ref="checkboxRef"
    v-model="modelValue"
    @click="preventClick"
    type="checkbox"
    class="w-5 h-5 hover:cursor-pointer accent-theme"
  />
</template>
