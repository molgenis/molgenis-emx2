<script setup lang="ts">
import type { IInputProps, ITreeNodeState } from "~/types/types";
import TreeNode from "~/components/input/TreeNode.vue";
import type { Ref } from "vue";

const props = defineProps<
  IInputProps & {
    isArray?: boolean;
    ontologySchemaId: string;
    ontologyTableId: string;
    filter?: any;
  }
>();
const emit = defineEmits(["focus", "blur"]);
//the selected values
const modelValue = defineModel<string[] | string>();
//labels for the selected values
const valueLabels: Ref<Record<string, string>> = ref({});
//state of the tree that is shown
const ontologyTree: Ref<ITreeNodeState[]> = ref([]);
//intermediate selected values
const intermediates: Ref<string[]> = ref([]);
//toggle for showing search
const showSearch = ref<boolean>(false);
// the search value
const searchTerms: Ref<string> = ref("");
//initial loading state
const initLoading = ref(true);

onMounted(() => {
  init();
  initLoading.value = false;
});
watch(() => props.ontologySchemaId, init);
watch(() => props.ontologyTableId, init);
watch(() => modelValue.value, applySelectedStates);

/* retrieves terms, optionally as children to a parent */
async function retrieveTerms(
  parentNode: ITreeNodeState | undefined = undefined
): Promise<ITreeNodeState[]> {
  const variables: any = {
    termFilter: Object.assign(
      props.filter || {},
      parentNode
        ? { parent: { name: { equals: parentNode.name } } }
        : { parent: { _is_null: true } }
    ),
  };

  if (searchTerms.value) {
    variables.searchFilter = Object.assign({}, variables.termFilter, {
      _search_including_parents: searchTerms.value,
    });
  }

  let query = searchTerms.value
    ? `query myquery($termFilter:${props.ontologyTableId}Filter, $searchFilter:${props.ontologyTableId}Filter) {
        retrieveTerms: ${props.ontologyTableId}(filter:$termFilter, limit:1000, orderby:{order:ASC,name:ASC}){name,label,definition,code,codesystem,ontologyTermURI,children(limit:1){name}}
        searchMatch: ${props.ontologyTableId}(filter:$searchFilter, limit:1000, orderby:{order:ASC,name:ASC}){name}
       }`
    : `query myquery($termFilter:${props.ontologyTableId}Filter) {
        retrieveTerms: ${props.ontologyTableId}(filter:$termFilter, limit:1000, orderby:{order:ASC,name:ASC}){name,label,definition,code,codesystem,ontologyTermURI,children(limit:1){name}}
       }`;

  const data = await fetchGraphql(props.ontologySchemaId, query, variables);

  return data.retrieveTerms.map((row: any) => {
    return {
      name: row.name,
      parentNode: parentNode,
      label: row.label,
      description: row.definition,
      code: row.code,
      codeSystem: row.codeSystem,
      uri: row.ontologyTermURI,
      selectable: true,
      children: row.children,
      //visibility is used for search hiding
      visible: searchTerms.value
        ? data.searchMatch?.some(
            (match: boolean) => (match as any).name === row.name
          ) || false
        : true,
    };
  });
}

async function retrieveSelectedPathsAndLabelsForModelValue(): Promise<void> {
  if (
    props.isArray && Array.isArray(modelValue.value)
      ? modelValue.value.length === 0
      : !modelValue.value
  ) {
    valueLabels.value = {};
    intermediates.value = [];
  } else {
    const graphqlFilter = {
      _match_any_including_parents: modelValue.value,
    };
    const data = await fetchGraphql(
      props.ontologySchemaId,
      `query ontologyPaths($filter:${props.ontologyTableId}Filter) {ontologyPaths: ${props.ontologyTableId}(filter:$filter,limit:1000){name,label}}`,
      {
        filter: graphqlFilter,
      }
    );
    valueLabels.value = Object.fromEntries(
      data.ontologyPaths.map((row: any) => [row.name, row.label || row.name])
    );
    intermediates.value = data.ontologyPaths.map(
      (term: { name: string }) => term.name
    );
  }
}

/** initial load */
async function init() {
  ontologyTree.value = await retrieveTerms();
  await applySelectedStates();
}

/** apply selection UI state on selection changes */
async function applySelectedStates() {
  await retrieveSelectedPathsAndLabelsForModelValue();
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
    getAllChildren(node).forEach((child) => (child.selected = "selected"));
  } else if (intermediates.value.includes(node.name)) {
    node.selected = "intermediate";
    node.children?.forEach((child) => applyStateToNode(child));
  } else {
    node.selected = "unselected";
    getAllChildren(node).forEach((child) => (child.selected = "unselected"));
  }
}

function getAllChildren(node: ITreeNodeState): ITreeNodeState[] {
  const result: ITreeNodeState[] = [];
  node.children?.forEach((child) =>
    result.push(child, ...getAllChildren(child))
  );
  return result;
}

function getAllParents(node: ITreeNodeState): ITreeNodeState[] {
  if (node.parentNode) {
    return [node.parentNode, ...getAllParents(node.parentNode)];
  } else {
    return [];
  }
}

function toggleSelect(node: ITreeNodeState) {
  if (props.disabled) return;
  if (!props.isArray) {
    modelValue.value = modelValue.value === node.name ? undefined : node.name;
  } else if (Array.isArray(modelValue.value)) {
    //if a selected value then simply deselect
    if (modelValue.value.includes(node.name)) {
      modelValue.value = modelValue.value.filter(
        (value) => value !== node.name
      );
    }
    //if deselection of a node in a selected parent
    //then select all siblings except current node
    //recursively!
    else if (
      node.parentNode &&
      getAllParents(node).some((parent) =>
        modelValue.value?.includes(parent.name)
      )
    ) {
      console.log("here");
      const itemsToBeRemoved = [
        node.name,
        ...getAllParents(node).map((parent) => parent.name),
      ];
      const itemsToBeAdded: string[] = getAllParents(node)
        .map((parent) =>
          parent.selected === "selected"
            ? parent.children.map((child) => child.name)
            : []
        )
        .flat();
      //remove parent node from select and add all siblings
      modelValue.value = [...modelValue.value, ...itemsToBeAdded].filter(
        (name) => !itemsToBeRemoved.includes(name)
      );
    }
    // if we select last selected child
    // then we need toggle select on parent instead
    else if (
      node.parentNode &&
      node.parentNode.children
        .filter((child) => child.name != node.name)
        .every((child) => child.selected === "selected")
    ) {
      toggleSelect(node.parentNode);
    }
    // if we simply select a node
    // then make sure to deselect all its children
    else {
      const itemsToBeRemoved: string[] = getAllChildren(node).map(
        (child) => child.name
      );
      modelValue.value = [
        ...modelValue.value.filter(
          (value) => !itemsToBeRemoved.includes(value)
        ),
        node.name,
      ];
    }
  }
  emit("focus");
}

async function toggleExpand(node: ITreeNodeState) {
  if (!node.expanded) {
    const children = await retrieveTerms(node);
    node.children = children.map((child) => {
      return {
        name: child.name,
        label: child.label,
        code: child.code,
        codeSystem: child.codesystem,
        uri: child.uri,
        description: child.description,
        visible: child.visible,
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
    await applySelectedStates();
  } else {
    node.expanded = false;
  }
}

function deselect(name: string) {
  if (props.disabled) return;
  if (props.isArray && Array.isArray(modelValue.value)) {
    modelValue.value = modelValue.value.filter((value) => value != name);
  } else {
    modelValue.value = undefined;
  }
}

function clearSelection() {
  if (props.disabled) return;
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
  <div v-if="initLoading">
    <BaseIcon name="progress-activity" />
  </div>
  <div v-else>
    <InputGroupContainer
      :id="`${id}-checkbox-group`"
      class="border-l-4 border-transparent"
      @blur="emit('blur')"
      @focus="emit('focus')"
    >
      <div
        class="flex flex-wrap gap-2 mb-2 max-h-[300px] overflow-y-auto"
        v-if="Object.keys(valueLabels).length > 0"
      >
        <Button
          v-for="name in Array.isArray(modelValue)
            ? (modelValue as string[]).sort()
            : modelValue ? [modelValue] : []"
          icon="cross"
          iconPosition="right"
          type="filterWell"
          size="tiny"
          @click="deselect(name as string)"
        >
          {{ valueLabels[name] }}
        </Button>
      </div>
      <div class="flex flex-wrap gap-2 mb-2">
        <InputLabel :for="`search-for-${id}`" class="sr-only">
          search in ontology
        </InputLabel>
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
          placeholder="Search in terms"
          :aria-hidden="!showSearch"
        />
      </div>
      <fieldset>
        <legend class="sr-only">select ontology terms</legend>
        <TreeNode
          :id="id"
          :nodes="ontologyTree"
          :isRoot="true"
          :valid="valid"
          :invalid="invalid"
          :disabled="disabled"
          @toggleExpand="toggleExpand"
          @toggleSelect="toggleSelect"
          class="border-l-2 border-transparent pl-4 pb-2 max-h-[500px] overflow-y-auto"
          :class="{
            'border-l-invalid': invalid,
            'border-l-valid': valid,
          }"
        />
      </fieldset>
    </InputGroupContainer>
  </div>
</template>
