import type {
  IColumn,
  ITableMetaData,
} from "../../../metadata-utils/src/types";
import fetchMetadata from "./fetchMetadata";

export async function getSubclassColumns(
  schemaId: string,
  tableId: string
): Promise<IColumn[]> {
  const schemaMetadata = await fetchMetadata(schemaId);
  const parentTable = schemaMetadata.tables.find((t) => t.id === tableId);
  if (!parentTable) return [];

  return collectSubclassColumns(
    parentTable.name,
    schemaMetadata.tables,
    new Set(parentTable.columns.map((c) => c.id))
  );
}

function collectSubclassColumns(
  parentName: string,
  allTables: ITableMetaData[],
  parentColumnIds: Set<string>,
  result: IColumn[] = []
): IColumn[] {
  const subclasses = allTables.filter(
    (t) => (t as any).inheritName === parentName
  );

  for (const subclass of subclasses) {
    for (const col of subclass.columns) {
      if (
        !parentColumnIds.has(col.id) &&
        !result.some((r) => r.id === col.id)
      ) {
        result.push({ ...col, sourceTableId: subclass.id } as IColumn);
      }
    }

    collectSubclassColumns(subclass.name, allTables, parentColumnIds, result);
  }

  return result;
}
