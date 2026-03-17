import { ref, toValue, type MaybeRefOrGetter } from "vue";
import type { IGraphQLFilter } from "../../types/filters";
import fetchGraphql from "./fetchGraphql";

interface UseFilterCountsOptions {
  crossFilter: MaybeRefOrGetter<IGraphQLFilter | undefined>;
  schemaId: MaybeRefOrGetter<string | undefined>;
  tableId: MaybeRefOrGetter<string | undefined>;
  columnPath: MaybeRefOrGetter<string | undefined>;
  keyField: MaybeRefOrGetter<string>;
}

function buildNestedFieldSelector(columnPath: string, keyField: string): string {
  const segments = columnPath.split(".");
  let result = `${segments[segments.length - 1]} { ${keyField} }`;
  for (let i = segments.length - 2; i >= 0; i--) {
    result = `${segments[i]} { ${result} }`;
  }
  return result;
}

function buildNestedFilterValue(columnPath: string, value: any): any {
  const segments = columnPath.split(".");
  let result = value;
  for (let i = segments.length - 1; i >= 0; i--) {
    result = { [segments[i]]: result };
  }
  return result;
}

function extractNestedValue(obj: any, columnPath: string): any {
  const segments = columnPath.split(".");
  let current = obj;
  for (const segment of segments) {
    if (!current) return undefined;
    current = current[segment];
  }
  return current;
}

function sanitizeAlias(name: string): string {
  return "c_" + name.replace(/[^a-zA-Z0-9]/g, "_");
}

export function useFilterCounts(options: UseFilterCountsOptions) {
  const facetCounts = ref<Map<string, number>>(new Map());
  const countsLoading = ref(false);

  async function fetchCounts(names: string[]) {
    const crossFilter = toValue(options.crossFilter);
    const schemaId = toValue(options.schemaId);
    const tableId = toValue(options.tableId);
    const columnPath = toValue(options.columnPath);
    const keyField = toValue(options.keyField);

    if (!crossFilter || !schemaId || !tableId || !columnPath || names.length === 0) return;
    if (columnPath.includes(".")) return;

    countsLoading.value = true;

    const fieldSelector = buildNestedFieldSelector(columnPath, keyField);
    const query = `
      query($filter: ${tableId}Filter) {
        ${tableId}_groupBy(filter: $filter) {
          count
          ${fieldSelector}
        }
      }
    `;

    try {
      const filterValue = buildNestedFilterValue(columnPath, { [keyField]: { equals: names } });
      const result = await fetchGraphql(schemaId, query, {
        filter: { ...crossFilter, ...filterValue },
      });

      const groupByResults = result[`${tableId}_groupBy`];
      if (Array.isArray(groupByResults)) {
        for (const item of groupByResults) {
          const termObj = extractNestedValue(item, columnPath);
          if (termObj?.[keyField]) {
            facetCounts.value.set(termObj[keyField], item.count || 0);
          }
        }
      }

      for (const name of names) {
        if (!facetCounts.value.has(name)) {
          facetCounts.value.set(name, 0);
        }
      }

      facetCounts.value = new Map(facetCounts.value);
    } catch (error) {
      console.warn(`Failed to fetch counts for ${columnPath}:`, error);
    } finally {
      countsLoading.value = false;
    }
  }

  async function fetchParentCounts(parentNames: string[]) {
    const crossFilter = toValue(options.crossFilter);
    const schemaId = toValue(options.schemaId);
    const tableId = toValue(options.tableId);
    const columnPath = toValue(options.columnPath);

    if (!crossFilter || !schemaId || !tableId || !columnPath || parentNames.length === 0) return;
    if (columnPath.includes(".")) return;

    const aliases = parentNames.map((name) => ({
      alias: sanitizeAlias(name),
      name,
    }));

    const queryParts = aliases.map(
      ({ alias }) => `
      ${alias}: ${tableId}_agg(filter: $filter_${alias}) {
        count
      }
    `
    );

    const variableDefinitions = aliases
      .map(({ alias }) => `$filter_${alias}: ${tableId}Filter`)
      .join(", ");

    const query = `query(${variableDefinitions}) { ${queryParts.join("\n")} }`;

    const variables: Record<string, any> = {};
    for (const { alias, name } of aliases) {
      const nestedFilter = buildNestedFilterValue(columnPath, {
        _match_any_including_children: name,
      });
      variables[`filter_${alias}`] = { ...crossFilter, ...nestedFilter };
    }

    try {
      const result = await fetchGraphql(schemaId, query, variables);
      for (const { alias, name } of aliases) {
        const aggResult = result[alias];
        facetCounts.value.set(name, aggResult?.count || 0);
      }
      facetCounts.value = new Map(facetCounts.value);
    } catch (error) {
      console.warn(`Failed to fetch parent counts for ${columnPath}:`, error);
    }
  }

  return { facetCounts, countsLoading, fetchCounts, fetchParentCounts };
}
