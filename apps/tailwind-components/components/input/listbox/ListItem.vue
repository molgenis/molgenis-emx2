<template>
  <li
    ref="li"
    role="option"
    class="flex justify-start items-center gap-3 pl-3 py-1 bg-input border-t-[1px] hover:cursor-pointer hover:bg-input-focused focus:bg-input-focused hover:text-input-focused"
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
defineProps<{
  isSelected?: boolean;
  label: string | number | boolean;
}>();

const li = useTemplateRef<HTMLLIElement>("li");
defineExpose({
  li,
});
</script>
