import type { IColumn } from "../../../metadata-utils/src";
import type { IRow } from "../../../metadata-utils/src/types";

export function getRowFilter(
  columns: IColumn[],
  rowId: IRow
): Record<string, any> {
  return columns
    ?.filter((column: IColumn) => column.key === 1)
    .reduce((accum: any, column: IColumn) => {
      const value = rowId[column.id];
      if (
        value !== null &&
        typeof value === "object" &&
        !Array.isArray(value)
      ) {
        const nested: Record<string, any> = {};
        for (const [key, val] of Object.entries(value)) {
          nested[key] = { equals: val };
        }
        accum[column.id] = nested;
      } else {
        accum[column.id] = { equals: value };
      }
      return accum;
    }, {});
}
