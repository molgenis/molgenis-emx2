<script setup lang="ts">
import type { ITreeNode } from "../../../tailwind-components/types/types";
import type { IOntologyRespItem } from "~/interfaces/types";

const props = withDefaults(
  defineProps<{
    tableId: string;
    modelValue: { name: string }[];
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
  ? (await fetchOntology(props.tableId)).data[props.tableId]
  : props.options;

const showSearch = computed(() => data?.length > 10);

function listToTree(list: IOntologyRespItem[]): ITreeNode[] {
  const allNodes = list.map((repsElement: IOntologyRespItem) => {
    return {
      name: repsElement.name,
      description: repsElement.definition,
      parent: repsElement.parent?.name,
      children: [] as ITreeNode[],
    };
  });

  for (let i = 0; i < allNodes.length; i++) {
    const node = allNodes[i];
    if (node.parent) {
      const parent = allNodes.find((n) => n.name === node.parent);
      if (parent) {
        parent.children.push(node);
      }
    }
  }

  return allNodes.filter((n) => !n.parent);
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

const isSearchModalOpen = ref(false);

const optionsFilter = ref("");

const filteredNodes = computed(() => {
  function addParents(
    node: IOntologyRespItem,
    parents: Set<IOntologyRespItem>
  ) {
    if (node.parent) {
      const parentName = node.parent.name;
      const parent = data.find((n) => n.name === parentName);
      if (parent) {
        parents.add(parent);
        addParents(parent, parents);
      }
    }
  }

  const parents: Set<IOntologyRespItem> = new Set();

  const filteredNodes = data.filter((node) => {
    const searchValue = optionsFilter.value.toLowerCase();

    if (
      node.name.toLowerCase().includes(searchValue) ||
      node.definition?.toLowerCase().includes(searchValue)
    ) {
      addParents(node, parents);
      return true;
    }
  });

  return Array.from(new Set([...parents, ...filteredNodes]));
});

const filteredTree = computed(() => listToTree(filteredNodes.value));
</script>

<template>
  <div v-if="showSearch" class="flex items-center py-1 -ml-2">
    <button class="flex items-center" @click="isSearchModalOpen = true">
      <BaseIcon name="search" class="text-search-filter-expand" :width="18" />
      <span class="ml-3 text-search-filter-expand text-body-sm hover:underline">
        Search
      </span>
    </button>
    <CustomTooltip
      label="Search"
      hoverColor="white"
      content="Search the ontology tree for options to filter on."
      class="ml-3"
    />
    <Modal :shown="isSearchModalOpen" @close="isSearchModalOpen = false">
      <section class="lg:px-12.5 px-4 text-gray-900 xl:rounded-3px py-8">
        <h2 class="mb-5 uppercase text-heading-4xl font-display">
          {{ filterLabel }}
        </h2>
        <FilterSearch v-model="optionsFilter" :inverted="true"></FilterSearch>
        <div v-if="selectedNodesNames.length" class="py-2">
          Active filters: {{ selectedNodesNames.join(", ") }}
        </div>
        <div class="mb-5 prose max-w-none">
          <InputTree
            :nodes="filteredTree"
            v-model="selectedNodesNames"
            :isMultiSelect="true"
            :mobileDisplay="mobileDisplay"
            :expandSelected="true"
            :inverted="true"
          >
          </InputTree>
        </div>
      </section>
    </Modal>
  </div>
  <InputTree
    :nodes="rootNodes"
    v-model="selectedNodesNames"
    :isMultiSelect="true"
    :mobileDisplay="mobileDisplay"
    :expandSelected="true"
  >
  </InputTree>
</template>
