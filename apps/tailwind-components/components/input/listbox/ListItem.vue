<template>
  <li
    ref="li"
    role="option"
    class="flex justify-start items-center gap-3 h-[56px] pl-3 py-1 bg-input border-b-[1px] last:border-b-0 hover:cursor-pointer hover:bg-input-focused focus:bg-input-focused hover:text-input-focused focus:text-input-focused"
    :class="{
      'text-input': !isSelected,
      'bg-input-checked text-input-checked': isSelected,
    }"
    :aria-selected="isSelected"
  >
    <BaseIcon
      name="Check"
      class="fill-check invisible"
      :class="{
        '!visible': isSelected,
      }"
      :width="18"
    />
    <span>{{ label }}</span>
  </li>
</template>

<script lang="ts" setup>
import { useTemplateRef } from "vue";

defineProps<{
  isSelected?: boolean;
  label: string | number | boolean;
}>();

const li = useTemplateRef<HTMLLIElement>("li");
defineExpose({
  li,
});
</script>
