<script setup lang="ts">
import type { IInputProps, ITreeNodeState } from "~/types/types";
import TreeNode from "~/components/input/TreeNode.vue";

const props = defineProps<
  IInputProps & {
    isArray?: boolean;
    //todo: do we need to change to radio button instead of checkboxes when !isArray?
    ontologySchemaId: string;
    ontologyTableId: string;
  }
>();
//the selected values
const modelValue = defineModel<string[] | string>();
//state of the tree that is shown
const ontologyTree: Ref<ITreeNodeState[]> = ref([]);

onMounted(init);
watch(() => props.ontologySchemaId, init);
watch(() => props.ontologyTableId, init);

/* retrieves terms, optionally as children to a parent */
async function retrieveTerms(
  parentNode: ITreeNodeState | undefined = undefined
): Promise<ITreeNodeState[]> {
  //todo for later: add a 'loaded' flag so we can skip loading if loaded before

  const graphqlFilter = parentNode
    ? { parent: { name: { equals: parentNode.name } } }
    : { parent: { _is_null: true } };

  //todo: replace with custom query, now it retrieves too much
  const result = await fetchTableData(
    props.ontologySchemaId,
    props.ontologyTableId,
    {
      filter: graphqlFilter,
      expandLevel: 1,
      orderby: { name: "ASC" },
      limit: 10000,
    }
  );

  return result.rows?.map((row) => {
    return {
      name: row.name,
      parentNode: parentNode,
      selectable: true,
      children: row.children,
      visible: true, //visible is silly
    };
  });
}

async function retrieveIntermediateTermNamesForModelValue(): Promise<string[]> {
  const graphqlFilter = {
    _match_any_including_parents: modelValue.value,
    parent: { _is_null: true },
  };
  //todo: replace with custom query, now it retrieves too much
  const result = await fetchTableData(
    props.ontologySchemaId,
    props.ontologyTableId,
    {
      filter: graphqlFilter,
      expandLevel: 0,
      limit: 10000,
    }
  );
  console.log(result.rows);
  return result.rows?.map((row) => row.name);
}

/** initial load */
async function init() {
  ontologyTree.value = await retrieveTerms();

  //apply intermediate selected state
  const intermediates = await retrieveIntermediateTermNamesForModelValue();
  ontologyTree.value
    .filter((term) => intermediates.includes(term.name))
    .forEach((term) => (term.selected = "intermediate"));

  //apply selection
  ontologyTree.value
    .filter((term) =>
      props.isArray
        ? modelValue.value?.includes(term.name)
        : modelValue.value === term.name
    )
    .forEach((term) => (term.selected = "selected"));
}

function getParents(node: ITreeNodeState): ITreeNodeState[] {
  if (node.parentNode) {
    return [node.parentNode, ...getParents(node.parentNode)];
  } else {
    return [];
  }
}

function toggleSelect(node: ITreeNodeState) {
  console.log("toggle select: " + node.name);
  if (!props.isArray) {
    //deselect else first
    ontologyTree.value.forEach((term) => {
      if (node.name !== term.name) term.selected = "unselected";
      getAllChildren(term).forEach((child) => {
        if (node.name !== child.name) {
          child.selected = "unselected";
        }
      });
    });
  }
  if (node.selected == "selected") {
    //update UI state
    node.selected = "unselected";
    const itemsToBeRemoved: string[] = [node.name]; //deselection
    const itemsToBeAdded: string[] = []; //adding siblings selection in case of array
    // unselect all children
    getAllChildren(node).forEach((child) => {
      child.selected = "unselected";
      itemsToBeRemoved.push(child.name);
    });
    //if all siblings are unselected then remove 'intermediate' from parent
    if (
      node.parentNode &&
      !node.parentNode.children.some((child) => child.selected === "selected")
    ) {
      getParents(node).forEach((parentNode) => {
        parentNode.selected = "unselected";
        itemsToBeRemoved.push(parentNode.name);
      });
    }
    //else apply intermediate to parent
    else {
      getParents(node).forEach((parentNode) => {
        parentNode.selected = "intermediate";
        itemsToBeRemoved.push(parentNode.name);
        itemsToBeAdded.push(
          ...parentNode.children
            .filter((child) => child.selected === "selected")
            .map((child) => child.name)
        );
      });
    }
    //update modelValue
    if (props.isArray && Array.isArray(modelValue.value)) {
      modelValue.value = [
        ...new Set([
          ...modelValue.value.filter(
            (value) => !itemsToBeRemoved.includes(value)
          ),
          ...itemsToBeAdded,
        ]),
      ];
    } else {
      modelValue.value = undefined;
    }
  } else {
    //if all siblings are selected, select the parent instead
    node.selected = "selected";
    if (
      node.parentNode &&
      node.parentNode.children.every((child) => child.selected === "selected")
    ) {
      console.log("one");
      return toggleSelect(node.parentNode);
    } else {
      console.log("two");

      //mark all parents as intermediate
      getParents(node).forEach(
        (parentNode) => (parentNode.selected = "intermediate")
      );
      //mark all children loaded children as selected
      if (props.isArray) {
        getAllChildren(node).forEach((child) => (child.selected = "selected"));
      }
      //update modelValue
      if (props.isArray && Array.isArray(modelValue.value)) {
        const childrenToBeRemoved =
          node.children?.map((child) => child.name) || [];
        modelValue.value = [
          node.name,
          ...new Set([
            ...modelValue.value.filter(
              (value) => !childrenToBeRemoved.includes(value)
            ),
          ]),
        ];
      } else {
        modelValue.value = node.name;
      }
    }
  }
}

const getAllChildren = (node: ITreeNodeState): ITreeNodeState[] => {
  let result: ITreeNodeState[] = [];
  if (node.children) {
    for (const child of node.children) {
      result.push(child, ...getAllChildren(child));
    }
  }
  return result;
};

async function toggleExpand(node: ITreeNodeState) {
  if (!node.expanded) {
    const children = await retrieveTerms(node);
    node.children = children.map((child) => {
      return {
        name: child.name,
        visible: true,
        children: child.children,
        selected: props.isArray
          ? modelValue.value?.includes(child.name)
            ? "selected"
            : node.selected
          : modelValue.value === child.name
          ? "selected"
          : "unselected",
        selectable: true,
        parentNode: node,
      };
    });
    node.expanded = true;
  } else {
    node.expanded = false;
  }
}
</script>

<template>
  <div>
    <TreeNode
      :id="id"
      :nodes="ontologyTree"
      :isRoot="true"
      @toggleExpand="toggleExpand"
      @toggleSelect="toggleSelect"
    />
    <pre></pre>
  </div>
</template>
