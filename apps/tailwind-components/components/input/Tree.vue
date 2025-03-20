<script setup lang="ts">
/* Possible future enhancements
- split the search out to a wrapping component
 */

import type { ITreeNode, ITreeNodeState } from "~/types/types";
import TreeNode from "./TreeNode.vue";

const props = withDefaults(
  defineProps<{
    id: string;
    /* tree model to be rendered */
    nodes: ITreeNode[];
    modelValue: string[];
    /* single vs multi select */
    isMultiSelect?: boolean;
    /* whether nodes should expand when selected */
    expandSelected?: boolean;
    /* whether colors should be inverted */
    inverted?: boolean;
    /* whether to include/exclude children of selected nodes in emit */
    emitSelectedChildren?: boolean;
  }>(),
  {
    isMultiSelect: true,
    expandSelected: false,
    inverted: false,
    emitSelectedChildren: true,
  }
);

const emit = defineEmits(["update:modelValue"]);

/* create node map for fast internal state management from props.nodes */
const nodeMap = ref({} as Record<string, ITreeNodeState>);
createNodeMap(props.nodes);
watch(
  () => props.nodes,
  (newValue) => {
    nodeMap.value = {};
    createNodeMap(newValue);
  }
);

function createNodeMap(nodes: ITreeNode[]) {
  nodes.forEach((node) => {
    nodeMap.value[node.name] = clone(node);
  });
}

function clone(node: ITreeNode): ITreeNodeState {
  const result = {
    name: node.name,
    description: node.description,
    visible: true,
    children: [] as ITreeNodeState[],
    selection: "unselected",
    expanded: false,
    selectable: true,
  };
  node.children?.forEach((child) => {
    const copy = clone(child);
    copy.parent = node.name;
    nodeMap.value[child.name] = copy;
    result.children.push(copy);
  });
  return result;
}

/* manage selection */

applyModelValueChangeToSelection(props.modelValue);
watch(
  () => props.modelValue,
  (newValue) => {
    applyModelValueChangeToSelection(newValue);
  }
);

function applyModelValueChangeToSelection(selection: string[]) {
  Object.values(nodeMap.value).forEach(
    (node) => (node.selected = "unselected")
  );
  selection.forEach((name) => {
    const node = nodeMap.value[name];
    node.selected = "selected";
    processSelectionChangeToParentAndChildNodes(node);
  });
}

function processSelectionChangeToParentAndChildNodes(node: ITreeNodeState) {
  //make child selection match selection state
  getAllChildren(node).forEach((child) => {
    child.selected = node.selected;
    child.visible = true; //in search you want to see effect of selecting
  });

  //update parents selection state to either selected, unselected, intermediate
  getAllParents(node).forEach((parent) => {
    const allSelected = parent.children?.every(
      (child) => child.selected === "selected"
    );
    const allUnselected = parent.children?.every(
      (child) => child.selected === "unselected"
    );
    if (allUnselected) {
      parent.selected = "unselected";
    } else if (allSelected) {
      parent.selected = "selected";
    } else {
      parent.selected = "intermediate";
    }
  });
}

function getAllChildren(node: ITreeNodeState): ITreeNodeState[] {
  return [node, ...(node.children || []).flatMap(getAllChildren)];
}

function getAllParents(node: ITreeNodeState): ITreeNodeState[] {
  return node.parent
    ? [nodeMap.value[node.parent], ...getAllParents(nodeMap.value[node.parent])]
    : [];
}

function toggleSelect(node: ITreeNodeState) {
  //toggle select of the named node
  const previousState = node.selected;
  if (node) {
    if (previousState !== "selected") {
      node.selected = "selected";
    } else {
      node.selected = "unselected";
    }
  }

  processSelectionChangeToParentAndChildNodes(node);

  //expand selected
  if (props.expandSelected && node.selected === "selected") {
    node.expanded = true;
  }

  //emit the selected node names
  //optionally excluding selected childnodes
  emitSelection();
}

function emitSelection() {
  emit(
    "update:modelValue",
    Object.values(nodeMap.value)
      .filter(
        (node) =>
          node.selected === "selected" &&
          (props.emitSelectedChildren ||
            !node.parent ||
            nodeMap.value[node.parent].selected !== "selected")
      )
      .map((node) => node.name)
  );
}

/* manage expand */
function toggleExpand(node: ITreeNodeState) {
  nodeMap.value[node.name].expanded =
    nodeMap.value[node.name].expanded !== true;
}

/* manage search */
const showOptionsSearch = ref(false); //if the search for options input should be shown
const optionsSearch = ref(""); //to store the value of the search
watch(optionsSearch, (newValue, oldValue) => {
  if (newValue !== oldValue) {
    if (newValue) {
      //hide all and then make matches visible
      Object.values(nodeMap.value).forEach((node) => {
        node.visible = false;
        node.expanded = false;
        node.selectable = false;
      });
      const searchValue = optionsSearch.value.toLowerCase();
      rootNodes.value.forEach((node) => {
        applySearch(searchValue, node);
        //expand unless too many hits
        if (node.children.filter((child) => child.visible).length === 1)
          node.expanded = true;
        getAllChildren(node).forEach((child) => {
          if (child.children.filter((child2) => child2.visible).length === 1)
            child.expanded = true;
        });
      });
    } else {
      showOptionsSearch.value = false;
      Object.values(nodeMap.value).forEach((node) => {
        node.visible = true;
        node.expanded = false;
        node.selectable = true;
      });
    }
  }
});

function applySearch(searchValue: string, node: ITreeNodeState) {
  if (
    node.name.toLowerCase().includes(searchValue) ||
    node.description?.toLowerCase().includes(searchValue)
  ) {
    node.visible = true;
    node.selectable = true;
    getAllChildren(node).forEach((child) => {
      child.visible = true;
      child.selectable = true;
    });
    getAllParents(node).forEach((parent) => {
      parent.visible = true;
      //parents not selectable because might be incomplete, unless all children are visible
      if (!parent.children.some((child) => !child.visible)) {
        parent.selectable = true;
      }
    });
  } else {
    node.children.forEach((child) => applySearch(searchValue, child));
  }
}

function toggleSearch() {
  showOptionsSearch.value = !showOptionsSearch.value;
}

let timeoutID: number | NodeJS.Timeout | undefined = undefined;
function handleSearchInput(input: string) {
  clearTimeout(timeoutID);
  timeoutID = setTimeout(() => {
    optionsSearch.value = input;
  }, 500);
}

/* provide root nodes to be rendered */
const rootNodes = computed(() => {
  return Object.values(nodeMap.value).filter((node) => !node.parent);
});
</script>

<template>
  <ButtonText
    :id="`${id}-tree-search-button-toggle`"
    icon="Search"
    @click="toggleSearch"
    :aria-controls="`${id}-tree-search-input-container`"
    :aria-expanded="showOptionsSearch"
  >
    <span>Search for options</span>
  </ButtonText>
  <div v-if="showOptionsSearch" :id="`${id}-tree-search-input-container`">
    <label :for="`${id}-tree-search-input`" class="sr-only">search</label>
    <InputSearch
      :id="`${id}-tree-search-input`"
      :modelValue="optionsSearch"
      @update:modelValue="handleSearchInput"
      placeholder="Type to search in options..."
      :describedby="`${id}-tree-search-input-message`"
    />
    <div :id="`${id}-tree-search-input-message`">
      <span v-if="rootNodes.filter((node) => node.visible).length === 0">
        no results found
      </span>
    </div>
  </div>
  <TreeNode
    :id="id"
    :nodes="rootNodes"
    :inverted="inverted"
    :isRoot="true"
    @toggleSelect="toggleSelect"
    @toggleExpand="toggleExpand"
  />
</template>
