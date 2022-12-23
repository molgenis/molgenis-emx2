import { IColumn } from "./IColumn";
import { ISetting } from "./ISetting";

export interface ITableMetaData {
  columns?: IColumn[];
  name: string,
  tableType: string,
  id: string,
  description: string,
  externalSchema: string,
  semantics: string,
  settings: ISetting[]
}
