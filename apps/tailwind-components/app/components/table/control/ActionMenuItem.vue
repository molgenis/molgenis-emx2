<script setup lang="ts">
import BaseIcon from "../../BaseIcon.vue";

defineProps<{
  icon: string;
  label: string;
  disabled?: boolean;
}>();

const emit = defineEmits<{
  click: [];
}>();
</script>

<template>
  <li>
    <button
      type="button"
      class="flex w-full items-center gap-3 text-left px-[15px] py-[10px] whitespace-nowrap text-title-contrast-pop hover:bg-button-secondary-hover disabled:text-disabled disabled:hover:bg-transparent disabled:cursor-not-allowed"
      :disabled="disabled"
      @click="emit('click')"
    >
      <BaseIcon :name="icon" :width="20" class="shrink-0" />
      {{ label }}
    </button>
  </li>
</template>
