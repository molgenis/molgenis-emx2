import { IColumn } from "./IColumn";
import { ISetting } from "./ISetting";
import { ILocale } from "./ILocales";

export interface ITableMetaData {
  id: string;
  name: string;
  tableType: string;
  columns?: IColumn[];
  descriptions?: ILocale[];
  externalSchema: string;
  labels?: ILocale[];
  semantics?: string[];
  settings?: ISetting[];
}
