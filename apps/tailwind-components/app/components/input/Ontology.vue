<script setup lang="ts">
import {
  computed,
  defineEmits,
  defineModel,
  defineProps,
  ref,
  useTemplateRef,
  watch,
  withDefaults,
  type Ref,
  onMounted,
} from "vue";
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
    }
  >(),
  {
    limit: 20,
    selectCutOff: 25,
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
// if the select should be shown expanded
const showSelect = ref(false);

const counterOffset = ref<number>(0);
const filteredCount = ref<number>(0);
const totalCount = ref<number>(0);
const rootCount = ref<number>(0);

const treeContainer = useTemplateRef<HTMLUListElement>("treeContainer");

function reset() {
  ontologyTree.value = [];
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

/*initial state. Will load the labels for selection, and the whole ontology when small.
 * (large ontologies are loaded on showSelect)
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

  //query for counts
  if (!totalCount.value || !rootCount.value) {
    query += `totalCount:  ${props.ontologyTableId}_agg{count}`;
    query += `rootCount:  ${props.ontologyTableId}_agg(filter: {parent: { _is_null: true } }){count}`;
  }

  //query for whole ontology, if not too large
  if (
    !ontologyTree.value.length &&
    totalCount.value <= 25 &&
    rootCount.value <= 15
  ) {
    //retrieve whole ontology if not too big
    query += `allTerms: ${props.ontologyTableId}(limit: 25, orderby:{order:ASC,name:ASC}){name,parent{name},label,definition,code,codesystem,ontologyTermURI}`;
  }

  //execute the query with the variables
  query = reloadSelectionLabels
    ? `query myquery($pathFilter:${props.ontologyTableId}Filter){${query}}`
    : `query myquery{${query}}`;
  const data = await fetchGraphql(props.ontologySchemaId, query, variables);

  // update new counts if there
  totalCount.value = data.totalCount?.count || totalCount.value;
  rootCount.value = data.rootCount?.count || rootCount.value;

  //update the tree if we have whole ontology (otherwise that will work via retrieveTerms on showSelect
  if (!displayAsSelect.value) {
    ontologyTree.value = assembleTreeWithChildren(data.allTerms || []);
  }

  if (reloadSelectionLabels) {
    await applyModelValue(data);
  }
  initLoading.value = false;
}

function assembleTreeWithChildren(
  data: ITreeNodeState[],
  parentNode: ITreeNodeState | undefined = undefined
): ITreeNodeState[] {
  return (
    data
      .filter((row) => row.parent?.name == parentNode?.name)
      .map((row: any) => {
        const node = {
          name: row.name,
          parentNode: parentNode,
          label: row.label,
          description: row.definition,
          code: row.code,
          codeSystem: row.codesystem,
          uri: row.ontologyTermURI,
          selectable: true,
          visible: true,
        };
        node.children = assembleTreeWithChildren(data, node);
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

async function applyModelValue(data: any = undefined): Promise<void> {
  valueLabels.value = {};
  intermediates.value = [];
  if (data === undefined) {
    data = await fetchGraphql(
      props.ontologySchemaId,
      `query ontologyPaths($filter:${props.ontologyTableId}Filter) {ontologyPaths: ${props.ontologyTableId}(filter:$filter,limit:1000){name,label}}`,
      {
        filter: { _match_any_including_parents: modelValue.value },
      }
    );
  }
  valueLabels.value = Object.fromEntries(
    data.ontologyPaths.map((row: any) => [row.name, row.label || row.name])
  );
  intermediates.value = data.ontologyPaths.map(
    (term: { name: string }) => term.name
  );
  applySelectedStates();
}

/** apply selection UI state on selection changes */
function applySelectedStates() {
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
    // if we select last selected child
    // then we need toggle select on parent instead
    else if (
      node.parentNode &&
      node.parentNode.children
        .filter((child) => child.name != node.name)
        .every((child) => child.selected === "selected")
    ) {
      console.log("select parent");
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
    await updateSearch("");
  }
  emit("focus");
}

async function toggleTermExpand(node: ITreeNodeState) {
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
    applySelectedStates();
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
  updateSearch("");
}

function clearSelection() {
  if (props.disabled) return;
  modelValue.value = props.isArray ? [] : undefined;
}

async function updateSearch(value: string) {
  searchTerms.value = value;
  counterOffset.value = 0;
  ontologyTree.value = [];
  ontologyTree.value = [...(await retrieveTerms())];
  applySelectedStates();
}

const hasChildren = computed(() =>
  ontologyTree.value?.some((node) => node.children?.length)
);

const displayAsSelect = computed(() => {
  return (
    totalCount.value >= props.selectCutOff || rootCount.value >= props.limit
  );
});

async function toggleSelect() {
  if (showSelect.value) {
    showSelect.value = false;
  } else {
    ontologyTree.value = [...(await retrieveTerms())];
    applySelectedStates();
    showSelect.value = true;
  }
}

// Close dropdown when clicking outside
const wrapperRef = ref<HTMLElement | null>(null);
useClickOutside(wrapperRef, () => {
  showSelect.value = false;
});

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
      :id="`${id}-ontology`"
      class="border-transparent w-full relative"
      @focus="emit('focus')"
      @blur="emit('blur')"
    >
      <div
        v-show="displayAsSelect"
        class="flex items-center justify-between gap-2 m-2"
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
              @input="updateSearch(searchTerms)"
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
        ref="wrapperRef"
        :class="{
          'absolute z-20 max-h-[50vh] border bg-white overflow-y-auto w-full pl-4':
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
            @toggleExpand="toggleTermExpand"
            @toggleSelect="toggleTermSelect"
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
