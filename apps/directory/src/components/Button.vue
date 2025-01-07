<template>
  <button type="button" class="btn px-2" :class="buttonClass">
    {{ label }}
  </button>
</template>

<script setup lang="ts">
import { computed } from "vue";

export type TButtonType = "outline" | "full";
export type TButtonSize = "sm" | "md" | "lg";

const props = withDefaults(
  defineProps<{
    label: string;
    type?: TButtonType;
    size?: TButtonSize;
  }>(),
  {
    type: "full",
    size: "md",
  }
);

const buttonClass = computed(() => {
  return `btn-${props.type === "outline" ? "outline-" : ""}secondary btn-${
    props.size
  }`;
});
</script>
