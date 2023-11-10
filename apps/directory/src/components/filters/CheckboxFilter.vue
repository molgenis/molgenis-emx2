<template>
  <div>
    <MatchTypeRadiobutton
      v-if="showMatchTypeSelector"
      class="p-2 pb-0"
      :matchTypeForFilter="facetIdentifier"
    />

    <div class="d-flex flex-column scrollable-content pt-2">
      <CheckboxComponent
        v-for="(option, index) of checkboxOptions"
        :key="index"
        v-model="filterSelection"
        :option="option"
      />
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
import MatchTypeRadiobutton from "./base/MatchTypeRadiobutton.vue";
import CheckboxComponent from "./base/CheckboxComponent.vue";
import { useSettingsStore } from "../../stores/settingsStore";
import { useFiltersStore } from "../../stores/filtersStore";

export default {
  setup() {
    const settingsStore = useSettingsStore();
    const filtersStore = useFiltersStore();
    return { settingsStore, filtersStore };
  },
  name: "CheckboxFilter",
  components: {
    MatchTypeRadiobutton,
    CheckboxComponent,
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
    /**
     * A Promise-function that resolves with an array of options.
     * { text: 'foo', value: 'bar' }
     */
    options: {
      type: [Function],
      required: true,
    },
    currentlyActive: {
      type: Boolean,
      required: false,
      default: () => false,
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
      resolvedOptions: [],
      reducedOptions: [],
    };
  },
  watch: {
    optionsFilter(newValue) {
      if (!this.currentlyActive || this.filterSelection.length === 0) {
        this.reducedOptions = newValue;
      }
    },
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
    checkboxOptions() {
      if (this.reducedOptions.length) {
        const selectedValues = this.filterSelection.map(
          (selection) => selection.value
        );

        return this.resolvedOptions.filter(
          (option) =>
            this.reducedOptions.includes(option.value) ||
            selectedValues.includes(option.value)
        );
      } else {
        return this.resolvedOptions;
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
    this.reducedOptions = this.optionsFilter || [];

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
</style>
