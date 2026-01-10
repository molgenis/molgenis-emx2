<script setup lang="ts">
import { computed, ref, useTemplateRef, watch, type Ref, onMounted } from "vue";
import type { IInputProps, ITreeNodeState } from "../../../types/types";
import TreeNode from "../../components/input/TreeNode.vue";
import BaseIcon from "../BaseIcon.vue";
import Button from "../Button.vue";
import InputGroupContainer from "../input/InputGroupContainer.vue";
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
//the selected values
const modelValue = defineModel<string[] | string | undefined | null>();
//labels for the selected values
const valueLabels: Ref<Record<string, string>> = ref({});
//intermediate selected values
const intermediates: Ref<string[]> = ref([]);
//toggle for showing search
const showSearch = ref<boolean>(false);
// the search value
const searchTerms: Ref<string> = ref("");
//initial loading state
const initLoading = ref(true);
// if the select should be shown expanded
const showSelect = ref(false);

const counterOffset = ref<number>(0);
const filteredCount = ref<number>(0);
const totalCount = ref<number>(0);
const rootCount = ref<number>(0);

// Track loading state per node to prevent duplicates
const loadingNodes = ref<Set<string>>(new Set());

// Virtual root node to hold the ontology tree and its pagination state
const rootNode = ref<ITreeNodeState>({
  name: '__root__',
  label: 'Root',
  selectable: false,
  visible: true,
  children: [],
  loadMoreOffset: 0,
  loadMoreTotal: 0,
  loadMoreHasMore: false,
});

function reset() {
  rootNode.value = {
    name: '__root__',
    label: 'Root',
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

/*initial state. Will load the labels for selection, and the first page of root items.
 * NOTE: This makes TWO queries:
 * 1. reload() - Loads counts (totalCount, rootCount) and selected item labels
 * 2. loadPage() - Loads first page of root items
 * This is intentional to keep the queries separate and maintainable.
 */
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

  //query for counts (always needed)
  if (!totalCount.value || !rootCount.value) {
    query += `totalCount:  ${props.ontologyTableId}_agg{count}`;
    query += `rootCount:  ${props.ontologyTableId}_agg(filter: {parent: { _is_null: true } }){count}`;
  }

  //execute the query with the variables
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

  // For small ontologies (below cutoff) and NOT forceList: load everything expanded
  // For large ontologies or forceList: load first page paginated
  if (totalCount.value < props.selectCutOff && !props.forceList && !ontologyTree.value.length) {
    // Load entire small ontology in one go
    const query = `query {
      allTerms: ${props.ontologyTableId}(limit: ${totalCount.value}, orderby:{order:ASC,name:ASC}){
        name,parent{name},label,definition,code,codesystem,ontologyTermURI
      }
    }`;

    const allData = await fetchGraphql(props.ontologySchemaId, query, {});
    rootNode.value.children = assembleTree(allData.allTerms || []);
    applySelectedStates(); // Apply selection to the assembled tree
  } else {
    // Load first page using unified loadPage function
    await loadPage(rootNode.value, 0);
  }

  initLoading.value = false;
}

// Assemble tree from flat data (for small ontologies loaded all at once)
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
              codeSystem: row.codesystem,
              uri: row.ontologyTermURI,
              selectable: true,
              visible: true,
              children: [],
              expanded: false,
            };
            node.children = assembleTree(data, node);
            node.expanded = node.children.length > 0; // Auto-expand if has children
            return node;
          }) || []
  );
}

// UNIFIED PAGE LOADING - replaces retrieveTerms and handles all cases
async function loadPage(
    node: ITreeNodeState,
    offset: number = 0,
    searchValue: string | undefined = undefined,
    forceShowAll: boolean = false
): Promise<void> {
  // Determine parent node (undefined for root)
  const parentNode = node.name === '__root__' ? undefined : node;

  // Build filter for this specific parent level
  const variables: any = {
    termFilter: parentNode
        ? { parent: { name: { equals: parentNode.name } } }
        : { parent: { _is_null: true } },
  };

  // Apply search filter if searching and not forcing show all
  const shouldApplySearch = searchValue && !forceShowAll;
  if (shouldApplySearch) {
    variables.searchFilter = Object.assign({}, variables.termFilter, {
      _search_including_parents: searchValue,
    });
  }

  // Build query - use inline filters for aggregates to avoid backend variable bug
  const filterToUse = shouldApplySearch ? '$searchFilter' : '$termFilter';
  const variableDeclaration = shouldApplySearch
      ? `$termFilter:${props.ontologyTableId}Filter, $searchFilter:${props.ontologyTableId}Filter`
      : `$termFilter:${props.ontologyTableId}Filter`;

  // Convert filter objects to inline strings for aggregate queries
  // count: filtered by parent (and search if applicable)
  const countFilter = shouldApplySearch ? variables.searchFilter : variables.termFilter;
  const countFilterInline = JSON.stringify(countFilter)
      .replace(/"([^"]+)":/g, '$1:')  // Remove quotes from keys
      .replace(/true/g, 'true')       // Keep boolean true
      .replace(/false/g, 'false');    // Keep boolean false

  // totalCount: same parent filter but WITHOUT search (to show how many hidden by search)
  // This is the total available at this parent level, regardless of search
  const totalCountFilterInline = JSON.stringify(variables.termFilter)
      .replace(/"([^"]+)":/g, '$1:')
      .replace(/true/g, 'true')
      .replace(/false/g, 'false');

  const query = `query myquery(${variableDeclaration}) {
    retrieveTerms: ${props.ontologyTableId}(filter:${filterToUse}, orderby:{order:ASC,name:ASC}, limit:${props.limit}, offset:${offset}){name,label,definition,code,codesystem,ontologyTermURI,children(limit:1){name}}
    count: ${props.ontologyTableId}_agg(filter:${countFilterInline}){count}
    totalCount: ${props.ontologyTableId}_agg(filter:${totalCountFilterInline}){count}
  }`;

  console.log('📡 loadPage query:', {
    nodeName: node.name || 'root',
    offset,
    searchValue,
    forceShowAll,
    filterToUse,
    variables: JSON.stringify(variables, null, 2)
  });

  const data = await fetchGraphql(props.ontologySchemaId, query, variables);

  console.log('📡 loadPage response:', {
    nodeName: node.name || 'root',
    termsCount: data.retrieveTerms?.length || 0,
    count: data.count?.count,
    totalCount: data.totalCount?.count,
  });

  // Map results to tree nodes
  const newTerms = data.retrieveTerms?.map((row: any) => ({
    name: row.name,
    parentNode: parentNode,
    label: row.label,
    description: row.definition,
    code: row.code,
    codeSystem: row.codesystem,
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
  node.loadMoreHasMore = newTerms.length >= props.limit && itemsLoaded < totalAvailable;

  // Store unfilteredTotal for "show all" feature
  if (data.totalCount?.count !== undefined) {
    (node as any).unfilteredTotal = data.totalCount.count;
  }

  // Apply selection states to loaded items
  if (node.name === '__root__') {
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
    modelValue.value = modelValue.value === node.name ? undefined : node.name;
    showSelect.value = false;
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
        // if we select last child that wasn't selected yet
        // then we need to toggle select on parent instead
    // BUT ONLY if all children are loaded (no more to load)
    else if (
        node.parentNode &&
        !node.parentNode.loadMoreHasMore && // All children are loaded
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
  }
  if (searchTerms.value) {
    // Clear search - just set to empty, watcher will handle the update
    searchTerms.value = "";
  }
  emit("focus");
}

async function toggleTermExpand(node: ITreeNodeState, showAll: boolean = false) {
  if (!node.expanded) {
    // Store whether this node is showing all (bypassing search filter)
    (node as any).showingAll = showAll;

    // Load first page of children using unified loadPage
    await loadPage(node, 0, showAll ? undefined : searchTerms.value, showAll);

    node.expanded = true;
  } else {
    node.expanded = false;
  }
}

// Handler for when user clicks "show all" on a specific node during search
async function showAllChildrenOfNode(node: ITreeNodeState) {
  // If node is already showing all, do nothing
  if ((node as any).showingAll) {
    return;
  }

  // If node is already expanded, we need to reload its children
  if (node.expanded) {
    // Collapse first
    node.expanded = false;
    // Wait a tick for UI to update
    await new Promise(resolve => setTimeout(resolve, 0));
  }

  // Now expand with showAll=true
  await toggleTermExpand(node, true);
}

// Unified loadMoreTerms - just calls loadPage with offset
async function loadMoreTerms(node: ITreeNodeState) {
  const nodeKey = node.name || '__root__';

  // Prevent duplicate loads for the same node
  if (loadingNodes.value.has(nodeKey)) {
    console.log('⚠️ Already loading for node:', nodeKey);
    return;
  }

  if (!node.loadMoreHasMore) {
    console.log('⚠️ No more items to load for node:', nodeKey);
    return;
  }

  loadingNodes.value.add(nodeKey);

  try {
    // Check if this node is showing all children (bypassing search)
    const showingAll = (node as any).showingAll || false;

    // Pass current search value to maintain search context when loading more
    // Unless this node is explicitly showing all children
    const searchValue = showingAll ? undefined : (searchTerms.value || undefined);

    // Use unified loadPage function
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
  // Clear search - just set to empty, watcher will handle the update
  searchTerms.value = "";
}

function clearSelection() {
  if (props.disabled) return;
  modelValue.value = props.isArray ? [] : null;
}

// Debounced search watcher
let searchDebounceTimer: ReturnType<typeof setTimeout> | null = null;
let lastSearchValue: string = "";
let isSearching = false; // Flag to prevent watcher from triggering during search

watch(searchTerms, (newValue, oldValue) => {
  // Don't trigger if we're currently executing a search
  if (isSearching) {
    console.log('🔍 Watcher blocked: search in progress');
    return;
  }

  // Don't trigger on initial mount
  if (oldValue === undefined) {
    lastSearchValue = newValue;
    console.log('🔍 Watcher blocked: initial mount');
    return;
  }

  // Don't trigger if value hasn't actually changed
  if (newValue === lastSearchValue) {
    console.log('🔍 Watcher blocked: value unchanged');
    return;
  }

  // Only search if dropdown is open or not in select mode
  if (displayAsSelect.value && !showSelect.value) {
    console.log('🔍 Watcher blocked: dropdown not open');
    return;
  }

  console.log('🔍 Search watcher triggered:', { newValue, oldValue, lastSearchValue });

  // Clear existing timer
  if (searchDebounceTimer) {
    clearTimeout(searchDebounceTimer);
  }

  // Debounce the actual search
  searchDebounceTimer = setTimeout(() => {
    lastSearchValue = newValue;
    updateSearch(newValue);
  }, 500); // Increased to 500ms for large tables
});

async function updateSearch(value: string) {
  console.log('🔎 updateSearch called with:', value, 'isSearching flag:', isSearching);

  if (isSearching) {
    console.error('🚨 BLOCKED: updateSearch called while already searching!');
    return;
  }

  // Set flag to prevent watcher from triggering during this search
  isSearching = true;
  console.log('🔎 isSearching flag set to TRUE');

  try {
    counterOffset.value = 0;

    // Use unified loadPage - pass search value (or empty string for normal mode)
    await loadPage(rootNode.value, 0, value || "");

    console.log('🔎 Search complete');
  } finally {
    // Always clear the flag, even if there's an error
    console.log('🔎 isSearching flag set to FALSE');
    isSearching = false;
  }
}


const hasChildren = computed(() =>
    rootNode.value.children?.some((node) => node.children?.length)
);

const searchResultsSummary = computed(() => {
  if (!searchTerms.value) return null;

  const loaded = rootNode.value.children?.length || 0;
  const total = rootNode.value.loadMoreTotal || 0;
  const hasMore = rootNode.value.loadMoreHasMore;

  return {
    loaded,
    total,
    hasMore,
    showingAll: loaded >= total
  };
});

const displayAsSelect = computed(() => {
  // If forceList is true, never display as select
  if (props.forceList) {
    return false;
  }

  return (
      totalCount.value >= props.selectCutOff || rootCount.value >= props.limit
  );
});

const enableAutoLoad = computed(() => {
  // Disable auto-loading when forceList is true (manual load more only)
  return !props.forceList;
});

const ontologyTree = computed(() => rootNode.value.children || []);

async function toggleSelect() {
  if (showSelect.value) {
    showSelect.value = false;
  } else {
    // Load first page if not already loaded
    if (!rootNode.value.children || rootNode.value.children.length === 0) {
      await loadPage(rootNode.value, 0);
    }
    showSelect.value = true;
  }
}

// Close dropdown when clicking outside
const wrapperRef = useTemplateRef<HTMLElement>("wrapperRef");
useClickOutside(wrapperRef, () => {
  showSelect.value = false;
});

// Ref for the scroll container
const scrollContainerRef = useTemplateRef<HTMLElement>("scrollContainerRef");

onMounted(() => {
  reload();
});
</script>

<template>
  <pre></pre>
  <div v-if="initLoading" class="h-20 flex justify-start items-center">
    <BaseIcon name="progress-activity" class="animate-spin text-input" />
  </div>
  <div
      v-else-if="!initLoading && totalCount"
      :class="{
      'flex items-center border outline-none rounded-input cursor-pointer ':
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
      @click.stop="displayAsSelect ? (showSelect = true) : null"
  >
    <InputGroupContainer
        ref="wrapperRef"
        :id="`${id}-ontology`"
        class="border-transparent w-full relative"
        @focus="emit('focus')"
        @blur="emit('blur')"
    >
      <div
          v-show="displayAsSelect"
          class="flex items-center justify-between gap-2 px-2 h-input"
          @click.stop="toggleSelect"
      >
        <div class="flex flex-wrap items-center gap-2">
          <template v-if="modelValue" role="group">
            <Button
                v-for="name in Array.isArray(modelValue)
              ? (modelValue as string[]).sort()
              : modelValue ? [modelValue] : []"
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
          <div v-if="!disabled">
            <label :for="`search-for-${id}`" class="sr-only">
              search in ontology
            </label>
            <input
                :id="`search-for-${id}`"
                type="text"
                v-model="searchTerms"
                class="flex-1 min-w-[100px] bg-transparent focus:outline-none"
                placeholder="Search in terms"
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
          'absolute z-50 max-h-[50vh] border bg-input overflow-y-auto w-full pl-4':
            displayAsSelect,
        }"
          v-show="showSelect || !displayAsSelect"
      >
        <fieldset ref="treeContainer">
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
              class="pb-2"
              :class="{ 'pl-4': hasChildren }"
              aria-live="polite"
              aria-atomic="true"
          />

          <!-- Search results summary -->
          <div
              v-if="searchResultsSummary"
              class="mt-3 px-2 pb-2 text-body-sm text-input-description border-t border-input-border pt-2"
          >
            <div class="flex items-center justify-between">
              <span class="italic">
                Showing {{ searchResultsSummary.loaded }} matching term{{ searchResultsSummary.loaded !== 1 ? 's' : '' }}
                <template v-if="searchResultsSummary.hasMore">
                  of {{ searchResultsSummary.total }} total
                </template>
              </span>
              <button
                  @click="searchTerms = ''"
                  class="text-input hover:text-input-hover underline cursor-pointer text-body-sm"
                  type="button"
              >
                Show all terms
              </button>
            </div>
          </div>
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