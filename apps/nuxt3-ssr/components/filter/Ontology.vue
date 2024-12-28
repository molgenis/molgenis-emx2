<script setup lang="ts">
import type { Modal } from "#build/components";
import type { ITreeNode } from "../../../tailwind-components/types/types";
import type {
  IFilterCondition,
  IOntologyNode,
  IOntologyRespItem,
} from "~/interfaces/types";

const props = withDefaults(
  defineProps<{
    tableId: string;
    filter: Record<String, Filter>;
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

const showSearch = computed(() => data?.length > 10);

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
    return props.modelValue ? props.modelValue.map((n) => n.name) : [];
  },
  set(newValue) {
    // transform the names back to the original data structure for use in gql query
    const newConditions = newValue.map((name) => ({ name: name }));
    emit("update:modelValue", newConditions);
  },
});

const optionsFilter = ref("");

const filteredNodes = computed(() => {
  //convert into hash map first, much faster than using 'find'
  const map = {} as Record<string, IOntologyRespItem>;
  data.forEach((term) => {
    map[term.name] = term;
  });
  const parents: Set<IOntologyRespItem> = new Set();

  const filteredNodes = data.filter((node) => {
    const searchValue = optionsFilter.value.toLowerCase();

    if (
      node.name.toLowerCase().includes(searchValue) ||
      node.definition?.toLowerCase().includes(searchValue)
    ) {
      if (node.parent) {
        //get also the parents
        parents.add(map[node.parent.name]);
      }
      return true;
    }
  });

  return Array.from(new Set([...parents, ...filteredNodes]));
});

const filteredTree = computed(() => listToTree(filteredNodes.value));

const modal = ref<InstanceType<typeof Modal>>();

function showModal() {
  modal.value?.show();
}

function closeModal() {
  optionsFilter.value = "";
  modal.value?.close();
}

function removeSelectedNode(node: string) {
  selectedNodesNames.value = selectedNodesNames.value.filter((n) => n !== node);
}

function clearAll() {
  selectedNodesNames.value = [];
}
</script>

<template>
  <div v-if="showSearch" class="flex items-center py-1 -ml-2">
    <button class="flex items-center ml-8" @click="showModal">
      <BaseIcon
        name="search"
        :class="`text-search-filter-expand${mobileDisplay ? '-mobile' : ''}`"
        :width="18"
      />
      <span
        class="ml-2 text-body-sm hover:underline"
        :class="`text-search-filter-expand${mobileDisplay ? '-mobile' : ''}`"
      >
        Search for options
      </span>
    </button>

    <Modal ref="modal" title="Search" :subtitle="filterLabel">
      <template #header>
        <FilterSearch
          v-model="optionsFilter"
          :inverted="true"
          placeholder="search for options"
        ></FilterSearch>

        <div v-if="selectedNodesNames.length" class="py-2 text-gray-900">
          <div class="flex flex-wrap gap-3 content-around p-3">
            <span
              class="text-heading-sm text-gray-600 h-8 flex-row-reverse flex items-center"
              >Active filters:</span
            >
            <template v-if="selectedNodesNames.length > 5">
              <Button
                @click="clearAll"
                icon="trash"
                icon-position="right"
                size="tiny"
                type="filterWell"
              >
                {{ selectedNodesNames.length }} items selected
              </Button>
            </template>
            <template v-else>
              <template v-for="selectedNodeName in selectedNodesNames">
                <Button
                  @click="removeSelectedNode(selectedNodeName)"
                  icon="trash"
                  icon-position="right"
                  size="tiny"
                  type="filterWell"
                >
                  {{ selectedNodeName }}
                </Button>
              </template>
            </template>
            <Button
              icon="trash"
              icon-position="right"
              size="tiny"
              type="filterWell"
              @click="clearAll"
              >Remove all
            </Button>
          </div>
        </div>
      </template>

      <div class="pl-1 pb-3">
        <InputTree
          v-if="filteredTree.length"
          :nodes="filteredTree"
          v-model="selectedNodesNames"
          :isMultiSelect="true"
          :inverted="true"
          :expandSelected="true"
        />
        <div v-else class="text-gray-900 text-body-sm">No results found</div>
      </div>

      <template #footer>
        <button
          @click="closeModal"
          class="flex items-center border rounded-full h-10.5 px-5 text-heading-lg gap-3 tracking-widest uppercase font-display bg-button-primary text-button-primary border-button-primary hover:bg-button-primary-hover hover:text-button-primary-hover hover:border-button-primary-hover"
        >
          Show results
        </button>
      </template>
    </Modal>
  </div>
  <InputTree
    :nodes="rootNodes"
    v-model="selectedNodesNames"
    :isMultiSelect="true"
    :inverted="mobileDisplay"
    :expandSelected="true"
  >
  </InputTree>
</template>
