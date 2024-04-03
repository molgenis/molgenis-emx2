<template>
  <div>
    <div class="px-2 py-2 d-flex">
      <div class="buttonbar">
        <button
          v-for="ontologyId of ontologyIdentifiers"
          :key="ontologyId + '-button'"
          @click="selectedOntology = ontologyId"
          :class="{ active: selectedOntology === ontologyId }"
        >
          {{ ontologyId }}
        </button>
      </div>
      <input
        type="text"
        placeholder="Search a disease"
        class="ml-2 ontology-search"
        v-model="ontologyQuery"
      />
    </div>
    <hr class="p-0 m-0" />
    <div class="ontology pt-3 d-flex justify-content-center">
      <template v-for="ontologyId of ontologyIdentifiers" :key="ontologyId">
        <div v-show="selectedOntology === ontologyId">
          <spinner class="mt-4 mb-5" v-if="!resolvedOptions" />
          <tree-component
            v-else-if="displayOptions.length"
            :options="displayOptions"
            :filter="ontologyQuery"
            :facetIdentifier="facetIdentifier"
          />
          <div v-else class="pb-3">No results found</div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useFiltersStore } from "../../stores/filtersStore";
import TreeComponent from "./base/TreeComponent.vue";
//@ts-ignore
import { Spinner } from "../../../../molgenis-components";
import { computed, ref } from "vue";

const filtersStore = useFiltersStore();

const { facetIdentifier, ontologyIdentifiers, options } = defineProps<{
  facetIdentifier: string;
  ontologyIdentifiers: string[];
  options: Function;
}>();

let ontologyQuery = ref("");
let resolvedOptions = ref<Record<string, any> | undefined>(undefined);
let selectedOntology = ref(ontologyIdentifiers[0]);

options()
  .then((response: any) => {
    resolvedOptions.value = response || {};
  })
  .catch((error: any) => {
    console.log(`Error resolving ontology facet options: ${error}`);
  });

let displayOptions = computed(() => {
  if (!resolvedOptions.value) return [];
  if (!ontologyQuery.value) {
    return resolvedOptions.value[selectedOntology.value] || [];
  }

  let matchingOptions = [];
  for (const ontologyItem of resolvedOptions.value[selectedOntology.value]) {
    if (
      filtersStore.ontologyItemMatchesQuery(ontologyItem, ontologyQuery.value)
    ) {
      matchingOptions.push(ontologyItem);
      continue;
    } else if (ontologyItem.children) {
      if (
        filtersStore.checkOntologyDescendantsIfMatches(
          ontologyItem.children,
          ontologyQuery.value
        )
      ) {
        matchingOptions.push(ontologyItem);
        continue;
      }
    }
  }
  return matchingOptions;
});
</script>

<style scoped>
.btn-link:focus {
  box-shadow: none;
}

.buttonbar > button {
  border-radius: 0;
}

.buttonbar > button:focus {
  outline: none;
}

.buttonbar > button:focus-visible {
  outline: dotted 1px;
  outline: -webkit-focus-ring-color auto 5px;
}

.buttonbar > button:first-child {
  border-top-left-radius: 8px;
  border-bottom-left-radius: 8px;
}
.buttonbar > button:last-child {
  border-left-width: 0;
  border-top-right-radius: 8px;
  border-bottom-right-radius: 8px;
}

.buttonbar > button.active {
  border-style: inset;
}

.ontology {
  max-width: 95vw;
  min-width: 30rem;
  width: auto;
  overflow: auto;
  max-height: 25rem;
  white-space: nowrap;
}

.ontology-search {
  width: 100%;
}
</style>
