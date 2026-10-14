<template>
  <button
    type="button"
    role="combobox"
    ref="button"
    aria-haspopup="listbox"
    :aria-required="required"
    :aria-expanded="expanded"
    :aria-activedescendant="selectedElementId"
    :disabled="disabled"
    class="flex justify-start items-center self-stretch flex-1 min-w-32 text-left pl-4 bg-transparent focus:outline-none"
    :class="{ 'cursor-not-allowed': disabled }"
  >
    <slot></slot>
    <div class="w-[60px] ml-auto flex flex-col">
      <BaseIcon :width="18" name="caret-up" class="mx-auto -my-1" />
      <BaseIcon :width="18" name="caret-down" class="mx-auto -my-1" />
    </div>
  </button>
</template>

<script lang="ts" setup>
import { useTemplateRef } from "vue";
import BaseIcon from "../../BaseIcon.vue";

defineProps<{
  required?: boolean;
  disabled?: boolean;
  expanded?: boolean;
  selectedElementId?: string;
}>();

const button = useTemplateRef<HTMLButtonElement>("button");
defineExpose({
  button,
});
</script>
