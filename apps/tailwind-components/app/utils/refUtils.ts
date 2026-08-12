import { isMultiValuedType, isRefType } from "../../../metadata-utils/src";
import type { IColumn } from "../../../metadata-utils/src/types";

export function isRefLikeDetail(cellDetailColumn: IColumn) {
  return isRefType(cellDetailColumn.columnType);
}

export const isArrayLikeDetail = (cellDetailColumn: IColumn) => {
  const type = cellDetailColumn.columnType;
  return Boolean(type) && isMultiValuedType(type);
};
