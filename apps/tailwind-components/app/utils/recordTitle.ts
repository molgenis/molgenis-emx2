import type {
  columnValue,
  IColumn,
  IRow,
} from "../../../metadata-utils/src/types";
import { columnValueToString } from "./columnValueToString";
import { flattenObject } from "./flattenObject";

/** The record's name: the given template rendered, else its primary key values joined. */
export function recordTitle(
  columns: IColumn[],
  rowData: IRow | null,
  template?: string
): string {
  if (template) {
    // A template that fails to interpolate must still return "", never undefined, so a caller's `|| fallback` runs.
    return columnValueToString(rowData, template) || "";
  }
  return columns
    .filter((column) => column.key === 1)
    .map((column) => keyValueText(rowData?.[column.id]))
    .filter(Boolean)
    .join(" - ");
}

function keyValueText(value: columnValue): string {
  if (value === null || value === undefined) {
    return "";
  }
  return typeof value === "object"
    ? flattenObject(value).trim()
    : String(value);
}
