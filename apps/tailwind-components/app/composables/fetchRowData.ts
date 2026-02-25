import { getRowFilter } from "~/utils/getRowFilter";
import type { columnValue, IRow } from "../../../metadata-utils/src/types";
import fetchTableData from "./fetchTableData";
import fetchTableMetadata from "./fetchTableMetadata";

export default async (
  schemaId: string,
  tableId: string,
  rowId: IRow,
  expandLevel: number = 2
): Promise<Record<string, columnValue>> => {
  const metadata = await fetchTableMetadata(schemaId, tableId);
  const filter = getRowFilter(metadata.columns, rowId);

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
  return row ?? {};
};
