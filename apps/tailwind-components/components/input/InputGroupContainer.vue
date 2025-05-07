<template>
  <div ref="inputGroup">
    <slot></slot>
  </div>
</template>

<script lang="ts" setup>
import { useFocusWithin } from "@vueuse/core";
import { useTemplateRef, watch } from "vue";
const inputGroupElem = useTemplateRef<HTMLDivElement>("inputGroup");
const { focused } = useFocusWithin(inputGroupElem);

const emit = defineEmits(["focus", "blur"]);

watch(focused, (focused) => {
  if (focused) {
    emit("focus");
  } else {
    emit("blur");
  }
});
</script>
