import { IColumn } from "./IColumn";
import { ILocale } from "./ILocales";
import { ISetting } from "./ISetting";

export interface ITableMetaData {
  id: string;
  label: string;
  description?: string;
  tableType: string;
  columns: IColumn[];
  externalSchema: string;
  semantics?: string[];
  settings?: ISetting[];
}
