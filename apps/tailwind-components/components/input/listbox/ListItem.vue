<template>
  <li
    ref="li"
    role="option"
    class="flex justify-start items-center gap-3 pl-3 py-1 bg-listbox text-listbox border-t-[1px] border-t-listbox-option hover:cursor-pointer hover:bg-listbox-hover hover:text-listbox focus:bg-listbox-hover focus:text-listbox focus:ring-blue-300"
    :class="{
      '!bg-listbox-selected !text-listbox-selected': isSelected,
    }"
    :aria-selected="isSelected"
  >
    <BaseIcon
      name="Check"
      class="fill-listbox-selected invisible"
      :class="{
        '!visible': isSelected,
      }"
      :width="18"
    />
    <span v-if="label">{{ label }}</span>
  </li>
</template>

<script lang="ts" setup>
withDefaults(
  defineProps<{
    isSelected: boolean;
    label?: string | number | boolean;
  }>(),
  {
    isSelected: false,
  }
);

const li = useTemplateRef<HTMLLIElement>("li");
defineExpose({
  li,
});
</script>
