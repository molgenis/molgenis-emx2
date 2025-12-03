<script setup lang="ts">
import { fetchGraphql } from "#imports";
import {
  computed,
  defineEmits,
  defineModel,
  defineProps,
  nextTick,
  onMounted,
  ref,
  useTemplateRef,
  watch,
  withDefaults,
  type Ref,
} from "vue";
import type { IInputProps, ITreeNodeState } from "../../../types/types";
import TreeNode from "../../components/input/TreeNode.vue";
import BaseIcon from "../BaseIcon.vue";
import Button from "../Button.vue";
import InputGroupContainer from "../input/InputGroupContainer.vue";
import TextNoResultsMessage from "../text/NoResultsMessage.vue";
import { useClickOutside } from "../../composables/useClickOutside";

const props = withDefaults(
  defineProps<
    IInputProps & {
      isArray?: boolean;
      //todo: do we need to change to radio button instead of checkboxes when !isArray?
      ontologySchemaId: string;
      ontologyTableId: string;
      limit?: number;
    }
  >(),
  {
    limit: 20,
  }
);

const emit = defineEmits(["focus", "blur"]);
//the selected values
const modelValue = defineModel<string[] | string | undefined>();
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

const counterOffset = ref<number>(0);
const maxTableRows = ref<number>(0);
const maxOntologyNodes = ref<number>(0);

const noTreeInputsFound = ref<boolean>(false);
const treeContainer = useTemplateRef<HTMLUListElement>("treeContainer");
const treeInputs = ref();
const showSelect = ref(false);

function setTreeInputs() {
  treeInputs.value = treeContainer.value?.querySelectorAll("ul li");
}

onMounted(() => {
  init()
    .then(async () => {
      await getMaxParentNodes();
      await nextTick();
      setTreeInputs();
    })
    .catch((err) => {
      throw new Error(err);
    })
    .finally(() => {
      initLoading.value = false;
    });
});

watch(() => props.ontologySchemaId, init);
watch(() => props.ontologyTableId, init);
watch(() => modelValue.value, applySelectedStates);
watch(
  () => treeContainer.value,
  async () => {
    await nextTick();
    setTreeInputs();
    if (!treeInputs.value) {
      noTreeInputsFound.value = true;
    }
  }
);

/* retrieves all terms, for small ontologies */
async function retrieveAllTerms() {
  let query = `query myquery {
        retrieveTerms: ${props.ontologyTableId}(orderby:{order:ASC,name:ASC}){name,parent{name},label,definition,code,codesystem,ontologyTermURI}
       }`;
  const data = await fetchGraphql(props.ontologySchemaId, query, {});

  return assembleChildren(data.retrieveTerms || []);
}

function assembleChildren(
  data: ITreeNodeState[],
  parentName: string | null = null
): ITreeNodeState[] {
  return (
    data
      .filter((row) => row.parent?.name == parentName)
      .map((row: any) => {
        const node = {
          name: row.name,
          label: row.label,
          description: row.definition,
          code: row.code,
          codeSystem: row.codesystem,
          uri: row.ontologyTermURI,
          selectable: true,
          visible: true,
          children: assembleChildren(data, row.name),
        };
        node.expanded = node.children.length > 0;
        return node;
      }) || []
  );
}

/* retrieves terms, optionally as children to a parent */
async function retrieveTerms(
  parentNode: ITreeNodeState | undefined = undefined
): Promise<ITreeNodeState[]> {
  const variables: any = {
    termFilter: parentNode
      ? { parent: { name: { equals: parentNode.name } } }
      : { parent: { _is_null: true } },
  };

  if (searchTerms.value) {
    variables.searchFilter = Object.assign({}, variables.termFilter, {
      _search_including_parents: searchTerms.value,
    });
  }

  let query = searchTerms.value
    ? `query myquery($termFilter:${props.ontologyTableId}Filter, $searchFilter:${props.ontologyTableId}Filter) {
        retrieveTerms: ${props.ontologyTableId}(filter:$termFilter, orderby:{order:ASC,name:ASC}){name,label,definition,code,codesystem,ontologyTermURI,children(limit:1){name}}
        searchMatch: ${props.ontologyTableId}(filter:$searchFilter, orderby:{order:ASC,name:ASC}){name}
       }`
    : `query myquery($termFilter:${props.ontologyTableId}Filter) {
        retrieveTerms: ${props.ontologyTableId}(filter:$termFilter, orderby:{order:ASC,name:ASC}){name,label,definition,code,codesystem,ontologyTermURI,children(limit:1){name}}
       }`;

  const data = await fetchGraphql(props.ontologySchemaId, query, variables);
  await getMaxParentNodes(variables);

  return (
    data.retrieveTerms?.map((row: any) => {
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
    }) || []
  );
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
  await getMaxTableRows();
  if (searchTerms.value.length === 0 && maxTableRows.value <= 25) {
    //retrieve all, expanded
    ontologyTree.value = [...(await retrieveAllTerms())];
  } else {
    //only retrieve root
    ontologyTree.value = [...(await retrieveTerms())];
  }
  await applySelectedStates();
}

/** apply selection UI state on selection changes */
async function applySelectedStates() {
  await retrieveSelectedPathsAndLabelsForModelValue();
  ontologyTree.value?.forEach((term) => {
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
  if (searchTerms.value) toggleSearch();
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
  if (searchTerms.value) toggleSearch();
}

function clearSelection() {
  if (props.disabled) return;
  modelValue.value = props.isArray ? [] : undefined;
}

function toggleSearch() {
  showSearch.value = !showSearch.value;
  searchTerms.value = "";
  init();
}

async function updateSearch(value: string) {
  searchTerms.value = value;
  counterOffset.value = 0;
  ontologyTree.value = [];
  await init();
}

const hasChildren = computed(() =>
  ontologyTree.value?.some((node) => node.children?.length)
);

async function getMaxTableRows() {
  const data = await fetchGraphql(
    props.ontologySchemaId,
    `query {${props.ontologyTableId}_agg(search: "${
      searchTerms.value || ""
    }") { count }} `,
    {}
  );
  maxTableRows.value = data[`${props.ontologyTableId}_agg`].count;
}

async function getMaxParentNodes(variables?: any) {
  const gqlVariables = {
    filter: { parent: { _is_null: true } },
    search: variables?.searchTerms || "",
  };

  const query = `query GetNodes ($filter: ${props.ontologyTableId}Filter, $search: String) {
    ${props.ontologyTableId}_agg (filter:$filter, search:$search) {
      count
    }
  }`;
  const data = await fetchGraphql(props.ontologySchemaId, query, gqlVariables);
  maxOntologyNodes.value = data[`${props.ontologyTableId}_agg`].count;
}

const displayAsSelect = computed(() => {
  return (
    maxOntologyNodes.value > props.limit ||
    maxTableRows.value > 25 ||
    searchTerms.value.length > 0
  );
});

// Close dropdown when clicking outside
const wrapperRef = ref<HTMLElement | null>(null);
useClickOutside(wrapperRef, () => {
  showSelect.value = false;
});
</script>

<template>
  <div v-if="initLoading" class="h-20 flex justify-start items-center">
    <BaseIcon name="progress-activity" class="animate-spin text-input" />
  </div>
  <div
    v-else-if="!initLoading && maxOntologyNodes"
    :class="{
      'flex items-center border outline-none rounded-input cursor-pointer':
        displayAsSelect,
    }"
    @click.stop="displayAsSelect ? (showSelect = true) : null"
  >
    <InputGroupContainer
      :id="`${id}-ontology`"
      class="border-transparent w-full relative"
      @focus="emit('focus')"
      @blur="emit('blur')"
    >
      <div
        v-show="displayAsSelect"
        class="flex items-center justify-between gap-2 m-2"
        @click.stop="showSelect = !showSelect"
      >
        <div class="flex flex-wrap items-center gap-2">
          <template v-if="modelValue" role="group">
            <Button
              v-if="Array.isArray(modelValue) && modelValue.length > 1"
              :id="`${id}-button-clear`"
              icon="cross"
              iconPosition="right"
              type="filterWell"
              size="tiny"
              class="mr-2"
              @click.stop="clearSelection"
            >
              clear all
            </Button>
            <Button
              v-for="name in Array.isArray(modelValue)
              ? (modelValue as string[]).sort()
              : modelValue ? [modelValue] : []"
              icon="cross"
              iconPosition="right"
              type="filterWell"
              size="tiny"
              @click.stop="deselect(name as string)"
            >
              {{ valueLabels[name] }}
            </Button>
          </template>
          <div>
            <label :for="`search-for-${id}`" class="sr-only">
              search in ontology
            </label>
            <input
              :id="`search-for-${id}`"
              type="text"
              v-model="searchTerms"
              @input="updateSearch(searchTerms)"
              class="flex-1 min-w-[100px] bg-transparent focus:outline-none"
              placeholder="Search in terms"
              autocomplete="off"
              @click.stop="showSelect = true"
            />
          </div>
        </div>
        <div>
          <BaseIcon
            v-show="showSelect"
            name="caret-up"
            @click.stop="showSelect = false"
          />
          <BaseIcon
            v-show="!showSelect"
            name="caret-down"
            class="justify-end"
          />
        </div>
      </div>
      <div
        ref="wrapperRef"
        :class="{
          'absolute z-20 max-h-[50vh] border rounded-input bg-white overflow-y-auto w-full pl-4':
            displayAsSelect,
        }"
        v-show="showSelect || !displayAsSelect"
      >
        <fieldset ref="treeContainer">
          <legend class="sr-only">select ontology terms</legend>
          <TreeNode
            :id="id"
            ref="tree"
            :nodes="ontologyTree"
            :isRoot="true"
            :valid="valid"
            :invalid="invalid"
            :disabled="disabled"
            :multiselect="isArray"
            @toggleExpand="toggleExpand"
            @toggleSelect="toggleSelect"
            @show-outside-results="noTreeInputsFound = false"
            class="pb-2"
            :class="{ 'pl-4': hasChildren }"
            aria-live="polite"
            aria-atomic="true"
          />
        </fieldset>
      </div>
    </InputGroupContainer>
  </div>
  <div
    v-else
    class="py-4 flex justify-start items-center text-input-description"
  >
    <TextNoResultsMessage
      :label="`Ontology '${props.ontologyTableId}' in schema '${props.ontologySchemaId}' is empty`"
    />
  </div>
</template>
