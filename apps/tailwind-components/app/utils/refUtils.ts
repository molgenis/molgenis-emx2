import { isMultiValuedType } from "../../../metadata-utils/src";
import type { IColumn } from "../../../metadata-utils/src/types";

export function isRefLikeDetail(cellDetailColumn: IColumn) {
  const type = cellDetailColumn.columnType;
  return (
    type === "REF" ||
    type === "REF_ARRAY" ||
    type === "RADIO" ||
    type === "CHECKBOX" ||
    type === "SELECT" ||
    type === "ONTOLOGY" ||
    type === "ONTOLOGY_ARRAY" ||
    type === "MULTISELECT" ||
    type === "REFBACK"
  );
}

// A payload can reach the modal with no columnType at all, so guard here rather
// than loosening the shared predicate's type.
export const isArrayLikeDetail = (cellDetailColumn: IColumn) =>
  !!cellDetailColumn.columnType &&
  isMultiValuedType(cellDetailColumn.columnType);
