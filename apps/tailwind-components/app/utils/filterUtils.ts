import {
  isFileType,
  isLayoutColumnType,
  isOntologyType,
  isOptionKeyRefType,
  isRefType,
  isTableRefType,
} from "../../../metadata-utils/src";
import type { IColumn, columnValue } from "../../../metadata-utils/src/types";
import type { IFilterValue } from "../../types/filters";
import type { CountedOption } from "./fetchCounts";

export const BOOL_LABELS: Record<string, string> = {
  true: "Yes",
  false: "No",
  _null_: "Not set",
};

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

const COUNTABLE_VALUE_TYPES = new Set(["BOOL", "STRING_ARRAY"]);

const DEFAULT_FILTER_REF_TYPES = new Set(["RADIO", "SELECT"]);

export const isCountableType = (columnType: string): boolean =>
  isRefType(columnType) || COUNTABLE_VALUE_TYPES.has(columnType);

export const isRangeType = (columnType: string): boolean =>
  RANGE_TYPES.has(columnType);

export const isDrillableRefType = (columnType: string): boolean =>
  isTableRefType(columnType) && !isOptionKeyRefType(columnType);

export function navDepth(columnType: string): 1 | 2 {
  if (columnType === "REF" || columnType === "SELECT") return 2;
  return 1;
}

export function shouldExcludeSelfRef(
  col: IColumn,
  parentTableId: string
): boolean {
  return col.refTableId === parentTableId;
}

export function isExcludedColumn(col: IColumn): boolean {
  return (
    isLayoutColumnType(col.columnType) ||
    isFileType(col.columnType) ||
    col.id.startsWith("mg_")
  );
}

export function computeDefaultFilters(columns: IColumn[]): string[] {
  return columns
    .filter(
      (col) =>
        !isExcludedColumn(col) &&
        (isOntologyType(col.columnType) ||
          DEFAULT_FILTER_REF_TYPES.has(col.columnType))
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
    isTableRefType(column.columnType) &&
    options.length > 0 &&
    options[0]?.keyObject !== undefined
  ) {
    const firstKey = options[0].keyObject!;
    const keyNames = Object.keys(firstKey);
    const isComposite = keyNames.length > 1;
    const isSingleNonNameKey = keyNames.length === 1 && keyNames[0] !== "name";
    if (isComposite || isSingleNonNameKey) {
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
