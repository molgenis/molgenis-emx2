import { ITableMetaData } from "./ITableMetaData";

export interface IRefModalData {
  metadata: ITableMetaData;
  [property: string]: string | ITableMetaData;
}
