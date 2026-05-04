import type { columnValue, IColumn } from "../../../metadata-utils/src/types";

export function isFieldDisabled(
  rowKey: Record<string, columnValue>,
  column: IColumn
) {
  const hasPkey = rowKey && rowKey[column.id] && column.key === 1;
  return (
    column.readonly === "true" || hasPkey || column.columnType === "AUTO_ID"
  );
}
