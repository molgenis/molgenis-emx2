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
        <tree-component
          :facetIdentifier="facetIdentifier"
          v-if="displayOptions && selectedOntology == ontologyId"
          :options="displayOptions"
          :filter="ontologyQuery"
        />
        <spinner
          class="mt-4 mb-5"
          v-if="!displayOptions.length && selectedOntology == ontologyId"
        />
      </template>
    </div>
  </div>
</template>

<script>
import { useFiltersStore } from "../../stores/filtersStore";
import TreeComponent from "./base/TreeComponent.vue";
import { Spinner } from "../../../../molgenis-components";

export default {
  name: "OntologyFilter",
  components: {
    TreeComponent,
    Spinner,
  },
  setup() {
    const filtersStore = useFiltersStore();
    return { filtersStore };
  },
  props: {
    /** a JSON friendly identifier */
    facetIdentifier: {
      type: String,
      required: true,
    },
    ontologyIdentifiers: {
      type: Array,
      required: true,
    },
    /**
     * A Promise-function that resolves with an array of options.
     * { text: 'foo', value: 'bar' }
     */
    options: {
      type: [Function],
      required: true,
    },
  },
  data() {
    return {
      ontologyQuery: "",
      resolvedOptions: {},
      selectedOntology:
        this.ontologyIdentifiers[0] /** we start with the top one */,
    };
  },
  computed: {
    ontologyOptions() {
      return this.resolvedOptions || {};
    },
    displayOptions() {
      if (!this.ontologyQuery)
        return this.ontologyOptions[this.selectedOntology] || [];
      const matchingOptions = [];

      for (const ontologyItem of this.ontologyOptions[this.selectedOntology]) {
        if (
          this.filtersStore.ontologyItemMatchesQuery(
            ontologyItem,
            this.ontologyQuery
          )
        ) {
          matchingOptions.push(ontologyItem);
          continue;
        } else if (ontologyItem.children) {
          if (
            this.filtersStore.checkOntologyDescendantsIfMatches(
              ontologyItem.children,
              this.ontologyQuery
            )
          ) {
            matchingOptions.push(ontologyItem);
            continue;
          }
        }
      }
      return matchingOptions;
    },
  },
  created() {
    this.options().then((response) => {
      this.resolvedOptions = response;
    });
  },
};
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
