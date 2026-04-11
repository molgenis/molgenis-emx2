import type { IColumn, columnValue } from "../../../metadata-utils/src/types";
import type { IFilterValue } from "../../types/filters";
import type { CountedOption } from "./fetchCounts";

export const MAX_NESTING_DEPTH = 5;

export const REF_EXPANDABLE_TYPES = [
  "REF",
  "REF_ARRAY",
  "SELECT",
  "MULTISELECT",
  "REFBACK",
];

export const COUNTABLE_TYPES = new Set([
  "ONTOLOGY",
  "ONTOLOGY_ARRAY",
  "BOOL",
  "RADIO",
  "CHECKBOX",
]);

export const REF_FILTER_TYPES = new Set(["RADIO", "CHECKBOX"]);

export const isRefFilterType = (columnType: string): boolean =>
  REF_FILTER_TYPES.has(columnType);

export const RANGE_TYPES = new Set([
  "INT",
  "INT_ARRAY",
  "DECIMAL",
  "DECIMAL_ARRAY",
  "LONG",
  "LONG_ARRAY",
  "NON_NEGATIVE_INT",
  "NON_NEGATIVE_INT_ARRAY",
  "DATE",
  "DATE_ARRAY",
  "DATETIME",
  "DATETIME_ARRAY",
]);

const SELECTABLE_FILTER_TYPES = new Set([
  "ONTOLOGY",
  "ONTOLOGY_ARRAY",
  "BOOL",
  "BOOL_ARRAY",
  "RADIO",
  "CHECKBOX",
  "DATE",
  "DATE_ARRAY",
  "DATETIME",
  "DATETIME_ARRAY",
  "INT",
  "INT_ARRAY",
  "DECIMAL",
  "DECIMAL_ARRAY",
  "LONG",
  "NON_NEGATIVE_INT",
  "NON_NEGATIVE_INT_ARRAY",
]);

const STRING_FILTER_TYPES = new Set([
  "STRING",
  "STRING_ARRAY",
  "TEXT",
  "TEXT_ARRAY",
  "EMAIL",
  "EMAIL_ARRAY",
  "HYPERLINK",
  "HYPERLINK_ARRAY",
  "UUID",
  "UUID_ARRAY",
  "AUTO_ID",
]);

const REF_EXPANDABLE = new Set(REF_EXPANDABLE_TYPES);

const EXCLUDED_COLUMN_TYPES = new Set(["HEADING", "SECTION", "FILE"]);

const DEFAULT_FILTER_TYPES = new Set([
  "ONTOLOGY",
  "ONTOLOGY_ARRAY",
  "BOOL",
  "CHECKBOX",
  "RADIO",
]);

export const isCountableType = (columnType: string): boolean =>
  COUNTABLE_TYPES.has(columnType);

export const isRangeType = (columnType: string): boolean =>
  RANGE_TYPES.has(columnType);

export function isSelectableFilterType(ct: string): boolean {
  return SELECTABLE_FILTER_TYPES.has(ct);
}

export function isStringFilterType(ct: string): boolean {
  return STRING_FILTER_TYPES.has(ct);
}

export function isRefExpandable(ct: string): boolean {
  return REF_EXPANDABLE.has(ct);
}

export function navDepth(ct: string): 1 | 2 {
  if (ct === "REF" || ct === "SELECT") return 2;
  return 1;
}

export function shouldExcludeSelfRef(
  col: IColumn,
  parentTableId: string
): boolean {
  return col.refTableId === parentTableId;
}

export function isExcludedColumn(col: IColumn): boolean {
  return EXCLUDED_COLUMN_TYPES.has(col.columnType) || col.id.startsWith("mg_");
}

export function computeDefaultFilters(columns: IColumn[]): string[] {
  return columns
    .filter(
      (col) =>
        !isExcludedColumn(col) && DEFAULT_FILTER_TYPES.has(col.columnType)
    )
    .map((col) => col.id);
}

export function treeSelectionToFilterValue(
  selected: string[],
  column: IColumn,
  options: CountedOption[]
): IFilterValue | undefined {
  if (selected.length === 0) return undefined;

  if (
    isRefFilterType(column.columnType) &&
    options.length > 0 &&
    options[0]?.keyObject !== undefined
  ) {
    const firstKey = options[0].keyObject!;
    const isComposite = Object.keys(firstKey).length > 1;
    if (isComposite) {
      const optionsByName = new Map(
        options.map((option) => [option.name, option])
      );
      const values = selected.map((name) => {
        const option = optionsByName.get(name);
        return (option?.keyObject ?? { name }) as Record<string, unknown>;
      });
      return { operator: "equals", value: values as columnValue };
    }
  }

  return { operator: "equals", value: selected };
}

export function filterValueToTreeSelection(
  filterValue: IFilterValue | undefined
): string[] {
  if (!filterValue || filterValue.operator !== "equals") return [];
  const val = filterValue.value;
  if (!Array.isArray(val)) {
    if (typeof val === "string") return [val];
    return [];
  }
  return val
    .filter((value) => value !== null && value !== undefined)
    .map((value) => {
      if (typeof value === "object" && value !== null) {
        const values = Object.values(value as Record<string, unknown>);
        return values.length === 1
          ? String(values[0])
          : values.map(String).join(", ");
      }
      return String(value);
    });
}
