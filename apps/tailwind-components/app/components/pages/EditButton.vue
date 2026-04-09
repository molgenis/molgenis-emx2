<script lang="ts" setup>
import BaseIcon from "../BaseIcon.vue";

const props = withDefaults(
  defineProps<{
    fixIconPosition?: boolean;
  }>(),
  {
    fixIconPosition: false,
  }
);
</script>

<template>
  <button class="group relative outline-none" aria-haspopup="true">
    <slot></slot>
    <BaseIcon
      name="edit"
      :width="16"
      class="opacity-0 group-focus:opacity-100 group-hover:opacity-100"
      :class="{
        'inline-block ml-2.5 -mt-1': !fixIconPosition,
        'absolute h-6 w-6 bg-button-secondary-hover text-button-secondary-hover bottom-2.5 right-2.5':
          fixIconPosition,
      }"
      v-tooltip.bottom="'Edit'"
    />
  </button>
</template>
