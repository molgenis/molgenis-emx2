import { IColumn } from "./IColumn";
import { ISetting } from "./ISetting";

export interface ITableMetaData {
  id: string;
  label: string;
  description?: string;
  tableType: string;
  columns: IColumn[];
  schemaId: string;
  semantics?: string[];
  settings?: ISetting[];
}
