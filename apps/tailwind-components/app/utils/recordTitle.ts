import type {
  columnValue,
  IRow,
  ITableMetaData,
} from "../../../metadata-utils/src/types";
import { flattenObject } from "./flattenObject";

/** The record's primary key values, joined; "" when the row carries none. */
export function recordTitle(
  metadata: ITableMetaData,
  rowData?: IRow | null
): string {
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
