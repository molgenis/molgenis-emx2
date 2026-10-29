import { fetchMetadata } from "#imports";
import type {
  columnValue,
  IColumn,
  IRow,
  ITableMetaData,
  nonEmptyColumnValue,
} from "../../../metadata-utils/src/types";

export async function getPrimaryKey(
  row: IRow,
  tableId: string,
  schemaId: string
): Promise<Record<string, nonEmptyColumnValue>> {
  const schema = await fetchMetadata(schemaId);
  if (!schema || !schema.tables) {
    throw new Error(
      `Unable to fetch primary row key, Schema with id ${schemaId} not found`
    );
  }
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
        if (column.key === 1 && hasValue(cellValue)) {
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

  function hasValue(cellValue: columnValue): cellValue is nonEmptyColumnValue {
    return cellValue !== undefined && cellValue !== null && cellValue !== "";
  }

  async function getKeyValue(
    cellValue: any,
    column: IColumn,
    schemaId: string
  ) {
    if (typeof cellValue === "object" && column.refTableId) {
      return await getPrimaryKey(cellValue, column.refTableId, schemaId);
    }
    return cellValue;
  }
}
