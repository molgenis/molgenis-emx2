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

<script lang="ts">
import { IOntologyItem } from "../../interfaces/interfaces";
import { useFiltersStore } from "../../stores/filtersStore";
export default {
  setup() {
    const filtersStore = useFiltersStore();
    return { filtersStore };
  },
  props: {
    viewmodel: {
      type: Object,
      required: true,
    },
  },
  computed: {
    filterInfoDictionary() {
      return this.filtersStore.filterFacets.reduce(
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
    },
    matches() {
      let matches: IMatch[] = [];
      const facetIdentifiers = Object.keys(this.filtersStore.filters);
      for (const facetIdentifier of facetIdentifiers) {
        //@ts-ignore
        const activeFilterValues = this.filtersStore.filters[facetIdentifier];

        if (activeFilterValues?.length) {
          /** no need to check further if there are no active filters */

          const filterColumn: string | string[] =
            this.filterInfoDictionary[facetIdentifier].column;
          const columns: string[] = Array.isArray(filterColumn)
            ? filterColumn
            : [filterColumn];

          for (const columnId of columns) {
            const filterLabel =
              this.filterInfoDictionary[facetIdentifier].label;
            const potentialMatch = extractValue(columnId, this.viewmodel);

            if (!potentialMatch) {
              continue;
            }
            const match: IMatch = getMatch(
              potentialMatch,
              filterLabel,
              activeFilterValues,
              this.filtersStore.filterOptionsCache,
              facetIdentifier
            );

            if (match.value.length > 0) {
              /** check if it is there, because it can already have been matched on biobank / collection. Do not need it twice */
              const existingMatch = matches.find(
                (match) => match.name === filterLabel
              );
              if (existingMatch) {
                match.value = [
                  //@ts-ignore
                  ...new Set(match.value.concat(existingMatch.value)),
                ];
                matches = matches.filter((match) => match.name !== filterLabel);
              }
              matches.push(match);
            }
          }
        }
      }
      return matches;
    },
  },
};

function getMatch(
  potentialMatch:
    | { id: string; name: string; label: string }
    | { id: string; name: string; label: string }[],
  filterLabel: string,
  activeFilterValues: any[],
  filterOptions: Record<string, any>,
  facetIdentifier: string
): IMatch {
  let match: IMatch = {
    name: filterLabel,
    value: [],
  };

  for (const activeFilterValue of activeFilterValues) {
    const options = filterOptions[facetIdentifier];
    if (!options) {
      continue; /** if the filteroption does not exist */
    }
    if (!Array.isArray(options)) {
      const ontologyMatch = getMultiOntologyMatch(options, activeFilterValue);
      if (ontologyMatch) {
        match.value.push(ontologyMatch);
      }
    } else {
      const filterOption = options.find(
        (option: Record<string, any>) =>
          option.value === activeFilterValue.value
      );

      if (!filterOption) continue;

      const filterValue = filterOption.value;

      const isArrayMatch: boolean =
        Array.isArray(potentialMatch) &&
        potentialMatch.some(
          (item) =>
            item.id === filterValue ||
            item.name === filterValue ||
            item.label === filterValue
        );

      const isObjectMatch: boolean =
        //@ts-ignore
        typeof potentialMatch === "object" && filterValue === potentialMatch.id;
      const isValueMatch: boolean =
        filterValue.toString() === potentialMatch.toString();

      if (isArrayMatch || isObjectMatch || isValueMatch) {
        match.value.push(filterOption.text);
      }
    }
  }
  return match;
}

function getMultiOntologyMatch(
  options: Record<string, IOntologyItem[]>,
  activeFilterValue: IOntologyItem
) {
  return options.allItems.find((value) => value.name === activeFilterValue.name)
    ?.name;
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
