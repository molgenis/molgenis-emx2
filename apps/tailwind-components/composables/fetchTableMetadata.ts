import type { ITableMetaData } from "../../metadata-utils/src/types";

export default async (
  schemaId: string,
  tableId: string
): Promise<ITableMetaData> => {
  const schemaMetadata = await fetchMetadata(schemaId);
  const tableMetadata = schemaMetadata.tables.find(
    (table) => table.id === tableId
  );
  return (
    tableMetadata ||
    Promise.reject(`Table ${tableId} not found in schema ${schemaId}`)
  );
};
