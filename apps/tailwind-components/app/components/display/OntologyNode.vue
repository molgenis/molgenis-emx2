<script setup lang="ts">
import { computed, ref } from "vue";
import type { IOntologyTreeItem } from "../../../types/types";
import { useOntologyItemPaging } from "../../composables/useOntologyItemPaging";
import OntologyRow from "./OntologyRow.vue";

const props = withDefaults(
  defineProps<{
    node: IOntologyTreeItem;
    isRootNode?: boolean;
    collapseAll?: boolean;
    maxItems?: number;
    itemStep?: number;
    hidden?: boolean;
  }>(),
  {
    collapseAll: true,
    isRootNode: false,
    itemStep: 5,
    hidden: false,
  }
);

const collapsed = ref(props.collapseAll);
const toggleCollapse = () => {
  collapsed.value = !collapsed.value;
};

const {
  isHidden: isChildHidden,
  showControl: showChildControl,
  isFullyExpanded: isChildFullyExpanded,
  controlLabel: childControlLabel,
  toggle: toggleChild,
} = useOntologyItemPaging(
  computed(() => props.node.children?.length ?? 0),
  computed(() => props.maxItems),
  computed(() => props.itemStep)
);
</script>

<template>
  <li class="relative" :class="{ hidden: hidden }">
    <OntologyRow
      :name="node.name"
      :definition="node.definition"
      :has-children="!!node.children?.length"
      :collapsed="collapsed"
      :marker="isRootNode ? 'blank' : 'connector'"
      @toggle="toggleCollapse()"
    />

    <ul
      v-if="node.children?.length"
      class="break-inside-avoid"
      :class="{ hidden: collapsed }"
    >
      <!-- pl-8 only: a nested row sits on the same vertical rhythm as its
           parent's rows, so indentation is horizontal, never vertical. -->
      <OntologyNode
        v-for="(child, index) in node.children"
        :key="child.name"
        class="pl-8"
        :node="child"
        :max-items="maxItems"
        :item-step="itemStep"
        :hidden="isChildHidden(index)"
      />
    </ul>
    <button
      v-if="node.children?.length && showChildControl"
      type="button"
      class="text-link text-body-sm ml-8 mt-1"
      :class="{ hidden: collapsed }"
      :aria-expanded="isChildFullyExpanded"
      @click="toggleChild"
    >
      {{ childControlLabel }}
    </button>
  </li>
</template>
