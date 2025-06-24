<script setup lang="ts">
import { fetchOntology } from "../../composables/fetchOntology";
import type { ITreeNode } from "../../../tailwind-components/types/types";
import type {
  IFilter,
  IFilterCondition,
  IOntologyRespItem,
} from "../../interfaces/types";
import { computed, useId } from "vue";

const props = withDefaults(
  defineProps<{
    tableId: string;
    filter: Record<string, IFilter>;
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
  set(newValue: any[]) {
    // transform the names back to the original data structure for use in gql query
    const newConditions = newValue.map((name) => ({ name: name }));
    emit("update:modelValue", newConditions);
  },
});

const id = useId();
</script>

<template>
  <InputTree
    :id="id"
    :nodes="rootNodes"
    v-model="(selectedNodesNames as string[])"
    :isMultiSelect="true"
    :inverted="mobileDisplay"
    :expandSelected="true"
    :emitSelectedChildren="true"
  >
  </InputTree>
</template>
