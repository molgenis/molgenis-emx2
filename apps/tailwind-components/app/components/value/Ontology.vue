<script setup lang="ts">
import { computed, ref, watch } from "vue";
import type { IColumn } from "../../../../metadata-utils/src/types";
import type { IOntologyTreeItem } from "../../../types/types";
import fetchOntologyAncestry from "../../composables/fetchOntologyAncestry";
import { buildOntologyTree } from "../../utils/buildOntologyTree";
import { resolveOntologyAncestry } from "../../utils/resolveOntologyAncestry";
import { useOntologyItemPaging } from "../../composables/useOntologyItemPaging";
import OntologyNode from "./OntologyNode.vue";
import OntologyRow from "./OntologyRow.vue";

const props = withDefaults(
  defineProps<{
    value: IOntologyTreeItem | IOntologyTreeItem[];
    metadata?: IColumn;
    collapseAll?: boolean;
    maxItems?: number;
    itemStep?: number;
  }>(),
  {
    collapseAll: true,
    itemStep: 5,
  }
);

const resolvedValue = ref(props.value);

watch(
  [() => props.value, () => props.metadata],
  async ([value, metadata]) => {
    resolvedValue.value = value;
    const schemaId = metadata?.refSchemaId;
    const tableId = metadata?.refTableId;
    if (!value || !schemaId || !tableId) {
      return;
    }
    const terms = Array.isArray(value) ? value : [value];
    try {
      const termsByName = await fetchOntologyAncestry(
        schemaId,
        tableId,
        terms.map((term) => term.name)
      );
      resolvedValue.value = resolveOntologyAncestry(terms, termsByName);
    } catch (err) {
      console.error("Failed to resolve ontology ancestry", err);
    }
  },
  { immediate: true }
);

const tree = computed(() => buildOntologyTree(resolvedValue.value));

const isList = computed(() => {
  return tree.value.every((node) => !node.children?.length);
});

const {
  isHidden: isRootHidden,
  showControl: showRootControl,
  isFullyExpanded: isRootFullyExpanded,
  controlLabel: rootControlLabel,
  toggle: toggleRoot,
} = useOntologyItemPaging(
  computed(() => tree.value.length),
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
        v-for="(item, index) in tree"
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
        v-for="(node, index) in tree"
        :key="node.name"
        :node="node"
        :collapse-all="collapseAll"
        :is-root-node="true"
        :max-items="maxItems"
        :item-step="itemStep"
        :hidden="isRootHidden(index)"
      />
    </template>
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
  </ul>
</template>
