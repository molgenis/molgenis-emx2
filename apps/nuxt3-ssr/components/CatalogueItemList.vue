<script setup lang="ts">
import type { IDefinitionListItem } from "~/interfaces/types";

withDefaults(
  defineProps<{
    items: IDefinitionListItem[];
    small?: boolean;
  }>(),
  {
    small: false,
  }
);

function emptyContent(item: IDefinitionListItem) {
  if (item.content === undefined || item.content === "") {
    return false;
  } else if (Array.isArray(item.content) && item.content.length === 0) {
    return false;
  } else if (
    Object.keys(item.content).length === 0 &&
    Object.getPrototypeOf(item.content) === Object.prototype
  ) {
    // empty object
    return false;
  }

  return true;
}
</script>

<template>
  <DefinitionList>
    <template
      :class="small ? smallClasses : useGridClasses"
      v-for="item in items.filter(emptyContent)"
    >
      <CatalogueListItem :item="item" :small="small" />
    </template>
  </DefinitionList>
</template>
