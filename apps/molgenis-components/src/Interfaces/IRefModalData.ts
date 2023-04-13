import { IRow } from "./IRow";
import { ITableMetaData } from "./ITableMetaData";

export interface IRefModalData extends IRow {
  metadata: ITableMetaData;
  [primaryKey: string]: string | ITableMetaData;
}
