<script setup lang="ts">
import type { IOntologyItem } from "metadata-utils";

const { tree } = withDefaults(
  defineProps<{
    tree: IOntologyItem[];
    collapseAll?: boolean;
  }>(),
  {
    collapseAll: true,
  }
);
</script>

<template>
  <ul class="text-body-base">
    <ContentTreeNode
      v-for="node in tree"
      :node="node"
      :collapse-all="collapseAll"
      :is-root-node="true"
    />
  </ul>
</template>
