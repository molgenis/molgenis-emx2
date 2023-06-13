<template>
  <div :key="option.name" v-for="option of displayOptions">
    <tree-branch-component
      :option="option"
      :facetIdentifier="facetIdentifier"
      @indeterminate-update="signalParentOurIndeterminateStatus"
      :parentSelected="parentSelected"
      :filter="filter"
    />
  </div>
</template>

<script>
import { useFiltersStore } from "../../../stores/filtersStore";
import TreeBranchComponent from "./TreeBranchComponent.vue";
export default {
  name: "TreeComponent",
  components: {
    TreeBranchComponent,
  },
  setup() {
    const filtersStore = useFiltersStore();
    return { filtersStore };
  },
  props: {
    facetIdentifier: {
      type: String,
      required: true,
    },
    options: {
      type: Array,
      required: true,
    },
    parentSelected: {
      type: Boolean,
      required: false,
      default: () => false,
    },
    filter: {
      type: String,
      required: false,
      default: () => "",
    },
  },
  emits: ["indeterminate-update"],
  data() {
    return {
      displaySize: 100,
    };
  },
  computed: {
    displayOptions() {
      if (this.filteredOptions.length <= 100) return this.filteredOptions;
      else return this.filteredOptions.slice(0, this.displaySize);
    },
    filteredOptions() {
      if (!this.filter) return this.sortedOptions || [];
      const matchingOptions = [];

      for (const ontologyItem of this.sortedOptions) {
        if (
          this.filtersStore.ontologyItemMatchesQuery(ontologyItem, this.filter)
        ) {
          matchingOptions.push(ontologyItem);
          continue;
        } else if (ontologyItem.children) {
          if (
            this.filtersStore.checkOntologyDescendantsIfMatches(
              ontologyItem.children,
              this.filter
            )
          ) {
            matchingOptions.push(ontologyItem);
            continue;
          }
        }
      }
      return matchingOptions;
    },
    sortedOptions() {
      if (this.options) {
        const copy = JSON.parse(JSON.stringify(this.options));

        return copy.sort(function (a, b) {
          if (a.code < b.code) {
            return -1;
          }
          if (a.code > b.code) {
            return 1;
          }
          return 0;
        });
      } else return [];
    },
  },
  methods: {
    signalParentOurIndeterminateStatus(status) {
      this.$emit("indeterminate-update", status);
    },
  },
};
</script>

<style scoped>
ul {
  margin-right: 1rem;
  list-style-type: none;
}
</style>