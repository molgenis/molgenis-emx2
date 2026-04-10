import type { IColumn } from "../../../metadata-utils/src/types";

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
