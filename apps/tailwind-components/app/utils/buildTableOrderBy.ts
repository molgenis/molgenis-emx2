import type { ITableMetaData } from "../../../metadata-utils/src/types";
import type { ITableSettings } from "../../types/types";

export type TableOrderBy = Record<string, string>[];

/**
 * Builds the graphql orderby list for a table.
 *
 * The primary key columns are always added as the last sort criteria. This gives the table a
 * stable order: without it the database returns rows in storage order, which moves a row to the
 * end of the table each time it is updated, and pages can overlap when the sorted column has
 * duplicate values.
 */
export function buildTableOrderBy(
  tableMetadata: ITableMetaData | null | undefined,
  orderby: ITableSettings["orderby"]
): TableOrderBy {
  const result: TableOrderBy = [];
  if (orderby.column) {
    result.push({ [orderby.column]: orderby.direction });
  }
  tableMetadata?.columns
    .filter((column) => column.key === 1 && column.id !== orderby.column)
    .forEach((column) => result.push({ [column.id]: "ASC" }));
  return result;
}
