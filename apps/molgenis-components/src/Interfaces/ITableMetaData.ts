import { IColumn } from "./IColumn";
import { ILocale } from "./ILocales";
import { ISetting } from "./ISetting";

export interface ITableMetaData {
  id: string;
  label: string;
  description?: string;
  tableType: string;
  columns: IColumn[];
  descriptions?: ILocale[];
  schemaId: string;
  labels?: ILocale[];
  semantics?: string[];
  settings?: ISetting[];
}
