import { fetchTableData, fetchTableMetadata } from "#imports";
import type {
  columnValue,
  IColumn,
  IRow,
} from "../../metadata-utils/src/types";

export default async (
  schemaId: string,
  tableId: string,
  rowId: IRow,
  expandLevel: number = 2
): Promise<Record<string, columnValue>> => {
  const metadata = await fetchTableMetadata(schemaId, tableId);
  const filter = metadata.columns
    ?.filter((column: IColumn) => column.key === 1)
    .reduce((accum: any, column: IColumn) => {
      accum[column.id] = { equals: rowId[column.id] };
      return accum;
    }, {});

  const resp = await fetchTableData(schemaId, tableId, {
    filter,
    expandLevel,
    limit: 1,
    offset: 0,
  });

  if (resp.rows.length === 0) {
    throw new Error(`No data found for rowId ${rowId} in table ${tableId}`);
  }

  const row = resp.rows[0];
  return row;
};
