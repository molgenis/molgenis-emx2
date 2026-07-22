import type { ITableMetaData } from "../../../metadata-utils/src/types";
import { ApiError } from "../utils/apiError";
import { DATA_NOT_FOUND_ERROR } from "../utils/constants";
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
    const message = `Could not find table "${tableId}" in schema "${schemaId}". ${DATA_NOT_FOUND_ERROR}`;
    console.error(message);
    throw new ApiError(message, 404);
  } else {
    return tableMetadata;
  }
};
