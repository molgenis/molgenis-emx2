<script setup lang="ts">
import type { Modal } from "#build/components";
import type { ITreeNode } from "../../../tailwind-components/types/types";
import type { IFilterCondition, IOntologyRespItem } from "~/interfaces/types";

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
</script>

<template>
  <InputTree
    :nodes="rootNodes"
    v-model="selectedNodesNames"
    :mobile-display="mobileDisplay"
    :isMultiSelect="true"
    :inverted="mobileDisplay"
    :expandSelected="true"
  >
  </InputTree>
</template>
