import type { IColumn } from "../../../metadata-utils/src/types";
import { REF_EXPANDABLE_TYPES } from "./filterConstants";

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
