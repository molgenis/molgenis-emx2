import type {
  columnValue,
  IRow,
  ITableMetaData,
} from "../../../metadata-utils/src/types";
import { subclassTableId } from "../utils/recordPath";
import fetchMetadata from "./fetchMetadata";
import fetchRowData from "./fetchRowData";
import fetchTableMetadata from "./fetchTableMetadata";

export interface IRecordOfOwnClass {
  metadata: ITableMetaData;
  row: Record<string, columnValue>;
}

export default async (
  schemaId: string,
  declaredTableId: string,
  rowId: IRow,
  expandLevel: number = 2
): Promise<IRecordOfOwnClass> => {
  const declaredRow = await fetchRowData(
    schemaId,
    declaredTableId,
    rowId,
    expandLevel
  );
  const schema = await fetchMetadata(schemaId);
  const ownTableId = subclassTableId(schema, declaredRow) ?? declaredTableId;

  return {
    metadata: await fetchTableMetadata(schemaId, ownTableId),
    row:
      ownTableId === declaredTableId
        ? declaredRow
        : await fetchRowData(schemaId, ownTableId, rowId, expandLevel),
  };
};
