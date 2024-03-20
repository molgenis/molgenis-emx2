<script setup lang="ts">
import type { ITreeNode } from "../../../tailwind-components/types/types";
import type { IOntologyRespItem } from "~/interfaces/types";

const props = withDefaults(
  defineProps<{
    tableId: string;
    modelValue: { name: string }[];
    options?: ITreeNode[];
    isMultiSelect?: boolean;
    mobileDisplay: boolean;
  }>(),
  {
    isMultiSelect: true,
    descriptionField: undefined,
  }
);

const emit = defineEmits(["update:modelValue"]);

let data: any[] = [];
if (!props.options) {
  const resp = await fetchOntology(props.tableId);

  data = resp?.data[props.tableId];
  let count = resp?.data[props.tableId + "_agg"].count;
} else {
  data = props.options;
}

function listToTree(list: IOntologyRespItem[]): ITreeNode[] {
  const allNodes = list.map((repsElement: IOntologyRespItem) => {
    return {
      name: repsElement.name,
      description: repsElement.definition,
      parent: repsElement.parent?.name,
      children: [] as ITreeNode[],
    };
  });

  for (let i = 0; i < allNodes.length; i++) {
    const node = allNodes[i];
    if (node.parent) {
      const parent = allNodes.find((n) => n.name === node.parent);
      if (parent) {
        parent.children.push(node);
      }
    }
  }

  return allNodes.filter((n) => !n.parent);
}

const rootNodes = computed(() => listToTree(data));

const selectedNodesNames = computed({
  get() {
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
    :isMultiSelect="true"
    :mobileDisplay="mobileDisplay"
    :expandSelected="true"
  >
  </InputTree>
</template>
