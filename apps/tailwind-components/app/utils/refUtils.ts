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

export const isArrayLikeDetail = (cellDetailColumn: IColumn) => {
  const type = cellDetailColumn.columnType;
  return (
    type?.endsWith("_ARRAY") ||
    type === "MULTISELECT" ||
    type === "CHECKBOX" ||
    type === "REFBACK"
  );
};
