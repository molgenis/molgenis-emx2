<template>
  <ul
    :id="id"
    role="listbox"
    tabindex="0"
    :aria-expanded="expanded"
    :aria-activedescendant="selectedElemId"
    class="absolute b-0 w-full z-10 bg-listbox"
    :class="{
      hidden: !expanded,
    }"
  >
    <slot></slot>
  </ul>
</template>

<script lang="ts" setup>
interface IListbox {
  id: string;
  expanded: boolean;
  selectedElemId?: string;
}

withDefaults(defineProps<IListbox>(), {
  expanded: false,
});
</script>
