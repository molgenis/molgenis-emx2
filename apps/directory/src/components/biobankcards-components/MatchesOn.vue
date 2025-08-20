<template>
  <div class="mx-1" v-if="matches.length">
    <div class="d-flex align-items-center flex-wrap">
      <label class="font-weight-bold mr-1">Because you searched for:</label>
      <span
        class="badge badge-info mb-1 mr-1"
        v-for="match in matches"
        :key="match.name"
      >
        {{ match.value.join(", ") }}
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useFiltersStore } from "../../stores/filtersStore";
import { IOntologyItem } from "../../interfaces/interfaces";
const filtersStore = useFiltersStore();

const { viewmodel } = defineProps<{
  viewmodel: Record<string, any>;
}>();
const filterInfoDictionary = computed(() => {
  return filtersStore.filterFacets.reduce(
    (
      accum: Record<string, { column: any; label: string }>,
      filter: {
        facetIdentifier: string;
        applyToColumn: any;
        facetTitle: string;
      }
    ) => {
      accum[filter.facetIdentifier] = {
        column: filter.applyToColumn,
        label: filter.facetTitle,
      };
      return accum;
    },
    {}
  );
});

const matches = computed(() => {
  let matches: IMatch[] = [];
  const filters: Record<string, any> = filtersStore.filters;
  const filterIds = Object.keys(filters);
  for (const filterId of filterIds) {
    const activeFilterValues = filters[filterId];

    if (activeFilterValues?.length) {
      /** no need to check further if there are no active filters */

      const filterColumn: string | string[] =
        filterInfoDictionary.value[filterId].column;
      const columns: string[] = Array.isArray(filterColumn)
        ? filterColumn
        : [filterColumn];

      for (const columnId of columns) {
        const filterName = filterInfoDictionary.value[filterId].label;
        const potentialMatch = extractValue(columnId, viewmodel);

        if (!potentialMatch) {
          continue;
        }
        const match: IMatch = getMatch(
          potentialMatch,
          filterName,
          activeFilterValues,
          filtersStore.filterOptionsCache,
          filterId
        );

        if (match.value.length > 0) {
          /** check if it is there, because it can already have been matched on biobank / collection. Do not need it twice */
          const existingMatch = matches.find(
            (match) => match.name === filterName
          );
          if (existingMatch) {
            match.value = [
              //@ts-ignore
              ...new Set(match.value.concat(existingMatch.value)),
            ];
            matches = matches.filter((match) => match.name !== filterName);
          }
          matches.push(match);
        }
      }
    }
  }
  return matches;
});

function getMatch(
  potentialMatch: string | IOntologyItem | IOntologyItem[],
  filterLabel: string,
  activeFilterValues: any[],
  filterOptions: Record<string, any>,
  filterId: string
): IMatch {
  let match: IMatch = {
    name: filterLabel,
    value: [],
  };

  for (const filter of activeFilterValues) {
    const options = filterOptions[filterId];
    if (!options) {
      continue; /** if the filteroption does not exist */
    }
    if (!Array.isArray(options)) {
      if (doesMatch(potentialMatch, filter.name)) {
        match.value.push(filter.label);
      }
    } else {
      const filterOption = options.find(
        (option: Record<string, any>) => option.value === filter.value
      );

      if (!filterOption) continue;

      const filterValue = filterOption.value;
      if (doesMatch(potentialMatch, filterValue)) {
        match.value.push(filterOption.text);
      }
    }
  }
  return match;
}

function doesMatch(potentialMatch: any, filterValue: any) {
  return (
    isArrayMatch(potentialMatch, filterValue) ||
    isObjectMatch(potentialMatch, filterValue) ||
    isStringMatch(potentialMatch, filterValue)
  );
}

function isStringMatch(potentialMatch: any, filterValue: string) {
  return filterValue.toString() === potentialMatch.toString();
}

function isObjectMatch(potentialMatch: any, filterValue: string) {
  return (
    typeof potentialMatch === "object" && filterValue === potentialMatch.id
  );
}

function isArrayMatch(potentialMatch: any, filterValue: string): boolean {
  return (
    Array.isArray(potentialMatch) &&
    potentialMatch.some(
      (item) =>
        item.id === filterValue ||
        item.name === filterValue ||
        item.label === filterValue
    )
  );
}

function extractValue(columnId: string, viewModel: Record<string, any>) {
  if (!columnId) return "";

  const pathParts = columnId.split(".");

  let value;
  for (const path of pathParts) {
    if (!value) {
      value = viewModel[path];
    } else if (Array.isArray(value)) {
      return value;
    } else {
      value = value[path];
    }
  }
  return value;
}

interface IMatch {
  name: string;
  value: string[];
}
</script>
