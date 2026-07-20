import { createError } from "nuxt/app";
import type { ITableMetaData } from "../../../metadata-utils/src/types";
import { DATA_NOT_FOUND_ERROR } from "../utils/constants";
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
    const message = `Could not find table "${tableId}" in schema "${schemaId}". ${DATA_NOT_FOUND_ERROR}`;
    console.error(message);
    throw createError({
      message,
      status: 404,
    });
  }

  if (options?.includeSubclassColumns) {
    const subclassColumns = getSubclassColumns(
      tableId,
      schemaMetadata.tables
    ).map((col) => ({ ...col, visible: "false" }));
    return {
      ...tableMetadata,
      columns: [...tableMetadata.columns, ...subclassColumns],
    };
  }

  return tableMetadata;
};
