import type { ITableMetaData } from "../../../metadata-utils/src/types";
import fetchMetadata from "./fetchMetadata";

export default async (
  schemaId: string,
  tableId: string
): Promise<ITableMetaData> => {
  const schemaMetadata = await fetchMetadata(schemaId);
  const tableMetadata = schemaMetadata.tables.find(
    (table) => table.id === tableId
  );
  if (!tableMetadata) {
    return Promise.reject(`Table ${tableId} not found in schema ${schemaId}`);
  }
  return tableMetadata;
};
