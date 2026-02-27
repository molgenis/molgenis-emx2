import type { IColumn } from "../../../metadata-utils/src";
import type { IRow } from "../../../metadata-utils/src/types";

export function getRowFilter(
  columns: IColumn[],
  rowId: IRow
): Record<string, any> {
  return columns
    ?.filter((column: IColumn) => column.key === 1)
    .reduce((accum: any, column: IColumn) => {
      accum[column.id] = { equals: rowId[column.id] };
      return accum;
    }, {});
}
