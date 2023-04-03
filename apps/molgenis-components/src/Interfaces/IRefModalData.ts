import { ITableMetaData } from "./ITableMetaData";

export interface IRefModalData {
  metadata: ITableMetaData;
  [primaryKey: string]: string | ITableMetaData;
}
