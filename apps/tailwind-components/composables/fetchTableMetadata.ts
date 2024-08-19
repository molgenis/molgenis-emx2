import type { ITableMetaData } from "../../meta-data-utils/src/types";

export default async (schemaId: string, tableId: string): Promise<ITableMetaData> => {
    const schemaMetadata = await fetchMetadata(schemaId);
    const tableMetadata = schemaMetadata.tables.find((table) => table.id === tableId);
    return tableMetadata || Promise.reject(`Table ${tableId} not found in schema ${schemaId}`);
}