<script setup lang="ts">
import { useTemplateRef, watch, onMounted, ref, computed } from "vue";
import { useEventListener, useFocusWithin } from "@vueuse/core";

const container = useTemplateRef<HTMLDivElement>("container");
const clickInside = ref<boolean>(false);

const { focused: containerIsFocused } = useFocusWithin(container);
const emit = defineEmits<{ (e: "isActive", value: boolean): void }>();
const status = computed<boolean>(() => {
  return containerIsFocused.value || clickInside.value;
});

function emitStatus(value?: boolean) {
  const val = value ?? status.value;
  emit("isActive", val);
}

function onClick(e: Event) {
  e.stopPropagation();
  clickInside.value =
    container.value?.contains(e.target as HTMLElement) || false;
}

watch(
  () => [containerIsFocused.value, clickInside.value],
  () => emitStatus()
);

onMounted(() => {
  useEventListener(container, "click", onClick);
  useEventListener(document, "click", onClick);
});
</script>

<template>
  <div ref="container">
    <slot></slot>
  </div>
</template>
