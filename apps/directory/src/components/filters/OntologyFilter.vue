<template>
  <div>
    <div class="px-2 py-2">
      <button
        v-for="ontologyId of ontologyIdentifiers"
        :key="ontologyId + '-button'"
      >
        {{ ontologyId }}
      </button>
    </div>
    <hr class="p-0 m-0" />
    <div class="ontology pt-3">
      <template v-for="ontologyId of ontologyIdentifiers" :key="ontologyId">
        <tree-component
          :facetIdentifier="facetIdentifier"
          v-if="ontologyOptions[ontologyId] && ontologyShown == ontologyId"
          :options="ontologyOptions[ontologyId]"
        />
      </template>
    </div>
  </div>
</template>

<script>
import TreeComponent from "./base/TreeComponent.vue";

export default {
  name: "OntologyFilter",
  components: {
    TreeComponent,
  },
  props: {
    facetTitle: {
      type: String,
      required: true,
    },
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
    /**
     * An array that contains values of options
     * which is used to only show the checkboxes that match
     * these values
     */
    optionsFilter: {
      type: Array,
      required: false,
    },
    showMatchTypeSelector: {
      type: Boolean,
      default: () => false,
    },
  },
  data() {
    return {
      resolvedOptions: {},
      ontologyShown:
        this.ontologyIdentifiers[0] /** we start with the top one */,
    };
  },
  computed: {
    ontologyOptions() {
      return this.resolvedOptions || {};
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

.ontology {
  min-height: 70vh;
  max-width: 95vw;
  width: auto;
  overflow: auto;
  max-height: 15rem;
  white-space: nowrap;
}
</style>
