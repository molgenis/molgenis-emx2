<script setup lang="ts">
import type { Modal } from "#build/components";
import type { ITreeNode } from "../../../tailwind-components/types/types";
import type { IFilterCondition, IOntologyRespItem } from "~/interfaces/types";
import { nodeArray } from "happy-dom/lib/PropertySymbol.d.ts.js";

const props = withDefaults(
  defineProps<{
    tableId: string;
    filter: Record<String, Filter>;
    modelValue: IFilterCondition[];
    options?: IOntologyRespItem[];
    isMultiSelect?: boolean;
    mobileDisplay: boolean;
    filterLabel: string;
  }>(),
  {
    isMultiSelect: true,
    descriptionField: undefined,
  }
);

const emit = defineEmits(["update:modelValue"]);

const data = !props.options
  ? (await fetchOntology(props.tableId, { filter: props.filter })).data[
      props.tableId
    ]
  : props.options;

const showSearch = computed(() => data?.length > 10);

function listToTree(list: IOntologyRespItem[]): ITreeNode[] {
  //create hash map so we don't need to use 'of 'find', much faster
  const allNodes = {} as Record<string, ITreeNode>;
  list.forEach((repsElement: IOntologyRespItem) => {
    allNodes[repsElement.name] = {
      name: repsElement.name,
      description: repsElement.definition,
      parent: repsElement.parent?.name,
      children: [] as ITreeNode[],
    };
  });

  //get all children connected
  Object.values(allNodes).forEach((node) => {
    if (node.parent) {
      const parent = allNodes[node.parent];
      if (parent) {
        parent.children.push(node);
      }
    }
  });

  console.log("listToTree");
  return Object.values(allNodes).filter((n) => !n.parent);
}

const rootNodes = computed(() => listToTree(data));

const selectedNodesNames = computed({
  get() {
    //current state
    return props.modelValue ? props.modelValue.map((n) => n.name) : [];
  },
  set(newValue) {
    // transform the names back to the original data structure for use in gql query
    const newConditions = newValue.map((name) => ({ name: name }));
    emit("update:modelValue", newConditions);
  },
});

const optionsFilter = ref("");

const filteredNodes = computed(() => {
  //convert into hash map first, much faster than using 'find'
  const map = {} as Record<string, IOntologyRespItem>;
  data.forEach((term) => {
    map[term.name] = term;
  });
  const parents: Set<IOntologyRespItem> = new Set();

  const filteredNodes = data.filter((node) => {
    const searchValue = optionsFilter.value.toLowerCase();

    if (
      node.name.toLowerCase().includes(searchValue) ||
      node.definition?.toLowerCase().includes(searchValue)
    ) {
      if (node.parent) {
        //get also the parents
        parents.add(map[node.parent.name]);
      }
      return true;
    }
  });

  return Array.from(new Set([...parents, ...filteredNodes]));
});

const filteredTree = computed(() => listToTree(filteredNodes.value));

const modal = ref<InstanceType<typeof Modal>>();

function showModal() {
  modal.value?.show();
}

function closeModal() {
  optionsFilter.value = "";
  modal.value?.close();
}

function removeSelectedNode(node: string) {
  selectedNodesNames.value = selectedNodesNames.value.filter((n) => n !== node);
}

function clearAll() {
  selectedNodesNames.value = [];
}
</script>

<template>
  <InputTree
    :nodes="rootNodes"
    v-model="selectedNodesNames as string[]"
    :mobile-display="mobileDisplay"
    :isMultiSelect="true"
    :inverted="mobileDisplay"
    :expandSelected="true"
  >
  </InputTree>
</template>
