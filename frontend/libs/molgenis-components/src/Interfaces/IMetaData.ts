import { ITableMetaData } from "./ITableMetaData";

export interface ISchemaMetaData {
  name: string;
  tables: ITableMetaData[];
}
