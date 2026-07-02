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
    throw createError({
      message: `Could not find table: ${tableId} in schema: ${schemaId}. Might you need to sign in or ask permission?`,
      status: 400,
    });
  } else {
    return tableMetadata;
  }
};
