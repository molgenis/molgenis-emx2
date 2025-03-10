<script setup lang="ts">
import type { IInputProps, ITreeNodeState } from "~/types/types";
import TreeNode from "~/components/input/TreeNode.vue";
import type { Ref } from "vue";

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
//intermediate selected values
const intermediates: Ref<string[]> = ref([]);
//toggle for showing search
const showSearch = ref<boolean>(false);
// the search value
const searchTerms: Ref<string> = ref("");

onMounted(init);
watch(() => props.ontologySchemaId, init);
watch(() => props.ontologyTableId, init);
watch(() => modelValue.value, applySelectedStates);

/* retrieves terms, optionally as children to a parent */
async function retrieveTerms(
  parentNode: ITreeNodeState | undefined = undefined
): Promise<ITreeNodeState[]> {
  //todo for later: add a 'loaded' flag so we can skip loading if loaded before
  let graphqlFilter: any = parentNode
    ? { parent: { name: { equals: parentNode.name } } }
    : { parent: { _is_null: true } };

  console.log("retrieve " + searchTerms.value);

  if (searchTerms.value) {
    graphqlFilter._like_including_parents = searchTerms.value;
    //todo: need search including parents so I can get the paths. TODO
  }

  const data = await fetchGraphql(
    props.ontologySchemaId,
    `query ${props.ontologyTableId}($filter:${props.ontologyTableId}Filter) {${props.ontologyTableId}(filter:$filter, limit:1000, orderby:{name:ASC}){name,children(limit:1){name}}}`,
    {
      filter: graphqlFilter,
    }
  );

  return data[props.ontologyTableId].map((row: any) => {
    return {
      name: row.name,
      parentNode: parentNode,
      selectable: true,
      children: row.children,
      visible: true, //visible is silly
    };
  });
}

async function retrieveSelectedPathsForModelValue(): Promise<string[]> {
  if (
    props.isArray && Array.isArray(modelValue.value)
      ? modelValue.value.length === 0
      : !modelValue.value
  ) {
    return [];
  }
  const graphqlFilter = {
    _match_any_including_parents: modelValue.value,
  };
  const data = await fetchGraphql(
    props.ontologySchemaId,
    `query ${props.ontologyTableId}($filter:${props.ontologyTableId}Filter) {${props.ontologyTableId}(filter:$filter,limit:1000){name}}`,
    {
      filter: graphqlFilter,
    }
  );

  return data[props.ontologyTableId].map((term: { name: string }) => term.name);
}

/** initial load */
async function init() {
  ontologyTree.value = await retrieveTerms();
  applySelectedStates();
}

/** apply selection UI state on selection changes */
async function applySelectedStates() {
  intermediates.value = await retrieveSelectedPathsForModelValue();
  ontologyTree.value.forEach((term) => {
    applyStateToNode(term);
  });
}

function applyStateToNode(node: ITreeNodeState): void {
  if (
    props.isArray
      ? modelValue.value?.includes(node.name)
      : modelValue.value === node.name
  ) {
    node.selected = "selected";
    //todo: to make sure all children from modelValue?
    getAllChildren(node).forEach((child) => (child.selected = "selected"));
  } else if (intermediates.value.includes(node.name)) {
    node.selected = "intermediate";
    node.children.forEach((child) => applyStateToNode(child));
  } else {
    node.selected = "unselected";
    getAllChildren(node).forEach((child) => (child.selected = "unselected"));
  }
}

function getAllChildren(node: ITreeNodeState): ITreeNodeState[] {
  const result: ITreeNodeState[] = node.children || [];
  node.children?.forEach((child) => result.push(...getAllChildren(child)));
  return result;
}

function toggleSelect(node: ITreeNodeState) {
  if (!props.isArray) {
    modelValue.value = modelValue.value === node.name ? undefined : node.name;
  } else if (Array.isArray(modelValue.value)) {
    //deselect directly
    if (modelValue.value.includes(node.name)) {
      modelValue.value = modelValue.value.filter(
        (value) => value !== node.name
      );
    } else {
      //if last child to be selected, we should deselect all siblings and select parent
      const itemsToBeAdded: string[] = [];
      const itemsToBeRemoved: string[] = []; //deselection
      if (
        node.parentNode &&
        node.parentNode.children.every((child) => child.selected === "selected")
      ) {
        console.log(
          "when all sibling selected then deselect parent, select siblings"
        );
        itemsToBeAdded.push(
          ...node.parentNode.children
            .map((node) => node.name)
            .filter((name) => node.name !== name)
        );
        itemsToBeRemoved.push(node.parentNode.name);
        modelValue.value = [
          ...modelValue.value.filter(
            (value) => !itemsToBeRemoved.includes(value)
          ),
          ...itemsToBeAdded,
        ];
      } else {
        node.selected = "selected";
        if (
          node.parentNode &&
          node.parentNode.children.every(
            (child) => child.selected === "selected"
          )
        ) {
          console.log(
            "when last sibling selected then select parent, deselect siblings"
          );
          itemsToBeRemoved.push(
            ...node.parentNode.children.map((node) => node.name)
          );
          itemsToBeAdded.push(node.parentNode.name);
          modelValue.value = [
            ...modelValue.value.filter(
              (value) => !itemsToBeRemoved.includes(value)
            ),
            ...itemsToBeAdded,
          ];
        } else {
          modelValue.value = [...modelValue.value, node.name];
        }
      }
    }
  }
  //ui selection state will be updated via watch on modelValue
}

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

function deselect(name: string) {
  if (props.isArray && Array.isArray(modelValue.value)) {
    modelValue.value = modelValue.value.filter((value) => value != name);
  } else {
    modelValue.value = undefined;
  }
}

function clearSelection() {
  modelValue.value = props.isArray ? [] : undefined;
}

function toggleSearch() {
  showSearch.value = !showSearch.value;
  if (!showSearch.value) init();
}

async function updateSearch(value: string) {
  searchTerms.value = value;
  await init();
}
</script>

<template>
  <div>
    <div
      class="flex flex-wrap gap-2 mb-2"
      v-if="Array.isArray(modelValue) ? modelValue.length : modelValue"
    >
      <Button
        v-for="label in Array.isArray(modelValue) ? modelValue : [modelValue]"
        icon="cross"
        iconPosition="right"
        type="filterWell"
        size="tiny"
        @click="deselect(label as string)"
      >
        {{ label }}
      </Button>
    </div>
    <div class="flex flex-wrap gap-2 mb-2">
      <ButtonText @click="toggleSearch" :aria-controls="`search-for-${id}`">
        Search
      </ButtonText>
      <ButtonText @click="clearSelection"> Clear all </ButtonText>
      <InputSearch
        v-if="showSearch"
        :id="`search-for-${id}`"
        :modelValue="searchTerms"
        @update:modelValue="updateSearch"
        class="mb-2"
        :placeholder="`Search in terms`"
        :aria-hidden="!showSearch"
      />
    </div>
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
