<script setup lang="ts">
import { computed, ref } from "vue";
import type { IOntologyTreeItem } from "../../utils/buildOntologyTree";
import { buildOntologyTree } from "../../utils/buildOntologyTree";
import CustomTooltip from "../CustomTooltip.vue";
import OntologyTreeNode from "./OntologyTreeNode.vue";

const props = withDefaults(
  defineProps<{
    value: IOntologyTreeItem | IOntologyTreeItem[];
    collapseAll?: boolean;
    maxItems?: number;
  }>(),
  {
    collapseAll: true,
    maxItems: 10,
  }
);

const tree = computed(() => buildOntologyTree(props.value));

const isList = computed(() => {
  return tree.value.every((node) => !node.children?.length);
});

const expanded = ref(false);

const displayedTree = computed(() => {
  if (!props.maxItems || expanded.value) return tree.value;
  return tree.value.slice(0, props.maxItems);
});

const hiddenCount = computed(() => {
  if (!props.maxItems) return 0;
  return Math.max(0, tree.value.length - props.maxItems);
});
</script>

<template>
  <div>
    <span v-if="isList && tree.length === 1">
      <div class="flex items-center">
        <span class="flex justify-center items-start">
          {{ tree[0]?.name }}
        </span>
        <div class="inline-flex items-center whitespace-nowrap">
          <div v-if="tree[0]?.definition" class="inline-block ml-1">
            <CustomTooltip
              label="Read more"
              hoverColor="white"
              :content="tree[0].definition"
            />
          </div>
        </div>
      </div>
    </span>
    <ul
      v-else
      class="text-body-base"
      :class="[isList ? 'grid gap-1 pl-4 list-disc list-outside' : '']"
    >
      <template v-if="isList">
        <li v-for="item in displayedTree" :key="item.name">
          <div class="flex items-center">
            <span class="flex justify-center items-start">
              {{ item.name }}
            </span>
            <div class="inline-flex items-center whitespace-nowrap">
              <div v-if="item.definition" class="inline-block ml-1">
                <CustomTooltip
                  label="Read more"
                  hoverColor="white"
                  :content="item.definition"
                />
              </div>
            </div>
          </div>
        </li>
      </template>
      <template v-else>
        <OntologyTreeNode
          v-for="node in displayedTree"
          :key="node.name"
          :node="node"
          :collapse-all="collapseAll"
          :is-root-node="true"
        />
      </template>
    </ul>
    <button
      v-if="hiddenCount > 0 && !expanded"
      class="text-link text-body-sm mt-1 block"
      @click="expanded = true"
    >
      Show {{ hiddenCount }} more
    </button>
    <button
      v-if="expanded && maxItems && tree.length > maxItems"
      class="text-link text-body-sm mt-1 block"
      @click="expanded = false"
    >
      Show less
    </button>
  </div>
</template>
