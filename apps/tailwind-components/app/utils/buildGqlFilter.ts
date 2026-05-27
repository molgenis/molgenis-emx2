import type { IColumn } from "../../../metadata-utils/src/types";
import type { IFilterValue, IGraphQLFilter } from "../../types/filters";

const ONTOLOGY_TYPES = ["ONTOLOGY", "ONTOLOGY_ARRAY"];

export function parseFilterTerms(input: string): string[] {
  const tokenRegex = /'([^']*)'|(\S+)/g;
  const terms: string[] = [];
  let match;

  while ((match = tokenRegex.exec(input)) !== null) {
    const token = match[1] ?? match[2]!;
    if (token.toLowerCase() !== "and") {
      terms.push(token);
    }
  }

  return terms.filter(Boolean);
}

export function setNestedValue(obj: any, path: string[], value: any): void {
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

export function equalsForBool(
  arr: string[],
  pathSegments: string[]
): { filterValueObj?: any; orClause?: any[] } {
  const hasNull = arr.includes("_null_");
  const boolValues = arr
    .filter((v) => v === "true" || v === "false")
    .map((v) => v === "true");

  if (hasNull && boolValues.length > 0) {
    const boolFilter: any = {};
    setNestedValue(boolFilter, pathSegments, {
      equals: boolValues.length === 1 ? boolValues[0] : boolValues,
    });
    const nullFilter: any = {};
    setNestedValue(nullFilter, pathSegments, { is_null: true });
    return { orClause: [boolFilter, nullFilter] };
  }

  if (hasNull) {
    return { filterValueObj: { is_null: true } };
  }
  if (boolValues.length === 1) {
    return { filterValueObj: { equals: boolValues[0] } };
  }
  return { filterValueObj: { equals: boolValues } };
}

export function equalsForDirectRef(
  arr: any[],
  column: IColumn,
  isDirectColumn: boolean,
  isRefType: boolean
): any {
  const hasPlainStringValues = arr.length > 0 && typeof arr[0] === "string";
  const hasMultiKeyObjects =
    arr.length > 0 &&
    typeof arr[0] === "object" &&
    arr[0] !== null &&
    Object.keys(arr[0] as Record<string, unknown>).length > 1;
  const hasAnyObjects =
    arr.length > 0 && typeof arr[0] === "object" && arr[0] !== null;

  if (isDirectColumn && column.refTableId && hasPlainStringValues) {
    return { equals: arr.map((v: any) => ({ name: v })) };
  }

  if (
    isDirectColumn &&
    column.refTableId &&
    (hasMultiKeyObjects || (!isRefType && hasAnyObjects))
  ) {
    return {
      _or: arr.map((keyObj: any) => {
        const entry: Record<string, { equals: unknown }> = {};
        for (const key of Object.keys(keyObj)) {
          entry[key] = { equals: keyObj[key] };
        }
        return entry;
      }),
    };
  }

  if (isRefType && hasAnyObjects) {
    const refField = Object.keys(arr[0] as Record<string, unknown>)[0]!;
    const refValues = arr.map(
      (v: any) => (v as Record<string, unknown>)[refField]
    );
    return { [refField]: { equals: refValues } };
  }

  return { _match_any: arr };
}

export function equalsForNestedRef(
  arr: any[],
  pathSegments: string[],
  resolvedType: string
): any {
  const refField = Object.keys(arr[0] as Record<string, unknown>)[0]!;
  const refValues = arr.map(
    (v: any) => (v as Record<string, unknown>)[refField]
  );
  const leafSegment = pathSegments[pathSegments.length - 1];

  if (ONTOLOGY_TYPES.includes(resolvedType)) {
    return { _match_any_including_children: refValues };
  }
  if (leafSegment === refField) {
    return { equals: refValues };
  }
  return { [refField]: { equals: refValues } };
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

          if (resolvedType === "BOOL") {
            const boolResult = equalsForBool(arr as string[], pathSegments);
            if (boolResult.orClause) {
              if (!filter._or) filter._or = [] as any;
              (filter._or as any[]).push(...boolResult.orClause);
              return;
            }
            filterValueObj = boolResult.filterValueObj;
          } else if (
            resolvedType === "RADIO" ||
            resolvedType === "CHECKBOX" ||
            ((resolvedType === "REF" ||
              resolvedType === "REF_ARRAY" ||
              resolvedType === "REFBACK" ||
              resolvedType === "SELECT" ||
              resolvedType === "MULTISELECT") &&
              pathSegments.length === 1)
          ) {
            const isDirectColumn = !columnTypeMap?.get(columnId);
            const isRefType =
              resolvedType === "REF" ||
              resolvedType === "REF_ARRAY" ||
              resolvedType === "REFBACK" ||
              resolvedType === "SELECT" ||
              resolvedType === "MULTISELECT";
            filterValueObj = equalsForDirectRef(
              arr,
              column,
              isDirectColumn,
              isRefType
            );
          } else if (
            arr.length > 0 &&
            typeof arr[0] === "object" &&
            arr[0] !== null
          ) {
            filterValueObj = equalsForNestedRef(
              arr,
              pathSegments,
              resolvedType
            );
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
          const terms = parseFilterTerms(value);
          if (terms.length <= 1) {
            filterValueObj = { like: terms[0] || value };
          } else {
            const likeFilters = terms.map((term: string) => {
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
        const [min, max] = value;
        const betweenFilter: Record<string, any> = {};
        if (min != null) betweenFilter.min = min;
        if (max != null) betweenFilter.max = max;
        if (Object.keys(betweenFilter).length > 0) {
          filterValueObj = { between: betweenFilter };
        }
        break;
      }

      case "notNull":
        filterValueObj = { is_null: false };
        break;

      case "isNull":
        filterValueObj = { is_null: true };
        break;
    }

    if (filterValueObj) {
      setNestedValue(filter, pathSegments, filterValueObj);
    }
  });

  return filter;
}
