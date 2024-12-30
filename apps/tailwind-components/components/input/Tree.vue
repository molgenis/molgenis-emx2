<script setup lang="ts">
import type { ITreeNode, ITreeNodeState } from "~/types/types";
import TreeNode from "./TreeNode.vue";

const props = withDefaults(
  defineProps<{
    nodes: ITreeNode[];
    modelValue: string[];
    isMultiSelect?: boolean;
    expandSelected?: boolean;
    isRoot?: boolean;
    inverted?: boolean;
    /* if when a node is selected also its children should be included in the emit*/
    emitSelectedChildren?: boolean;
    /* change layout to fit mobile layout*/
    mobileDisplay: boolean;
  }>(),
  {
    isMultiSelect: true,
    expandSelected: false,
    isRoot: true,
    inverted: false,
    includeSelectedChildren: false,
    mobileDisplay: false,
  }
);

//internal nodeMap for rapid manipulations of the nodes in the tree
const nodeMap = ref({} as Record<string, ITreeNodeState>);

const getAllChildren = (child: ITreeNodeState): ITreeNodeState[] => [
  child,
  ...(child.children || []).flatMap(getAllChildren),
];

const getAllParents = (node: ITreeNodeState): ITreeNodeState[] =>
  node.parent
    ? [nodeMap.value[node.parent], ...getAllParents(nodeMap.value[node.parent])]
    : [];

const clone = (node: ITreeNode): ITreeNodeState => {
  const result = {
    name: node.name,
    description: node.description,
    visible: true,
    children: [] as ITreeNodeState[],
    expanded: false,
  };
  node.children?.forEach((child) => {
    const copy = clone(child);
    copy.parent = node.name;
    nodeMap.value[child.name] = copy;
    result.children.push(copy);
  });
  return result;
};

props.nodes.forEach((node) => {
  nodeMap.value[node.name] = clone(node);
});

const emit = defineEmits(["update:modelValue"]);

function toggleExpand(name: string) {
  nodeMap.value[name].expanded = nodeMap.value[name].expanded !== true;
}

function updateParentsAndChildrenSelection(node: ITreeNodeState) {
  //make child selection match selection state
  getAllChildren(node).forEach((child) => {
    child.selected = node.selected;
    child.visible = true; //in search you want to see effect of selecting
  });

  //update parents
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

function toggleSelect(name: string) {
  //toggle select of the named node
  const node = nodeMap.value[name];
  const previousState = node.selected;
  if (node) {
    if (previousState !== "selected") {
      node.selected = "selected";
    } else {
      node.selected = "unselected";
    }
  }

  updateParentsAndChildrenSelection(node);

  //expand selected
  if (props.expandSelected && node.selected === "selected") {
    node.expanded = true;
  }

  //emit the selected node names
  //optionally excluding selected childnodes
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

//apply modelValue
function applySelection(selection: string[]) {
  Object.values(nodeMap.value).forEach(
    (node) => (node.selected = "unselected")
  );
  selection.forEach((name) => {
    const node = nodeMap.value[name];
    node.selected = "selected";
    updateParentsAndChildrenSelection(node);
  });
}

watch(
  () => props.modelValue,
  (newValue) => {
    applySelection(newValue);
  }
);

onMounted(() => {
  applySelection(props.modelValue);
});

const rootNodes = computed(() => {
  return Object.values(nodeMap.value).filter((node) => !node.parent);
});

const showOptionsSearch = ref(false);
const optionsSearch = ref("");
function applySearch(searchValue: string, node: ITreeNodeState) {
  if (
    node.name.toLowerCase().includes(searchValue) ||
    node.description?.toLowerCase().includes(searchValue)
  ) {
    node.visible = true;
    getAllChildren(node).forEach((child) => (child.visible = true));
    getAllParents(node).forEach((parent) => {
      parent.visible = true;
    });
  } else {
    node.children.forEach((child) => applySearch(searchValue, child));
  }
}
watch(optionsSearch, (newValue, oldValue) => {
  if (newValue !== oldValue) {
    if (newValue) {
      //hide all and then make matches visible
      Object.values(nodeMap.value).forEach((node) => {
        node.visible = false;
        node.expanded = false;
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
      });
    }
  }
});

let timeoutID: number | NodeJS.Timeout | undefined = undefined;
function handleInput(input: string) {
  clearTimeout(timeoutID);
  timeoutID = setTimeout(() => {
    optionsSearch.value = input;
  }, 500);
}

function toggleSearch() {
  showOptionsSearch.value = !showOptionsSearch.value;
}
</script>

<template>
  <button
    v-if="!showOptionsSearch"
    class="flex items-center ml-6"
    @click="toggleSearch"
  >
    <BaseIcon
      name="search"
      :class="`text-search-filter-expand${mobileDisplay ? '-mobile' : ''}`"
      :width="18"
    />
    <span
      class="ml-2 text-body-sm hover:underline"
      :class="`text-search-filter-expand${mobileDisplay ? '-mobile' : ''}`"
    >
      Search for options
    </span>
  </button>
  <input
    v-else
    :value="optionsSearch"
    @input="(event) => handleInput((event.target as HTMLInputElement).value)"
    type="search"
    class="w-full pr-4 font-sans text-black text-gray-300 outline-none rounded-search-input h-10 ring-red-500 pl-3 shadow-search-input focus:shadow-search-input hover:shadow-search-input search-input-mobile border"
    placeholder="Type to search in options..."
  />
  <span v-if="rootNodes.filter((node) => node.visible).length === 0"
    >no results found</span
  >
  <TreeNode
    :nodes="rootNodes"
    :inverted="inverted"
    :isRoot="true"
    @toggleSelect="toggleSelect"
    @toggleExpand="toggleExpand"
  />
</template>
