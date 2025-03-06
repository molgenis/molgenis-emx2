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
    node.selected = "unselected";
    // unselect all children
    getAllChildren(node).forEach((child) => (child.selected = "unselected"));
    //if all siblings are unselected then remove 'intermediate' from parent
    if (
      node.parentNode &&
      !node.parentNode.children.some((child) => child.selected === "selected")
    ) {
      let parentNode: ITreeNodeState | undefined = node.parentNode;
      while (parentNode) {
        parentNode.selected = "unselected";
        parentNode = parentNode.parentNode;
      }
    }
    //else apply intermediate to parent
    else {
      let parentNode: ITreeNodeState | undefined = node.parentNode;
      while (parentNode) {
        parentNode.selected = "intermediate";
        parentNode = parentNode.parentNode;
      }
    }
  } else {
    //if all siblings are selected, select the parent instead
    node.selected = "selected";
    if (
      node.parentNode &&
      node.parentNode.children.every((child) => child.selected === "selected")
    ) {
      console.log("should set selected on parent because all selected");
      //remove all children from the selection

      //and instead select the parent
      toggleSelect(node.parentNode);
    } else {
      //mark all parents as intermediate
      let parentNode = node.parentNode;
      while (parentNode) {
        console.log("set parent to intermediate " + parentNode.name);
        parentNode.selected = "intermediate";
        parentNode = parentNode.parentNode;
      }
      //mark all children loaded children as selected
      if (props.isArray)
        getAllChildren(node).forEach((child) => (child.selected = "selected"));
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
