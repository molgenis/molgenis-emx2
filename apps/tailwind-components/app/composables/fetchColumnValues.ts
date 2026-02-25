import fetchTableData from "./fetchTableData";
import fetchTableMetadata from "./fetchTableMetadata";
import { getRowFilter } from "~/utils/getRowFilter";
import type {
  columnValue,
  IColumn,
  IRow,
} from "../../../metadata-utils/src/types";

export default async (
  schemaId: string,
  tableId: string,
  rowId: IRow,
  columns: IColumn[],
  expandLevel: number = 2
): Promise<Record<string, columnValue>> => {
  const metadata = await fetchTableMetadata(schemaId, tableId);
  const rowFilter = getRowFilter(metadata.columns, rowId);

  const resp = await fetchTableData(schemaId, tableId, {
    filter: rowFilter,
    columns,
    expandLevel,
    limit: 1,
    offset: 0,
  });

  if (!resp.rows.length) {
    throw new Error(`No data found for rowId ${rowId} in table ${tableId}`);
  }

  const row = resp.rows[0];
  return row ?? {};
};
