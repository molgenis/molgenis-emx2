import { ITableMetaData } from "./ITableMetaData";

export interface ISchemaMetaData {
  id: string;
  label: string;
  tables: ITableMetaData[];
}
