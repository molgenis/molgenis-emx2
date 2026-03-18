import type { ITableMetaData } from "../../../metadata-utils/src/types";
import fetchMetadata from "./fetchMetadata";
import { getSubclassColumns } from "./getSubclassColumns";

export default async (
  schemaId: string,
  tableId: string,
  options?: { includeSubclassColumns?: boolean }
): Promise<ITableMetaData> => {
  const schemaMetadata = await fetchMetadata(schemaId);
  const tableMetadata = schemaMetadata.tables.find(
    (table) => table.id === tableId
  );
  if (!tableMetadata) {
    return Promise.reject(`Table ${tableId} not found in schema ${schemaId}`);
  }
  if (options?.includeSubclassColumns) {
    const subclassColumns = (await getSubclassColumns(schemaId, tableId)).map(
      (col) => ({ ...col, visible: "false" })
    );
    return {
      ...tableMetadata,
      columns: [...tableMetadata.columns, ...subclassColumns],
    };
  }
  return tableMetadata;
};
