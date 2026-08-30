<script setup lang="ts">
import { computed } from "vue";
import type { IOntologyTreeItem } from "../../utils/buildOntologyTree";
import { buildOntologyTree } from "../../utils/buildOntologyTree";
import CustomTooltip from "../CustomTooltip.vue";
import OntologyNode from "./OntologyNode.vue";

const props = withDefaults(
  defineProps<{
    value: IOntologyTreeItem | IOntologyTreeItem[];
    collapseAll?: boolean;
    inverted?: boolean;
  }>(),
  {
    collapseAll: true,
    inverted: false,
  }
);

const tree = computed(() => buildOntologyTree(props.value));

const isList = computed(() => {
  return tree.value.every((node) => !node.children?.length);
});
</script>

<template>
  <span v-if="isList && tree.length === 1">
    <div class="flex items-center">
      <span class="flex justify-center items-start">
        {{ tree[0]?.name }}
      </span>
      <div class="inline-flex items-center whitespace-nowrap">
        <div v-if="tree[0]?.definition" class="inline-block ml-1">
          <CustomTooltip
            label="Read more"
            :hoverColor="inverted ? 'none' : 'white'"
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
      <li v-for="item in tree" :key="item.name">
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
    </template>
    <template v-else>
      <OntologyNode
        v-for="node in tree"
        :key="node.name"
        :node="node"
        :collapse-all="collapseAll"
        :is-root-node="true"
      />
    </template>
  </ul>
</template>
