<script setup lang="ts">
import { computed } from "vue";
import type { IOntologyItem } from "~/interfaces/types";

const props = withDefaults(
  defineProps<{
    tree: IOntologyItem[];
    collapseAll?: boolean;
    inverted?: boolean;
  }>(),
  {
    collapseAll: true,
    inverted: false,
  }
);

const isList = computed(() => {
  return (
    props.tree.reduce((branches, node) => {
      if (node.children) {
        branches++;
      }
      return branches;
    }, 0) === 0
  );
});
</script>

<template>
  <!-- single item case-->
  <span v-if="isList && tree.length === 1">
    <div class="flex items-center">
      <span class="flex justify-center items-start">
        {{ tree[0].name }}
      </span>
      <div class="inline-flex items-center whitespace-nowrap">
        <div v-if="tree[0].definition" class="inline-block ml-1">
          <CustomTooltip
            label="Read more"
            :hoverColor="inverted ? 'none' : 'white'"
            :content="tree[0].definition"
          />
        </div>
      </div>
    </div>
  </span>
  <!-- list item case-->
  <ul
    v-else
    class="text-body-base"
    :class="[isList ? 'grid gap-1 pl-4 list-disc list-outside' : '']"
  >
    <li v-if="isList" v-for="item in tree">
      <div class="flex items-center">
        <span class="flex justify-center items-start">
          {{ item.name }}
        </span>
        <div class="inline-flex items-center whitespace-nowrap">
          <div v-if="item.definition" class="inline-block ml-1">
            <CustomTooltip
              label="Read more"
              :hoverColor="inverted ? 'none' : 'white'"
              :content="item.definition"
            />
          </div>
        </div>
      </div>
    </li>
    <!-- tree item case-->
    <ContentTreeNode
      v-else
      v-for="node in tree"
      :node="node"
      :collapse-all="collapseAll"
      :is-root-node="true"
    />
  </ul>
</template>
