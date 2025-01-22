<template>
  <div>
    <div class="d-flex float-right">
      <MatchTypeRadiobutton
        v-if="showMatchTypeSelector"
        class="p-2 pb-0"
        :matchTypeForFilter="facetIdentifier"
      />
    </div>
    <div class="px-2 py-2 d-flex">
      <div class="buttonbar">
        <button
          v-for="ontologyId of ontologyIdentifiers"
          :key="ontologyId + '-button'"
          @click="setSelectedOntology(ontologyId)"
          :class="{ active: selectedOntology === ontologyId }"
        >
          {{ ontologyId }}
        </button>
      </div>
      <input
        type="text"
        placeholder="Search a disease"
        class="ml-2 ontology-search"
        :value="ontologyQuery"
        @input="handleSearchFieldChanged"
      />
    </div>
    <hr class="p-0 m-0" />
    <div class="ontology pt-3 d-flex">
      <div v-if="!resolvedOptions" class="d-flex w-100 justify-content-center">
        <Spinner class="mt-4 mb-5" />
      </div>
      <template
        v-else
        v-for="ontologyId of ontologyIdentifiers"
        :key="ontologyId"
      >
        <div v-show="selectedOntology === ontologyId" class="w-100">
          <div v-if="displayOptions.length">
            <MessageWarning
              class="mx-3"
              v-if="
                facetIdentifier === 'Diagnosisavailable' &&
                filtersStore.filters['Diagnosisavailable']?.length >= 50
              "
              >You can only select 50 items at the same time</MessageWarning
            >
            <TreeComponent
              :options="displayOptions"
              :filter="ontologyQuery"
              :facetIdentifier="facetIdentifier"
            />
          </div>
          <div v-else class="d-flex w-100 justify-content-center pb-3">
            No results found
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { useFiltersStore } from "../../stores/filtersStore";
import TreeComponent from "./base/TreeComponent.vue";
//@ts-ignore
import { Spinner } from "../../../../molgenis-components";
import MatchTypeRadiobutton from "./base/MatchTypeRadiobutton.vue";
import * as _ from "lodash";
import { MessageWarning } from "molgenis-components";

const filtersStore = useFiltersStore();

const { facetIdentifier, ontologyIdentifiers, options, showMatchTypeSelector } =
  defineProps<{
    facetIdentifier: string;
    ontologyIdentifiers: string[];
    options: Function;
    showMatchTypeSelector: boolean;
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
    } else if (
      ontologyItem.children &&
      filtersStore.checkOntologyDescendantsIfMatches(
        ontologyItem.children,
        ontologyQuery.value
      )
    ) {
      matchingOptions.push(ontologyItem);
    }
  }
  return matchingOptions;
});

const handleSearchFieldChanged = _.debounce((event: any) => {
  const newFilter = event.target?.value;
  ontologyQuery.value = newFilter || "";
}, 500);

function setSelectedOntology(ontologyId: string) {
  selectedOntology.value = ontologyId;
}
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
