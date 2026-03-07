<script setup lang="ts">
import { computed } from "vue";

const props = withDefaults(
  defineProps<{
    type?: "standard" | "link";
  }>(),
  {
    type: "standard",
  }
);

const LIST_ITEM_TYPE_MAPPING = {
  standard: "pr-12.5 pb-[5px]",
  link: "pr-12.5 pb-2.5",
};

const listItemClasses = computed(() => {
  return LIST_ITEM_TYPE_MAPPING[props.type];
});
</script>

<template>
  <li :class="listItemClasses"><slot></slot></li>
</template>
