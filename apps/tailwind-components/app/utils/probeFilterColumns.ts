import fetchGraphql from "../composables/fetchGraphql";
import type { ICountFetcher } from "./createCountFetcher";

const ONTOLOGY_TYPES = new Set(["ONTOLOGY", "ONTOLOGY_ARRAY"]);

function sanitizeAlias(columnPath: string): string {
  return "probe_" + columnPath.replace(/[^a-zA-Z0-9]/g, "_");
}

function buildNotNullFilter(columnPath: string): Record<string, any> {
  const segments = columnPath.split(".");
  let result: Record<string, any> = { _notNull: true };
  for (let i = segments.length - 1; i >= 0; i--) {
    result = { [segments[i]!]: result };
  }
  return result;
}

async function probeNotNullColumns(
  schemaId: string,
  tableId: string,
  columnPaths: string[]
): Promise<Set<string>> {
  const aliases = columnPaths.map((path) => ({
    alias: sanitizeAlias(path),
    path,
  }));

  const variableDefinitions = aliases
    .map(({ alias }) => `$filter_${alias}: ${tableId}Filter`)
    .join(", ");

  const queryParts = aliases.map(
    ({ alias }) =>
      `${alias}: ${tableId}_agg(filter: $filter_${alias}) { count }`
  );

  const query = `query(${variableDefinitions}) { ${queryParts.join("\n")} }`;

  const variables: Record<string, any> = {};
  for (const { alias, path } of aliases) {
    variables[`filter_${alias}`] = buildNotNullFilter(path);
  }

  try {
    const result = await fetchGraphql(schemaId, query, variables);
    const hasData = new Set<string>();
    for (const { alias, path } of aliases) {
      if (result[alias]?.count > 0) {
        hasData.add(path);
      }
    }
    return hasData;
  } catch {
    return new Set(columnPaths);
  }
}

async function probeOntologyColumns(
  ontologyPaths: string[],
  getCountFetcher: (path: string) => ICountFetcher
): Promise<Set<string>> {
  if (ontologyPaths.length === 0) return new Set();

  const hasData = new Set<string>();
  const results = await Promise.all(
    ontologyPaths.map(async (path) => {
      try {
        const counts = await getCountFetcher(path).fetchAllOntologyBaseCounts();
        return { path, hasRecords: counts.size > 0 };
      } catch {
        return { path, hasRecords: true };
      }
    })
  );
  for (const { path, hasRecords } of results) {
    if (hasRecords) hasData.add(path);
  }
  return hasData;
}

export async function probeFilterColumns(
  schemaId: string,
  tableId: string,
  columnPaths: string[],
  columnTypes?: Map<string, string>,
  getCountFetcher?: (path: string) => ICountFetcher
): Promise<Set<string>> {
  if (columnPaths.length === 0) return new Set();

  const ontologyPaths: string[] = [];
  const otherPaths: string[] = [];

  for (const path of columnPaths) {
    const colType = columnTypes?.get(path);
    if (colType && ONTOLOGY_TYPES.has(colType) && getCountFetcher) {
      ontologyPaths.push(path);
    } else {
      otherPaths.push(path);
    }
  }

  const [notNullResults, ontologyResults] = await Promise.all([
    otherPaths.length > 0
      ? probeNotNullColumns(schemaId, tableId, otherPaths)
      : Promise.resolve(new Set<string>()),
    getCountFetcher
      ? probeOntologyColumns(ontologyPaths, getCountFetcher)
      : Promise.resolve(new Set<string>(ontologyPaths)),
  ]);

  const combined = new Set<string>();
  for (const path of notNullResults) combined.add(path);
  for (const path of ontologyResults) combined.add(path);
  return combined;
}
