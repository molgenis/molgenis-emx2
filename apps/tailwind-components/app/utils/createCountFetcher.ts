import type { IGraphQLFilter } from "../../types/filters";
import fetchGraphql from "../composables/fetchGraphql";

export interface ICountFetcher {
  fetchRefCounts(
    options: Map<string, Record<string, unknown>>
  ): Promise<Map<string, number>>;
  fetchOntologyLeafCounts(names: string[]): Promise<Map<string, number>>;
  fetchOntologyParentCounts(names: string[]): Promise<Map<string, number>>;
  getCrossFilter(): IGraphQLFilter | undefined;
}

function buildNestedFieldSelector(
  columnPath: string,
  keyField: string
): string {
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
    result = { [segments[i]!]: result };
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

async function _fetchAggCounts(
  schemaId: string,
  tableId: string,
  names: string[],
  buildItemFilter: (name: string) => any,
  crossFilter?: IGraphQLFilter
): Promise<Map<string, number>> {
  if (names.length === 0) return new Map();

  const aliases = names.map((name) => ({
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
    variables[`filter_${alias}`] = { ...crossFilter, ...buildItemFilter(name) };
  }

  const result = await fetchGraphql(schemaId, query, variables);
  const counts = new Map<string, number>();
  for (const { alias, name } of aliases) {
    const aggResult = result[alias];
    counts.set(name, aggResult?.count || 0);
  }
  return counts;
}

export function createCountFetcher(config: {
  schemaId: string;
  tableId: string;
  columnPath: string;
  getCrossFilter: () => IGraphQLFilter | undefined;
}): ICountFetcher {
  async function _fetchRefCounts(
    options: Map<string, Record<string, unknown>>,
    crossFilter?: IGraphQLFilter
  ): Promise<Map<string, number>> {
    if (options.size === 0) return new Map();

    const firstEntry = options.values().next().value;
    if (!firstEntry) return new Map();
    const keyField = Object.keys(firstEntry)[0] ?? "name";

    if (config.columnPath.includes(".")) {
      try {
        return await _fetchAggCounts(
          config.schemaId,
          config.tableId,
          [...options.keys()],
          (name) =>
            buildNestedFilterValue(config.columnPath, {
              [keyField]: { equals: [name] },
            }),
          crossFilter
        );
      } catch (error) {
        console.warn(
          `Failed to fetch ref counts for ${config.columnPath}:`,
          error
        );
        return new Map();
      }
    }

    const fieldSelector = buildNestedFieldSelector(config.columnPath, keyField);
    const names = [...options.keys()];

    const query = `
      query($filter: ${config.tableId}Filter) {
        ${config.tableId}_groupBy(filter: $filter) {
          count
          ${fieldSelector}
        }
      }
    `;

    const filterValue = buildNestedFilterValue(config.columnPath, {
      [keyField]: { equals: names },
    });

    try {
      const result = await fetchGraphql(config.schemaId, query, {
        filter: { ...crossFilter, ...filterValue },
      });

      const counts = new Map<string, number>();
      const groupByResults = result[`${config.tableId}_groupBy`];
      if (Array.isArray(groupByResults)) {
        for (const item of groupByResults) {
          const termObj = extractNestedValue(item, config.columnPath);
          if (termObj?.[keyField]) {
            counts.set(termObj[keyField], item.count || 0);
          }
        }
      }
      for (const label of options.keys()) {
        if (!counts.has(label)) {
          counts.set(label, 0);
        }
      }
      return counts;
    } catch (error) {
      console.warn(
        `Failed to fetch ref counts for ${config.columnPath}:`,
        error
      );
      return new Map();
    }
  }

  async function _fetchOntologyLeafCounts(
    names: string[],
    crossFilter?: IGraphQLFilter
  ): Promise<Map<string, number>> {
    if (names.length === 0) return new Map();

    if (config.columnPath.includes(".")) {
      try {
        return await _fetchAggCounts(
          config.schemaId,
          config.tableId,
          names,
          (name) =>
            buildNestedFilterValue(config.columnPath, {
              _match_any_including_children: name,
            }),
          crossFilter
        );
      } catch (error) {
        console.warn(
          `Failed to fetch ontology leaf counts for ${config.columnPath}:`,
          error
        );
        return new Map();
      }
    }

    const fieldSelector = buildNestedFieldSelector(config.columnPath, "name");

    const query = `
      query($filter: ${config.tableId}Filter) {
        ${config.tableId}_groupBy(filter: $filter) {
          count
          ${fieldSelector}
        }
      }
    `;

    const filterValue = buildNestedFilterValue(config.columnPath, {
      name: { equals: names },
    });

    try {
      const result = await fetchGraphql(config.schemaId, query, {
        filter: { ...crossFilter, ...filterValue },
      });

      const counts = new Map<string, number>();
      const groupByResults = result[`${config.tableId}_groupBy`];
      if (Array.isArray(groupByResults)) {
        for (const item of groupByResults) {
          const termObj = extractNestedValue(item, config.columnPath);
          if (termObj?.name) {
            counts.set(termObj.name, item.count || 0);
          }
        }
      }
      for (const name of names) {
        if (!counts.has(name)) {
          counts.set(name, 0);
        }
      }
      return counts;
    } catch (error) {
      console.warn(
        `Failed to fetch ontology leaf counts for ${config.columnPath}:`,
        error
      );
      return new Map();
    }
  }

  async function _fetchOntologyParentCounts(
    names: string[],
    crossFilter?: IGraphQLFilter
  ): Promise<Map<string, number>> {
    if (names.length === 0) return new Map();

    try {
      return await _fetchAggCounts(
        config.schemaId,
        config.tableId,
        names,
        (name) =>
          buildNestedFilterValue(config.columnPath, {
            _match_any_including_children: name,
          }),
        crossFilter
      );
    } catch (error) {
      console.warn(
        `Failed to fetch ontology parent counts for ${config.columnPath}:`,
        error
      );
      return new Map();
    }
  }

  return {
    getCrossFilter: config.getCrossFilter,
    fetchRefCounts: (options) =>
      _fetchRefCounts(options, config.getCrossFilter()),
    fetchOntologyLeafCounts: (names) =>
      _fetchOntologyLeafCounts(names, config.getCrossFilter()),
    fetchOntologyParentCounts: (names) =>
      _fetchOntologyParentCounts(names, config.getCrossFilter()),
  };
}
