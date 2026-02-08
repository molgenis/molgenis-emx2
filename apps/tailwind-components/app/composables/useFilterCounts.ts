import { ref, watch, type Ref } from "vue";
import { useDebounceFn } from "@vueuse/core";
import type { IColumn } from "../../../metadata-utils/src/types";
import type { IFilterValue, IGraphQLFilter } from "../../types/filters";
import { buildGraphQLFilter } from "../utils/buildFilter";
import fetchGraphql from "./fetchGraphql";

const ONTOLOGY_TYPES = ["ONTOLOGY", "ONTOLOGY_ARRAY"];

interface UseFilterCountsOptions {
  schemaId: Ref<string>;
  tableId: Ref<string>;
  filterStates: Ref<Map<string, IFilterValue>>;
  columns: Ref<IColumn[]>;
  visibleFilterIds: Ref<string[]>;
  searchValue: Ref<string>;
}

function buildCrossFilter(
  filterStates: Map<string, IFilterValue>,
  excludeColumnId: string,
  columns: IColumn[],
  searchValue: string
): IGraphQLFilter {
  const crossFilterMap = new Map<string, IFilterValue>();
  filterStates.forEach((value, key) => {
    if (key !== excludeColumnId) {
      crossFilterMap.set(key, value);
    }
  });
  return buildGraphQLFilter(crossFilterMap, columns, searchValue);
}

function sanitizeAlias(name: string): string {
  return "c_" + name.replace(/[^a-zA-Z0-9]/g, "_");
}

interface ParentCountCache {
  columnId: string;
  crossFilterHash: string;
  counts: Map<string, number>;
}

export function useFilterCounts(options: UseFilterCountsOptions) {
  const facetCounts = ref<Map<string, Map<string, number>>>(new Map());
  const isLoading = ref(false);
  const parentCountCache = ref<ParentCountCache | null>(null);

  async function fetchLeafCounts() {
    const visibleOntologyColumns = options.visibleFilterIds.value
      .map((id) => options.columns.value.find((c) => c.id === id))
      .filter(
        (col): col is IColumn =>
          col !== undefined && ONTOLOGY_TYPES.includes(col.columnType)
      );

    if (visibleOntologyColumns.length === 0) {
      facetCounts.value.clear();
      return;
    }

    isLoading.value = true;
    const newFacetCounts = new Map<string, Map<string, number>>();

    for (const column of visibleOntologyColumns) {
      const crossFilter = buildCrossFilter(
        options.filterStates.value,
        column.id,
        options.columns.value,
        options.searchValue.value
      );

      const query = `
        query($filter: ${options.tableId.value}Filter) {
          ${options.tableId.value}_groupBy(filter: $filter) {
            count
            ${column.id} {
              name
            }
          }
        }
      `;

      try {
        const result = await fetchGraphql(options.schemaId.value, query, {
          filter: crossFilter,
        });

        const groupByResults = result[`${options.tableId.value}_groupBy`];
        const columnCounts = new Map<string, number>();

        if (Array.isArray(groupByResults)) {
          for (const item of groupByResults) {
            const termObj = item[column.id];
            if (termObj?.name) {
              columnCounts.set(termObj.name, item.count || 0);
            }
          }
        }

        newFacetCounts.set(column.id, columnCounts);
      } catch (error) {
        console.warn(`Failed to fetch counts for column ${column.id}:`, error);
        newFacetCounts.set(column.id, new Map());
      }
    }

    facetCounts.value = newFacetCounts;
    isLoading.value = false;
    parentCountCache.value = null;
  }

  const debouncedFetchLeafCounts = useDebounceFn(fetchLeafCounts, 500);

  watch(
    [
      options.filterStates,
      options.visibleFilterIds,
      options.searchValue,
      options.tableId,
      options.columns,
    ],
    () => {
      debouncedFetchLeafCounts();
    },
    { deep: true, immediate: true }
  );

  async function fetchParentCounts(
    columnId: string,
    parentNames: string[]
  ): Promise<Map<string, number>> {
    if (parentNames.length === 0) {
      return new Map();
    }

    const crossFilter = buildCrossFilter(
      options.filterStates.value,
      columnId,
      options.columns.value,
      options.searchValue.value
    );

    const crossFilterHash = JSON.stringify(crossFilter);

    if (
      parentCountCache.value?.columnId === columnId &&
      parentCountCache.value.crossFilterHash === crossFilterHash
    ) {
      const cached = parentCountCache.value.counts;
      const allCached = parentNames.every((name) => cached.has(name));
      if (allCached) {
        const result = new Map<string, number>();
        for (const name of parentNames) {
          result.set(name, cached.get(name) || 0);
        }
        return result;
      }
    }

    const aliases = parentNames.map((name) => ({
      alias: sanitizeAlias(name),
      name,
    }));

    const queryParts = aliases.map(
      ({ alias, name }) => `
      ${alias}: ${options.tableId.value}_agg(filter: $filter_${alias}) {
        count
      }
    `
    );

    const variableDefinitions = aliases
      .map(({ alias }) => `$filter_${alias}: ${options.tableId.value}Filter`)
      .join(", ");

    const query = `
      query(${variableDefinitions}) {
        ${queryParts.join("\n")}
      }
    `;

    const variables: Record<string, any> = {};
    for (const { alias, name } of aliases) {
      variables[`filter_${alias}`] = {
        ...crossFilter,
        [columnId]: {
          _match_any_including_children: name,
        },
      };
    }

    try {
      const result = await fetchGraphql(
        options.schemaId.value,
        query,
        variables
      );

      const counts = new Map<string, number>();
      for (const { alias, name } of aliases) {
        const aggResult = result[alias];
        counts.set(name, aggResult?.count || 0);
      }

      parentCountCache.value = {
        columnId,
        crossFilterHash,
        counts,
      };

      return counts;
    } catch (error) {
      console.warn(
        `Failed to fetch parent counts for column ${columnId}:`,
        error
      );
      return new Map();
    }
  }

  return {
    facetCounts,
    fetchParentCounts,
    isLoading,
  };
}
