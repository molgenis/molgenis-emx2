<template>
  <div class="d-flex flex-column">
    <div :key="option.name" v-for="option of displayOptions">
      <TreeBranchComponent
        :option="option"
        :facetIdentifier="facetIdentifier"
        @indeterminate-update="signalParentOurIndeterminateStatus"
        :parentSelected="parentSelected"
        :filter="filter"
      />
    </div>
    <span
      v-if="filteredOptions.length > displaySize"
      class="badge badge-info mx-5 mb-2"
    >
      Too many results, showing the first {{ displaySize }}. Please use the
      search functionality.
    </span>
  </div>
</template>

<script setup lang="ts">
import { computed, toRefs } from "vue";
import { useFiltersStore } from "../../../stores/filtersStore";
import TreeBranchComponent from "./TreeBranchComponent.vue";

const filtersStore = useFiltersStore();
const displaySize = 100;

const props = withDefaults(
  defineProps<{
    facetIdentifier: string;
    options: any[];
    parentSelected?: boolean;
    filter?: string;
  }>(),
  { filter: "" }
);

const { facetIdentifier } = props;
const { filter, options, parentSelected } = toRefs(props);

const emit = defineEmits(["indeterminate-update"]);

const filteredOptions = computed(() => {
  const sortedOptions = sortOptions(options.value);
  return filterOptions(sortedOptions, filter.value);
});

const displayOptions = computed(() => {
  if (filteredOptions.value.length <= 100) {
    return filteredOptions.value;
  } else {
    return getTopPaginatedOptions();
  }
});

function getTopPaginatedOptions() {
  const topItems = filteredOptions.value.slice(0, displaySize);
  const currentSelectedOntologies: any[] =
    filtersStore.getFilterValue(facetIdentifier);

  if (!currentSelectedOntologies) {
    return topItems;
  } else {
    const currentSelectedOntologyCodes: any[] = currentSelectedOntologies.map(
      (ontologyItem) => ontologyItem.code
    );
    const currentDisplayedOntologyCodes = topItems.map((item) => item.code);
    const missingCodes = currentSelectedOntologyCodes.filter(
      (selectedOntologyItem) =>
        !currentDisplayedOntologyCodes.includes(selectedOntologyItem)
    );

    const missingOptions = filteredOptions.value.filter((option) =>
      missingCodes.includes(option.code)
    );

    return missingOptions.concat(topItems);
  }
}

function signalParentOurIndeterminateStatus(status: boolean) {
  emit("indeterminate-update", status);
}

function sortOptions(options: any[] | undefined) {
  if (options?.length) {
    let copy = JSON.parse(JSON.stringify(options));

    return copy.sort((a: { code: string }, b: { code: string }) => {
      if (a.code < b.code) {
        return -1;
      }
      if (a.code > b.code) {
        return 1;
      }
      return 0;
    });
  } else {
    return [];
  }
}

function filterOptions(options: any[], filter: string) {
  if (!filter) return options || [];
  const matchingOptions = [];

  for (const ontologyItem of options) {
    if (filtersStore.ontologyItemMatchesQuery(ontologyItem, filter)) {
      matchingOptions.push(ontologyItem);
      continue;
    } else if (ontologyItem.children) {
      if (
        filtersStore.checkOntologyDescendantsIfMatches(
          ontologyItem.children,
          filter
        )
      ) {
        matchingOptions.push(ontologyItem);
        continue;
      }
    }
  }
  return matchingOptions;
}
</script>

<style scoped>
ul {
  margin-right: 1rem;
  list-style-type: none;
}
</style>
