import fetchMetadata from "./fetchMetadata";
import type {
  IColumn,
  IRow,
  ITableMetaData,
} from "../../metadata-utils/src/types";

export default async function fetchRowPrimaryKey(
  row: IRow,
  tableId: string,
  schemaId: string
): Promise<Record<string, string>> {
  const schema = await fetchMetadata(schemaId);
  const tableMetadata = schema.tables.find(
    (table: ITableMetaData) => table.id === tableId
  );
  if (!tableMetadata?.columns) {
    throw new Error("Empty columns in metadata");
  } else {
    return await tableMetadata.columns.reduce(
      async (accumPromise: Promise<IRow>, column: IColumn): Promise<IRow> => {
        let accum: IRow = await accumPromise;
        const cellValue = row[column.id];
        if (column.key === 1 && cellValue) {
          accum[column.id] = await getKeyValue(
            cellValue,
            column,
            column.refSchemaId || schema.id
          );
        }
        return accum;
      },
      Promise.resolve({})
    );
  }

  async function getKeyValue(
    cellValue: any,
    column: IColumn,
    schemaId: string
  ) {
    if (typeof cellValue === "string") {
      return cellValue;
    } else {
      if (column.refTableId) {
        return await fetchRowPrimaryKey(cellValue, column.refTableId, schemaId);
      }
    }
  }
}
