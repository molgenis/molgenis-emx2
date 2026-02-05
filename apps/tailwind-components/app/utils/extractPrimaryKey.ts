import type {
  IColumn,
  IRow,
  ITableMetaData,
} from "../../../metadata-utils/src/types";

export function extractPrimaryKey(
  row: IRow,
  metadata: ITableMetaData
): Record<string, any> {
  const result: Record<string, any> = {};
  for (const col of metadata.columns) {
    if (col.key === 1 && row[col.id] !== undefined) {
      const value = row[col.id];
      if (
        typeof value === "object" &&
        value !== null &&
        !Array.isArray(value)
      ) {
        // Nested ref - recursively extract if we have ref metadata
        if (col.refTableId && (col as any).refTableMetadata) {
          result[col.id] = extractPrimaryKey(
            value as IRow,
            (col as any).refTableMetadata
          );
        } else {
          result[col.id] = value;
        }
      } else {
        result[col.id] = value;
      }
    }
  }
  return result;
}
