import type { ITableMetaData } from "../../../metadata-utils/src/types";
import fetchMetadata from "./fetchMetadata";
import { createError } from "nuxt/app";

export default async (
  schemaId: string,
  tableId: string
): Promise<ITableMetaData> => {
  const schemaMetadata = await fetchMetadata(schemaId).catch((error) => {
    throw error;
  });
  const tableMetadata = schemaMetadata.tables.find(
    (table) => table.id === tableId
  );

  if (!tableMetadata) {
    const message = `Could not find table: ${tableId} in schema: ${schemaId}. Might you need to sign in or ask permission?`;
    console.error(message);
    throw createError({
      message,
      status: 404,
    });
  } else {
    return tableMetadata;
  }
};
