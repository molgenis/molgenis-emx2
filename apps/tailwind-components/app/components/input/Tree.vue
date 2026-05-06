<script setup lang="ts">
import type {
  ITreeNode,
  ITreeNodeState,
  SelectionState,
} from "../../../types/types";
import TreeNode from "./TreeNode.vue";
import { computed, ref, watch } from "vue";
import InputSearch from "./Search.vue";

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
    /* suppress internal search when search is handled by a parent component */
    disableInternalSearch?: boolean;
  }>(),
  {
    isMultiSelect: true,
    expandSelected: false,
    inverted: false,
    emitSelectedChildren: true,
    disableInternalSearch: false,
  }
);

const emit = defineEmits(["update:modelValue"]);

/* create node map for fast internal state management from props.nodes */
const nodeMap = ref({} as Record<string, ITreeNodeState>);
createNodeMap(props.nodes);
watch(
  () => props.nodes,
  (newValue) => {
    const expandedState: Record<string, boolean> = {};
    for (const [key, node] of Object.entries(nodeMap.value)) {
      expandedState[key] = node.expanded === true;
    }
    nodeMap.value = {};
    createNodeMap(newValue);
    for (const [key, wasExpanded] of Object.entries(expandedState)) {
      if (nodeMap.value[key]) nodeMap.value[key].expanded = wasExpanded;
    }
    applyModelValueChangeToSelection(props.modelValue);
  }
);

function createNodeMap(nodes: ITreeNode[]) {
  nodes.forEach((node) => {
    nodeMap.value[node.name] = clone(node);
  });
  autoExpandSmallTree();
}

function autoExpandSmallTree() {
  const totalNodes = Object.keys(nodeMap.value).length;
  if (totalNodes <= 25) {
    for (const node of Object.values(nodeMap.value)) {
      if (node.children && node.children.length > 0) {
        node.expanded = true;
      }
    }
  }
}

function clone(node: ITreeNode): ITreeNodeState {
  const result = {
    name: node.name,
    label: node.label,
    description: node.description,
    visible: true,
    children: [] as ITreeNodeState[],
    selected: "unselected" as SelectionState,
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
    if (node) {
      node.selected = "selected";
      processSelectionChangeToParentAndChildNodes(node);
    }
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
  if (!node.parent) return [];
  const parentNode = nodeMap.value[node.parent];
  if (!parentNode) return [];
  return [parentNode, ...getAllParents(parentNode)];
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
            nodeMap.value[node.parent]?.selected !== "selected")
      )
      .map((node) => node.name)
  );
}

/* manage expand */
function toggleExpand(node: ITreeNodeState) {
  const targetNode = nodeMap.value[node.name];
  if (targetNode) {
    targetNode.expanded = targetNode.expanded !== true;
  }
}

/* manage search */
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
        if (node.children.filter((child: any) => child.visible).length === 1)
          node.expanded = true;
        getAllChildren(node).forEach((child) => {
          if (
            child.children.filter((child2: any) => child2.visible).length === 1
          )
            child.expanded = true;
        });
      });
    } else {
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
      if (!parent.children.some((child: any) => !child.visible)) {
        parent.selectable = true;
      }
    });
  } else {
    node.children.forEach((child: any) => applySearch(searchValue, child));
  }
}

let timeoutID: number | NodeJS.Timeout | undefined = undefined;
function handleSearchInput(input: string | number | undefined) {
  clearTimeout(timeoutID);
  timeoutID = setTimeout(() => {
    optionsSearch.value = input == null ? "" : String(input);
  }, 500);
}

/* provide root nodes to be rendered */
const rootNodes = computed(() => {
  return Object.values(nodeMap.value).filter((node) => !node.parent);
});

const virtualRootNode = computed<ITreeNodeState>(() => ({
  name: "__root__",
  label: "Root",
  visible: true,
  children: rootNodes.value,
  selected: "unselected",
  expanded: true,
  selectable: false,
}));

function countAllNodes(nodes: ITreeNode[]): number {
  return nodes.reduce(
    (sum, node) => sum + 1 + countAllNodes(node.children ?? []),
    0
  );
}

function hasAnyChildren(nodes: ITreeNode[]): boolean {
  return nodes.some((node) => (node.children?.length ?? 0) > 0);
}

const showSearch = computed(
  () =>
    !props.disableInternalSearch &&
    (countAllNodes(props.nodes) > 25 || hasAnyChildren(props.nodes))
);
</script>

<template>
  <div v-if="showSearch" :id="`${id}-tree-search-input-container`">
    <label :for="`${id}-tree-search-input`" class="sr-only"
      >Search options</label
    >
    <InputSearch
      :id="`${id}-tree-search-input`"
      :modelValue="optionsSearch"
      @update:modelValue="handleSearchInput"
      placeholder="Search..."
      :describedby="`${id}-tree-search-input-message`"
      size="tiny"
    />
    <div :id="`${id}-tree-search-input-message`">
      <span
        v-if="
          optionsSearch && rootNodes.filter((node) => node.visible).length === 0
        "
      >
        no results found
      </span>
    </div>
  </div>
  <TreeNode
    :id="id"
    :parentNode="virtualRootNode"
    :inverted="inverted"
    :isRoot="true"
    @toggleSelect="toggleSelect"
    @toggleExpand="toggleExpand"
  />
</template>
