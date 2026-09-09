import type {
  columnValue,
  IRow,
  ITableMetaData,
} from "../../../metadata-utils/src/types";
import { columnValueToString } from "./columnValueToString";
import { flattenObject } from "./flattenObject";

/** The record's name: its table's label template, else its primary key values joined. */
export function recordTitle(
  metadata: ITableMetaData,
  rowData?: IRow | null
): string {
  if (metadata.labelTemplate) {
    // A template that fails to interpolate must still return "", never undefined, so a caller's `|| fallback` runs.
    return columnValueToString(rowData, metadata.labelTemplate) || "";
  }
  return metadata.columns
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
