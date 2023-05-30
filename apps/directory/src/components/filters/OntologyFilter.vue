<template>
  <div>
    <div class="d-flex flex-column scrollable-content pt-2">
      <!-- <CheckboxComponent
        v-for="(option, index) of checkboxOptions"
        :key="index"
        v-model="filterSelection"
        :option="option"
      /> -->
      <template v-for="ontologyId of ontologyIdentifiers" :key="ontologyId">
        {{ ontologyId }}
        <tree-component
          v-if="ontologyOptions[ontologyId]"
          :options="ontologyOptions[ontologyId]"
        />
      </template>
    </div>
    <div>
      <button
        v-if="selectAll"
        type="button"
        class="btn btn-link p-2"
        @click.prevent="toggleSelect"
      >
        {{ selectAllText }}
      </button>
    </div>
  </div>
</template>

<script>
// import CheckboxComponent from "./base/CheckboxComponent.vue";
import { useSettingsStore } from "../../stores/settingsStore";
import { useFiltersStore } from "../../stores/filtersStore";
import TreeComponent from "./base/TreeComponent.vue";

export default {
  setup() {
    const settingsStore = useSettingsStore();
    const filtersStore = useFiltersStore();
    return { settingsStore, filtersStore };
  },
  name: "OntologyFilter",
  components: {
    TreeComponent,
    // MatchTypeRadiobutton,
    // CheckboxComponent,
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
    /**
     * This is the v-model value; an array of selected options.
     * Can also be a { text, value } object array
     */
    modelValue: {
      type: Array,
      default: () => [],
    },
    /**
     * Whether to use (De)Select All or not.
     */
    selectAll: {
      type: Boolean,
      required: false,
      default: () => true,
    },
    showMatchTypeSelector: {
      type: Boolean,
      default: () => false,
    },
  },
  data() {
    return {
      selection: [],
      resolvedOptions: {},
    };
  },
  computed: {
    uiText() {
      return this.settingsStore.uiText;
    },
    selectAllText() {
      if (this.filterSelection && this.filterSelection.length > 0) {
        return this.uiText["deselect_all"];
      } else {
        return this.uiText["select_all"];
      }
    },
    filterSelection: {
      get() {
        return this.filtersStore.getFilterValue(this.facetIdentifier) || [];
      },
      set(value) {
        this.filtersStore.updateFilter(this.facetIdentifier, value);
      },
    },
    ontologyOptions() {
      return this.resolvedOptions || {};
    },
  },
  methods: {
    toggleSelect() {
      if (this.filterSelection && this.filterSelection.length > 0) {
        this.filterSelection = [];
      } else {
        this.filterSelection = this.checkboxOptions;
      }
    },
  },
  created() {
    this.options().then((response) => {
      // const itemsSplitByOntology = {};

      // for (const ontologyItem of response) {
      //   for (const ontologyId of this.ontologyIdentifiers) {
      //     if (
      //       ontologyItem.value.toLowerCase().includes(ontologyId.toLowerCase())
      //     ) {
      //       if (!itemsSplitByOntology[ontologyId]) {
      //         itemsSplitByOntology[ontologyId] = [ontologyItem];
      //       } else {
      //         itemsSplitByOntology[ontologyId].push(ontologyItem);
      //       }
      //     }
      //   }
      // }
      this.resolvedOptions = response;
    });
  },
};
</script>

<style scoped>
.btn-link:focus {
  box-shadow: none;
}
</style>