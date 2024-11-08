<template>
  <li
    role="option"
    :aria-selected="selected"
    class="flex justify-start items-center gap-3 pl-3 py-1 text-listbox border-t-[1px] border-t-listbox-option hover:cursor-pointer hover:bg-listbox-hover hover:text-listbox focus:bg-listbox-hover focus:text-listbox"
    :class="{
      '!bg-listbox-selected !text-listbox-selected': selected,
    }"
  >
    <BaseIcon
      name="Check"
      class="fill-listbox-selected"
      :class="selected ? 'visible' : 'invisible'"
      :width="18"
    />
    <slot></slot>
  </li>
</template>

<script lang="ts" setup>
withDefaults(
  defineProps<{
    selected: boolean;
  }>(),
  {
    selected: false,
  }
);
</script>
