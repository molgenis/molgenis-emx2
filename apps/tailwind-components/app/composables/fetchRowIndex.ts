import type { ITableMetaData } from "../../../metadata-utils/src/types";
import type { TableOrderBy } from "../utils/buildTableOrderBy";
import { getPrimaryKey } from "../utils/getPrimaryKey";
import { getColumnIds } from "./fetchTableData";

/**
 * Returns the position (0-based) of the row with the given primary key in the table, using the
 * same filter, search and order as the table view. Returns -1 when the row is not in the result,
 * for example because a filter or search hides it.
 *
 * Only the primary key columns are fetched, but for all matching rows.
 */
export async function fetchRowIndex(
  schemaId: string,
  tableMetadata: ITableMetaData,
  rowIdString: string,
  options: {
    orderby: TableOrderBy;
    filter?: object;
    search?: string;
  }
): Promise<number> {
  const tableId = tableMetadata.id;
  const keyColumns = tableMetadata.columns.filter((column) => column.key === 1);
  if (!keyColumns.length) {
    return -1;
  }
  const columnIds = await getColumnIds(schemaId, tableId, 0, keyColumns);
  const search = options.search?.trim();

  const query = `query ${tableId}($filter:${tableId}Filter, $orderby:[${tableId}orderby]${
    search ? ", $search:String" : ""
  }) {
    ${tableId}(filter:$filter, orderby:$orderby${
    search ? ", search:$search" : ""
  }) {
      ${columnIds}
    }
  }`;

  const response: any = await $fetch(`/${schemaId}/graphql`, {
    method: "POST",
    body: {
      query,
      variables: {
        filter: options.filter ?? {},
        orderby: options.orderby,
        ...(search ? { search } : {}),
      },
    },
  });

  const rows = response?.data?.[tableId] ?? [];
  for (let index = 0; index < rows.length; index++) {
    const key = await getPrimaryKey(rows[index], tableId, schemaId);
    if (JSON.stringify(key) === rowIdString) {
      return index;
    }
  }
  return -1;
}
