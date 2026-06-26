<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";

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

// Bind `checked` one-way so the DOM always reflects the model, even when the
// model is driven externally and `preventClick` suppresses the native toggle
// (a `v-model` checkbox can desync in that case).
const isChecked = computed(
  () => modelValue.value === true || modelValue.value === "true"
);

function onChange(event: Event) {
  modelValue.value = (event.target as HTMLInputElement).checked;
}

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
    :checked="isChecked"
    @change="onChange"
    @click="preventClick"
    type="checkbox"
    class="w-5 h-5 hover:cursor-pointer accent-theme"
  />
</template>
