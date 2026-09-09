<script setup lang="ts">
import { computed, ref, watch } from "vue";
import type { IOntologyTreeItem } from "../../../types/types";
import { buildOntologyTree } from "../../utils/buildOntologyTree";
import {
  countOntologyNodes,
  limitOntologyTree,
} from "../../utils/limitOntologyTree";
import { useOntologyItemPaging } from "../../composables/useOntologyItemPaging";
import OntologyNode from "./OntologyNode.vue";
import OntologyRow from "./OntologyRow.vue";

const props = withDefaults(
  defineProps<{
    value: IOntologyTreeItem | IOntologyTreeItem[];
    collapseAll?: boolean;
    /** Rows a level shows before the control; unset means no bound. */
    maxItems?: number;
    itemStep?: number;
    renderLimit?: number;
  }>(),
  {
    collapseAll: true,
    itemStep: 5,
    renderLimit: 1000,
  }
);

const tree = computed(() => buildOntologyTree(props.value));

const isList = computed(() => {
  return tree.value.every((node) => !node.children?.length);
});

// Beyond this budget a node is genuinely absent from the DOM, unlike item paging which only hides.
const rendered = ref(props.renderLimit);

watch(
  [() => props.renderLimit, () => props.value],
  () => (rendered.value = props.renderLimit)
);

const limitedTree = computed(() =>
  limitOntologyTree(tree.value, rendered.value)
);

const hasUnrendered = computed(
  () => countOntologyNodes(tree.value) > rendered.value
);

function renderMore() {
  rendered.value += props.renderLimit;
}

const {
  isHidden: isRootHidden,
  showControl: showRootControl,
  isFullyExpanded: isRootFullyExpanded,
  controlLabel: rootControlLabel,
  toggle: toggleRoot,
} = useOntologyItemPaging(
  computed(() => limitedTree.value.length),
  computed(() => props.maxItems),
  computed(() => props.itemStep)
);
</script>

<template>
  <OntologyRow
    v-if="isList && tree.length === 1"
    :name="tree[0]?.name ?? ''"
    :definition="tree[0]?.definition"
    marker="flush"
  />
  <ul v-else class="text-body-base" :class="[isList ? 'grid gap-1' : '']">
    <template v-if="isList">
      <li
        v-for="(item, index) in limitedTree"
        :key="item.name"
        :class="{ hidden: isRootHidden(index) }"
      >
        <OntologyRow
          :name="item.name"
          :definition="item.definition"
          marker="bullet"
        />
      </li>
    </template>
    <template v-else>
      <OntologyNode
        v-for="(node, index) in limitedTree"
        :key="node.name"
        :node="node"
        :collapse-all="collapseAll"
        :is-root-node="true"
        :max-items="maxItems"
        :item-step="itemStep"
        :hidden="isRootHidden(index)"
      />
    </template>
    <!-- Kept as trailing <li>s, not siblings of the <ul>, so the component
         keeps ONE root element whether or not a control is showing. -->
    <li v-if="showRootControl" class="list-none">
      <button
        type="button"
        class="text-link text-body-sm"
        :aria-expanded="isRootFullyExpanded"
        @click="toggleRoot"
      >
        {{ rootControlLabel }}
      </button>
    </li>
    <li v-if="hasUnrendered" class="list-none">
      <button type="button" class="text-link text-body-sm" @click="renderMore">
        Load more
      </button>
    </li>
  </ul>
</template>
