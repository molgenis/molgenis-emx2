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

const ontologyTreeCache = new Map<string, CountedOption[]>();

function ontologyTreeCacheKey(refSchemaId: string, refTableId: string): string {
  return `${refSchemaId}::${refTableId}`;
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

function buildNestedField(segments: string[], leaf: string): string {
  if (segments.length === 1) return `${segments[0]}${leaf ? " " + leaf : ""}`;
  return `${segments[0]} { ${buildNestedField(segments.slice(1), leaf)} }`;
}

function getNestedValue(row: any, segments: string[]): any {
  let current = row;
  for (const seg of segments) {
    if (current == null) return null;
    current = current[seg];
  }
  return current;
}

async function fetchFullOntologyTree(
  refSchemaId: string,
  refTableId: string,
  fetcher: (schemaId: string, query: string, variables: any) => Promise<any>
): Promise<CountedOption[]> {
  const cacheKey = ontologyTreeCacheKey(refSchemaId, refTableId);
  if (ontologyTreeCache.has(cacheKey)) {
    return ontologyTreeCache.get(cacheKey)!;
  }

  const query = `{ ${refTableId} { name label parent { name } } }`;

  try {
    const data = await fetcher(refSchemaId, query, null);
    const terms: Array<{
      name: string;
      label?: string;
      parent?: { name: string } | null;
    }> = data?.[refTableId] ?? [];

    const nodeMap = new Map<string, CountedOption>();
    for (const term of terms) {
      nodeMap.set(term.name, {
        name: term.name,
        label: term.label,
        count: 0,
        children: [],
      });
    }

    const roots: CountedOption[] = [];
    for (const term of terms) {
      const node = nodeMap.get(term.name)!;
      const parentName = term.parent?.name;
      if (parentName && nodeMap.has(parentName)) {
        nodeMap.get(parentName)!.children!.push(node);
      } else {
        roots.push(node);
      }
    }

    ontologyTreeCache.set(cacheKey, roots);
    return roots;
  } catch {
    return [];
  }
}

function applyLeafCountsToTree(
  nodes: CountedOption[],
  countMap: Map<string, number>
): void {
  for (const node of nodes) {
    node.count = countMap.get(node.name) ?? 0;
    if (node.children && node.children.length > 0) {
      applyLeafCountsToTree(node.children, countMap);
    }
  }
}

function collectParentNodes(nodes: CountedOption[]): CountedOption[] {
  const parents: CountedOption[] = [];
  for (const node of nodes) {
    if (node.children && node.children.length > 0) {
      parents.push(node);
      parents.push(...collectParentNodes(node.children));
    }
  }
  return parents;
}

function pruneEmptyBranches(nodes: CountedOption[]): CountedOption[] {
  const result: CountedOption[] = [];
  for (const node of nodes) {
    const prunedChildren = node.children
      ? pruneEmptyBranches(node.children)
      : [];
    const hasCount = node.count > 0;
    const hasVisibleChildren = prunedChildren.length > 0;
    if (hasCount || hasVisibleChildren) {
      result.push({ ...node, children: prunedChildren });
    }
  }
  return result;
}

function cloneTree(nodes: CountedOption[]): CountedOption[] {
  return nodes.map((node) => ({
    ...node,
    children: node.children ? cloneTree(node.children) : [],
  }));
}

async function fetchOntologyCounts(
  schemaId: string,
  tableId: string,
  columnId: string,
  crossFilter: IGraphQLFilter,
  refSchemaId: string,
  refTableId: string,
  fetcher: (schemaId: string, query: string, variables: any) => Promise<any>
): Promise<CountedOption[]> {
  const fullTree = await fetchFullOntologyTree(
    refSchemaId,
    refTableId,
    fetcher
  );
  if (fullTree.length === 0) {
    return [];
  }

  const treeClone = cloneTree(fullTree);

  const filterStr = serializeFilterForQuery(crossFilter);
  const filterArg = filterStr ? `(filter: {${filterStr}})` : "";
  const segments = columnId.split(".");
  const isNested = segments.length > 1;

  let fieldSelection: string;
  const leaf = "{ name label parent { name } }";
  fieldSelection = isNested
    ? buildNestedField(segments, leaf)
    : `${columnId} { name label parent { name } }`;

  const groupByQuery = `{
    ${tableId}_groupBy${filterArg} {
      count
      ${fieldSelection}
    }
  }`;

  let leafCountMap = new Map<string, number>();
  try {
    const data = await fetcher(schemaId, groupByQuery, null);
    const rows: any[] = data?.[`${tableId}_groupBy`] ?? [];
    for (const row of rows) {
      const term = isNested ? getNestedValue(row, segments) : row[columnId];
      if (term?.name) {
        leafCountMap.set(term.name, row.count);
      }
    }
  } catch {
    leafCountMap = new Map();
  }

  applyLeafCountsToTree(treeClone, leafCountMap);

  const parentNodes = collectParentNodes(treeClone);
  if (parentNodes.length > 0) {
    const leafColumnId = segments[segments.length - 1]!;

    await Promise.all(
      parentNodes.map(async (parentNode) => {
        const parentFilter: IGraphQLFilter = {
          ...crossFilter,
          [leafColumnId]: {
            _match_any_including_children: [parentNode.name],
          },
        };
        const parentFilterStr = serializeFilterForQuery(parentFilter);
        const aggQuery = `{
          ${tableId}_agg(filter: {${parentFilterStr}}) {
            count
          }
        }`;
        try {
          const aggData = await fetcher(schemaId, aggQuery, null);
          parentNode.count = aggData?.[`${tableId}_agg`]?.count ?? 0;
        } catch {
          parentNode.count = 0;
        }
      })
    );
  }

  return pruneEmptyBranches(treeClone);
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

export async function fetchCounts(
  schemaId: string,
  tableId: string,
  columnId: string,
  columnType: string,
  crossFilter: IGraphQLFilter,
  fetcher: (schemaId: string, query: string, variables: any) => Promise<any>,
  refTableId?: string | null,
  refSchemaId?: string | null
): Promise<CountedOption[]> {
  const isOntology =
    columnType === "ONTOLOGY" || columnType === "ONTOLOGY_ARRAY";
  const filterStr = serializeFilterForQuery(crossFilter);
  const filterArg = filterStr ? `(filter: {${filterStr}})` : "";
  const segments = columnId.split(".");
  const isNested = segments.length > 1;

  if (isOntology && refTableId) {
    const resolvedRefSchemaId = refSchemaId || schemaId;
    return fetchOntologyCounts(
      schemaId,
      tableId,
      columnId,
      crossFilter,
      resolvedRefSchemaId,
      refTableId,
      fetcher
    );
  }

  let fieldSelection: string;
  if (isOntology) {
    const leaf = "{ name label parent { name } }";
    fieldSelection = isNested
      ? buildNestedField(segments, leaf)
      : `${columnId} { name label parent { name } }`;
  } else {
    fieldSelection = isNested ? buildNestedField(segments, "") : columnId;
  }

  const query = `{
      ${tableId}_groupBy${filterArg} {
        count
        ${fieldSelection}
      }
    }`;

  try {
    const data = await fetcher(schemaId, query, null);
    const rows: any[] = data?.[`${tableId}_groupBy`] ?? [];

    if (isOntology) {
      if (isNested) {
        return reconstructOntologyTree(
          rows.map((row) => ({
            ...row,
            [columnId]: getNestedValue(row, segments),
          })),
          columnId
        );
      }
      return reconstructOntologyTree(rows, columnId);
    }

    const isBool = columnType === "BOOL";
    if (isBool) {
      const countMap = new Map<string, number>();
      for (const row of rows) {
        const val = isNested ? getNestedValue(row, segments) : row[columnId];
        const key = val === null || val === undefined ? "_null_" : String(val);
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
      .filter((row) => {
        const val = isNested ? getNestedValue(row, segments) : row[columnId];
        return val !== null && val !== undefined;
      })
      .map((row) => {
        const val = isNested ? getNestedValue(row, segments) : row[columnId];
        return { name: String(val), count: row.count };
      });
  } catch {
    return [];
  }
}

export { ontologyTreeCache };
