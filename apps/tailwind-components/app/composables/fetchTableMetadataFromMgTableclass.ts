import type { ITableMetaData } from "../../../metadata-utils/src/types";
import fetchTableMetadata from "./fetchTableMetadata";

export default async (
  mgTableclass: string | undefined
): Promise<ITableMetaData | undefined> => {
  if (!mgTableclass) return undefined;

  const dotIndex = mgTableclass.indexOf(".");
  if (dotIndex < 0) return undefined;

  const schemaId = mgTableclass.slice(0, dotIndex);
  const tableId = mgTableclass.slice(dotIndex + 1);
  if (!schemaId || !tableId) return undefined;

  try {
    return await fetchTableMetadata(schemaId, tableId);
  } catch {
    return undefined;
  }
};
