import { columnValueToString } from "./columnValueToString";
import type { IGraphQLFilter } from "../../types/filters";
import type { ITreeNode } from "../../types/types";
import { getColumnIds } from "../composables/fetchTableData";

export interface CountedOption extends Omit<ITreeNode, "children"> {
  count: number;
  children?: CountedOption[];
  keyObject?: Record<string, any>;
}

interface OntologyTermNode {
  name: string;
  label?: string;
  parentName: string | null;
  count: number;
}

export async function fetchCounts(
  schemaId: string,
  tableId: string,
  columnId: string,
  columnType: string,
  crossFilter: IGraphQLFilter,
  fetcher: (schemaId: string, query: string, variables: any) => Promise<any>,
  refTableId?: string | null,
  refSchemaId?: string | null,
  refLabel?: string | null
): Promise<CountedOption[]> {
  if (columnType === "ONTOLOGY" || columnType === "ONTOLOGY_ARRAY") {
    return fetchOntologyWithAncestors(
      schemaId,
      tableId,
      columnId,
      columnType,
      crossFilter,
      refSchemaId ?? null,
      refTableId ?? null,
      fetcher
    );
  }

  if (columnType === "BOOL") {
    return fetchBoolGroupBy(schemaId, tableId, columnId, crossFilter, fetcher);
  }

  if (columnType === "RADIO" || columnType === "CHECKBOX") {
    let keyExpansion: string | undefined;
    if (refTableId) {
      keyExpansion = (
        await getColumnIds(refSchemaId ?? schemaId, refTableId, 0)
      ).trim();
    }
    return fetchFlatGroupBy(
      schemaId,
      tableId,
      columnId,
      crossFilter,
      fetcher,
      keyExpansion,
      refLabel
    );
  }

  return [];
}

// === STRATEGIES ===

async function fetchBoolGroupBy(
  schemaId: string,
  tableId: string,
  columnId: string,
  crossFilter: IGraphQLFilter,
  fetcher: (schemaId: string, query: string, variables: any) => Promise<any>
): Promise<CountedOption[]> {
  const filterArg = buildFilterArg(crossFilter);
  const segments = columnId.split(".");
  const fieldSelection =
    segments.length > 1 ? buildNestedField(segments, "") : columnId;

  let rows: any[];
  try {
    rows = await fireGroupByQuery(
      schemaId,
      tableId,
      fieldSelection,
      filterArg,
      fetcher
    );
  } catch {
    return [
      { name: "true", label: "Yes", count: 0 },
      { name: "false", label: "No", count: 0 },
      { name: "_null_", label: "Not set", count: 0 },
    ];
  }

  const countMap = new Map<string, number>();
  for (const row of rows) {
    const val =
      segments.length > 1 ? getNestedValue(row, segments) : row[columnId];
    const key = val === null || val === undefined ? "_null_" : String(val);
    countMap.set(key, row.count);
  }

  return [
    { name: "true", label: "Yes", count: countMap.get("true") ?? 0 },
    { name: "false", label: "No", count: countMap.get("false") ?? 0 },
    { name: "_null_", label: "Not set", count: countMap.get("_null_") ?? 0 },
  ];
}

async function fetchFlatGroupBy(
  schemaId: string,
  tableId: string,
  columnId: string,
  crossFilter: IGraphQLFilter,
  fetcher: (schemaId: string, query: string, variables: any) => Promise<any>,
  keyFieldExpansion?: string,
  refLabel?: string | null
): Promise<CountedOption[]> {
  const filterArg = buildFilterArg(crossFilter);
  const segments = columnId.split(".");

  let fieldSelection: string;
  if (keyFieldExpansion) {
    const leaf = `{ ${keyFieldExpansion} }`;
    fieldSelection =
      segments.length > 1
        ? buildNestedField(segments, leaf)
        : `${columnId} ${leaf}`;
  } else {
    fieldSelection =
      segments.length > 1 ? buildNestedField(segments, "") : columnId;
  }

  let rows: any[];
  try {
    rows = await fireGroupByQuery(
      schemaId,
      tableId,
      fieldSelection,
      filterArg,
      fetcher
    );
  } catch {
    return [];
  }

  return rows
    .map((row) => {
      const val =
        segments.length > 1 ? getNestedValue(row, segments) : row[columnId];
      if (keyFieldExpansion) {
        if (val === null || val === undefined || typeof val !== "object")
          return null;
        const entries = Object.entries(val).filter(
          ([, v]) => v !== null && v !== undefined
        );
        if (entries.length === 0) return null;
        const keyObject = Object.fromEntries(entries);
        const name = refLabel
          ? columnValueToString(val as any, refLabel) ?? ""
          : entries.length === 1
          ? String(entries[0]![1])
          : entries.map(([, v]) => String(v)).join(", ");
        return { name, keyObject, count: row.count };
      }
      if (val === null || val === undefined) return null;
      return { name: String(val), count: row.count };
    })
    .filter((item): item is CountedOption => item !== null);
}

async function fetchOntologyWithAncestors(
  schemaId: string,
  tableId: string,
  columnId: string,
  columnType: string,
  crossFilter: IGraphQLFilter,
  refSchemaId: string | null,
  refTableId: string | null,
  fetcher: (schemaId: string, query: string, variables: any) => Promise<any>
): Promise<CountedOption[]> {
  const terms = await groupByOntologyTerms(
    schemaId,
    tableId,
    columnId,
    crossFilter,
    fetcher
  );
  if (terms.size === 0) return [];

  const allTerms = await resolveOntologyAncestorChain(
    terms,
    refTableId,
    refSchemaId ?? schemaId,
    fetcher
  );
  const tree = buildTreeFromOntologyTerms(allTerms);

  if (columnType === "ONTOLOGY_ARRAY") {
    await fetchOntologyParentCountsFromServer(
      tree,
      schemaId,
      tableId,
      columnId,
      crossFilter,
      fetcher
    );
  } else {
    rollupOntologyParentCountsFromChildren(tree);
  }

  return tree;
}

// === ONTOLOGY SUB-STEPS ===

async function groupByOntologyTerms(
  schemaId: string,
  tableId: string,
  columnId: string,
  crossFilter: IGraphQLFilter,
  fetcher: (schemaId: string, query: string, variables: any) => Promise<any>
): Promise<Map<string, OntologyTermNode>> {
  const filterArg = buildFilterArg(crossFilter);
  const segments = columnId.split(".");
  const ontologyLeaf = "{ name label parent { name } }";
  const fieldSelection =
    segments.length > 1
      ? buildNestedField(segments, ontologyLeaf)
      : `${columnId} ${ontologyLeaf}`;

  let rows: any[];
  try {
    rows = await fireGroupByQuery(
      schemaId,
      tableId,
      fieldSelection,
      filterArg,
      fetcher
    );
  } catch {
    return new Map();
  }

  const knownTerms = new Map<string, OntologyTermNode>();
  for (const row of rows) {
    const term =
      segments.length > 1 ? getNestedValue(row, segments) : row[columnId];
    if (term?.name) {
      knownTerms.set(term.name, {
        name: term.name,
        label: term.label,
        parentName: term.parent?.name ?? null,
        count: row.count,
      });
    }
  }
  return knownTerms;
}

async function resolveOntologyAncestorChain(
  directTerms: Map<string, OntologyTermNode>,
  refTableId: string | null,
  refSchemaId: string,
  fetcher: (schemaId: string, query: string, variables: any) => Promise<any>
): Promise<Map<string, OntologyTermNode>> {
  if (!refTableId) return directTerms;

  const termNames = [...directTerms.keys()];
  const namesJson = termNames.map((n) => JSON.stringify(n)).join(", ");
  const ancestorQuery = `{ ${refTableId}(filter: {_match_any_including_parents: [${namesJson}]}) { name label parent { name } } }`;

  try {
    const ancData = await fetcher(refSchemaId, ancestorQuery, null);
    const ancRows: Array<{
      name: string;
      label?: string;
      parent?: { name: string } | null;
    }> = ancData?.[refTableId] ?? [];

    const allTerms = new Map(directTerms);
    for (const anc of ancRows) {
      if (!allTerms.has(anc.name)) {
        allTerms.set(anc.name, {
          name: anc.name,
          label: anc.label,
          parentName: anc.parent?.name ?? null,
          count: 0,
        });
      }
    }
    return allTerms;
  } catch {
    return directTerms;
  }
}

function rollupOntologyParentCountsFromChildren(nodes: CountedOption[]): void {
  for (const node of nodes) {
    if (node.children && node.children.length > 0) {
      rollupOntologyParentCountsFromChildren(node.children);
      if (node.count === 0) {
        node.count = node.children.reduce((sum, child) => sum + child.count, 0);
      }
    }
  }
}

async function fetchOntologyParentCountsFromServer(
  tree: CountedOption[],
  schemaId: string,
  tableId: string,
  columnId: string,
  crossFilter: IGraphQLFilter,
  fetcher: (schemaId: string, query: string, variables: any) => Promise<any>
): Promise<void> {
  const segments = columnId.split(".");
  const leafColumnId = segments[segments.length - 1]!;
  const parentNodes = collectTreeNodesWithChildren(tree);

  await Promise.all(
    parentNodes.map(async (parentNode) => {
      const parentFilter: IGraphQLFilter = {
        ...crossFilter,
        [leafColumnId]: {
          _match_any_including_children: [parentNode.name],
        },
      };
      const aggQuery = `{
            ${tableId}_agg(filter: {${serializeFilterForQuery(parentFilter)}}) {
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

function buildTreeFromOntologyTerms(
  knownTerms: Map<string, OntologyTermNode>
): CountedOption[] {
  const nodeMap = new Map<string, CountedOption>();
  for (const term of knownTerms.values()) {
    nodeMap.set(term.name, {
      name: term.name,
      label: term.label,
      count: term.count,
      children: [],
    });
  }

  const roots: CountedOption[] = [];
  for (const term of knownTerms.values()) {
    const node = nodeMap.get(term.name)!;
    if (term.parentName && nodeMap.has(term.parentName)) {
      nodeMap.get(term.parentName)!.children!.push(node);
    } else {
      roots.push(node);
    }
  }

  return roots;
}

function collectTreeNodesWithChildren(nodes: CountedOption[]): CountedOption[] {
  const parents: CountedOption[] = [];
  for (const node of nodes) {
    if (node.children && node.children.length > 0) {
      parents.push(node);
      parents.push(...collectTreeNodesWithChildren(node.children));
    }
  }
  return parents;
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

  return Object.entries(filter)
    .map(([k, v]) => `${k}: ${valueToGql(v)}`)
    .join(", ");
}

function buildFilterArg(filter: IGraphQLFilter): string {
  const filterStr = serializeFilterForQuery(filter);
  return filterStr ? `(filter: {${filterStr}})` : "";
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

async function fireGroupByQuery(
  schemaId: string,
  tableId: string,
  fieldSelection: string,
  filterArg: string,
  fetcher: (schemaId: string, query: string, variables: any) => Promise<any>
): Promise<any[]> {
  const query = `{
    ${tableId}_groupBy${filterArg} {
      count
      ${fieldSelection}
    }
  }`;
  const data = await fetcher(schemaId, query, null);
  return data?.[`${tableId}_groupBy`] ?? [];
}
