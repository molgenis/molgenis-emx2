import type { IColumn } from "../../../metadata-utils/src/types";
import { isExcludedColumn } from "./filterTreeUtils";

const DEFAULT_FILTER_TYPES = new Set([
  "ONTOLOGY",
  "ONTOLOGY_ARRAY",
  "BOOL",
  "CHECKBOX",
  "RADIO",
]);

export function computeDefaultFilters(columns: IColumn[]): string[] {
  return columns
    .filter(
      (col) =>
        !isExcludedColumn(col) && DEFAULT_FILTER_TYPES.has(col.columnType)
    )
    .map((col) => col.id);
}
