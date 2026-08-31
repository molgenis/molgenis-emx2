import type { IRow, ITableMetaData } from "../../../metadata-utils/src/types";

export interface SubclassRecord {
  tableMetadata: ITableMetaData;
  row: IRow;
}

/**
 * A row loaded against its parent table only carries the parent's columns.
 * `mg_tableclass` names the row's own table (`<schemaId>.<tableId>`); when
 * that differs from the route table, fetch that table's metadata and row so
 * the caller can render the row's own columns instead of the parent's.
 * Any failure, or a value that does not resolve to another table, leaves the
 * route table's metadata and row untouched, so this never throws.
 */
export default async function resolveSubclassRecord(
  schemaId: string,
  routeTableId: string,
  row: IRow | null | undefined,
  rowKeys: IRow,
  fetchSubclassMetadata: (
    schemaId: string,
    tableId: string
  ) => Promise<ITableMetaData>,
  fetchSubclassRow: (
    schemaId: string,
    tableId: string,
    rowId: IRow
  ) => Promise<IRow>
): Promise<SubclassRecord | null> {
  const mgTableclass = row?.mg_tableclass;
  if (typeof mgTableclass !== "string") {
    return null;
  }

  const dotIndex = mgTableclass.indexOf(".");
  if (dotIndex < 0) {
    return null;
  }
  const subclassSchemaId = mgTableclass.slice(0, dotIndex);
  const subclassTableId = mgTableclass.slice(dotIndex + 1);
  if (
    subclassSchemaId !== schemaId ||
    !subclassTableId ||
    subclassTableId === routeTableId
  ) {
    return null;
  }

  try {
    const tableMetadata = await fetchSubclassMetadata(
      schemaId,
      subclassTableId
    );
    const subclassRow = await fetchSubclassRow(
      schemaId,
      subclassTableId,
      rowKeys
    );
    return { tableMetadata, row: subclassRow };
  } catch (error) {
    console.error(
      `Could not load "${mgTableclass}" for this row, showing "${routeTableId}" instead.`,
      error
    );
    return null;
  }
}
