import fetchGraphql from "../composables/fetchGraphql";

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

export async function probeFilterColumns(
  schemaId: string,
  tableId: string,
  columnPaths: string[]
): Promise<Set<string>> {
  if (columnPaths.length === 0) return new Set();

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
