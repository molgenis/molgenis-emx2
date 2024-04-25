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
    activeFilters(): Record<string, any> {
      return this.filtersStore.filters;
    },
    filterOptionsCache(): Record<string, { value: string; text: string }[]> {
      return this.filtersStore.filterOptionsCache;
    },
    matches() {
      let matches: IMatch[] = [];
      const facetIdentifiers = Object.keys(this.activeFilters);
      for (const facetIdentifier of facetIdentifiers) {
        const activeFilterValues = this.activeFilters[facetIdentifier];

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
              this.filterOptionsCache,
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
  filterOptionsCache: Record<string, any>,
  facetIdentifier: string
): IMatch {
  let match: IMatch = {
    name: filterLabel,
    value: [],
  };

  for (const activeFilterValue of activeFilterValues) {
    const optionsCache = filterOptionsCache[facetIdentifier];
    if (!optionsCache) {
      continue; /** if the filteroption does not exist */
    }
    if (!Array.isArray(optionsCache)) {
      const matches = getMultiOntologyMatch(optionsCache, activeFilterValue);
      match.value.push(...matches);
    } else {
      const filterOption = optionsCache.find(
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
  options: Record<
    string,
    { label: string; name: string; code: string; children: any; parent: any }[]
  >,
  activeFilterValue: { label: string; name: string; code: string }
) {
  return options.allItems
    .filter((option) => {
      return option.name === activeFilterValue.name;
    })
    .map((option) => {
      return option.label;
    });
}

function flattenOntology(
  ontology: Record<string, any>[]
): Record<string, any>[] {
  return ontology.reduce((accum: Record<string, any>[], item) => {
    const children = item.children ? flattenOntology(item.children) : [];
    accum.push(item, ...children);
    return accum;
  }, []);
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
