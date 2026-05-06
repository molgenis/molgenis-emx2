import type { IColumn } from "../../../metadata-utils/src/types";

export function isRefLikeDetail(cellDetailColumn: IColumn) {
  const type = cellDetailColumn.columnType;
  return (
    type === "REF" ||
    type === "RADIO" ||
    type === "CHECKBOX" ||
    type === "SELECT" ||
    type === "ONTOLOGY" ||
    type === "REFBACK" ||
    type === "MULTISELECT"
  );
}

export const isArrayLikeDetail = (cellDetailColumn: IColumn) => {
  const type = cellDetailColumn.columnType;
  return (
    type?.endsWith("_ARRAY") || type === "MULTISELECT" || type === "CHECKBOX"
  );
};
