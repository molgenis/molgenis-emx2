<script setup lang="ts">
import {
  computed,
  ref,
  useTemplateRef,
  watch,
  type Ref,
  onMounted,
  nextTick,
} from "vue";
import type { IInputProps, ITreeNodeState } from "../../../types/types";
import TreeNode from "../../components/input/TreeNode.vue";
import BaseIcon from "../BaseIcon.vue";
import Button from "../Button.vue";
import InputGroupContainer from "../input/InputGroupContainer.vue";
import InputSearch from "../input/Search.vue";
import TextNoResultsMessage from "../text/NoResultsMessage.vue";
import { useClickOutside } from "../../composables/useClickOutside";
import fetchGraphql from "../../composables/fetchGraphql";

const props = withDefaults(
  defineProps<
    IInputProps & {
      isArray?: boolean;
      ontologySchemaId: string;
      ontologyTableId: string;
      limit?: number;
      selectCutOff?: number;
      forceList?: boolean; // Force list display (no select dropdown) with manual load more only
    }
  >(),
  {
    limit: 20,
    selectCutOff: 25,
    forceList: false,
  }
);

const emit = defineEmits(["focus", "blur"]);
const modelValue = defineModel<string[] | string | undefined | null>();
const valueLabels: Ref<Record<string, string>> = ref({});
const intermediates: Ref<string[]> = ref([]);
const showSearch = ref<boolean>(false);
const searchTerms: Ref<string> = ref("");
const initLoading = ref(true);
const showSelect = ref(false);

const counterOffset = ref<number>(0);
const filteredCount = ref<number>(0);
const totalCount = ref<number>(0);
const rootCount = ref<number>(0);

const loadingNodes = ref<Set<string>>(new Set());

// Virtual root node to hold the ontology tree and its pagination state
const rootNode = ref<ITreeNodeState>({
  name: "__root__",
  label: "Root",
  selectable: false,
  visible: true,
  children: [],
  loadMoreOffset: 0,
  loadMoreTotal: 0,
  loadMoreHasMore: false,
});

function reset() {
  rootNode.value = {
    name: "__root__",
    label: "Root",
    selectable: false,
    visible: true,
    children: [],
    loadMoreOffset: 0,
    loadMoreTotal: 0,
    loadMoreHasMore: false,
  };
  valueLabels.value = {};
  intermediates.value = [];
  showSearch.value = false;
  searchTerms.value = "";
  initLoading.value = true;
  showSelect.value = false;
  counterOffset.value = 0;
  filteredCount.value = 0;
  totalCount.value = 0;
  rootCount.value = 0;
  loadingNodes.value.clear();
  reload();
}

watch(() => props.ontologySchemaId, reset);
watch(() => props.ontologyTableId, reset);
watch(
  () => modelValue.value,
  () => {
    applyModelValue();
  }
);

async function reload() {
  //goal is to have only one query to server as the network has most performance impact
  let query = "";
  const variables: any = {};

  //query for the labels for the modelValue if needed
  const reloadSelectionLabels =
    props.isArray && Array.isArray(modelValue.value)
      ? modelValue.value.length > 0
      : modelValue.value;
  if (reloadSelectionLabels) {
    //retrieve paths of all selected terms
    query = `ontologyPaths: ${props.ontologyTableId}(filter:$pathFilter,limit:1000){name,label}`;
    variables.pathFilter = { _match_any_including_parents: modelValue.value };
  } else {
    valueLabels.value = {};
    intermediates.value = [];
  }

  //query for counts
  if (!totalCount.value || !rootCount.value) {
    query += `totalCount:  ${props.ontologyTableId}_agg{count}`;
    query += `rootCount:  ${props.ontologyTableId}_agg(filter: {parent: { _is_null: true } }){count}`;
  }

  query = reloadSelectionLabels
    ? `query myquery($pathFilter:${props.ontologyTableId}Filter){${query}}`
    : `query myquery{${query}}`;
  const data = await fetchGraphql(props.ontologySchemaId, query, variables);

  // update new counts if there
  totalCount.value = data.totalCount?.count || totalCount.value;
  rootCount.value = data.rootCount?.count || rootCount.value;

  if (reloadSelectionLabels) {
    await applyModelValue(data);
  }

  if (
    totalCount.value < props.selectCutOff &&
    !props.forceList &&
    !ontologyTree.value.length
  ) {
    // Load entire small ontology in one go
    const query = `query {
      allTerms: ${props.ontologyTableId}(limit: ${totalCount.value}, orderby:{order:ASC,name:ASC}){
        name,parent{name},label,definition,code,codesystem,ontologyTermURI
      }
    }`;

    const allData = await fetchGraphql(props.ontologySchemaId, query, {});
    rootNode.value.children = assembleTree(allData.allTerms || []);
    applySelectedStates();
  } else {
    await loadPage(rootNode.value, 0);
  }

  initLoading.value = false;
}

function assembleTree(
  data: any[],
  parentNode: ITreeNodeState | undefined = undefined
): ITreeNodeState[] {
  return (
    data
      .filter((row) => row.parent?.name == parentNode?.name)
      .map((row: any) => {
        const node: ITreeNodeState = {
          name: row.name,
          parentNode: parentNode,
          label: row.label,
          description: row.definition,
          code: row.code,
          codesystem: row.codesystem,
          uri: row.ontologyTermURI,
          selectable: true,
          visible: true,
          children: [],
          expanded: false,
        };
        node.children = assembleTree(data, node);
        node.expanded = node.children.length > 0;
        return node;
      }) || []
  );
}

async function loadPage(
  node: ITreeNodeState,
  offset: number = 0,
  searchValue: string | undefined = undefined,
  forceShowAll: boolean = false
): Promise<void> {
  const parentNode = node.name === "__root__" ? undefined : node;

  const variables: any = {
    termFilter: parentNode
      ? { parent: { name: { equals: parentNode.name } } }
      : { parent: { _is_null: true } },
  };

  const shouldApplySearch = searchValue && !forceShowAll;
  if (shouldApplySearch) {
    variables.searchFilter = Object.assign({}, variables.termFilter, {
      _search_including_parents: searchValue,
    });
  }

  const retrieveTermsFilter = shouldApplySearch
    ? "$searchFilter"
    : "$termFilter";

  const variableDeclaration = shouldApplySearch
    ? `$searchFilter:${props.ontologyTableId}Filter`
    : `$termFilter:${props.ontologyTableId}Filter`;

  const countFilter = shouldApplySearch
    ? variables.searchFilter
    : variables.termFilter;
  const countFilterInline = JSON.stringify(countFilter)
    .replace(/"([^"]+)":/g, "$1:") // Remove quotes from keys
    .replace(/true/g, "true") // Keep boolean true
    .replace(/false/g, "false"); // Keep boolean false

  const totalCountFilterInline = JSON.stringify(variables.termFilter)
    .replace(/"([^"]+)":/g, "$1:")
    .replace(/true/g, "true")
    .replace(/false/g, "false");

  const query = `query myquery(${variableDeclaration}) {
    retrieveTerms: ${props.ontologyTableId}(filter:${retrieveTermsFilter}, orderby:{order:ASC,name:ASC}, limit:${props.limit}, offset:${offset}){name,label,definition,code,codesystem,ontologyTermURI,children(limit:1){name}}
    count: ${props.ontologyTableId}_agg(filter:${countFilterInline}){count}
    totalCount: ${props.ontologyTableId}_agg(filter:${totalCountFilterInline}){count}
  }`;

  const data = await fetchGraphql(props.ontologySchemaId, query, variables);

  const newTerms =
    data.retrieveTerms?.map((row: any) => ({
      name: row.name,
      parentNode: parentNode,
      label: row.label,
      description: row.definition,
      code: row.code,
      codesystem: row.codesystem,
      uri: row.ontologyTermURI,
      selectable: true,
      children: row.children,
      visible: true,
    })) || [];

  // Update node's children
  if (offset === 0) {
    node.children = newTerms;
  } else {
    node.children = [...(node.children || []), ...newTerms];
  }

  // Update pagination state
  const itemsLoaded = offset + newTerms.length;
  const totalAvailable = data.count?.count || 0;

  node.loadMoreOffset = itemsLoaded;
  node.loadMoreTotal = totalAvailable;
  node.loadMoreHasMore =
    newTerms.length >= props.limit && itemsLoaded < totalAvailable;

  if (data.totalCount?.count !== undefined) {
    node.unfilteredTotal = data.totalCount.count;
  }

  if (node.name === "__root__") {
    // For root level, apply to all root children
    applySelectedStates();
  } else {
    // For nested nodes, apply to this node (which recursively applies to its children)
    applyStateToNode(node);
  }
}

async function applyModelValue(data: any = undefined): Promise<void> {
  valueLabels.value = {};
  intermediates.value = [];
  if (data === undefined && modelValue.value) {
    data = await fetchGraphql(
      props.ontologySchemaId,
      `query ontologyPaths($filter:${props.ontologyTableId}Filter) {ontologyPaths: ${props.ontologyTableId}(filter:$filter,limit:1000){name,label}}`,
      {
        filter: { _match_any_including_parents: modelValue.value },
      }
    );
  }
  if (data && data.ontologyPaths) {
    valueLabels.value = Object.fromEntries(
      data.ontologyPaths.map((row: any) => [row.name, row.label || row.name])
    );
    intermediates.value = data.ontologyPaths.map(
      (term: { name: string }) => term.name
    );
  } else {
    valueLabels.value = {};
    intermediates.value = [];
  }
  applySelectedStates();
}

/** apply selection UI state on selection changes */
function applySelectedStates() {
  rootNode.value.children?.forEach((term) => {
    applyStateToNode(term);
  });
}

function applyStateToNode(node: ITreeNodeState): void {
  const isSelected = props.isArray
    ? modelValue.value?.includes(node.name)
    : modelValue.value === node.name;
  const isIntermediate = intermediates.value.includes(node.name);

  if (isSelected) {
    node.selected = "selected";
    getAllChildren(node).forEach((child) => (child.selected = "selected"));
  } else if (isIntermediate) {
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

async function toggleTermSelect(node: ITreeNodeState) {
  if (props.disabled) return;
  if (!props.isArray) {
    modelValue.value = modelValue.value === node.name ? null : node.name;
    await toggleSelect();
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
    } else if (
      node.parentNode &&
      (!searchTerms.value || node.parentNode.showingAll) &&
      !node.parentNode.loadMoreHasMore &&
      node.parentNode.children
        .filter((child) => child.name != node.name)
        .every((child) => child.selected === "selected")
    ) {
      await toggleTermSelect(node.parentNode);
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
    searchTerms.value = "";
  }
  emit("focus");
}

async function toggleTermExpand(
  node: ITreeNodeState,
  showAll: boolean = false
) {
  if (!node.expanded) {
    node.showingAll = showAll;

    await loadPage(node, 0, showAll ? undefined : searchTerms.value, showAll);

    node.expanded = true;
  } else {
    node.expanded = false;
  }
}

async function showAllChildrenOfNode(node: ITreeNodeState) {
  if (node.showingAll) {
    return;
  }

  (node as any).filteredCount = node.loadMoreTotal || 0;

  if (node.expanded) {
    node.expanded = false;
    await new Promise((resolve) => setTimeout(resolve, 0));
  }

  await toggleTermExpand(node, true);
}

async function applyFilterToNode(node: ITreeNodeState) {
  if (!node.showingAll) {
    return;
  }
  node.expanded = false;
  node.showingAll = false;
  await new Promise((resolve) => setTimeout(resolve, 0));
  await toggleTermExpand(node, false);
}

async function loadMoreTerms(node: ITreeNodeState) {
  const nodeKey = node.name || "__root__";
  if (loadingNodes.value.has(nodeKey)) {
    return;
  }
  if (!node.loadMoreHasMore) {
    return;
  }
  loadingNodes.value.add(nodeKey);

  try {
    const showingAll = node.showingAll || false;
    const searchValue = showingAll ? undefined : searchTerms.value || undefined;
    await loadPage(node, node.loadMoreOffset || 0, searchValue, showingAll);
  } finally {
    loadingNodes.value.delete(nodeKey);
  }
}

function deselect(name: string) {
  if (props.disabled) return;
  if (props.isArray && Array.isArray(modelValue.value)) {
    modelValue.value = modelValue.value.filter((value) => value != name);
  } else {
    modelValue.value = null;
  }
  searchTerms.value = "";
}

function clearSelection() {
  if (props.disabled) {
    return;
  }
  modelValue.value = props.isArray ? [] : null;

  emit("blur");
}

let searchDebounceTimer: ReturnType<typeof setTimeout> | null = null;
let lastSearchValue: string = "";
let isSearching = false;

watch(searchTerms, (newValue, oldValue) => {
  if (isSearching) {
    return;
  }

  if (oldValue === undefined) {
    lastSearchValue = newValue;
    return;
  }

  if (newValue === lastSearchValue) {
    return;
  }

  const selectModeCheck =
    displayAsSelect.value && !showSelect.value && !props.forceList;

  if (selectModeCheck) {
    return;
  }

  if (searchDebounceTimer) {
    clearTimeout(searchDebounceTimer);
  }

  searchDebounceTimer = setTimeout(() => {
    lastSearchValue = newValue;
    updateSearch(newValue);
  }, 500);
});

function toggleSearch() {
  showSearch.value = !showSearch.value;
  if (!showSearch.value) {
    searchTerms.value = "";
  }
}

async function updateSearch(value: string) {
  if (isSearching) {
    return;
  }

  isSearching = true;

  try {
    counterOffset.value = 0;
    await loadPage(rootNode.value, 0, value || "");
  } finally {
    isSearching = false;
  }
}

const hasChildren = computed(() =>
  rootNode.value.children?.some((node) => node.children?.length)
);

const displayAsSelect = computed(() => {
  if (props.forceList) {
    return false;
  }

  return (
    totalCount.value >= props.selectCutOff || rootCount.value >= props.limit
  );
});

const enableAutoLoad = computed(() => {
  return !props.forceList;
});

const ontologyTree = computed(() => rootNode.value.children || []);

const searchInput = ref<HTMLInputElement | null>(null);
async function toggleSelect() {
  if (showSelect.value) {
    showSelect.value = false;
    searchTerms.value = "";
  } else {
    if (!rootNode.value.children || rootNode.value.children.length === 0) {
      await loadPage(rootNode.value, 0);
    }
    showSelect.value = true;
    nextTick(() => {
      searchInput.value?.focus();
    });
  }
}

const wrapperRef = useTemplateRef<HTMLElement>("wrapperRef");
useClickOutside(wrapperRef, () => {
  if (showSelect.value) {
    toggleSelect();
  }
});

const scrollContainerRef = useTemplateRef<HTMLElement>("scrollContainerRef");

onMounted(() => {
  reload();
});
</script>

<template>
  <div v-if="initLoading" class="h-20 flex justify-start items-center">
    <BaseIcon name="progress-activity" class="animate-spin text-input" />
  </div>
  <div
    v-else-if="!initLoading && totalCount"
    :class="{
      'flex flex-col items-start border outline-none rounded-input':
        displayAsSelect,
      'bg-input ': displayAsSelect && !disabled,
      'border-disabled': displayAsSelect && disabled,
      'border-valid text-valid': valid && !disabled,
      'border-invalid text-invalid': invalid && !disabled,
      'text-disabled cursor-not-allowed': disabled,
      'bg-disabled border-valid text-valid cursor-not-allowed':
        valid && disabled,
      'bg-disabled border-invalid text-invalid cursor-not-allowed':
        invalid && disabled,
      'text-input hover:border-input-hover focus-within:border-input-focused':
        !disabled && !invalid && !valid,
    }"
  >
    <template v-if="forceList">
      <div class="w-full flex items-center gap-2 px-2 py-2">
        <Button
          icon="Search"
          type="text"
          size="tiny"
          @click.stop="toggleSearch"
        >
          {{ showSearch ? "Hide" : "Show" }} search
        </Button>
        <InputSearch
          :id="`${id}-search-list`"
          v-if="showSearch"
          size="tiny"
          v-model="searchTerms"
          placeholder="Type to search..."
          class="flex-1"
        />
      </div>
      <div
        v-if="modelValue"
        role="group"
        class="flex flex-wrap items-center gap-2"
      >
        <Button
          v-for="name in Array.isArray(modelValue)
              ? (modelValue as string[]).sort()
              : modelValue ? [modelValue] : []"
          :key="name"
          icon="cross"
          iconPosition="right"
          type="filterWell"
          size="tiny"
          :class="{
            'text-disabled cursor-not-allowed': disabled,
            'text-valid bg-valid': valid,
            'text-invalid bg-invalid': invalid,
          }"
          @click.stop="deselect(name as string)"
        >
          {{ valueLabels[name] }}
        </Button>
      </div>
    </template>

    <InputGroupContainer
      ref="wrapperRef"
      :id="`${id}-ontology`"
      class="border-transparent w-full relative"
      @focus="emit('focus')"
    >
      <div
        v-show="displayAsSelect"
        class="flex items-center justify-between gap-2 p-2 min-h-input h-auto cursor-text"
        @click.stop="!showSelect && toggleSelect()"
      >
        <div class="flex flex-wrap items-center gap-2">
          <template v-if="modelValue && isArray" role="group">
            <Button
              v-for="name in Array.isArray(modelValue)
              ? (modelValue as string[]).sort()
              : modelValue ? [modelValue] : []"
              :key="name"
              icon="cross"
              iconPosition="right"
              type="filterWell"
              size="tiny"
              :class="{
                'text-disabled cursor-not-allowed': disabled,
                'text-valid bg-valid': valid,
                'text-invalid bg-invalid': invalid,
              }"
              @click.stop="deselect(name as string)"
            >
              {{ valueLabels[name] }}
            </Button>
          </template>
          <template v-else-if="modelValue && !showSelect">
            {{ valueLabels[modelValue as string] }}
          </template>
          <div v-if="!disabled && showSelect">
            <label :for="`search-for-${id}`" class="sr-only">
              search in ontology
            </label>
            <input
              :id="`search-for-${id}`"
              type="text"
              ref="searchInput"
              v-model="searchTerms"
              class="flex-grow basis-0 min-w-[10px] bg-transparent focus:outline-none"
              :placeholder="showSelect ? 'Search in terms' : ''"
              autocomplete="off"
              @click.stop="showSelect ? null : toggleSelect()"
            />
          </div>
        </div>
        <div class="flex items-center gap-2">
          <BaseIcon
            v-show="showSelect"
            name="caret-up"
            :class="{
              'text-valid': valid,
              'text-invalid': invalid,
              'text-disabled cursor-not-allowed': disabled,
              'text-input': !disabled,
            }"
            @click.stop="toggleSelect"
          />
          <BaseIcon
            v-show="!showSelect"
            name="caret-down"
            :class="{
              'text-valid': valid,
              'text-invalid': invalid,
              'text-disabled cursor-not-allowed': disabled,
              'text-input': !disabled,
            }"
            @click.stop="toggleSelect"
          />
        </div>
      </div>
      <div
        ref="scrollContainerRef"
        :class="{
          'absolute z-50 max-h-[50vh] border bg-input overflow-y-auto w-full':
            displayAsSelect,
        }"
        v-show="showSelect || !displayAsSelect"
      >
        <fieldset ref="treeContainer" class="pl-4">
          <legend class="sr-only">select ontology terms</legend>
          <TreeNode
            :id="id"
            ref="tree"
            :parentNode="rootNode"
            :isRoot="true"
            :valid="valid"
            :invalid="invalid"
            :disabled="disabled"
            :multiselect="isArray"
            :isSearching="!!searchTerms"
            :scrollContainer="scrollContainerRef"
            :enableAutoLoad="enableAutoLoad"
            @toggleExpand="toggleTermExpand"
            @toggleSelect="toggleTermSelect"
            @loadMore="loadMoreTerms"
            @showAllChildren="showAllChildrenOfNode"
            @applyFilter="applyFilterToNode"
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
  <Button
    v-if="isArray ? (modelValue || []).length > 0 : modelValue"
    @click="clearSelection"
    type="text"
    size="tiny"
    iconPosition="right"
    class="mr-2 underline cursor-pointer"
    :class="{ 'pl-4': hasChildren && !displayAsSelect }"
  >
    Clear
  </Button>
</template>
