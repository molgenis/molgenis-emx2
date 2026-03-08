import type {
  IColumn,
  ITableMetaData,
} from "../../../metadata-utils/src/types";

export function getSubclassColumns(
  tableId: string,
  allTables: ITableMetaData[]
): IColumn[] {
  const parentTable = allTables.find((t) => t.id === tableId);
  if (!parentTable) return [];

  const parentColumnIds = new Set(parentTable.columns.map((c) => c.id));
  const result: IColumn[] = [];

  const subclasses = allTables.filter((t) => (t as any).inheritId === tableId);

  for (const subclass of subclasses) {
    for (const col of subclass.columns) {
      if (
        !parentColumnIds.has(col.id) &&
        !result.some((r) => r.id === col.id)
      ) {
        result.push(
          Object.assign({ ...col }, { sourceTableId: subclass.id }) as IColumn
        );
      }
    }

    const deepCols = getSubclassColumns(subclass.id, allTables);
    for (const col of deepCols) {
      if (
        !parentColumnIds.has(col.id) &&
        !result.some((r) => r.id === col.id)
      ) {
        result.push(col);
      }
    }
  }

  return result;
}
