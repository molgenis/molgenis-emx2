import type { IGraphQLFilter } from "../../types/filters";

export interface CountedOption {
  name: string;
  label?: string;
  count: number;
  children?: CountedOption[];
}

const COUNTABLE_TYPES = new Set([
  "ONTOLOGY",
  "ONTOLOGY_ARRAY",
  "BOOL",
  "RADIO",
  "CHECKBOX",
]);

export function isCountableType(columnType: string): boolean {
  return COUNTABLE_TYPES.has(columnType);
}

function buildOntologyGroupByQuery(
  tableName: string,
  columnId: string,
  filter: IGraphQLFilter
): string {
  const filterArg =
    Object.keys(filter).length > 0
      ? `(filter: ${JSON.stringify(filter).replace(/"([^"]+)":/g, "$1:")})`
      : "";
  return `{
    ${tableName}_groupBy${filterArg} {
      count
      ${columnId} { name label parent { name } }
    }
  }`;
}

function buildFlatGroupByQuery(
  tableName: string,
  columnId: string,
  filter: IGraphQLFilter
): string {
  const filterArg =
    Object.keys(filter).length > 0
      ? `(filter: ${JSON.stringify(filter).replace(/"([^"]+)":/g, "$1:")})`
      : "";
  return `{
    ${tableName}_groupBy${filterArg} {
      count
      ${columnId}
    }
  }`;
}

function reconstructOntologyTree(
  rows: Array<{
    count: number;
    [key: string]: any;
  }>,
  columnId: string
): CountedOption[] {
  const nodeMap = new Map<string, CountedOption>();
  const roots: CountedOption[] = [];

  for (const row of rows) {
    const term = row[columnId];
    if (!term || !term.name) continue;
    const node: CountedOption = {
      name: term.name,
      label: term.label,
      count: row.count,
      children: [],
    };
    nodeMap.set(term.name, node);
  }

  for (const row of rows) {
    const term = row[columnId];
    if (!term || !term.name) continue;
    const node = nodeMap.get(term.name)!;
    const parentName = term.parent?.name;
    if (parentName && nodeMap.has(parentName)) {
      nodeMap.get(parentName)!.children!.push(node);
    } else {
      roots.push(node);
    }
  }

  return roots;
}

function serializeFilterForQuery(filter: IGraphQLFilter): string {
  if (Object.keys(filter).length === 0) return "";

  function valueToGql(val: unknown): string {
    if (val === null) return "null";
    if (typeof val === "string") return JSON.stringify(val);
    if (typeof val === "number" || typeof val === "boolean") return String(val);
    if (Array.isArray(val)) {
      return `[${val.map(valueToGql).join(", ")}]`;
    }
    if (typeof val === "object") {
      const obj = val as Record<string, unknown>;
      const entries = Object.entries(obj)
        .map(([k, v]) => `${k}: ${valueToGql(v)}`)
        .join(", ");
      return `{${entries}}`;
    }
    return JSON.stringify(val);
  }

  const entries = Object.entries(filter)
    .map(([k, v]) => `${k}: ${valueToGql(v)}`)
    .join(", ");
  return entries;
}

export async function fetchCounts(
  schemaId: string,
  tableId: string,
  columnId: string,
  columnType: string,
  crossFilter: IGraphQLFilter,
  fetcher: (schemaId: string, query: string, variables: any) => Promise<any>
): Promise<CountedOption[]> {
  const isOntology =
    columnType === "ONTOLOGY" || columnType === "ONTOLOGY_ARRAY";
  const filterStr = serializeFilterForQuery(crossFilter);
  const filterArg = filterStr ? `(filter: {${filterStr}})` : "";

  let query: string;
  if (isOntology) {
    query = `{
      ${tableId}_groupBy${filterArg} {
        count
        ${columnId} { name label parent { name } }
      }
    }`;
  } else {
    query = `{
      ${tableId}_groupBy${filterArg} {
        count
        ${columnId}
      }
    }`;
  }

  try {
    const data = await fetcher(schemaId, query, null);
    const rows: any[] = data?.[`${tableId}_groupBy`] ?? [];

    if (isOntology) {
      return reconstructOntologyTree(rows, columnId);
    }

    const isBool = columnType === "BOOL";
    if (isBool) {
      const countMap = new Map<string, number>();
      for (const row of rows) {
        const key =
          row[columnId] === null || row[columnId] === undefined
            ? "_null_"
            : String(row[columnId]);
        countMap.set(key, row.count);
      }
      return [
        { name: "true", label: "Yes", count: countMap.get("true") ?? 0 },
        { name: "false", label: "No", count: countMap.get("false") ?? 0 },
        {
          name: "_null_",
          label: "Not set",
          count: countMap.get("_null_") ?? 0,
        },
      ];
    }

    return rows
      .filter((row) => row[columnId] !== null && row[columnId] !== undefined)
      .map((row) => ({
        name: String(row[columnId]),
        count: row.count,
      }));
  } catch {
    return [];
  }
}
