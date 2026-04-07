import type { IColumn } from "../../../metadata-utils/src/types";
import type { IFilterValue, IGraphQLFilter } from "../../types/filters";

const ONTOLOGY_TYPES = ["ONTOLOGY", "ONTOLOGY_ARRAY"];

type ParsedTerms = { terms: string[] };

export function parseFilterTerms(input: string): ParsedTerms {
  const tokenRegex = /'([^']*)'|(\S+)/g;
  const tokens: string[] = [];
  let match;
  while ((match = tokenRegex.exec(input)) !== null) {
    tokens.push(match[1] ?? match[2]!);
  }

  const andIndices: number[] = [];
  for (let i = 0; i < tokens.length; i++) {
    if (tokens[i]!.toLowerCase() === "and") andIndices.push(i);
  }

  if (andIndices.length > 0) {
    const terms: string[] = [];
    let start = 0;
    for (const idx of andIndices) {
      const group = tokens.slice(start, idx).join(" ");
      if (group) terms.push(group);
      start = idx + 1;
    }
    const lastGroup = tokens.slice(start).join(" ");
    if (lastGroup) terms.push(lastGroup);
    return { terms: terms.filter(Boolean) };
  }

  return { terms: tokens.filter(Boolean) };
}

function setNestedValue(obj: any, path: string[], value: any): void {
  if (path.length === 0) return;
  let current = obj;
  for (let i = 0; i < path.length - 1; i++) {
    const key = path[i] as string;
    if (!current[key]) current[key] = {};
    current = current[key];
  }
  const lastKey = path[path.length - 1] as string;
  current[lastKey] = value;
}

export function buildGraphQLFilter(
  filterStates: Map<string, IFilterValue>,
  columns: IColumn[],
  searchValue?: string,
  columnTypeMap?: Map<string, string>
): IGraphQLFilter {
  const filter: IGraphQLFilter = {};

  if (searchValue && searchValue.trim()) {
    filter._search = searchValue.trim();
  }

  filterStates.forEach((filterValue, columnId) => {
    const pathSegments = columnId.split(".");
    const rootColumnId = pathSegments[0];
    const column = columns.find((c) => c.id === rootColumnId);
    if (!column) return;

    const { operator, value } = filterValue;

    let filterValueObj: any;
    switch (operator) {
      case "equals":
        if (typeof value === "object" && value !== null) {
          const arr = Array.isArray(value) ? value : [value];
          const resolvedType =
            columnTypeMap?.get(columnId) || column.columnType;
          if (arr.length > 0 && typeof arr[0] === "object" && arr[0] !== null) {
            const refField = Object.keys(arr[0] as Record<string, unknown>)[0]!;
            const refValues = arr.map(
              (v: any) => (v as Record<string, unknown>)[refField]
            );
            if (ONTOLOGY_TYPES.includes(resolvedType)) {
              filterValueObj = { _match_any_including_children: refValues };
            } else {
              filterValueObj = { [refField]: { equals: refValues } };
            }
          } else if (ONTOLOGY_TYPES.includes(resolvedType)) {
            filterValueObj = { _match_any_including_children: arr };
          } else {
            filterValueObj = { equals: arr };
          }
        } else {
          filterValueObj = { equals: value };
        }
        break;

      case "like": {
        if (typeof value === "string") {
          const parsed = parseFilterTerms(value);
          if (parsed.terms.length <= 1) {
            filterValueObj = { like: parsed.terms[0] || value };
          } else {
            const likeFilters = parsed.terms.map((term: string) => {
              const entry: any = {};
              setNestedValue(entry, pathSegments, { like: term });
              return entry;
            });
            if (!filter._and) filter._and = [] as any;
            (filter._and as any[]).push(...likeFilters);
            return;
          }
        } else {
          filterValueObj = { like: value };
        }
        break;
      }

      case "between": {
        const [min, max] = value as [any, any];
        const betweenFilter: Record<string, any> = {};
        if (min != null) betweenFilter.min = min;
        if (max != null) betweenFilter.max = max;
        if (Object.keys(betweenFilter).length > 0) {
          filterValueObj = { between: betweenFilter };
        }
        break;
      }

      case "notNull":
        filterValueObj = { notNull: true };
        break;

      case "isNull":
        filterValueObj = { isNull: true };
        break;
    }

    if (filterValueObj) {
      setNestedValue(filter, pathSegments, filterValueObj);
    }
  });

  return filter;
}
